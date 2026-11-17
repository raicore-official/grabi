package com.raisoft.grabi.data.download

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.core.content.ContextCompat
import com.raisoft.grabi.data.db.DownloadDao
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.data.settings.SettingsRepository
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
    private val notifier: DownloadNotifier,
    settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pumpMutex = Mutex()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pauseRequested = ConcurrentHashMap.newKeySet<String>()
    private val lastPercent = ConcurrentHashMap<String, Int>()

    @Volatile private var parallelism = 2
    @Volatile private var wifiOnly = false
    @Volatile private var wifiAvailable = true

    val downloads: StateFlow<List<DownloadEntity>> =
        dao.observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            settingsRepository.settings.collect { settings ->
                parallelism = settings.downloadParallelism
                wifiOnly = settings.wifiOnly
                pump()
            }
        }
        registerNetworkWatch()
    }

    suspend fun enqueue(url: String, title: String, option: MediaOption, subtitles: Boolean): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(
            DownloadEntity(
                id = id,
                url = url,
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
            ),
        )
        pump()
        return id
    }

    fun pause(id: String) {
        if (activeJobs.containsKey(id)) {
            pauseRequested.add(id)
            engine.cancel(id)
            directDownloader.cancel(id)
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
                        scope.launch { dao.setProgress(entity.id, pct / 100f, speed, eta) }
                        notifier.progress(entity, pct / 100f, speed, eta)
                    }
                }
                val file = if (option.kind == MediaKind.IMAGE) {
                    val outputDir = File(context.getExternalFilesDir(null), "Grabi").apply { mkdirs() }
                    directDownloader.download(
                        entity.url,
                        File(outputDir, imageFileName(entity.title, entity.url)),
                        entity.id,
                        onProgress,
                    )
                } else {
                    engine.download(entity.url, option, entity.subtitles, entity.id, onProgress)
                }
                val uri = MediaStorePublisher.publish(context, file, option.kind)
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

        private fun imageFileName(title: String, url: String): String {
            val base = title.take(60).replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "image" }
            val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase()
            val safeExt = if (ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")) ext else "jpg"
            return "$base.$safeExt"
        }
    }
}