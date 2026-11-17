package com.raisoft.grabi.ui.screens.autobot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.vpn.VpnRepository
import com.raisoft.grabi.data.vpn.autobot.AutoBotManager
import com.raisoft.grabi.domain.vpn.ExpiryRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AutoBotViewModel @Inject constructor(
    private val manager: AutoBotManager,
    vpn: VpnRepository,
) : ViewModel() {

    val settings = manager.settings
    val phase = manager.phase
    val activity = manager.activity
    val providerStatuses = manager.providerStatuses
    val candidate = manager.candidate
    val providers = manager.providers
    val connection = vpn.connection
    val entries = vpn.entries

    val activeRecord: StateFlow<ExpiryRecord?> =
        combine(manager.settings, manager.expiryRecords) { prefs, records ->
            records[prefs.activeConfigKey]
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    fun setEnabled(value: Boolean) = manager.setEnabled(value)
    fun setFullAuto(value: Boolean) = manager.setFullAuto(value)
    fun setAutoFallback(value: Boolean) = manager.setAutoFallback(value)
    fun setAutoRenew(value: Boolean) = manager.setAutoRenew(value)
    fun setExpiryProtection(value: Boolean) = manager.setExpiryProtection(value)
    fun setPreferredProvider(value: String) = manager.setPreferredProvider(value)
    fun setCountryPref(value: String) = manager.setCountryPref(value)
    fun retry() = manager.retryNow()

    fun importAssisted(text: String, name: String) {
        viewModelScope.launch {
            _importResult.value = manager.importAssisted(text, name) ?: "ok"
        }
    }

    fun consumeImportResult() {
        _importResult.value = null
    }
}