package com.raisoft.grabi.domain

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

object LinkValidator {

    private val urlPattern = Regex("""(?:https?://|magnet:\?)\S+""", RegexOption.IGNORE_CASE)

    fun isValidUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            when {
                (scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty() -> true
                scheme == "magnet" && trimmed.contains("xt=urn:btih:", ignoreCase = true) -> true
                else -> false
            }
        } catch (_: URISyntaxException) {
            false
        }
    }

    fun isTorrent(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith("magnet:", ignoreCase = true) ||
            trimmed.substringBefore('?').endsWith(".torrent", ignoreCase = true)
    }

    fun torrentTitle(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("magnet:", ignoreCase = true)) {
            val dn = Regex("[?&]dn=([^&]+)", RegexOption.IGNORE_CASE)
                .find(trimmed)?.groupValues?.get(1)
            if (!dn.isNullOrBlank()) {
                return runCatching { URLDecoder.decode(dn, "UTF-8") }.getOrDefault(dn)
            }
            return "Torrent"
        }
        return trimmed.substringBefore('?').substringAfterLast('/')
            .replace(Regex("""\.torrent$""", RegexOption.IGNORE_CASE), "")
            .ifBlank { "Torrent" }
    }

    // Share intents often wrap the link in text ("Watch this: <url>"); pull the first URL out.
    fun extractUrl(text: String): String? =
        urlPattern.find(text)?.value?.trimEnd('.', ',', ';', ')', ']', '"', '\'')

    private val directExtensions = setOf(
        "mp4", "mkv", "webm", "mov", "avi", "m4v",
        "mp3", "m4a", "opus", "ogg", "flac", "wav", "aac",
    )

    fun isDirectMediaUrl(input: String): Boolean {
        val path = try {
            URI(input.trim()).path
        } catch (_: URISyntaxException) {
            null
        } ?: return false
        return path.substringAfterLast('.', "").lowercase() in directExtensions
    }
}