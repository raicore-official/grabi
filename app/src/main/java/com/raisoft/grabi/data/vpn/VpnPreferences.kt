package com.raisoft.grabi.data.vpn

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.raisoft.grabi.domain.vpn.ConfigTrust
import com.raisoft.grabi.domain.vpn.VpnConfigEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.vpnDataStore by preferencesDataStore(name = "vpn")

@Singleton
class VpnPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val entriesKey = stringPreferencesKey("entries_json")
    private val killSwitchKey = booleanPreferencesKey("kill_switch")
    private val excludedAppsKey = stringSetPreferencesKey("excluded_apps")
    private val lastConnectedKey = stringPreferencesKey("last_connected")
    private val finderKey = stringPreferencesKey("finder_json")
    private val finderUpdatedKey = longPreferencesKey("finder_updated")

    val entries: Flow<List<VpnConfigEntry>> =
        context.vpnDataStore.data.map { decodeEntries(it[entriesKey]) }

    val killSwitch: Flow<Boolean> =
        context.vpnDataStore.data.map { it[killSwitchKey] ?: true }

    val excludedApps: Flow<Set<String>> =
        context.vpnDataStore.data.map { it[excludedAppsKey] ?: emptySet() }

    val lastConnected: Flow<String> =
        context.vpnDataStore.data.map { it[lastConnectedKey] ?: "" }

    val finderCache: Flow<Pair<List<VpnConfigEntry>, Long>> =
        context.vpnDataStore.data.map {
            decodeEntries(it[finderKey]) to (it[finderUpdatedKey] ?: 0L)
        }

    suspend fun addEntry(entry: VpnConfigEntry) {
        context.vpnDataStore.edit { prefs ->
            val current = decodeEntries(prefs[entriesKey])
            prefs[entriesKey] = encodeEntries(current + entry)
        }
    }

    suspend fun removeEntry(name: String) {
        context.vpnDataStore.edit { prefs ->
            val current = decodeEntries(prefs[entriesKey])
            prefs[entriesKey] = encodeEntries(current.filterNot { it.name == name })
        }
    }

    suspend fun setKillSwitch(enabled: Boolean) {
        context.vpnDataStore.edit { it[killSwitchKey] = enabled }
    }

    suspend fun setExcludedApps(apps: Set<String>) {
        context.vpnDataStore.edit { it[excludedAppsKey] = apps }
    }

    suspend fun setLastConnected(name: String) {
        context.vpnDataStore.edit { it[lastConnectedKey] = name }
    }

    suspend fun saveFinder(entries: List<VpnConfigEntry>, timestamp: Long) {
        context.vpnDataStore.edit {
            it[finderKey] = encodeEntries(entries)
            it[finderUpdatedKey] = timestamp
        }
    }

    suspend fun entriesNow(): List<VpnConfigEntry> = entries.first()

    private fun encodeEntries(entries: List<VpnConfigEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("name", entry.name)
                    .put("configText", entry.configText)
                    .put("country", entry.country)
                    .put("source", entry.source)
                    .put("trust", entry.trust.name)
                    .put("addedAt", entry.addedAt)
                    .put("endpoint", entry.endpoint ?: ""),
            )
        }
        return array.toString()
    }

    private fun decodeEntries(json: String?): List<VpnConfigEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                runCatching {
                    val obj = array.getJSONObject(i)
                    VpnConfigEntry(
                        name = obj.getString("name"),
                        configText = obj.getString("configText"),
                        country = obj.optString("country", "OTHER"),
                        source = obj.optString("source", ""),
                        trust = runCatching { ConfigTrust.valueOf(obj.optString("trust")) }
                            .getOrDefault(ConfigTrust.IMPORTED),
                        addedAt = obj.optLong("addedAt"),
                        endpoint = obj.optString("endpoint").ifBlank { null },
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }
}