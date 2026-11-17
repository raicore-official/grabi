package com.raisoft.grabi.data.browser

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.browserDataStore by preferencesDataStore(name = "browser")

@Singleton
class BrowserHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Entry(val url: String, val title: String, val visitedAt: Long)

    private val historyKey = stringPreferencesKey("history_json")
    private val tabsKey = stringPreferencesKey("tabs_json")

    val history: Flow<List<Entry>> = context.browserDataStore.data.map { prefs ->
        parseEntries(prefs[historyKey])
    }

    suspend fun record(url: String, title: String) {
        context.browserDataStore.edit { prefs ->
            val entries = parseEntries(prefs[historyKey]).filterNot { it.url == url }
            val updated = (listOf(Entry(url, title, System.currentTimeMillis())) + entries).take(200)
            prefs[historyKey] = toJson(updated)
        }
    }

    suspend fun clearHistory() {
        context.browserDataStore.edit { it.remove(historyKey) }
    }

    suspend fun saveTabs(tabs: List<Pair<String, String>>) {
        context.browserDataStore.edit { prefs ->
            val array = JSONArray()
            tabs.take(8).forEach { (url, title) ->
                array.put(JSONObject().put("url", url).put("title", title))
            }
            prefs[tabsKey] = array.toString()
        }
    }

    suspend fun loadTabs(): List<Pair<String, String>> {
        val json = context.browserDataStore.data.first()[tabsKey] ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                obj.getString("url") to obj.optString("title")
            }
        }.getOrDefault(emptyList())
    }

    private fun parseEntries(json: String?): List<Entry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Entry(obj.getString("url"), obj.optString("title"), obj.optLong("visitedAt"))
            }
        }.getOrDefault(emptyList())
    }

    private fun toJson(entries: List<Entry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("url", entry.url)
                    .put("title", entry.title)
                    .put("visitedAt", entry.visitedAt),
            )
        }
        return array.toString()
    }
}