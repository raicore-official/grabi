package com.raisoft.grabi.domain

import java.net.URI
import java.net.URISyntaxException

object LinkValidator {

    private val urlPattern = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    fun isValidUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty()
        } catch (_: URISyntaxException) {
            false
        }
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