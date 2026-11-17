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
}