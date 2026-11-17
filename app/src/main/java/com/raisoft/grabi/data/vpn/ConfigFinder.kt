package com.raisoft.grabi.data.vpn

import com.raisoft.grabi.domain.vpn.ConfigTrust
import com.raisoft.grabi.domain.vpn.CountryClassifier
import com.raisoft.grabi.domain.vpn.VpnConfigEntry
import com.raisoft.grabi.domain.vpn.WgConfigUtils
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Singleton
class ConfigFinder @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun refresh(): List<VpnConfigEntry> = withContext(Dispatchers.IO) {
        val repos = LinkedHashMap<String, String>()
        for (query in QUERIES) {
            if (repos.size >= MAX_REPOS) break
            val json = getJson(
                "https://api.github.com/search/repositories?q=${URLEncoder.encode(query, "UTF-8")}" +
                    "&sort=updated&per_page=8",
            )
            val items = json.optJSONArray("items") ?: continue
            for (i in 0 until items.length()) {
                if (repos.size >= MAX_REPOS) break
                val item = items.optJSONObject(i) ?: continue
                if (item.optBoolean("archived")) continue
                val fullName = item.optString("full_name")
                if (fullName.isBlank() || fullName in repos) continue
                repos[fullName] = item.optString("description")
            }
        }
        if (repos.isEmpty()) throw IOException("No source repositories found")

        val results = ArrayList<VpnConfigEntry>()
        val seen = HashSet<String>()
        for (fullName in repos.keys) {
            if (results.size >= MAX_RESULTS) break
            val tree = runCatching {
                getJson("https://api.github.com/repos/$fullName/git/trees/HEAD?recursive=1")
            }.getOrNull() ?: continue
            val blobs = tree.optJSONArray("tree") ?: continue
            val confs = ArrayList<String>()
            for (i in 0 until blobs.length()) {
                val blob = blobs.optJSONObject(i) ?: continue
                val path = blob.optString("path")
                if (blob.optString("type") != "blob" || !path.endsWith(".conf")) continue
                if (path.contains("server", ignoreCase = true)) continue
                confs += path
                if (confs.size >= PER_REPO_CAP) break
            }
            for (path in confs) {
                if (results.size >= MAX_RESULTS) break
                val text = runCatching {
                    getString("https://raw.githubusercontent.com/$fullName/HEAD/$path")
                }.getOrNull() ?: continue
                val config = runCatching { WgConfigUtils.parse(text) }.getOrNull() ?: continue
                if (!WgConfigUtils.isUsableClient(config)) continue
                val endpoint = WgConfigUtils.endpointLabel(config)
                val dedupeKey = "$endpoint|${WgConfigUtils.publicKeyOf(config)}"
                if (!seen.add(dedupeKey)) continue
                val fileName = path.substringAfterLast('/').removeSuffix(".conf")
                val country = CountryClassifier.classify(
                    listOf(fileName, path, fullName.substringAfterLast('/')),
                )
                results += VpnConfigEntry(
                    name = uniqueName(WgConfigUtils.sanitizeName(fileName), results),
                    configText = text.trim(),
                    country = country,
                    source = fullName,
                    trust = ConfigTrust.COMMUNITY,
                    addedAt = System.currentTimeMillis(),
                    endpoint = endpoint,
                )
            }
        }
        results
    }

    suspend fun fetchConfigText(url: String): String = withContext(Dispatchers.IO) {
        getString(url)
    }

    private fun uniqueName(base: String, existing: List<VpnConfigEntry>): String {
        val names = existing.map { it.name }.toSet()
        if (base !in names) return base
        var i = 2
        while (WgConfigUtils.sanitizeName("$base-$i") in names) i++
        return WgConfigUtils.sanitizeName("$base-$i")
    }

    private fun getJson(url: String): JSONObject = JSONObject(getString(url))

    private fun getString(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Grabi-VPN")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val hint = if (response.code == 403 || response.code == 429) {
                    " — GitHub rate limit reached, try again later"
                } else {
                    ""
                }
                throw IOException("Request failed (HTTP ${response.code})$hint")
            }
            return response.body?.string() ?: throw IOException("Empty response")
        }
    }

    private companion object {
        val QUERIES = listOf(
            "wireguard configs",
            "wireguard-configs in:name",
            "free wireguard client",
        )
        const val MAX_REPOS = 6
        const val MAX_RESULTS = 24
        const val PER_REPO_CAP = 6
    }
}