package com.raisoft.grabi.data.vpn.autobot

enum class GenerationMode {
    AUTOMATIC,
    MANUAL,
    ACCOUNT_REQUIRED,
    APP_ONLY,
    UNSUPPORTED,
}

enum class ExpiryPolicy {
    PROVIDER_DEFINED,
    FIXED_DURATION,
    UNKNOWN,
}

data class VpnProvider(
    val id: String,
    val name: String,
    val websiteUrl: String,
    val officialConfigUrl: String?,
    val supportedProtocols: List<String>,
    val generationMode: GenerationMode,
    val instructions: List<String>,
    val requiresAccount: Boolean,
    val requiresHumanVerification: Boolean,
    val expiryPolicy: ExpiryPolicy?,
    val importFormat: String?,
)

object VpnProviderCatalog {
    val entries = listOf(
        VpnProvider(
            id = "vpnbook",
            name = "VPNBook",
            websiteUrl = "https://www.vpnbook.com",
            officialConfigUrl = "https://www.vpnbook.com/freevpn/wireguard-vpn",
            supportedProtocols = listOf("WireGuard"),
            generationMode = GenerationMode.MANUAL,
            instructions = listOf(
                "Open the official WireGuard generator.",
                "Choose a server country and an available UDP port.",
                "Complete the provider verification if requested.",
                "Generate and download the WireGuard configuration.",
                "Return to Grabi and import the downloaded .conf file.",
            ),
            requiresAccount = false,
            requiresHumanVerification = true,
            expiryPolicy = ExpiryPolicy.FIXED_DURATION,
            importFormat = ".conf",
        ),
        VpnProvider(
            id = "protonvpn",
            name = "Proton VPN",
            websiteUrl = "https://protonvpn.com",
            officialConfigUrl = "https://protonvpn.com/support/wireguard-configurations/",
            supportedProtocols = listOf("WireGuard", "OpenVPN"),
            generationMode = GenerationMode.ACCOUNT_REQUIRED,
            instructions = listOf(
                "Sign in to your Proton VPN account on the official site.",
                "Open the manual WireGuard configuration generator.",
                "Choose the server and download the configuration.",
                "Return to Grabi and import the downloaded configuration.",
            ),
            requiresAccount = true,
            requiresHumanVerification = false,
            expiryPolicy = ExpiryPolicy.PROVIDER_DEFINED,
            importFormat = ".conf",
        ),
        VpnProvider(
            id = "windscribe",
            name = "Windscribe",
            websiteUrl = "https://windscribe.com",
            officialConfigUrl = "https://windscribe.com/",
            supportedProtocols = listOf("WireGuard", "OpenVPN"),
            generationMode = GenerationMode.ACCOUNT_REQUIRED,
            instructions = listOf(
                "Sign in to the official Windscribe website.",
                "Use the account configuration workflow for your chosen protocol.",
                "Download the generated configuration and return to Grabi.",
            ),
            requiresAccount = true,
            requiresHumanVerification = false,
            expiryPolicy = ExpiryPolicy.PROVIDER_DEFINED,
            importFormat = ".conf",
        ),
        VpnProvider(
            id = "privadovpn",
            name = "PrivadoVPN",
            websiteUrl = "https://privadovpn.com",
            officialConfigUrl = "https://privadovpn.com/",
            supportedProtocols = listOf("WireGuard", "OpenVPN"),
            generationMode = GenerationMode.ACCOUNT_REQUIRED,
            instructions = listOf(
                "Sign in to the official PrivadoVPN website.",
                "Follow its current manual configuration workflow.",
                "Download a supported configuration and return to Grabi.",
            ),
            requiresAccount = true,
            requiresHumanVerification = false,
            expiryPolicy = ExpiryPolicy.PROVIDER_DEFINED,
            importFormat = ".conf",
        ),
        VpnProvider(
            id = "tunnelbear",
            name = "TunnelBear",
            websiteUrl = "https://www.tunnelbear.com",
            officialConfigUrl = null,
            supportedProtocols = emptyList(),
            generationMode = GenerationMode.APP_ONLY,
            instructions = listOf(
                "TunnelBear is supported through its official app.",
                "A generic WireGuard configuration import is not available.",
            ),
            requiresAccount = true,
            requiresHumanVerification = false,
            expiryPolicy = null,
            importFormat = null,
        ),
        VpnProvider(
            id = "github",
            name = "GitHub community",
            websiteUrl = "https://github.com",
            officialConfigUrl = null,
            supportedProtocols = listOf("WireGuard"),
            generationMode = GenerationMode.AUTOMATIC,
            instructions = listOf(
                "Community configurations are not official provider accounts.",
                "Review and validate every configuration before connecting.",
            ),
            requiresAccount = false,
            requiresHumanVerification = false,
            expiryPolicy = ExpiryPolicy.UNKNOWN,
            importFormat = ".conf",
        ),
    )
}
