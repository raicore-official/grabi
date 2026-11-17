package com.raisoft.grabi.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.browser.AdblockEngine
import com.raisoft.grabi.data.download.YtDlpEngine
import com.raisoft.grabi.data.settings.SettingsRepository
import com.raisoft.grabi.data.settings.ThemeMode
import com.raisoft.grabi.data.settings.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val engine: YtDlpEngine,
    private val adblock: AdblockEngine,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
    )

    private val _engineVersion = MutableStateFlow<String?>(null)
    val engineVersion: StateFlow<String?> = _engineVersion.asStateFlow()

    private val _adblockUpdating = MutableStateFlow(false)
    val adblockUpdating: StateFlow<Boolean> = _adblockUpdating.asStateFlow()

    init {
        refreshEngineVersion()
    }

    fun refreshEngineVersion() {
        _engineVersion.value = engine.binaryVersion()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setDownloadParallelism(value: Int) {
        viewModelScope.launch { settingsRepository.setDownloadParallelism(value) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWifiOnly(enabled) }
    }

    fun setAdblockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAdblockEnabled(enabled) }
    }

    fun updateAdblock() {
        viewModelScope.launch {
            _adblockUpdating.value = true
            runCatching { adblock.update() }
            _adblockUpdating.value = false
        }
    }
}