package com.raisoft.grabi.data.vpn

import com.wireguard.android.backend.Tunnel

class GrabiTunnel(
    private val name: String,
    private val listener: (Tunnel.State) -> Unit,
) : Tunnel {

    @Volatile
    var state: Tunnel.State = Tunnel.State.DOWN
        private set

    override fun getName(): String = name

    override fun onStateChange(newState: Tunnel.State) {
        state = newState
        listener(newState)
    }
}