package com.raisoft.grabi.data.download

import android.content.Context
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.core.content.ContextCompat
import com.raisoft.grabi.data.db.DownloadDao
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.data.settings.SettingsRepository
import com.raisoft.grabi.data.playback.DebugLog
import com.raisoft.grabi.data.stream.StreamRepository
import com.raisoft.grabi.domain.VideoIds
import com.raisoft.grabi.domain.model.DownloadStatus
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.domain.model.MediaOption
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadDao,
    private val engine: YtDlpEngine,
    private val directDownloader: DirectDownloader,
    private val torrentEngine: TorrentEngine,
    private val notifier: DownloadNotifier,
    private val streamRepository: StreamRepository,
    settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pumpMutex = Mutex()
    private val enqueueMutex = Mutex()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pauseRequested = ConcurrentHashMap.newKeySet<String>()
    private val lastPercent = ConcurrentHashMap<String, Int>()

    @Volatile private var parallelism = 2
    @Volatile private var wifiOnly = false
    @Volatile private var wifiAvailable = true
    @Volatile private var destination = ""

    val downloads: StateFlow<List<DownloadEntity>> =
        dao.observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            settingsRepository.settings.collect { settings ->
                parallelism = settings.downloadParallelism
                wifiOnly = settings.wifiOnly
                destination = settings.downloadDestination
                pump()
            }
        }
        registerNetworkWatch()
    }

    suspend fun enqueue(
        url: String,
        title: String,
        option: MediaOption,
        subtitles: Boolean,
        videoId: String = VideoIds.of(url),
        thumbnailUrl: String? = null,
    ): String {
        return enqueueMutex.withLock {
            val existing = dao.latestByVideoId(videoId, option.kind.name)
            if (existing != null) {
                val reusable = existing.status in setOf(
                    DownloadStatus.QUEUED.name,
                    DownloadStatus.RUNNING.name,
                    DownloadStatus.PAUSED.name,
                    DownloadStatus.WAITING_WIFI.name,
                ) || (existing.status == DownloadStatus.DONE.name && fileExists(existing))
                if (reusable) return@withLock existing.id
            }
            val id = UUID.randomUUID().toString()
            dao.upsert(
                DownloadEntity(
                    id = id,
                    url = url,
                    videoId = videoId,
                    title = title,
                    optionLabel = option.label,
                    kind = option.kind.name,
                    formatSelector = option.formatSelector,
                    audioFormat = option.audioFormat,
                    subtitles = subtitles,
                    status = DownloadStatus.QUEUED.name,
                    progress = 0f,
                    speed = null,
                    etaSeconds = -1,
                    filePath = null,
                    contentUri = null,
                    error = null,
                    createdAt = System.currentTimeMillis(),
                    thumbnailUrl = thumbnailUrl,
                ),
            )
            pump()
            id
        }
    }

    fun pause(id: String) {
        if (activeJobs.containsKey(id)) {
            pauseRequested.add(id)
            engine.cancel(id)
            directDownloader.cancel(id)
            torrentEngine.cancel(id)
        } else {
            scope.launch {
                dao.setStatus(id, DownloadStatus.PAUSED.name)
                notifier.cancel(id)
            }
        }
    }

    fun resume(id: String) {
        pauseRequested.remove(id)
        scope.launch {
            dao.setStatus(id, DownloadStatus.QUEUED.name)
            pump()
        }
    }

    fun pauseAll() {
        downloads.value
            .filter { it.status == DownloadStatus.RUNNING.name || it.status == DownloadStatus.QUEUED.name }
            .forEach { pause(it.id) }
    }

    fun resumeAll() {
        downloads.value
            .filter { it.status == DownloadStatus.PAUSED.name }
            .forEach { resume(it.id) }
    }

    fun retry(id: String) {
        pauseRequested.remove(id)
        scope.launch {
            dao.setStatus(id, DownloadStatus.QUEUED.name)
            pump()
        }
    }

    fun cancel(id: String) {
        if (activeJobs.containsKey(id)) {
            engine.cancel(id)
            directDownloader.cancel(id)
            torrentEngine.cancel(id)
        } else {
            scope.launch {
                dao.setStatus(id, DownloadStatus.CANCELED.name)
                notifier.cancel(id)
            }
        }
    }

    fun delete(id: String) {
        scope.launch {
            val entity = dao.byId(id) ?: return@launch
            if (activeJobs.containsKey(id)) {
                engine.cancel(id)
                directDownloader.cancel(id)
                torrentEngine.cancel(id)
            }
            entity.contentUri?.let {
                runCatching { context.contentResolver.delete(Uri.parse(it), null, null) }
            }
            entity.filePath?.let { runCatching { File(it).delete() } }
            dao.delete(id)
            notifier.cancel(id)
        }
    }

    fun recoverAndPump() {
        scope.launch {
            dao.recoverStale()
            pump()
        }
    }

    fun statesByKey(): kotlinx.coroutines.flow.Flow<Map<String, DownloadEntity>> =
        dao.observeAll().map { list ->
            list.filter { it.videoId.isNotBlank() }
                .groupBy { it.videoId + ":" + it.kind }
                .mapValues { (_, rows) -> representative(rows) }
                .filterValues { it != null }
                .mapValues { it.value!! }
        }.flowOn(Dispatchers.IO)

    fun statesForKey(videoId: String, kind: String): kotlinx.coroutines.flow.Flow<DownloadEntity?> =
        statesByKey().map { it["$videoId:$kind"] }

    suspend fun localFileFor(videoId: String, kind: String): String? =
        dao.doneByVideoId(videoId, kind).firstOrNull { fileExists(it) }
            ?.let { it.contentUri ?: it.filePath }

    fun fileExists(entity: DownloadEntity): Boolean {
        entity.contentUri?.let { uri ->
            return runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { true } ?: false
            }.getOrDefault(false)
        }
        return entity.filePath?.let { File(it).isFile } ?: false
    }

    private fun representative(rows: List<DownloadEntity>): DownloadEntity? =
        rows.firstOrNull {
            it.status in setOf(
                DownloadStatus.RUNNING.name,
                DownloadStatus.QUEUED.name,
                DownloadStatus.PAUSED.name,
                DownloadStatus.WAITING_WIFI.name,
            )
        } ?: rows.firstOrNull { it.status == DownloadStatus.DONE.name && fileExists(it) }

    fun pump() {
        scope.launch { pumpMutex.withLock { pumpLocked() } }
    }

    private suspend fun pumpLocked() {
        if (wifiOnly && !wifiAvailable) {
            dao.pending().forEach { dao.setStatus(it.id, DownloadStatus.WAITING_WIFI.name) }
        } else {
            dao.waitingWifi().forEach { dao.setStatus(it.id, DownloadStatus.QUEUED.name) }
            val slots = (parallelism - activeJobs.size).coerceAtLeast(0)
            dao.pending().take(slots).forEach { launchJob(it) }
        }
        syncService()
    }

    private fun launchJob(entity: DownloadEntity) {
        activeJobs[entity.id] = scope.launch {
            dao.setStatus(entity.id, DownloadStatus.RUNNING.name)
            notifier.progress(entity, 0f, null, -1)
            try {
                val option = MediaOption(
                    id = entity.id,
                    label = entity.optionLabel,
                    kind = MediaKind.valueOf(entity.kind),
                    formatSelector = entity.formatSelector,
                    audioFormat = entity.audioFormat,
                    sizeBytes = null,
                )
                val onProgress: (Float, Long, String) -> Unit = { percent, eta, line ->
                    val pct = percent.coerceIn(0f, 100f).toInt()
                    if (lastPercent[entity.id] != pct) {
                        lastPercent[entity.id] = pct
                        val speed = SPEED_REGEX.find(line)?.groupValues?.get(1)
                            ?: ARIA_SPEED_REGEX.find(line)?.groupValues?.get(1)?.let { "$it/s" }
                        scope.launch { dao.setProgress(entity.id, pct / 100f, speed, eta) }
                        notifier.progress(entity, pct / 100f, speed, eta)
                    }
                }
                val startedAt = System.currentTimeMillis()
                val cached = streamRepository.cached(entity.url)
                val cachedOption = when (option.kind) {
                    MediaKind.VIDEO -> cached?.videoOptions?.firstOrNull {
                        it.label == entity.optionLabel && it.audioUrl == null
                    } ?: cached?.videoOptions?.firstOrNull {
                        it.audioUrl == null && it.height in 1..720
                    }
                    MediaKind.IMAGE -> null
                    MediaKind.AUDIO -> cached?.audioOptions?.firstOrNull()
                    MediaKind.FILE -> null
                }
                val file = if (isTorrentUrl(entity.url)) {
                    val torrentDir = File(context.getExternalFilesDir(null), "Grabi/torrents/${entity.id}")
                    torrentEngine.download(entity.url, torrentDir, entity.id, onProgress)
                } else if (option.kind == MediaKind.IMAGE || option.kind == MediaKind.FILE) {
                    val outputDir = File(context.getExternalFilesDir(null), "Grabi").apply { mkdirs() }
                    directDownloader.downloadRanges(
                        entity.url,
                        File(outputDir, fileName(entity.title, entity.url, option.kind)),
                        entity.id,
                        onProgress,
                        parallelism,
                    )
                } else if (cachedOption != null) {
                    DebugLog.line("download ${entity.id} starting from cached stream (no re-extraction)")
                    val outputDir = File(context.getExternalFilesDir(null), "Grabi").apply { mkdirs() }
                    val ext = if (option.kind == MediaKind.VIDEO) "mp4" else "m4a"
                    directDownloader.downloadRanges(
                        cachedOption.videoUrl ?: cachedOption.audioUrl!!,
                        File(outputDir, imageFileName(entity.title, entity.url).substringBeforeLast('.') + ".$ext"),
                        entity.id,
                        onProgress,
                        parallelism,
                    )
                } else {
                    engine.download(entity.url, option, entity.subtitles, entity.id, onProgress)
                }
                if (option.kind == MediaKind.VIDEO || option.kind == MediaKind.AUDIO) {
                    verifyTracks(file, option.kind)
                }
                val seconds = (System.currentTimeMillis() - startedAt) / 1000.0
                if (seconds > 0) {
                    DebugLog.line(
                        "download ${entity.id} done avg=${(file.length() / seconds / 1024).toInt()} KB/s",
                    )
                }
                val uri = publishToDestination(file, option.kind)
                dao.finish(
                    entity.id,
                    DownloadStatus.DONE.name,
                    if (uri != null) null else file.absolutePath,
                    uri?.toString(),
                )
                notifier.done(entity, uri?.toString() ?: file.absolutePath)
            } catch (e: YoutubeDL.CanceledException) {
                val paused = pauseRequested.remove(entity.id)
                val status = if (paused) DownloadStatus.PAUSED else DownloadStatus.CANCELED
                dao.setStatus(entity.id, status.name)
                notifier.stopped(entity, status)
            } catch (e: CancellationException) {
                val paused = pauseRequested.remove(entity.id)
                val status = if (paused) DownloadStatus.PAUSED else DownloadStatus.CANCELED
                dao.setStatus(entity.id, status.name)
                notifier.stopped(entity, status)
            } catch (e: Exception) {
                dao.setStatus(entity.id, DownloadStatus.FAILED.name, e.message?.take(400))
                notifier.failed(entity, e.message)
            } finally {
                activeJobs.remove(entity.id)
                lastPercent.remove(entity.id)
                pump()
            }
        }
    }

    private fun verifyTracks(file: File, kind: MediaKind) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            var hasVideo = false
            var hasAudio = false
            var videoDecodable = true
            val codecs = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/")) {
                    hasVideo = true
                    if (runCatching { codecs.findDecoderForFormat(format) }.getOrNull() == null) {
                        videoDecodable = false
                    }
                }
                if (mime.startsWith("audio/")) hasAudio = true
            }
            val retriever = MediaMetadataRetriever()
            val duration = runCatching {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }.getOrDefault(0L)
            runCatching { retriever.release() }
            val missing = when {
                kind == MediaKind.VIDEO && !hasVideo -> "no video track (merge failed)"
                kind == MediaKind.VIDEO && !videoDecodable -> "video codec not decodable on this device"
                kind == MediaKind.AUDIO && !hasAudio -> "no audio track"
                duration <= 0 -> "zero duration"
                else -> null
            }
            if (missing != null) {
                file.delete()
                throw java.io.IOException("Downloaded file is unplayable: $missing. Retry picks an H.264/AAC format.")
            }
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun publishToDestination(file: File, kind: MediaKind): Uri? {
        if (destination.isNotBlank()) {
            val safUri = runCatching {
                SafPublisher.publish(context, file, kind, Uri.parse(destination))
            }.getOrNull()
            if (safUri != null) return safUri
        }
        return MediaStorePublisher.publish(context, file, kind)
    }

    private fun syncService() {
        val intent = Intent(context, DownloadService::class.java)
        if (activeJobs.isNotEmpty()) {
            // Background FGS starts are blocked on Android 12+; the queue resumes on next foreground pump.
            runCatching { ContextCompat.startForegroundService(context, intent) }
        } else {
            context.stopService(intent)
        }
    }

    private fun registerNetworkWatch() {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivity.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    refreshWifi(connectivity)
                    pump()
                }

                override fun onLost(network: Network) {
                    refreshWifi(connectivity)
                    pump()
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    refreshWifi(connectivity)
                    pump()
                }
            },
        )
    }

    private fun refreshWifi(connectivity: ConnectivityManager) {
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        wifiAvailable = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    companion object {
        private val SPEED_REGEX = Regex("""at\s+(\S+/s)""")
        private val ARIA_SPEED_REGEX = Regex("""DL:(\S+)""")

        fun isTorrentUrl(url: String): Boolean =
            url.startsWith("magnet:", ignoreCase = true) ||
                url.substringBefore('?').endsWith(".torrent", ignoreCase = true)

        private fun imageFileName(title: String, url: String): String {
            val base = title.take(60).replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "image" }
            val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase()
            val safeExt = if (ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")) ext else "jpg"
            return "$base.$safeExt"
        }

        private fun fileName(title: String, url: String, kind: MediaKind): String {
            if (kind == MediaKind.IMAGE) return imageFileName(title, url)
            val safeTitle = title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "download" }
            val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase()
                .takeIf { it.matches(Regex("[a-z0-9]{1,8}")) } ?: "bin"
            return "$safeTitle.$ext"
        }
    }
}