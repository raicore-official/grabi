package com.raisoft.grabi.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.settings.SettingsRepository
import com.raisoft.grabi.data.vpn.VpnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    vpnRepository: VpnRepository,
) : ViewModel() {

    val killSwitch: StateFlow<Boolean> = vpnRepository.killSwitch

    fun setKillSwitch(enabled: Boolean) = vpnRepository.setKillSwitch(enabled)

    fun setDestination(uri: String) {
        viewModelScope.launch { settingsRepository.setDownloadDestination(uri) }
    }

    fun complete() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}