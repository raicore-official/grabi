package com.raisoft.grabi.data.download

import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

/** Plain HTTP download for images and cached muxed streams; yt-dlp does not handle still images. */
@Singleton
class DirectDownloader @Inject constructor(
    private val client: OkHttpClient,
) {

    private val calls = ConcurrentHashMap<String, Call>()

    fun cancel(id: String) {
        calls.keys.filter { it == id || it.startsWith("$id-") }.forEach { calls.remove(it)?.cancel() }
    }

    suspend fun downloadRanges(
        url: String,
        target: File,
        processId: String,
        onProgress: (percent: Float, etaSeconds: Long, line: String) -> Unit,
        segments: Int = 6,
    ): File = withContext(Dispatchers.IO) {
        val head = runCatching {
            client.newCall(Request.Builder().url(url).head().build()).execute()
        }.getOrNull()
        val total = head?.header("Content-Length")?.toLongOrNull() ?: -1L
        val rangeable = head?.header("Accept-Ranges") == "bytes"
        head?.close()
        val count = segments.coerceIn(1, 8)
        if (!rangeable || total < 4_000_000L || count <= 1) {
            return@withContext download(url, target, processId, onProgress)
        }
        target.parentFile?.mkdirs()
        RandomAccessFile(target, "rw").use { it.setLength(total) }
        val chunk = total / count
        val copied = AtomicLong(0)
        val progressLock = Any()
        var lastTimestamp = System.currentTimeMillis()
        var lastBytes = 0L
        try {
            coroutineScope {
                (0 until count).map { i ->
                    async {
                        val start = i * chunk
                        val end = if (i == count - 1) total - 1 else start + chunk - 1
                        val call = client.newCall(
                            Request.Builder().url(url).header("Range", "bytes=$start-$end").build(),
                        )
                        calls["$processId-$i"] = call
                        call.execute().use { response ->
                            if (response.code != 206) throw RangeUnsupportedException()
                            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                            val body = response.body ?: throw IOException("Empty response body")
                            RandomAccessFile(target, "rw").use { raf ->
                                raf.seek(start)
                                body.byteStream().use { input ->
                                    val buffer = ByteArray(64 * 1024)
                                    while (true) {
                                        ensureActive()
                                        val read = input.read(buffer)
                                        if (read < 0) break
                                        raf.write(buffer, 0, read)
                                        val done = copied.addAndGet(read.toLong())
                                        synchronized(progressLock) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastTimestamp >= 500) {
                                                val bytesPerSecond =
                                                    (done - lastBytes) * 1000.0 / (now - lastTimestamp)
                                                lastTimestamp = now
                                                lastBytes = done
                                                val eta = if (bytesPerSecond > 0) {
                                                    ((total - done) / bytesPerSecond).toLong()
                                                } else {
                                                    -1L
                                                }
                                                onProgress(
                                                    done * 100f / total,
                                                    eta,
                                                    "at ${formatSpeed(bytesPerSecond)}",
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.awaitAll()
            }
            onProgress(100f, 0L, "")
            target
        } catch (e: RangeUnsupportedException) {
            target.delete()
            download(url, target, processId, onProgress)
        } catch (e: IOException) {
            if (calls.keys.any { it.startsWith(processId) && calls[it]?.isCanceled() == true }) {
                throw CancellationException("Download canceled")
            }
            throw e
        } finally {
            calls.keys.filter { it == processId || it.startsWith("$processId-") }.forEach { calls.remove(it) }
        }
    }

    private class RangeUnsupportedException : IOException("Server ignored Range header")

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