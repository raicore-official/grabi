package com.raisoft.grabi.data.vpn.autobot

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.raisoft.grabi.domain.vpn.AutoBotPhase
import com.raisoft.grabi.domain.vpn.ExpiryRecord
import com.raisoft.grabi.domain.vpn.RemoteVpnConfig
import com.raisoft.grabi.domain.vpn.VpnProtocol
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.autoBotDataStore by preferencesDataStore(name = "autobot")

data class AutoBotPrefs(
    val enabled: Boolean = false,
    val fullAuto: Boolean = false,
    val autoFallback: Boolean = true,
    val autoRenew: Boolean = true,
    val expiryProtection: Boolean = true,
    val preferredProvider: String = "",
    val countryPref: String = "",
    val providerPriority: List<String> = listOf("github", "vpnbook"),
    val activeConfigKey: String = "",
    val lastDiscovery: Long = 0L,
    val lastRenewal: Long = 0L,
    val nextRenewal: Long = 0L,
)

data class AutoBotActivity(val at: Long, val text: String)

@Singleton
class AutoBotPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val fullAutoKey = booleanPreferencesKey("full_auto")
    private val autoFallbackKey = booleanPreferencesKey("auto_fallback")
    private val autoRenewKey = booleanPreferencesKey("auto_renew")
    private val expiryProtectionKey = booleanPreferencesKey("expiry_protection")
    private val preferredProviderKey = stringPreferencesKey("preferred_provider")
    private val countryPrefKey = stringPreferencesKey("country_pref")
    private val priorityKey = stringPreferencesKey("provider_priority")
    private val activeKey = stringPreferencesKey("active_config_key")
    private val lastDiscoveryKey = longPreferencesKey("last_discovery")
    private val lastRenewalKey = longPreferencesKey("last_renewal")
    private val nextRenewalKey = longPreferencesKey("next_renewal")
    private val phaseKey = stringPreferencesKey("phase")
    private val recordsKey = stringPreferencesKey("expiry_records")
    private val candidateKey = stringPreferencesKey("candidate")
    private val activityKey = stringPreferencesKey("activity")
    private val statusesKey = stringPreferencesKey("provider_statuses")

    val prefs: Flow<AutoBotPrefs> = context.autoBotDataStore.data.map { p ->
        AutoBotPrefs(
            enabled = p[enabledKey] ?: false,
            fullAuto = p[fullAutoKey] ?: false,
            autoFallback = p[autoFallbackKey] ?: true,
            autoRenew = p[autoRenewKey] ?: true,
            expiryProtection = p[expiryProtectionKey] ?: true,
            preferredProvider = p[preferredProviderKey] ?: "",
            countryPref = p[countryPrefKey] ?: "",
            providerPriority = (p[priorityKey] ?: "github,vpnbook").split(",").filter { it.isNotBlank() },
            activeConfigKey = p[activeKey] ?: "",
            lastDiscovery = p[lastDiscoveryKey] ?: 0L,
            lastRenewal = p[lastRenewalKey] ?: 0L,
            nextRenewal = p[nextRenewalKey] ?: 0L,
        )
    }

    val phase: Flow<AutoBotPhase> = context.autoBotDataStore.data.map { p ->
        runCatching { AutoBotPhase.valueOf(p[phaseKey] ?: "OFF") }.getOrDefault(AutoBotPhase.OFF)
    }

    val records: Flow<Map<String, ExpiryRecord>> = context.autoBotDataStore.data.map { p ->
        decodeRecords(p[recordsKey])
    }

    val candidate: Flow<RemoteVpnConfig?> = context.autoBotDataStore.data.map { p ->
        decodeCandidate(p[candidateKey])
    }

    val activity: Flow<List<AutoBotActivity>> = context.autoBotDataStore.data.map { p ->
        decodeActivity(p[activityKey])
    }

    val providerStatuses: Flow<Map<String, String>> = context.autoBotDataStore.data.map { p ->
        decodeStatuses(p[statusesKey])
    }

    suspend fun prefsNow(): AutoBotPrefs = prefs.first()

    suspend fun setEnabled(value: Boolean) = edit { it[enabledKey] = value }
    suspend fun setFullAuto(value: Boolean) = edit { it[fullAutoKey] = value }
    suspend fun setAutoFallback(value: Boolean) = edit { it[autoFallbackKey] = value }
    suspend fun setAutoRenew(value: Boolean) = edit { it[autoRenewKey] = value }
    suspend fun setExpiryProtection(value: Boolean) = edit { it[expiryProtectionKey] = value }
    suspend fun setPreferredProvider(value: String) = edit { it[preferredProviderKey] = value }
    suspend fun setCountryPref(value: String) = edit { it[countryPrefKey] = value }
    suspend fun setProviderPriority(value: List<String>) = edit { it[priorityKey] = value.joinToString(",") }
    suspend fun savePhase(value: AutoBotPhase) = edit { it[phaseKey] = value.name }

    suspend fun saveSchedule(
        lastDiscovery: Long? = null,
        lastRenewal: Long? = null,
        nextRenewal: Long? = null,
        activeConfigKey: String? = null,
    ) = edit { p ->
        lastDiscovery?.let { p[lastDiscoveryKey] = it }
        lastRenewal?.let { p[lastRenewalKey] = it }
        nextRenewal?.let { p[nextRenewalKey] = it }
        activeConfigKey?.let { p[activeKey] = it }
    }

    suspend fun saveCandidate(config: RemoteVpnConfig?) = edit { p ->
        if (config == null) p.remove(candidateKey) else p[candidateKey] = encodeCandidate(config)
    }

    suspend fun saveRecord(record: ExpiryRecord) = edit { p ->
        val map = decodeRecords(p[recordsKey]).toMutableMap()
        map[record.configKey] = record
        p[recordsKey] = encodeRecords(map)
    }

    suspend fun setProviderStatus(id: String, status: String) = edit { p ->
        val obj = runCatching { JSONObject(p[statusesKey] ?: "{}") }.getOrDefault(JSONObject())
        obj.put(id, status)
        p[statusesKey] = obj.toString()
    }

    suspend fun addActivity(text: String) = edit { p ->
        val current = decodeActivity(p[activityKey])
        val updated = (listOf(AutoBotActivity(System.currentTimeMillis(), text)) + current).take(30)
        val array = JSONArray()
        updated.forEach { array.put(JSONObject().put("at", it.at).put("text", it.text)) }
        p[activityKey] = array.toString()
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.autoBotDataStore.edit(block)
    }

    private fun encodeRecords(map: Map<String, ExpiryRecord>): String {
        val obj = JSONObject()
        map.forEach { (key, record) ->
            obj.put(
                key,
                JSONObject()
                    .put("generatedAt", record.generatedAt)
                    .put("expiresAt", record.expiresAt ?: -1L)
                    .put("policy", record.providerExpiryPolicy)
                    .put("source", record.expirySource)
                    .put("lastCheckedAt", record.lastCheckedAt)
                    .put("invalid", record.invalid),
            )
        }
        return obj.toString()
    }

    private fun decodeRecords(json: String?): Map<String, ExpiryRecord> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val map = HashMap<String, ExpiryRecord>()
            for (key in obj.keys()) {
                val r = obj.getJSONObject(key)
                val expiresAt = r.optLong("expiresAt", -1L)
                map[key] = ExpiryRecord(
                    configKey = key,
                    generatedAt = r.optLong("generatedAt"),
                    expiresAt = expiresAt.takeIf { it > 0 },
                    providerExpiryPolicy = r.optString("policy"),
                    expirySource = r.optString("source"),
                    lastCheckedAt = r.optLong("lastCheckedAt"),
                    invalid = r.optBoolean("invalid"),
                )
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun encodeCandidate(config: RemoteVpnConfig): String =
        JSONObject()
            .put("providerId", config.providerId)
            .put("name", config.name)
            .put("country", config.country)
            .put("protocol", config.protocol.name)
            .put("configText", config.configText)
            .put("endpoint", config.endpoint ?: "")
            .put("generatedAt", config.generatedAt)
            .put("expiresAt", config.expiresAt ?: -1L)
            .put("expirySource", config.expirySource)
            .toString()

    private fun decodeCandidate(json: String?): RemoteVpnConfig? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(json)
            val expiresAt = o.optLong("expiresAt", -1L)
            RemoteVpnConfig(
                providerId = o.getString("providerId"),
                name = o.getString("name"),
                country = o.optString("country", "OTHER"),
                protocol = runCatching { VpnProtocol.valueOf(o.optString("protocol")) }
                    .getOrDefault(VpnProtocol.WIREGUARD),
                configText = o.getString("configText"),
                endpoint = o.optString("endpoint").ifBlank { null },
                generatedAt = o.optLong("generatedAt"),
                expiresAt = expiresAt.takeIf { it > 0 },
                expirySource = o.optString("expirySource", "none"),
            )
        }.getOrNull()
    }

    private fun decodeActivity(json: String?): List<AutoBotActivity> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                AutoBotActivity(o.optLong("at"), o.optString("text"))
            }
        }.getOrDefault(emptyList())
    }

    private fun decodeStatuses(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.optString(it) }
        }.getOrDefault(emptyMap())
    }
}