package com.raisoft.grabi.data.vpn

import android.content.Context
import android.net.Uri
import com.raisoft.grabi.domain.vpn.ConfigTrust
import com.raisoft.grabi.domain.vpn.CountryClassifier
import com.raisoft.grabi.domain.vpn.SystemVpnInfo
import com.raisoft.grabi.domain.vpn.VpnConfigEntry
import com.raisoft.grabi.domain.vpn.VpnConnectionState
import com.raisoft.grabi.domain.vpn.VpnStatus
import com.raisoft.grabi.domain.vpn.WgConfigUtils
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.KeyPair
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class VpnRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backend: TunnelBackend,
    private val prefs: VpnPreferences,
    private val finder: ConfigFinder,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connection = MutableStateFlow(VpnConnectionState())
    val connection: StateFlow<VpnConnectionState> = _connection.asStateFlow()

    val entries: StateFlow<List<VpnConfigEntry>> = prefs.entries.stateIn(
        scope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    val killSwitch: StateFlow<Boolean> = prefs.killSwitch.stateIn(
        scope, SharingStarted.Eagerly, true,
    )
    val excludedApps: StateFlow<Set<String>> = prefs.excludedApps.stateIn(
        scope, SharingStarted.WhileSubscribed(5_000), emptySet(),
    )

    private val _finderResults = MutableStateFlow<List<VpnConfigEntry>>(emptyList())
    val finderResults: StateFlow<List<VpnConfigEntry>> = _finderResults.asStateFlow()
    private val _finderBusy = MutableStateFlow(false)
    val finderBusy: StateFlow<Boolean> = _finderBusy.asStateFlow()
    private val _finderError = MutableStateFlow<String?>(null)
    val finderError: StateFlow<String?> = _finderError.asStateFlow()
    private val _finderUpdated = MutableStateFlow(0L)
    val finderUpdated: StateFlow<Long> = _finderUpdated.asStateFlow()

    private val _systemVpn = MutableStateFlow(SystemVpnInfo())
    val systemVpn: StateFlow<SystemVpnInfo> = _systemVpn.asStateFlow()

    private var tunnel: GrabiTunnel? = null
    private var blockTunnel: GrabiTunnel? = null
    private var activeEntry: VpnConfigEntry? = null
    private var userDisconnect = false
    private var connectJob: Job? = null
    private var statsJob: Job? = null
    private var connectStartedAt = 0L

    init {
        backend.onAlwaysOn { scope.launch { autoReconnect() } }
        scope.launch {
            val (cached, timestamp) = prefs.finderCache.first()
            _finderResults.value = cached
            _finderUpdated.value = timestamp
            refreshSystemVpn()
            adoptRunningTunnel()
        }
    }

    fun connect(entry: VpnConfigEntry) {
        connectJob?.cancel()
        connectJob = scope.launch {
            userDisconnect = false
            activeEntry = entry
            _connection.value = VpnConnectionState(
                status = VpnStatus.CONNECTING,
                activeName = entry.name,
                activeCountry = entry.country,
            )
            try {
                val config = WgConfigUtils.withExcludedApps(
                    WgConfigUtils.parse(entry.configText),
                    prefs.excludedApps.first(),
                )
                val t = GrabiTunnel(entry.name) { state -> onBackendStateChange(state) }
                tunnel = t
                blockTunnel = null
                connectStartedAt = System.currentTimeMillis()
                backend.connect(t, config)
                prefs.setLastConnected(entry.name)
                _connection.update { it.copy(status = VpnStatus.VERIFYING) }
                if (verifyHandshake(t, config)) {
                    _connection.update { it.copy(status = VpnStatus.CONNECTED, error = null) }
                    startStats(t, config)
                } else {
                    userDisconnect = true
                    runCatching { backend.disconnect(t) }
                    tunnel = null
                    failConnection("no_handshake")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                userDisconnect = true
                tunnel?.let { runCatching { backend.disconnect(it) } }
                tunnel = null
                failConnection(e.message ?: "connect_failed")
            }
        }
    }

    fun disconnect() {
        scope.launch {
            userDisconnect = true
            connectJob?.cancel()
            statsJob?.cancel()
            _connection.update { it.copy(status = VpnStatus.DISCONNECTING) }
            tunnel?.let { runCatching { backend.disconnect(it) } }
            blockTunnel?.let { runCatching { backend.disconnect(it) } }
            tunnel = null
            blockTunnel = null
            activeEntry = null
            prefs.setLastConnected("")
            _connection.value = VpnConnectionState()
            refreshSystemVpn()
        }
    }

    fun setKillSwitch(enabled: Boolean) {
        scope.launch {
            prefs.setKillSwitch(enabled)
            if (!enabled && _connection.value.status == VpnStatus.BLOCKED) {
                blockTunnel?.let { runCatching { backend.disconnect(it) } }
                blockTunnel = null
                _connection.value = VpnConnectionState()
            }
        }
    }

    fun setExcludedApps(apps: Set<String>) {
        scope.launch { prefs.setExcludedApps(apps) }
    }

    suspend fun importConfig(
        rawName: String,
        text: String,
        source: String,
        trust: ConfigTrust,
    ): VpnConfigEntry {
        val config = WgConfigUtils.parse(text)
        if (!WgConfigUtils.isUsableClient(config)) {
            throw IllegalArgumentException("Config has no peer endpoint")
        }
        val base = WgConfigUtils.sanitizeName(rawName)
        val existing = prefs.entriesNow().map { it.name }.toSet()
        var name = base
        var i = 2
        while (name in existing) {
            name = WgConfigUtils.sanitizeName("$base-$i")
            i++
        }
        val endpoint = WgConfigUtils.endpointLabel(config)
        val entry = VpnConfigEntry(
            name = name,
            configText = text.trim(),
            country = CountryClassifier.classify(listOf(rawName, endpoint ?: "")),
            source = source,
            trust = trust,
            addedAt = System.currentTimeMillis(),
            endpoint = endpoint,
        )
        prefs.addEntry(entry)
        return entry
    }

    suspend fun importFromUrl(url: String): VpnConfigEntry {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" && uri.scheme != "http") {
            throw IllegalArgumentException("Only http(s) URLs are supported")
        }
        val text = finder.fetchConfigText(url)
        val rawName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".conf")
            ?.ifBlank { null } ?: "imported"
        return importConfig(rawName, text, uri.host ?: url, ConfigTrust.IMPORTED)
    }

    fun deleteEntry(name: String) {
        scope.launch {
            if (_connection.value.activeName == name &&
                _connection.value.status != VpnStatus.IDLE
            ) {
                disconnect()
            }
            prefs.removeEntry(name)
        }
    }

    fun saveFound(entry: VpnConfigEntry) {
        scope.launch {
            runCatching {
                importConfig(entry.name, entry.configText, entry.source, ConfigTrust.COMMUNITY)
            }.onFailure { _finderError.value = it.message ?: "save_failed" }
        }
    }

    fun refreshFinder() {
        if (_finderBusy.value) return
        _finderBusy.value = true
        _finderError.value = null
        scope.launch {
            try {
                val results = finder.refresh()
                val now = System.currentTimeMillis()
                _finderResults.value = results
                _finderUpdated.value = now
                prefs.saveFinder(results, now)
                if (results.isEmpty()) _finderError.value = "empty"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _finderError.value = e.message ?: "finder_failed"
            } finally {
                _finderBusy.value = false
            }
        }
    }

    fun refreshSystemVpn() {
        scope.launch {
            _systemVpn.value = SystemVpnInfo(
                alwaysOn = runCatching { backend.alwaysOn() }.getOrDefault(false),
                lockdown = runCatching { backend.lockdown() }.getOrDefault(false),
            )
        }
    }

    private suspend fun verifyHandshake(t: GrabiTunnel, config: Config): Boolean {
        val peerKey = config.peers.firstOrNull()?.publicKey ?: return false
        val deadline = System.currentTimeMillis() + VERIFY_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            delay(1_000)
            if (t.state == Tunnel.State.DOWN) return false
            val stats = runCatching { backend.statistics(t) }.getOrNull() ?: continue
            if (stats.peer(peerKey).latestHandshakeEpochMillis() >= connectStartedAt - 2_000) {
                return true
            }
        }
        return false
    }

    private fun startStats(t: GrabiTunnel, config: Config) {
        statsJob?.cancel()
        val peerKey = config.peers.firstOrNull()?.publicKey
        statsJob = scope.launch {
            while (isActive) {
                val stats = runCatching { backend.statistics(t) }.getOrNull()
                if (stats != null) {
                    val handshake = peerKey?.let { stats.peer(it).latestHandshakeEpochMillis() } ?: 0L
                    _connection.update {
                        it.copy(
                            rxBytes = stats.totalRx(),
                            txBytes = stats.totalTx(),
                            handshakeAgeSeconds = if (handshake > 0) {
                                (System.currentTimeMillis() - handshake) / 1_000
                            } else {
                                null
                            },
                        )
                    }
                }
                delay(2_000)
            }
        }
    }

    private fun onBackendStateChange(state: Tunnel.State) {
        if (state != Tunnel.State.DOWN) return
        scope.launch {
            if (userDisconnect) return@launch
            val status = _connection.value.status
            if (status != VpnStatus.CONNECTED &&
                status != VpnStatus.VERIFYING &&
                status != VpnStatus.CONNECTING
            ) {
                return@launch
            }
            statsJob?.cancel()
            tunnel = null
            if (killSwitch.value && engageBlackhole()) {
                _connection.value = VpnConnectionState(
                    status = VpnStatus.BLOCKED,
                    activeName = activeEntry?.name,
                    activeCountry = activeEntry?.country,
                    error = "dropped",
                )
            } else {
                failConnection("dropped")
            }
        }
    }

    private suspend fun engageBlackhole(): Boolean {
        val t = GrabiTunnel(BLOCK_TUNNEL_NAME) { }
        return runCatching {
            backend.connect(t, blackholeConfig())
            blockTunnel = t
            true
        }.getOrDefault(false)
    }

    private fun failConnection(error: String) {
        if (_connection.value.status == VpnStatus.BLOCKED) return
        _connection.value = VpnConnectionState(
            status = VpnStatus.ERROR,
            activeName = activeEntry?.name,
            activeCountry = activeEntry?.country,
            error = error,
        )
    }

    private suspend fun autoReconnect() {
        val status = _connection.value.status
        if (status == VpnStatus.CONNECTED ||
            status == VpnStatus.CONNECTING ||
            status == VpnStatus.VERIFYING
        ) {
            return
        }
        val last = prefs.lastConnected.first()
        val entry = prefs.entriesNow().find { it.name == last } ?: return
        connect(entry)
    }

    private suspend fun adoptRunningTunnel() {
        val last = prefs.lastConnected.first()
        if (last.isBlank() || last !in backend.runningNames()) return
        val entry = prefs.entriesNow().find { it.name == last }
        val t = GrabiTunnel(last) { state -> onBackendStateChange(state) }
        if (runCatching { backend.state(t) }.getOrNull() != Tunnel.State.UP) return
        tunnel = t
        activeEntry = entry
        _connection.value = VpnConnectionState(
            status = VpnStatus.CONNECTED,
            activeName = last,
            activeCountry = entry?.country,
        )
        val config = entry?.let { runCatching { WgConfigUtils.parse(it.configText) }.getOrNull() }
        if (config != null) startStats(t, config)
    }

    private fun blackholeConfig(): Config {
        val keyPair = KeyPair()
        val interfaze = Interface.Builder()
            .setKeyPair(keyPair)
            .addAddress(InetNetwork.parse("10.66.0.1/32"))
            .build()
        val peer = Peer.Builder()
            .setPublicKey(keyPair.publicKey)
            .addAllowedIp(InetNetwork.parse("0.0.0.0/0"))
            .addAllowedIp(InetNetwork.parse("::/0"))
            .parseEndpoint("127.0.0.1:9")
            .build()
        return Config.Builder().setInterface(interfaze).addPeer(peer).build()
    }

    private companion object {
        const val VERIFY_TIMEOUT_MS = 15_000L
        const val BLOCK_TUNNEL_NAME = "grabi-block"
    }
}