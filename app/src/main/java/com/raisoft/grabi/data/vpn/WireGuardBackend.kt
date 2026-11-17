package com.raisoft.grabi.data.vpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class WireGuardBackend @Inject constructor(
    @ApplicationContext context: Context,
) : TunnelBackend {

    private val backend = GoBackend(context)

    override suspend fun connect(tunnel: Tunnel, config: Config) {
        withContext(Dispatchers.IO) { backend.setState(tunnel, Tunnel.State.UP, config) }
    }

    override suspend fun disconnect(tunnel: Tunnel) {
        withContext(Dispatchers.IO) { backend.setState(tunnel, Tunnel.State.DOWN, null) }
    }

    override suspend fun state(tunnel: Tunnel): Tunnel.State =
        withContext(Dispatchers.IO) { backend.getState(tunnel) }

    override suspend fun statistics(tunnel: Tunnel): Statistics =
        withContext(Dispatchers.IO) { backend.getStatistics(tunnel) }

    override suspend fun runningNames(): Set<String> =
        withContext(Dispatchers.IO) { backend.runningTunnelNames }

    override suspend fun alwaysOn(): Boolean =
        withContext(Dispatchers.IO) { backend.isAlwaysOn }

    override suspend fun lockdown(): Boolean =
        withContext(Dispatchers.IO) { backend.isLockdownEnabled }

    override fun onAlwaysOn(callback: () -> Unit) {
        GoBackend.setAlwaysOnCallback { callback() }
    }
}