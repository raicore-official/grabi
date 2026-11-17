package com.raisoft.grabi.data.vpn.autobot

import com.raisoft.grabi.domain.vpn.GenerationResult
import com.raisoft.grabi.domain.vpn.ProviderAvailability
import com.raisoft.grabi.domain.vpn.ProviderCapabilities
import com.raisoft.grabi.domain.vpn.RemoteVpnConfig

interface VpnConfigSource {
    val id: String
    val displayName: String
    val capabilities: ProviderCapabilities
    suspend fun discover(): List<RemoteVpnConfig>
    suspend fun generate(country: String?): GenerationResult
    suspend fun availability(): ProviderAvailability
}