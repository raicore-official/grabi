package com.raisoft.grabi.data.vpn.autobot

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRegistry @Inject constructor(
    gitHub: GitHubConfigSource,
    vpnBook: VpnBookSource,
) {
    val sources: List<VpnConfigSource> = listOf(gitHub, vpnBook)

    fun ordered(priority: List<String>): List<VpnConfigSource> =
        sources.sortedBy { source ->
            val index = priority.indexOf(source.id)
            if (index < 0) Int.MAX_VALUE else index
        }

    fun byId(id: String): VpnConfigSource? = sources.firstOrNull { it.id == id }
}