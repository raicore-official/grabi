package com.raisoft.grabi.ui.screens.vpn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raisoft.grabi.data.vpn.VpnRepository
import com.raisoft.grabi.data.vpn.autobot.ConfigExpiryManager
import com.raisoft.grabi.domain.vpn.ConfigKeys
import com.raisoft.grabi.domain.vpn.ConfigTrust
import com.raisoft.grabi.domain.vpn.VpnConfigEntry
import com.raisoft.grabi.domain.vpn.ExpiryRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(val packageName: String, val label: String)

@HiltViewModel
class VpnViewModel @Inject constructor(
    private val repository: VpnRepository,
    private val expiryManager: ConfigExpiryManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val connection = repository.connection
    val entries = repository.entries
    val killSwitch = repository.killSwitch
    val excludedApps = repository.excludedApps
    val finderResults = repository.finderResults
    val finderBusy = repository.finderBusy
    val finderError = repository.finderError
    val finderUpdated = repository.finderUpdated
    val systemVpn = repository.systemVpn
    val expiryRecords = expiryManager.records

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    fun vpnConsentIntent(): Intent? = VpnService.prepare(context)

    fun connect(entry: VpnConfigEntry) = repository.connect(entry)

    fun disconnect() = repository.disconnect()

    fun setKillSwitch(enabled: Boolean) = repository.setKillSwitch(enabled)

    fun deleteEntry(name: String) = repository.deleteEntry(name)

    fun refreshFinder() = repository.refreshFinder()

    fun saveFound(entry: VpnConfigEntry) = repository.saveFound(entry)

    fun refreshSystemVpn() = repository.refreshSystemVpn()

    fun clearImportError() {
        _importError.value = null
    }

    fun importText(name: String, text: String) {
        viewModelScope.launch {
            runCatching {
                repository.importConfig(name.ifBlank { "imported" }, text, "Imported", ConfigTrust.IMPORTED)
            }.onFailure { _importError.value = it.message ?: "import_failed" }
        }
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IOException("Could not read file")
                }
                val rawName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".conf")
                    ?: "imported"
                repository.importConfig(rawName, text, "Imported", ConfigTrust.IMPORTED)
            }.onFailure { _importError.value = it.message ?: "import_failed" }
        }
    }

    fun importUrl(url: String) {
        viewModelScope.launch {
            runCatching { repository.importFromUrl(url.trim()) }
                .onFailure { _importError.value = it.message ?: "import_failed" }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                pm.getInstalledApplications(0)
                    .filter {
                        it.packageName != context.packageName &&
                            pm.getLaunchIntentForPackage(it.packageName) != null
                    }
                    .map { AppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
                    .sortedBy { it.label.lowercase() }
            }
            _installedApps.value = apps
        }
    }

    fun setExcludedApps(apps: Set<String>) = repository.setExcludedApps(apps)

    fun expiryFor(entry: VpnConfigEntry?): ExpiryRecord? =
        entry?.let { expiryRecords.value[ConfigKeys.of(it.configText)] }
}