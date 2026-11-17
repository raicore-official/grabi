package com.raisoft.grabi.ui.screens.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.webkit.WebChromeClient
import com.raisoft.grabi.data.browser.AdblockEngine
import com.raisoft.grabi.data.browser.BrowserEngine
import com.raisoft.grabi.data.browser.BrowserHistoryRepository
import com.raisoft.grabi.data.browser.TabManager
import com.raisoft.grabi.data.download.DownloadManager
import com.raisoft.grabi.data.settings.SettingsRepository
import com.raisoft.grabi.data.settings.UserSettings
import com.raisoft.grabi.domain.LinkValidator
import com.raisoft.grabi.domain.MediaFormatMapper
import com.raisoft.grabi.domain.model.DetectedMedia
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.domain.model.MediaOption
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URI
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabManager: TabManager,
    private val adblock: AdblockEngine,
    private val downloadManager: DownloadManager,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: BrowserHistoryRepository,
) : ViewModel() {

    val tabs = tabManager.tabs
    val activeTab = tabManager.activeTab

    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings(),
    )

    val history = historyRepository.history.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _adblockUpdating = MutableStateFlow(false)
    val adblockUpdating = _adblockUpdating.asStateFlow()

    init {
        tabManager.restore()
    }

    fun openInitialUrl(url: String) {
        if (url.isNotBlank() && activeTab.value?.url != url) tabManager.loadUrl(url)
    }

    fun engineFor(id: String): BrowserEngine? = tabManager.engineFor(id)

    fun openTab(incognito: Boolean) {
        tabManager.openTab("", incognito)
    }

    fun closeTab(id: String) = tabManager.closeTab(id)

    fun switchTo(id: String) = tabManager.switchTo(id)

    fun goBack() = tabManager.goBack()

    fun goForward() = tabManager.goForward()

    fun reload() = tabManager.reload()

    fun setFileChooserHandler(handler: ((WebChromeClient.FileChooserParams) -> Unit)?) {
        tabManager.onFileChooser = handler
    }

    fun setDownloadHandler(handler: ((String) -> Unit)?) {
        tabManager.onDownload = handler
    }

    fun acceptFileChooser(uris: Array<Uri>?) = tabManager.acceptFileChooser(uris)

    fun submitUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val url = when {
            LinkValidator.isValidUrl(trimmed) -> trimmed
            !trimmed.contains(' ') && trimmed.contains('.') -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=" + URLEncoder.encode(trimmed, "UTF-8")
        }
        tabManager.loadUrl(url)
    }

    fun downloadDetected(media: DetectedMedia) {
        val title = media.url.substringBefore('?').substringAfterLast('/').ifBlank { "media" }
        val option = when (media.kind) {
            MediaKind.IMAGE ->
                MediaOption("image", "Image", MediaKind.IMAGE, media.url, null, media.sizeBytes)
            MediaKind.VIDEO ->
                MediaOption(
                    "best", "Video", MediaKind.VIDEO,
                    MediaFormatMapper.AVC_VIDEO_SELECTOR, null, null,
                )
            MediaKind.AUDIO ->
                MediaOption("audio", "Audio", MediaKind.AUDIO, "bestaudio/best", "m4a", null)
        }
        viewModelScope.launch { downloadManager.enqueue(media.url, title, option, false) }
    }

    val downloadStates: kotlinx.coroutines.flow.StateFlow<Map<String, com.raisoft.grabi.data.db.DownloadEntity>> =
        downloadManager.statesByKey()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun pauseResumeDownload(id: String) {
        val entity = downloadStates.value.values.firstOrNull { it.id == id } ?: return
        if (entity.status == com.raisoft.grabi.domain.model.DownloadStatus.PAUSED.name) {
            downloadManager.resume(id)
        } else {
            downloadManager.pause(id)
        }
    }

    fun deleteDownload(id: String) = downloadManager.delete(id)

    fun applyTheme(dark: Boolean, backgroundArgb: Int) {
        tabManager.applyThemeToAll(dark, backgroundArgb)
    }

    fun toggleSiteAllowlist() {
        val host = activeTab.value?.url
            ?.let { runCatching { URI(it).host }.getOrNull() }
            ?.lowercase()
            ?: return
        viewModelScope.launch { settingsRepository.toggleAdblockAllowlist(host) }
    }

    fun updateAdblock() {
        viewModelScope.launch {
            _adblockUpdating.value = true
            runCatching { adblock.update() }
            _adblockUpdating.value = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearHistory() }
    }
}