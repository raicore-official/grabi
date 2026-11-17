package com.raisoft.grabi.data.vpn.autobot

import android.content.Context
import com.raisoft.grabi.data.playback.DebugLog
import com.raisoft.grabi.data.vpn.VpnRepository
import com.raisoft.grabi.domain.vpn.AutoBotPhase
import com.raisoft.grabi.domain.vpn.AutoBotStateMachine
import com.raisoft.grabi.domain.vpn.AutoBotTrigger
import com.raisoft.grabi.domain.vpn.CandidateRanker
import com.raisoft.grabi.domain.vpn.ConfigKeys
import com.raisoft.grabi.domain.vpn.ConfigTrust
import com.raisoft.grabi.domain.vpn.ConfigValidator
import com.raisoft.grabi.domain.vpn.CountryClassifier
import com.raisoft.grabi.domain.vpn.ExpiryCalculator
import com.raisoft.grabi.domain.vpn.ExpiryRecord
import com.raisoft.grabi.domain.vpn.ExpiryStatus
import com.raisoft.grabi.domain.vpn.GenerationResult
import com.raisoft.grabi.domain.vpn.RemoteVpnConfig
import com.raisoft.grabi.domain.vpn.VpnConfigEntry
import com.raisoft.grabi.domain.vpn.VpnProtocol
import com.raisoft.grabi.domain.vpn.VpnStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class AutoBotManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AutoBotPreferences,
    private val registry: ProviderRegistry,
    private val expiry: ConfigExpiryManager,
    private val vpn: VpnRepository,
    private val notifications: ExpiryNotificationManager,
    private val httpClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runMutex = Mutex()

    val settings: StateFlow<AutoBotPrefs> =
        prefs.prefs.stateIn(scope, SharingStarted.Eagerly, AutoBotPrefs())
    val activity: StateFlow<List<AutoBotActivity>> =
        prefs.activity.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val providerStatuses: StateFlow<Map<String, String>> =
        prefs.providerStatuses.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val candidate: StateFlow<RemoteVpnConfig?> =
        prefs.candidate.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
    val expiryRecords: StateFlow<Map<String, ExpiryRecord>> = expiry.records

    private val _phase = MutableStateFlow(AutoBotPhase.OFF)
    val phase: StateFlow<AutoBotPhase> = _phase.asStateFlow()

    val providers: List<VpnConfigSource> = registry.sources

    fun onAppStart() {
        scope.launch {
            val p = prefs.prefsNow()
            if (!p.enabled) return@launch
            _phase.value = AutoBotPhase.IDLE
            syncSchedule(p)
            if (p.nextRenewal <= System.currentTimeMillis()) runCycle()
        }
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            prefs.setEnabled(enabled)
            if (enabled) {
                transition(AutoBotTrigger.ENABLE)
                log("AutoBot enabled")
                syncSchedule(prefs.prefsNow())
                runCycle()
            } else {
                transition(AutoBotTrigger.DISABLE)
                AutoBotScheduler.sync(context, false, 0L, null)
                log("AutoBot disabled")
            }
        }
    }

    fun setFullAuto(value: Boolean) = scope.launch { prefs.setFullAuto(value) }.let {}
    fun setAutoFallback(value: Boolean) = scope.launch { prefs.setAutoFallback(value) }.let {}
    fun setAutoRenew(value: Boolean) = scope.launch { prefs.setAutoRenew(value) }.let {}
    fun setExpiryProtection(value: Boolean) = scope.launch { prefs.setExpiryProtection(value) }.let {}
    fun setPreferredProvider(value: String) = scope.launch { prefs.setPreferredProvider(value) }.let {}
    fun setCountryPref(value: String) = scope.launch { prefs.setCountryPref(value) }.let {}
    fun setProviderPriority(value: List<String>) = scope.launch { prefs.setProviderPriority(value) }.let {}

    fun retryNow() {
        scope.launch {
            transition(AutoBotTrigger.RETRY)
            runCycle()
        }
    }

    suspend fun runCycle(allowReplace: Boolean = true): Boolean {
        val p = prefs.prefsNow()
        if (!p.enabled) return true
        if (!runMutex.tryLock()) return true
        try {
            transition(AutoBotTrigger.START)
            val sources = registry.ordered(p.providerPriority).let { ordered ->
                if (p.preferredProvider.isNotBlank() && !p.autoFallback) {
                    ordered.filter { it.id == p.preferredProvider }
                } else {
                    ordered
                }
            }
            val discovered = ArrayList<RemoteVpnConfig>()
            for (source in sources) {
                runCatching { source.discover() }
                    .onSuccess { list ->
                        discovered += list
                        prefs.setProviderStatus(source.id, "OK · ${list.size} found")
                    }
                    .onFailure { e ->
                        prefs.setProviderStatus(source.id, classify(e))
                        log("${source.id} discovery failed: ${e.message?.take(120)}")
                    }
            }
            prefs.saveSchedule(lastDiscovery = System.currentTimeMillis())
            transition(AutoBotTrigger.DISCOVERED)

            var needsUser = false
            if (discovered.none { it.configText.isNotBlank() }) {
                val assisted = sources.firstOrNull { it.capabilities.requiresHumanVerification }
                if (assisted != null) {
                    when (val generation = assisted.generate(p.countryPref.ifBlank { null })) {
                        is GenerationResult.HumanVerificationRequired -> {
                            needsUser = true
                            prefs.setProviderStatus(assisted.id, "Needs human verification")
                            notifications.providerVerification(assisted.displayName)
                            log("${assisted.id} requires human verification")
                        }
                        is GenerationResult.Success -> discovered += generation.config
                        else -> Unit
                    }
                }
            }

            val valid = ArrayList<RemoteVpnConfig>()
            val seen = HashSet<String>()
            for (config in discovered.filter { it.configText.isNotBlank() }) {
                val key = ConfigKeys.of(config.configText)
                if (!seen.add(key)) continue
                when (val result = ConfigValidator.validate(config.configText)) {
                    is ConfigValidator.Result.Valid -> {
                        expiry.touchOrInsert(key, config)
                        valid += config
                    }
                    is ConfigValidator.Result.Invalid ->
                        log("rejected ${config.name}: ${result.reason}")
                }
            }
            val ranked = CandidateRanker.rank(
                valid,
                p.providerPriority,
                p.preferredProvider,
                p.countryPref,
                expiry::record,
                System.currentTimeMillis(),
            )
            val best = ranked.firstOrNull()
            prefs.saveCandidate(best)
            if (best == null) {
                transition(if (needsUser) AutoBotTrigger.USER_REQUIRED else AutoBotTrigger.NO_CANDIDATE)
                syncSchedule(p)
                return true
            }
            notifications.configFound(best.name)
            transition(AutoBotTrigger.VALIDATED)

            val currentKey = p.activeConfigKey.ifBlank { null }
            val currentStatus = expiry.status(currentKey)
            val connected = vpn.connection.value.status == VpnStatus.CONNECTED
            val expiryDriven = p.expiryProtection &&
                currentStatus in setOf(ExpiryStatus.EXPIRED, ExpiryStatus.EXPIRING_SOON, ExpiryStatus.INVALID)
            val shouldReplace = allowReplace && p.fullAuto && (expiryDriven || (!connected && p.autoRenew))
            if (shouldReplace) {
                replace(best, p)
            } else {
                transition(if (connected) AutoBotTrigger.VERIFIED else AutoBotTrigger.VALIDATED)
                if (!p.fullAuto) log("candidate ready — full automation is off")
            }
            syncSchedule(prefs.prefsNow())
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log("cycle failed: ${e.message?.take(160)}")
            transition(AutoBotTrigger.VERIFY_FAILED)
            return false
        } finally {
            runMutex.unlock()
        }
    }

    suspend fun checkExpiry() {
        val p = prefs.prefsNow()
        if (!p.enabled) return
        val key = p.activeConfigKey.ifBlank { return }
        val record = expiry.record(key) ?: return
        val now = System.currentTimeMillis()
        expiry.markChecked(key)
        when (ExpiryCalculator.status(record, now)) {
            ExpiryStatus.EXPIRING_SOON -> {
                val hours = (ExpiryCalculator.remainingMs(record, now) ?: 0L) / 3_600_000
                notifications.expiring(hours)
                log("configuration expiring in ${hours}h")
            }
            ExpiryStatus.EXPIRED -> {
                notifications.expired()
                log("configuration expired")
                runCycle()
            }
            else -> Unit
        }
        syncSchedule(p)
    }

    suspend fun importAssisted(text: String, rawName: String): String? {
        val validation = ConfigValidator.validate(text)
        if (validation is ConfigValidator.Result.Invalid) return validation.reason
        val valid = validation as ConfigValidator.Result.Valid
        val now = System.currentTimeMillis()
        val key = ConfigKeys.of(text)
        expiry.insertAssisted(key, now, now + VpnBookSource.VALIDITY_MS, "7d")
        val candidate = RemoteVpnConfig(
            providerId = "vpnbook",
            name = rawName.ifBlank { "vpnbook" },
            country = CountryClassifier.classify(listOf(rawName, valid.endpoint)),
            protocol = VpnProtocol.WIREGUARD,
            configText = text.trim(),
            endpoint = valid.endpoint,
            generatedAt = now,
            expiresAt = now + VpnBookSource.VALIDITY_MS,
            expirySource = "provider-policy",
        )
        prefs.saveCandidate(candidate)
        notifications.configValidated(candidate.name)
        log("assisted import validated: ${candidate.name}")
        if (_phase.value == AutoBotPhase.OFF) _phase.value = AutoBotPhase.IDLE
        replace(candidate, prefs.prefsNow())
        return null
    }

    private suspend fun replace(candidate: RemoteVpnConfig, p: AutoBotPrefs) {
        transition(AutoBotTrigger.REPLACE)
        notifications.renewalStarted(candidate.name)
        log("replacing with ${candidate.providerId}/${candidate.name}")
        val oldName = vpn.connection.value.activeName
        val oldEntry = vpn.entries.value.firstOrNull { it.name == oldName }
        val oldKey = oldEntry?.let { ConfigKeys.of(it.configText) }
        val oldStatus = expiry.status(oldKey)
        val rollback = if (oldEntry != null &&
            oldStatus != ExpiryStatus.EXPIRED &&
            oldStatus != ExpiryStatus.INVALID
        ) {
            oldEntry
        } else {
            null
        }
        vpn.disconnect().join()
        val entry = try {
            vpn.importConfig(
                candidate.name,
                candidate.configText,
                candidate.providerId,
                if (candidate.providerId == "github") ConfigTrust.COMMUNITY else ConfigTrust.IMPORTED,
            )
        } catch (e: Exception) {
            log("import failed: ${e.message?.take(120)}")
            notifications.renewalFailed(e.message ?: "import failed")
            transition(AutoBotTrigger.VERIFY_FAILED)
            restore(rollback)
            return
        }
        vpn.connect(entry)
        val result = withTimeoutOrNull(45_000) {
            vpn.connection.first {
                it.status == VpnStatus.CONNECTED ||
                    it.status == VpnStatus.ERROR ||
                    it.status == VpnStatus.BLOCKED
            }
        }
        val verified = result?.status == VpnStatus.CONNECTED && connectivityOk()
        if (verified) {
            prefs.saveSchedule(
                activeConfigKey = ConfigKeys.of(candidate.configText),
                lastRenewal = System.currentTimeMillis(),
            )
            transition(AutoBotTrigger.VERIFIED)
            notifications.renewalSuccess(candidate.name)
            log("renewal verified: ${candidate.name}")
            if (oldEntry != null && rollback == null) vpn.deleteEntry(oldEntry.name)
        } else {
            transition(AutoBotTrigger.VERIFY_FAILED)
            notifications.renewalFailed(result?.error ?: "handshake/connectivity verification failed")
            log("renewal failed for ${candidate.name}")
            vpn.deleteEntry(entry.name)
            restore(rollback)
        }
    }

    private suspend fun restore(rollback: VpnConfigEntry?) {
        if (rollback != null) {
            vpn.connect(rollback)
            log("restored previous configuration ${rollback.name}")
        } else if (vpn.killSwitch.value) {
            vpn.engageProtection()
            log("no valid rollback — kill-switch protection engaged")
        }
    }

    private fun connectivityOk(): Boolean = runCatching {
        httpClient.newCall(
            Request.Builder().url("https://www.google.com/generate_204").build(),
        ).execute().use { it.code == 204 }
    }.getOrDefault(false)

    private suspend fun syncSchedule(p: AutoBotPrefs) {
        val now = System.currentTimeMillis()
        val activeRecord = expiry.record(p.activeConfigKey)
        val candidateRecord = candidate.value?.let { expiry.record(ConfigKeys.of(it.configText)) }
        val renewalFromExpiry = listOfNotNull(activeRecord, candidateRecord)
            .mapNotNull { ExpiryCalculator.nextRenewalAt(it, now) }
            .minOrNull()
        val next = renewalFromExpiry
            ?: (now + AutoBotScheduler.DISCOVERY_INTERVAL_HOURS * 3_600_000)
        prefs.saveSchedule(nextRenewal = next)
        AutoBotScheduler.sync(
            context,
            p.enabled,
            next,
            activeRecord?.expiresAt ?: candidateRecord?.expiresAt,
        )
    }

    private fun transition(trigger: AutoBotTrigger) {
        _phase.value = AutoBotStateMachine.next(_phase.value, trigger)
        scope.launch { prefs.savePhase(_phase.value) }
    }

    private fun log(text: String) {
        DebugLog.line("autobot: $text")
        scope.launch { prefs.addActivity(text) }
    }

    private fun classify(e: Throwable): String = when {
        e.message?.contains("rate limit", ignoreCase = true) == true -> "Rate limited"
        e is IOException -> "Network error"
        else -> "Unavailable"
    }
}