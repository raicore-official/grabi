package com.raisoft.grabi.domain.vpn

data class ProviderCapabilities(
    val automaticDiscovery: Boolean,
    val automaticGeneration: Boolean,
    val automaticDownload: Boolean,
    val requiresHumanVerification: Boolean,
    val protocols: Set<VpnProtocol>,
    val expiryMetadata: Boolean,
)