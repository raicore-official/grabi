package com.raisoft.grabi.domain.vpn

object VpnBookParser {

    data class Server(val id: String, val hostname: String, val country: String)

    val PORTS = listOf(443, 80, 123, 25018)

    private val HOST = Regex("""\b([a-z]{2}\d{1,4})\.vpnbook\.com\b""")
    private val COUNTRIES = mapOf(
        "us" to "US",
        "ca" to "CA",
        "uk" to "GB",
        "de" to "DE",
        "fr" to "FR",
    )

    fun parseServers(html: String): List<Server> =
        HOST.findAll(html)
            .map { it.groupValues[1] }
            .distinct()
            .mapNotNull { id ->
                COUNTRIES[id.take(2)]?.let { country -> Server(id, "$id.vpnbook.com", country) }
            }
            .toList()

    fun requiresHumanVerification(html: String): Boolean =
        html.contains("turnstileSiteKey") || html.contains("challenges.cloudflare.com")
}