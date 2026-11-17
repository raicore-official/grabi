package com.raisoft.grabi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.browser.TabManager
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.data.download.DownloadManager
import com.raisoft.grabi.data.stream.FeedRepository
import com.raisoft.grabi.data.stream.StreamRepository
import com.raisoft.grabi.domain.model.DownloadStatus
import com.raisoft.grabi.domain.model.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val tabManager: TabManager,
    private val streamRepository: StreamRepository,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    data class FeedState(
        val kiosks: List<String> = emptyList(),
        val kioskId: String? = null,
        val query: String? = null,
        val items: List<FeedItem> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
    )

    private val _feed = MutableStateFlow(FeedState())
    val feed: StateFlow<FeedState> = _feed.asStateFlow()
    private val feedCache = mutableMapOf<String, List<FeedItem>>()

    val downloadStates: StateFlow<Map<String, DownloadEntity>> =
        downloadManager.statesByKey()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun pauseResumeDownload(id: String) {
        val entity = downloadStates.values.firstOrNull { it.id == id } ?: return
        if (entity.status == DownloadStatus.PAUSED.name) {
            downloadManager.resume(id)
        } else {
            downloadManager.pause(id)
        }
    }

    fun deleteDownload(id: String) = downloadManager.delete(id)

    private fun prefetch(items: List<FeedItem>) {
        items.take(6).forEach { streamRepository.prefetch(it.url) }
    }

    init {
        viewModelScope.launch {
            val kiosks = feedRepository.kiosks()
            val default = feedRepository.defaultKiosk() ?: kiosks.firstOrNull()
            _feed.update { it.copy(kiosks = kiosks, kioskId = default) }
            default?.let { loadKiosk(it) }
        }
    }

    fun selectKiosk(id: String) {
        if (_feed.value.kioskId == id && _feed.value.query == null) return
        _feed.update { it.copy(kioskId = id, query = null) }
        loadKiosk(id)
    }

    private fun loadKiosk(id: String) {
        feedCache[id]?.let { cached ->
            _feed.update { it.copy(items = cached, loading = false, error = null) }
            return
        }
        viewModelScope.launch {
            _feed.update { it.copy(loading = true, error = null) }
            runCatching { feedRepository.kioskItems(id) }
                .onSuccess { items ->
                    feedCache[id] = items
                    prefetch(items)
                    _feed.update { it.copy(items = items, loading = false) }
                }
                .onFailure { e ->
                    _feed.update { it.copy(loading = false, error = e.message?.take(200)) }
                }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _feed.update { it.copy(query = query, loading = true, error = null) }
            runCatching { feedRepository.search(query) }
                .onSuccess { items ->
                    prefetch(items)
                    _feed.update { it.copy(items = items, loading = false) }
                }
                .onFailure { e ->
                    _feed.update { it.copy(loading = false, error = e.message?.take(200)) }
                }
        }
    }

    fun clearSearch() {
        val kiosk = _feed.value.kioskId ?: return
        _feed.update { it.copy(query = null) }
        loadKiosk(kiosk)
    }

    fun openSite(url: String) {
        tabManager.openTab(url)
    }
}