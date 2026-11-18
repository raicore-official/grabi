package com.raisoft.grabi.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun setDestination(uri: String) {
        viewModelScope.launch { settingsRepository.setDownloadDestination(uri) }
    }

    fun complete() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}