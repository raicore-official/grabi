package com.raisoft.grabi.data.vpn

import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config

interface TunnelBackend {
    suspend fun connect(tunnel: Tunnel, config: Config)
    suspend fun disconnect(tunnel: Tunnel)
    suspend fun state(tunnel: Tunnel): Tunnel.State
    suspend fun statistics(tunnel: Tunnel): Statistics
    suspend fun runningNames(): Set<String>
    suspend fun alwaysOn(): Boolean
    suspend fun lockdown(): Boolean
    fun onAlwaysOn(callback: () -> Unit)
}