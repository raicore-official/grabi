package com.raisoft.grabi.data.browser

import android.content.Context
import com.raisoft.grabi.data.settings.SettingsRepository
import com.raisoft.grabi.domain.browser.FilterListParser
import com.raisoft.grabi.domain.browser.FilterRules
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class AdblockEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    @Volatile private var rules: FilterRules = FilterRules.EMPTY
    @Volatile private var enabled = true
    @Volatile private var allowlist: Set<String> = emptySet()

    private val loaded = AtomicBoolean(false)
    private val updateMutex = Mutex()
    private val dir = File(context.filesDir, "adblock")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        scope.launch {
            settingsRepository.settings.collect { settings ->
                enabled = settings.adblockEnabled
                allowlist = settings.adblockAllowlist
            }
        }
    }

    fun shouldBlock(url: String, pageUrl: String): Boolean {
        if (!enabled || rules === FilterRules.EMPTY) return false
        val host = hostOf(url) ?: return false
        if (allowlist.any { host == it || host.endsWith(".$it") }) return false
        val path = runCatching { URI(url).path }.getOrNull().orEmpty()
        return rules.isBlocked(url, host, path)
    }

    suspend fun ensureFresh() = withContext(Dispatchers.IO) {
        ensureLoaded()
        val lastUpdated = settingsRepository.settings.first().adblockLastUpdated
        if (System.currentTimeMillis() - lastUpdated > MAX_AGE_MS) {
            runCatching { update() }
        }
    }

    suspend fun update() = withContext(Dispatchers.IO) {
        updateMutex.withLock {
            dir.mkdirs()
            val lines = SOURCES.map { (name, url) ->
                val text = fetch(url)
                File(dir, name).writeText(text)
                text.lineSequence()
            }.reduce { acc, seq -> acc + seq }
            rules = FilterListParser.parse(lines)
            loaded.set(true)
            settingsRepository.setAdblockLastUpdated(System.currentTimeMillis())
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded.get()) return
        updateMutex.withLock {
            if (loaded.get()) return
            val files = dir.listFiles()?.filter { it.extension == "txt" }.orEmpty()
            if (files.isNotEmpty()) {
                rules = FilterListParser.parse(files.asSequence().flatMap { it.readLines().asSequence() })
            }
            loaded.set(true)
        }
    }

    private fun fetch(url: String): String {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            return response.body?.string() ?: throw IOException("Empty filter list")
        }
    }

    private fun hostOf(url: String): String? =
        runCatching { URI(url).host }.getOrNull()?.lowercase()

    companion object {
        private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
        private val SOURCES = listOf(
            "easylist.txt" to "https://easylist.to/easylist/easylist.txt",
            "easyprivacy.txt" to "https://easylist.to/easylist/easyprivacy.txt",
            "ublock-filters.txt" to "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
        )
    }
}