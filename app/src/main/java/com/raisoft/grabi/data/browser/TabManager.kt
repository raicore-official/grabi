package com.raisoft.grabi.data.browser

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.net.Uri
import com.raisoft.grabi.domain.model.DetectedMedia
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class TabManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adblock: AdblockEngine,
    private val historyRepository: BrowserHistoryRepository,
) {

    data class Tab(
        val id: String,
        val url: String,
        val title: String,
        val incognito: Boolean,
        val progress: Int,
        val detected: List<DetectedMedia>,
        val canGoBack: Boolean,
        val canGoForward: Boolean,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()
    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()
    private val engines = ConcurrentHashMap<String, WebViewEngine>()
    private var restored = false
    private var themeDark = false
    private var themeBackground: Int? = null
    var onFileChooser: ((WebChromeClient.FileChooserParams) -> Unit)? = null
    var onDownload: ((String) -> Unit)? = null

    val activeTab: StateFlow<Tab?> = combine(_tabs, _activeId) { tabs, id ->
        tabs.firstOrNull { it.id == id }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun restore() {
        if (restored) return
        restored = true
        scope.launch {
            val saved = historyRepository.loadTabs()
            if (saved.isEmpty()) {
                openTab("")
            } else {
                saved.forEach { (url, title) ->
                    _tabs.update { it + newTab(url, title, incognito = false) }
                }
                _activeId.value = _tabs.value.firstOrNull()?.id
            }
            adblock.ensureFresh()
        }
    }

    fun openTab(url: String, incognito: Boolean = false): String {
        if (_tabs.value.size >= MAX_TABS) {
            val victim = _tabs.value.firstOrNull { it.id != _activeId.value } ?: _tabs.value.first()
            closeTab(victim.id)
        }
        val tab = newTab(url, "", incognito)
        _tabs.update { it + tab }
        _activeId.value = tab.id
        persistTabs()
        return tab.id
    }

    fun closeTab(id: String) {
        engines.remove(id)?.destroy()
        val wasIncognito = _tabs.value.firstOrNull { it.id == id }?.incognito == true
        _tabs.update { list -> list.filterNot { it.id == id } }
        if (_activeId.value == id) _activeId.value = _tabs.value.lastOrNull()?.id
        if (wasIncognito && _tabs.value.none { it.incognito }) {
            CookieManager.getInstance().removeAllCookies(null)
        }
        if (_tabs.value.isEmpty()) openTab("")
        persistTabs()
    }

    fun switchTo(id: String) {
        if (_tabs.value.any { it.id == id }) _activeId.value = id
    }

    fun applyThemeToAll(dark: Boolean, backgroundArgb: Int) {
        themeDark = dark
        themeBackground = backgroundArgb
        engines.values.forEach { it.applyTheme(dark, backgroundArgb) }
    }

    /** Must be called on the main thread; creates the WebView lazily per tab. */
    fun engineFor(id: String): BrowserEngine? {
        val tab = _tabs.value.firstOrNull { it.id == id } ?: return null
        return engines.getOrPut(id) {
            WebViewEngine(context, tab.incognito, adblock, listenerFor(id)).also { engine ->
                themeBackground?.let { engine.applyTheme(themeDark, it) }
                if (tab.url.isNotBlank()) engine.loadUrl(tab.url)
            }
        }
    }

    fun loadUrl(url: String) {
        val id = _activeId.value ?: return
        update(id) { it.copy(url = url, title = "", detected = emptyList()) }
        engineFor(id)?.loadUrl(url)
    }

    fun goBack() {
        _activeId.value?.let { engines[it] }?.goBack()
    }

    fun goForward() {
        _activeId.value?.let { engines[it] }?.goForward()
    }

    fun reload() {
        _activeId.value?.let { engines[it] }?.reload()
    }

    fun acceptFileChooser(uris: Array<Uri>?) {
        _activeId.value?.let { engines[it] }?.acceptFileChooser(uris)
    }

    private fun newTab(url: String, title: String, incognito: Boolean) = Tab(
        id = UUID.randomUUID().toString(),
        url = url,
        title = title,
        incognito = incognito,
        progress = 0,
        detected = emptyList(),
        canGoBack = false,
        canGoForward = false,
    )

    private fun listenerFor(id: String) = object : BrowserEngine.Listener {
        override fun onPageStarted(url: String) {
            update(id) { it.copy(url = url, progress = 0) }
        }

        override fun onProgress(progress: Int) {
            update(id) { it.copy(progress = progress) }
        }

        override fun onPageFinished(url: String, title: String) {
            update(id) { it.copy(url = url, title = title, progress = 100) }
            val tab = _tabs.value.firstOrNull { it.id == id }
            if (tab != null && !tab.incognito && url.startsWith("http")) {
                scope.launch { historyRepository.record(url, title.ifBlank { url }) }
            }
            persistTabs()
        }

        override fun onTitle(title: String) {
            update(id) { it.copy(title = title) }
        }

        override fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
            update(id) { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
        }

        override fun onMediaDetected(media: DetectedMedia) {
            update(id) { tab ->
                if (tab.detected.any { it.url == media.url }) {
                    tab
                } else {
                    tab.copy(detected = (tab.detected + media).takeLast(50))
                }
            }
        }

        override fun onFileChooser(params: WebChromeClient.FileChooserParams) {
            if (_activeId.value == id) onFileChooser?.invoke(params)
        }

        override fun onDownload(
            url: String,
            userAgent: String?,
            contentDisposition: String?,
            mimeType: String?,
        ) {
            if (_activeId.value == id) onDownload?.invoke(url)
        }
    }

    private fun update(id: String, transform: (Tab) -> Tab) {
        _tabs.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun persistTabs() {
        val snapshot = _tabs.value.filterNot { it.incognito }.map { it.url to it.title }
        scope.launch { historyRepository.saveTabs(snapshot) }
    }

    companion object {
        private const val MAX_TABS = 8
    }
}