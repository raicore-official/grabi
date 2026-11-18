package com.raisoft.grabi.data.download

import bt.Bt
import bt.data.file.FileSystemStorage
import bt.dht.DHTConfig
import bt.dht.DHTModule
import bt.runtime.BtClient
import bt.runtime.Config
import bt.tracker.http.HttpTrackerModule
import com.raisoft.grabi.data.playback.DebugLog
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class TorrentEngine @Inject constructor() {

    private val clients = ConcurrentHashMap<String, BtClient>()
    private val stopped = ConcurrentHashMap.newKeySet<String>()

    fun cancel(processId: String) {
        stopped.add(processId)
        clients[processId]?.stop()
    }

    suspend fun download(
        url: String,
        targetDir: File,
        processId: String,
        onProgress: (Float, Long, String) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        val config = object : Config() {
            override fun getNumOfHashingThreads(): Int = 2
        }
        val dhtModule = DHTModule(
            object : DHTConfig() {
                override fun shouldUseRouterBootstrap(): Boolean = true
            },
        )
        val builder = Bt.client()
            .config(config)
            .storage(FileSystemStorage(targetDir.toPath()))
            .module(HttpTrackerModule())
            .module(dhtModule)
            .stopWhenDownloaded()
        if (url.startsWith("magnet:", ignoreCase = true)) {
            builder.magnet(url)
        } else {
            builder.torrent(URL(url))
        }
        var totalBytes = 0L
        var chunkSize = 0L
        builder.afterTorrentFetched { torrent ->
            totalBytes = torrent.size
            chunkSize = torrent.chunkSize
            DebugLog.line("torrent $processId fetched name=${torrent.name} size=$totalBytes")
        }
        val client = builder.build()
        clients[processId] = client
        var lastDownloaded = 0L
        var lastTimestamp = System.currentTimeMillis()
        try {
            client.startAsync(
                { state ->
                    val piecesTotal = state.piecesTotal
                    val percent = if (piecesTotal > 0) {
                        state.piecesComplete * 100f / piecesTotal
                    } else {
                        0f
                    }
                    val now = System.currentTimeMillis()
                    val seconds = (now - lastTimestamp) / 1000.0
                    val speed = if (seconds > 0) {
                        ((state.downloaded - lastDownloaded) / seconds).toLong().coerceAtLeast(0)
                    } else {
                        0L
                    }
                    lastDownloaded = state.downloaded
                    lastTimestamp = now
                    val remainingBytes = state.piecesRemaining * chunkSize
                    val eta = if (speed > 0 && remainingBytes > 0) remainingBytes / speed else -1L
                    onProgress(percent, eta, "DL:${speedLabel(speed)}")
                },
                1_000,
            ).join()
        } finally {
            clients.remove(processId)
        }
        if (stopped.remove(processId)) {
            throw CancellationException("Torrent stopped")
        }
        targetDir.walkTopDown().filter { it.isFile }.maxByOrNull { it.length() }
            ?: throw IOException("Torrent finished but produced no files")
    }

    private fun speedLabel(bytesPerSecond: Long): String = when {
        bytesPerSecond >= 1_048_576 -> "%.1fMB".format(bytesPerSecond / 1_048_576.0)
        bytesPerSecond >= 1024 -> "%.0fKB".format(bytesPerSecond / 1024.0)
        else -> "${bytesPerSecond}B"
    }
}