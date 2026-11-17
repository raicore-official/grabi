package com.raisoft.grabi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.download.DownloadController
import com.raisoft.grabi.data.download.DownloadJob
import com.raisoft.grabi.data.download.PendingLink
import com.raisoft.grabi.data.download.YtDlpEngine
import com.raisoft.grabi.domain.LinkValidator
import com.raisoft.grabi.domain.model.MediaInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val engine: YtDlpEngine,
    private val controller: DownloadController,
    private val pendingLink: PendingLink,
) : ViewModel() {

    data class UiState(
        val link: String = "",
        val linkInvalid: Boolean = false,
        val fetching: Boolean = false,
        val info: MediaInfo? = null,
        val selectedOptionId: String? = null,
        val subtitles: Boolean = false,
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()
    val download: StateFlow<DownloadJob?> = controller.state

    init {
        viewModelScope.launch {
            pendingLink.url.collect { shared ->
                if (shared != null) {
                    pendingLink.consume()
                    _ui.update { it.copy(link = shared, linkInvalid = false) }
                    fetch(shared)
                }
            }
        }
    }

    fun onLinkChange(value: String) {
        _ui.update { it.copy(link = value, linkInvalid = false) }
    }

    fun fetch(urlOverride: String? = null) {
        if (_ui.value.fetching) return
        val raw = urlOverride ?: _ui.value.link
        val candidate = LinkValidator.extractUrl(raw) ?: raw.trim()
        if (!LinkValidator.isValidUrl(candidate)) {
            _ui.update { it.copy(linkInvalid = true) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(fetching = true, error = null, info = null, selectedOptionId = null) }
            try {
                val info = engine.fetchInfo(candidate)
                _ui.update {
                    it.copy(
                        fetching = false,
                        info = info,
                        selectedOptionId = info.options.firstOrNull()?.id,
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(fetching = false, error = e.message?.take(400) ?: "Failed to fetch video info")
                }
            }
        }
    }

    fun selectOption(id: String) {
        _ui.update { it.copy(selectedOptionId = id) }
    }

    fun setSubtitles(enabled: Boolean) {
        _ui.update { it.copy(subtitles = enabled) }
    }

    fun startDownload() {
        val state = _ui.value
        val info = state.info ?: return
        val option = info.options.firstOrNull { it.id == state.selectedOptionId } ?: return
        controller.start(info.webpageUrl ?: state.link, info.title, option, state.subtitles)
    }

    fun cancelDownload() = controller.cancel()

    fun dismissDownload() = controller.dismiss()

    fun consumeError() {
        _ui.update { it.copy(error = null) }
    }
}