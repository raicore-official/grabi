package com.raisoft.grabi.data.vpn.autobot

import com.raisoft.grabi.data.vpn.ConfigFinder
import com.raisoft.grabi.domain.vpn.GenerationResult
import com.raisoft.grabi.domain.vpn.ProviderAvailability
import com.raisoft.grabi.domain.vpn.ProviderCapabilities
import com.raisoft.grabi.domain.vpn.ProviderErrorKind
import com.raisoft.grabi.domain.vpn.RemoteVpnConfig
import com.raisoft.grabi.domain.vpn.VpnProtocol
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubConfigSource @Inject constructor(
    private val finder: ConfigFinder,
) : VpnConfigSource {

    override val id = "github"
    override val displayName = "GitHub community"
    override val capabilities = ProviderCapabilities(
        automaticDiscovery = true,
        automaticGeneration = true,
        automaticDownload = true,
        requiresHumanVerification = false,
        protocols = setOf(VpnProtocol.WIREGUARD),
        expiryMetadata = false,
    )

    override suspend fun discover(): List<RemoteVpnConfig> =
        finder.refresh().map { entry ->
            RemoteVpnConfig(
                providerId = id,
                name = entry.name,
                country = entry.country,
                protocol = VpnProtocol.WIREGUARD,
                configText = entry.configText,
                endpoint = entry.endpoint,
                generatedAt = entry.addedAt,
                expiresAt = null,
                expirySource = "none",
            )
        }

    override suspend fun generate(country: String?): GenerationResult = try {
        val configs = discover()
        val pick = configs.firstOrNull { country == null || it.country.equals(country, true) }
            ?: configs.firstOrNull()
        if (pick != null) {
            GenerationResult.Success(pick)
        } else {
            GenerationResult.Unavailable("no usable community configurations found")
        }
    } catch (e: Exception) {
        GenerationResult.Error(ProviderErrorKind.NETWORK, e.message ?: "discovery failed")
    }

    override suspend fun availability(): ProviderAvailability =
        ProviderAvailability(true, "GitHub search API")
}