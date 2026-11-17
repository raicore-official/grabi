package com.raisoft.grabi.domain.vpn

enum class VpnProtocol { WIREGUARD, OPENVPN }

data class RemoteVpnConfig(
    val providerId: String,
    val name: String,
    val country: String,
    val protocol: VpnProtocol,
    val configText: String,
    val endpoint: String?,
    val generatedAt: Long,
    val expiresAt: Long?,
    val expirySource: String,
)