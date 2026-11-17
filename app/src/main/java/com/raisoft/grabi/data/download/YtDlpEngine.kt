package com.raisoft.grabi.data.download

import android.content.Context
import com.raisoft.grabi.domain.MediaFormatMapper
import com.raisoft.grabi.domain.model.FormatSpec
import com.raisoft.grabi.domain.model.MediaInfo
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.domain.model.MediaOption
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class YtDlpEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val initMutex = Mutex()
    private var initialized = false

    suspend fun warmUp() {
        ensureInitialized()
        withContext(Dispatchers.IO) {
            runCatching {
                YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            }
        }
    }

    fun binaryVersion(): String? =
        runCatching { YoutubeDL.getInstance().versionName(context) }.getOrNull()

    suspend fun updateEngine(): String = withContext(Dispatchers.IO) {
        ensureInitialized()
        YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
        binaryVersion() ?: "unknown"
    }

    fun cancel(processId: String) {
        YoutubeDL.getInstance().destroyProcessById(processId)
    }

    suspend fun fetchInfo(url: String): MediaInfo {
        ensureInitialized()
        val request = YoutubeDLRequest(url).addOption("--no-playlist")
            .addOption("--extractor-args", "youtube:player_client=android,ios")
        val info = withContext(Dispatchers.IO) { YoutubeDL.getInstance().getInfo(request) }
        val formats = info.formats.orEmpty().map {
            FormatSpec(
                formatId = it.formatId.orEmpty(),
                ext = it.ext,
                height = it.height,
                vcodec = it.vcodec,
                acodec = it.acodec,
                sizeBytes = if (it.fileSize > 0) it.fileSize else it.fileSizeApproximate,
            )
        }
        return MediaInfo(
            title = info.title ?: info.fulltitle ?: url,
            uploader = info.uploader,
            durationSeconds = info.duration,
            thumbnailUrl = info.thumbnail,
            webpageUrl = info.webpageUrl ?: url,
            options = MediaFormatMapper.options(formats),
        )
    }

    suspend fun streamUrls(url: String, selector: String): List<String> {
        ensureInitialized()
        val request = YoutubeDLRequest(url)
            .addOption("-f", selector)
            .addOption("--no-playlist")
            .addOption("-g")
        val response = withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().execute(request, "urls-${System.nanoTime()}")
        }
        return response.out.lines().map { it.trim() }.filter { it.startsWith("http") }
    }

    suspend fun download(
        url: String,
        option: MediaOption,
        subtitles: Boolean,
        processId: String,
        onProgress: (percent: Float, etaSeconds: Long, line: String) -> Unit,
    ): File {
        ensureInitialized()
        val outputDir = File(context.getExternalFilesDir(null), "Grabi").apply { mkdirs() }
        val request = YoutubeDLRequest(url)
            .addOption("-f", option.formatSelector)
            .addOption("--no-playlist")
            .addOption("--restrict-filenames")
            .addOption("--newline")
            .addOption("--no-simulate")
            .addOption("--progress")
            .addOption("-o", File(outputDir, "%(title)s.%(ext)s").absolutePath)
            .addOption("--print", "after_move:filepath")
        val aria2c = File(context.applicationInfo.nativeLibraryDir, "libaria2c.so")
        if (aria2c.exists()) {
            request.addOption("--external-downloader", aria2c.absolutePath)
            request.addOption("--external-downloader-args", "aria2c:-x16 -s16 -k1M")
        }
        request.addOption("--concurrent-fragments", "8")
        request.addOption("--http-chunk-size", "10M")
        request.addOption("--extractor-args", "youtube:player_client=android,ios")
        if (option.kind == MediaKind.VIDEO) {
            request.addOption("--merge-output-format", "mp4")
        } else {
            request
                .addOption("-x")
                .addOption("--audio-format", option.audioFormat!!)
                .addOption("--audio-quality", "0")
        }
        if (subtitles) {
            request
                .addOption("--write-subs")
                .addOption("--sub-langs", "en")
                .addOption("--convert-subs", "srt")
        }
        val response = withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().execute(request, processId) { percent, eta, line ->
                onProgress(percent, eta, line)
            }
        }
        val printedPath = response.out.lines().lastOrNull { it.isNotBlank() }?.trim()
        return printedPath?.let(::File)?.takeIf { it.isFile }
            ?: outputDir.listFiles()?.maxByOrNull { it.lastModified() }
            ?: throw YoutubeDLException("Download finished but no output file was found")
    }

    private suspend fun ensureInitialized() {
        initMutex.withLock {
            if (!initialized) {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().init(context)
                    // YoutubeDL only points --ffmpeg-location at the binary; extraction happens here.
                    FFmpeg.getInstance().init(context)
                    Aria2c.getInstance().init(context)
                }
                initialized = true
            }
        }
    }
}