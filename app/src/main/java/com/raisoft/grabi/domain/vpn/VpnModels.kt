package com.raisoft.grabi.domain.vpn

enum class VpnStatus {
    IDLE,
    CONNECTING,
    VERIFYING,
    CONNECTED,
    DISCONNECTING,
    BLOCKED,
    ERROR,
}

enum class ConfigTrust {
    IMPORTED,
    COMMUNITY,
}

data class VpnConfigEntry(
    val name: String,
    val configText: String,
    val country: String,
    val source: String,
    val trust: ConfigTrust,
    val addedAt: Long,
    val endpoint: String? = null,
)

data class VpnConnectionState(
    val status: VpnStatus = VpnStatus.IDLE,
    val activeName: String? = null,
    val activeCountry: String? = null,
    val error: String? = null,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val handshakeAgeSeconds: Long? = null,
)

data class SystemVpnInfo(
    val alwaysOn: Boolean = false,
    val lockdown: Boolean = false,
)