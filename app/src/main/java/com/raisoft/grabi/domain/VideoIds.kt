package com.raisoft.grabi.domain

object VideoIds {

    private val patterns = listOf(
        Regex("""[?&]v=([A-Za-z0-9_-]{6,})"""),
        Regex("""youtu\.be/([A-Za-z0-9_-]{6,})"""),
        Regex("""/(?:shorts|embed|live)/([A-Za-z0-9_-]{6,})"""),
    )

    fun of(url: String): String =
        patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.get(1) } ?: url
}