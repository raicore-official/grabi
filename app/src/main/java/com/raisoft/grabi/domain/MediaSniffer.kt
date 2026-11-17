package com.raisoft.grabi.domain

import com.raisoft.grabi.domain.model.MediaKind
import java.net.URI

object MediaSniffer {

    private val videoExtensions = setOf("mp4", "m3u8", "webm", "mkv", "mov", "avi", "m4v", "ts")
    private val audioExtensions = setOf("mp3", "m4a", "opus", "ogg", "flac", "wav", "aac")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")

    fun classify(url: String, mimeType: String?): MediaKind? {
        mimeType?.lowercase()?.let { mime ->
            when {
                mime.startsWith("video/") ||
                    mime == "application/vnd.apple.mpegurl" ||
                    mime == "application/x-mpegurl" -> return MediaKind.VIDEO
                mime.startsWith("audio/") -> return MediaKind.AUDIO
                mime.startsWith("image/") -> return MediaKind.IMAGE
            }
        }
        val path = runCatching { URI(url).path }.getOrNull()?.lowercase() ?: return null
        return when (path.substringAfterLast('.', "")) {
            in videoExtensions -> MediaKind.VIDEO
            in audioExtensions -> MediaKind.AUDIO
            in imageExtensions -> MediaKind.IMAGE
            "apk", "zip", "rar", "7z", "pdf", "conf", "ovpn" -> MediaKind.FILE
            else -> null
        }
    }
}