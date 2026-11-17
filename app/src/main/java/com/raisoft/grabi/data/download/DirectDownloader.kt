package com.raisoft.grabi.data.download

import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

/** Plain HTTP download for images; yt-dlp does not handle still images. */
@Singleton
class DirectDownloader @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val calls = ConcurrentHashMap<String, Call>()

    fun cancel(id: String) {
        calls.remove(id)?.cancel()
    }

    suspend fun download(
        url: String,
        target: File,
        processId: String,
        onProgress: (percent: Float, etaSeconds: Long, line: String) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val call = client.newCall(Request.Builder().url(url).build())
        calls[processId] = call
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty response body")
                val total = body.contentLength()
                target.parentFile?.mkdirs()
                var copied = 0L
                var lastTimestamp = System.currentTimeMillis()
                var lastBytes = 0L
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            val now = System.currentTimeMillis()
                            if (now - lastTimestamp >= 500) {
                                val bytesPerSecond = (copied - lastBytes) * 1000.0 / (now - lastTimestamp)
                                val percent = if (total > 0) copied * 100f / total else 0f
                                val eta = if (total > 0 && bytesPerSecond > 0) {
                                    ((total - copied) / bytesPerSecond).toLong()
                                } else {
                                    -1L
                                }
                                onProgress(percent, eta, "at ${formatSpeed(bytesPerSecond)}")
                                lastTimestamp = now
                                lastBytes = copied
                            }
                        }
                    }
                }
                onProgress(100f, 0L, "")
                target
            }
        } catch (e: IOException) {
            if (call.isCanceled()) throw CancellationException("Download canceled")
            throw e
        } finally {
            calls.remove(processId)
        }
    }

    private fun formatSpeed(bytesPerSecond: Double): String = when {
        bytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(bytesPerSecond / 1_000_000)
        bytesPerSecond >= 1_000 -> "%.0f KB/s".format(bytesPerSecond / 1_000)
        else -> "%.0f B/s".format(bytesPerSecond)
    }
}