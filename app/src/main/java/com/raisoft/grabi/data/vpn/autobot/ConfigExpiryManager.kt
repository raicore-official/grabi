package com.raisoft.grabi.data.vpn.autobot

import com.raisoft.grabi.domain.vpn.ExpiryCalculator
import com.raisoft.grabi.domain.vpn.ExpiryRecord
import com.raisoft.grabi.domain.vpn.ExpiryStatus
import com.raisoft.grabi.domain.vpn.RemoteVpnConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class ConfigExpiryManager @Inject constructor(
    private val prefs: AutoBotPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val records: StateFlow<Map<String, ExpiryRecord>> =
        prefs.records.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun record(key: String): ExpiryRecord? = records.value[key]

    fun status(key: String?, now: Long = System.currentTimeMillis()): ExpiryStatus =
        ExpiryCalculator.status(key?.let { records.value[it] }, now)

    suspend fun touchOrInsert(key: String, config: RemoteVpnConfig) {
        val now = System.currentTimeMillis()
        val incoming = ExpiryRecord(
            configKey = key,
            generatedAt = config.generatedAt.takeIf { it > 0 } ?: now,
            expiresAt = config.expiresAt,
            providerExpiryPolicy = if (config.expiresAt != null) "provider" else "",
            expirySource = config.expirySource,
            lastCheckedAt = now,
        )
        prefs.saveRecord(ExpiryCalculator.merge(records.value[key], incoming))
    }

    suspend fun insertAssisted(key: String, generatedAt: Long, expiresAt: Long, policy: String) {
        val incoming = ExpiryRecord(
            configKey = key,
            generatedAt = generatedAt,
            expiresAt = expiresAt,
            providerExpiryPolicy = policy,
            expirySource = "provider-policy",
            lastCheckedAt = System.currentTimeMillis(),
        )
        prefs.saveRecord(ExpiryCalculator.merge(records.value[key], incoming))
    }

    suspend fun markChecked(key: String) {
        records.value[key]?.let { prefs.saveRecord(it.copy(lastCheckedAt = System.currentTimeMillis())) }
    }
}