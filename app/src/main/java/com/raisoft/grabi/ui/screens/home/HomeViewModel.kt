package com.raisoft.grabi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.browser.TabManager
import com.raisoft.grabi.data.stream.FeedRepository
import com.raisoft.grabi.domain.model.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val tabManager: TabManager,
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
                .onSuccess { items -> _feed.update { it.copy(items = items, loading = false) } }
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