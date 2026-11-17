package com.raisoft.grabi.data.vpn.autobot

import com.raisoft.grabi.domain.vpn.GenerationResult
import com.raisoft.grabi.domain.vpn.ProviderAvailability
import com.raisoft.grabi.domain.vpn.ProviderCapabilities
import com.raisoft.grabi.domain.vpn.RemoteVpnConfig
import com.raisoft.grabi.domain.vpn.VpnBookParser
import com.raisoft.grabi.domain.vpn.VpnProtocol
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class VpnBookSource @Inject constructor(
    private val client: OkHttpClient,
) : VpnConfigSource {

    override val id = "vpnbook"
    override val displayName = "VPNBook"
    override val capabilities = ProviderCapabilities(
        automaticDiscovery = true,
        automaticGeneration = false,
        automaticDownload = false,
        requiresHumanVerification = true,
        protocols = setOf(VpnProtocol.WIREGUARD),
        expiryMetadata = true,
    )

    override suspend fun discover(): List<RemoteVpnConfig> {
        val html = fetchPage()
        return VpnBookParser.parseServers(html).map { server ->
            RemoteVpnConfig(
                providerId = id,
                name = server.hostname,
                country = server.country,
                protocol = VpnProtocol.WIREGUARD,
                configText = "",
                endpoint = "${server.hostname}:${VpnBookParser.PORTS.first()}",
                generatedAt = 0L,
                expiresAt = null,
                expirySource = "none",
            )
        }
    }

    override suspend fun generate(country: String?): GenerationResult =
        GenerationResult.HumanVerificationRequired(
            PAGE_URL,
            "VPNBook protects config generation with Cloudflare Turnstile. A person must complete the check; AutoBot never bypasses it.",
        )

    override suspend fun availability(): ProviderAvailability = try {
        val servers = VpnBookParser.parseServers(fetchPage())
        if (servers.isEmpty()) {
            ProviderAvailability(false, "server list not found on page")
        } else {
            ProviderAvailability(true, "${servers.size} servers published")
        }
    } catch (e: Exception) {
        ProviderAvailability(false, e.message ?: "unreachable")
    }

    private suspend fun fetchPage(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(PAGE_URL)
            .header("User-Agent", "Grabi-AutoBot")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("VPNBook HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Empty VPNBook response")
        }
    }

    companion object {
        const val PAGE_URL = "https://www.vpnbook.com/freevpn/wireguard-vpn"
        const val VALIDITY_MS = 7L * 24 * 3_600_000
    }
}