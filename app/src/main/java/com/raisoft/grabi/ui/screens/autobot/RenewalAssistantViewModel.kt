package com.raisoft.grabi.ui.screens.autobot

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.vpn.VpnRepository
import com.raisoft.grabi.data.vpn.autobot.ProviderRegistry
import com.raisoft.grabi.data.vpn.autobot.AutoBotManager
import com.raisoft.grabi.data.vpn.autobot.VpnProvider
import com.raisoft.grabi.domain.vpn.ConfigKeys
import com.raisoft.grabi.domain.vpn.ConfigTrust
import com.raisoft.grabi.domain.vpn.ConfigValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class RenewalAssistantViewModel @Inject constructor(
    private val repository: VpnRepository,
    private val registry: ProviderRegistry,
    manager: AutoBotManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val activeExpiry = combine(manager.settings, manager.expiryRecords) { settings, records ->
        records[settings.activeConfigKey]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _importedName = MutableStateFlow<String?>(null)
    val importedName: StateFlow<String?> = _importedName.asStateFlow()

    fun provider(id: String): VpnProvider? = registry.provider(id)

    fun importFile(uri: Uri, providerId: String) {
        viewModelScope.launch {
            runCatching {
                val (text, name) = withContext(Dispatchers.IO) {
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "renewed"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IOException("Could not read configuration")
                    if (bytes.size > MAX_CONFIG_BYTES) throw IOException("Configuration is too large")
                    bytes.toString(Charsets.UTF_8) to fileName.removeSuffix(".conf")
                }
                val validation = ConfigValidator.validate(text)
                if (validation is ConfigValidator.Result.Invalid) {
                    throw IllegalArgumentException(validation.reason)
                }
                val key = ConfigKeys.of(text)
                if (repository.entries.value.any { ConfigKeys.of(it.configText) == key }) {
                    throw IllegalArgumentException("This configuration is already imported")
                }
                repository.importConfig(
                    name,
                    text,
                    provider(providerId)?.name ?: providerId,
                    ConfigTrust.IMPORTED,
                )
            }.onSuccess { entry ->
                _importedName.value = entry.name
                _message.value = "Configuration imported. Review it in VPN settings before connecting."
            }.onFailure { error ->
                _message.value = error.message ?: "Configuration import failed"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        private const val MAX_CONFIG_BYTES = 256 * 1024
    }
}
