package com.raisoft.grabi.ui.screens.vpn

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raisoft.grabi.R
import com.raisoft.grabi.domain.vpn.ConfigTrust
import com.raisoft.grabi.domain.vpn.CountryClassifier
import com.raisoft.grabi.domain.vpn.VpnConfigEntry
import com.raisoft.grabi.domain.vpn.VpnStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VpnScreen(
    onBack: () -> Unit,
    onOpenRenewal: (String) -> Unit = {},
    viewModel: VpnViewModel = hiltViewModel(),
) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val killSwitch by viewModel.killSwitch.collectAsStateWithLifecycle()
    val excludedApps by viewModel.excludedApps.collectAsStateWithLifecycle()
    val finderResults by viewModel.finderResults.collectAsStateWithLifecycle()
    val finderBusy by viewModel.finderBusy.collectAsStateWithLifecycle()
    val finderError by viewModel.finderError.collectAsStateWithLifecycle()
    val finderUpdated by viewModel.finderUpdated.collectAsStateWithLifecycle()
    val systemVpn by viewModel.systemVpn.collectAsStateWithLifecycle()
    val expiryRecords by viewModel.expiryRecords.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingCommunity by remember { mutableStateOf<VpnConfigEntry?>(null) }
    var consentEntry by remember { mutableStateOf<VpnConfigEntry?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showAppsDialog by remember { mutableStateOf(false) }
    var countryFilter by remember { mutableStateOf("ALL") }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val entry = consentEntry
        consentEntry = null
        if (result.resultCode == Activity.RESULT_OK && entry != null) viewModel.connect(entry)
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importFile(uri) }

    LaunchedEffect(Unit) { viewModel.refreshSystemVpn() }

    fun doConnect(entry: VpnConfigEntry) {
        val intent = viewModel.vpnConsentIntent()
        if (intent != null) {
            consentEntry = entry
            consentLauncher.launch(intent)
        } else {
            viewModel.connect(entry)
        }
    }

    fun requestConnect(entry: VpnConfigEntry) {
        if (entry.trust == ConfigTrust.COMMUNITY) pendingCommunity = entry else doConnect(entry)
    }

    val statusLabel = when (connection.status) {
        VpnStatus.IDLE -> stringResource(R.string.vpn_status_idle)
        VpnStatus.CONNECTING -> stringResource(R.string.vpn_status_connecting)
        VpnStatus.VERIFYING -> stringResource(R.string.vpn_status_verifying)
        VpnStatus.CONNECTED -> stringResource(R.string.vpn_status_connected)
        VpnStatus.DISCONNECTING -> stringResource(R.string.vpn_status_disconnecting)
        VpnStatus.BLOCKED -> stringResource(R.string.vpn_status_blocked)
        VpnStatus.ERROR -> stringResource(R.string.vpn_status_error)
    }
    val activeEntry = entries.find { it.name == connection.activeName }
    val activeExpiry = activeEntry?.let { expiryRecords[com.raisoft.grabi.domain.vpn.ConfigKeys.of(it.configText)] }
    val busy = connection.status == VpnStatus.CONNECTING || connection.status == VpnStatus.VERIFYING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.vpn_back),
                )
            }
            Text(
                text = stringResource(R.string.vpn_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = statusLabel, style = MaterialTheme.typography.titleLarge)
                val tunnelActive = connection.status == VpnStatus.CONNECTED ||
                    connection.status == VpnStatus.CONNECTING ||
                    connection.status == VpnStatus.VERIFYING
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (tunnelActive) "VPN is on" else "VPN is off",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = tunnelActive,
                        onCheckedChange = {
                            if (it && activeEntry != null) requestConnect(activeEntry)
                            else if (!it) viewModel.disconnect()
                        },
                        enabled = activeEntry != null && !busy,
                    )
                }
                if (connection.activeName != null) {
                    val country = connection.activeCountry?.let { CountryClassifier.country(it).name }
                    Text(
                        text = listOfNotNull(connection.activeName, country).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (connection.status == VpnStatus.CONNECTED) {
                    Text(
                        text = stringResource(
                            R.string.vpn_traffic,
                            Formatter.formatFileSize(context, connection.rxBytes),
                            Formatter.formatFileSize(context, connection.txBytes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    connection.handshakeAgeSeconds?.let {
                        Text(
                            text = stringResource(R.string.vpn_handshake, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                connection.error?.let { error ->
                    Text(
                        text = when (error) {
                            "no_handshake" -> stringResource(R.string.vpn_error_no_handshake)
                            "dropped" -> stringResource(R.string.vpn_error_dropped)
                            else -> error
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (connection.status == VpnStatus.BLOCKED) {
                    Text(
                        text = stringResource(R.string.vpn_blocked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when (connection.status) {
                    VpnStatus.CONNECTED, VpnStatus.CONNECTING, VpnStatus.VERIFYING -> {
                        Button(onClick = viewModel::disconnect) {
                            Text(stringResource(R.string.vpn_disconnect))
                        }
                    }
                    VpnStatus.BLOCKED, VpnStatus.ERROR -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (activeEntry != null) {
                                Button(onClick = { requestConnect(activeEntry) }) {
                                    Text(
                                        stringResource(
                                            if (connection.status == VpnStatus.BLOCKED) {
                                                R.string.vpn_reconnect
                                            } else {
                                                R.string.vpn_retry
                                            },
                                        ),
                                    )
                                }
                            }
                            TextButton(onClick = viewModel::disconnect) {
                                Text(stringResource(R.string.vpn_disconnect))
                            }
                        }
                    }
                    VpnStatus.IDLE -> {
                        Text(
                            text = stringResource(R.string.vpn_idle_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    VpnStatus.DISCONNECTING -> Unit
                }
                if (activeEntry != null && activeExpiry != null) {
                    val remaining = com.raisoft.grabi.domain.vpn.ExpiryCalculator.remainingMs(
                        activeExpiry,
                        System.currentTimeMillis(),
                    )
                    Text(
                        text = "Configuration expiry: ${
                            remaining?.let { formatRemaining(it) } ?: "unknown"
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (remaining != null &&
                        remaining <= com.raisoft.grabi.domain.vpn.ExpiryCalculator.EXPIRING_SOON_MS
                    ) {
                        Text(
                            text = "This configuration is nearing expiry.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onOpenRenewal(activeEntry.source.lowercase().ifBlank { "vpnbook" })
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Regenerate configuration")
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.vpn_protection),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vpn_kill_switch),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.vpn_kill_switch_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = killSwitch, onCheckedChange = viewModel::setKillSwitch)
                }
                Text(
                    text = stringResource(
                        R.string.vpn_system_status,
                        stringResource(if (systemVpn.alwaysOn) R.string.vpn_on else R.string.vpn_off),
                        stringResource(if (systemVpn.lockdown) R.string.vpn_on else R.string.vpn_off),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                        }
                    },
                ) {
                    Text(stringResource(R.string.vpn_system_settings))
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.vpn_my_configs),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (entries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.vpn_configs_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                entries.forEach { entry ->
                    ConfigRow(
                        entry = entry,
                        busy = busy && connection.activeName == entry.name,
                        onConnect = { requestConnect(entry) },
                        onDelete = { viewModel.deleteEntry(entry.name) },
                    )
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.vpn_import),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showPasteDialog = true }) {
                        Text(stringResource(R.string.vpn_import_paste))
                    }
                    OutlinedButton(onClick = { fileLauncher.launch(arrayOf("*/*")) }) {
                        Text(stringResource(R.string.vpn_import_file))
                    }
                    OutlinedButton(onClick = { showUrlDialog = true }) {
                        Text(stringResource(R.string.vpn_import_url))
                    }
                }
                Text(
                    text = stringResource(R.string.vpn_import_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.vpn_find),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::refreshFinder, enabled = !finderBusy) {
                        Text(stringResource(R.string.vpn_finder_refresh))
                    }
                }
                if (finderBusy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (finderUpdated > 0L) {
                    Text(
                        text = stringResource(R.string.vpn_finder_updated, ageLabel(finderUpdated)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                finderError?.let { error ->
                    Text(
                        text = if (error == "empty") {
                            stringResource(R.string.vpn_finder_empty)
                        } else {
                            error
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = stringResource(R.string.vpn_finder_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (finderResults.isNotEmpty()) {
                    val countries = finderResults.map { it.country }.distinct()
                        .sortedBy { code ->
                            val index = CountryClassifier.COMMON.indexOfFirst { it.code == code }
                            if (index >= 0) index else CountryClassifier.COMMON.size
                        }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = countryFilter == "ALL",
                            onClick = { countryFilter = "ALL" },
                            label = { Text(stringResource(R.string.vpn_finder_all)) },
                        )
                        countries.forEach { code ->
                            FilterChip(
                                selected = countryFilter == code,
                                onClick = { countryFilter = code },
                                label = { Text(CountryClassifier.country(code).name) },
                            )
                        }
                    }
                    val filtered = if (countryFilter == "ALL") {
                        finderResults
                    } else {
                        finderResults.filter { it.country == countryFilter }
                    }
                    if (filtered.isEmpty()) {
                        Text(
                            text = stringResource(R.string.vpn_finder_no_country),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    filtered.forEach { entry ->
                        FoundRow(
                            entry = entry,
                            onSave = { viewModel.saveFound(entry) },
                            onConnect = { requestConnect(entry) },
                        )
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.vpn_split_tunnel),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.vpn_split_tunnel_summary, excludedApps.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        viewModel.loadInstalledApps()
                        showAppsDialog = true
                    },
                ) {
                    Text(stringResource(R.string.vpn_split_tunnel_edit))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    pendingCommunity?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingCommunity = null },
            title = { Text(stringResource(R.string.vpn_trust_dialog_title)) },
            text = { Text(stringResource(R.string.vpn_trust_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingCommunity = null
                        doConnect(entry)
                    },
                ) {
                    Text(stringResource(R.string.vpn_trust_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCommunity = null }) {
                    Text(stringResource(R.string.vpn_cancel))
                }
            },
        )
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearImportError,
            title = { Text(stringResource(R.string.vpn_import_error_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearImportError) {
                    Text(stringResource(R.string.vpn_ok))
                }
            },
        )
    }

    if (showPasteDialog) {
        var name by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text(stringResource(R.string.vpn_import_paste)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.vpn_import_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.vpn_import_config_text)) },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPasteDialog = false
                        viewModel.importText(name, text)
                    },
                    enabled = text.isNotBlank(),
                ) {
                    Text(stringResource(R.string.vpn_import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text(stringResource(R.string.vpn_cancel))
                }
            },
        )
    }

    if (showUrlDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text(stringResource(R.string.vpn_import_url)) },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.vpn_import_url_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUrlDialog = false
                        viewModel.importUrl(url)
                    },
                    enabled = url.isNotBlank(),
                ) {
                    Text(stringResource(R.string.vpn_import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text(stringResource(R.string.vpn_cancel))
                }
            },
        )
    }

    if (showAppsDialog) {
        var draft by remember { mutableStateOf(excludedApps) }
        AlertDialog(
            onDismissRequest = { showAppsDialog = false },
            title = { Text(stringResource(R.string.vpn_split_tunnel)) },
            text = {
                Column(Modifier.heightIn(max = 400.dp)) {
                    Text(
                        text = stringResource(R.string.vpn_split_tunnel_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn {
                        items(installedApps, key = { it.packageName }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Checkbox(
                                    checked = app.packageName in draft,
                                    onCheckedChange = { checked ->
                                        draft = if (checked) {
                                            draft + app.packageName
                                        } else {
                                            draft - app.packageName
                                        }
                                    },
                                )
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAppsDialog = false
                        viewModel.setExcludedApps(draft)
                    },
                ) {
                    Text(stringResource(R.string.vpn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppsDialog = false }) {
                    Text(stringResource(R.string.vpn_cancel))
                }
            },
        )
    }
}

@Composable
private fun ConfigRow(
    entry: VpnConfigEntry,
    busy: Boolean,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = entry.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = listOfNotNull(
                    CountryClassifier.country(entry.country).name,
                    entry.endpoint,
                    entry.source,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    if (entry.trust == ConfigTrust.COMMUNITY) {
                        R.string.vpn_trust_community
                    } else {
                        R.string.vpn_trust_imported
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onConnect, enabled = !busy) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.vpn_connect),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.vpn_delete),
            )
        }
    }
}

@Composable
private fun FoundRow(entry: VpnConfigEntry, onSave: () -> Unit, onConnect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = entry.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = listOfNotNull(
                    CountryClassifier.country(entry.country).name,
                    entry.endpoint,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onSave) {
            Text(stringResource(R.string.vpn_save))
        }
        IconButton(onClick = onConnect) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.vpn_connect),
            )
        }
    }
}

@Composable
private fun ageLabel(timestamp: Long): String {
    val hours = (System.currentTimeMillis() - timestamp) / 3_600_000
    return when {
        hours < 1 -> stringResource(R.string.vpn_age_now)
        hours < 24 -> stringResource(R.string.vpn_age_hours, hours.toInt())
        else -> stringResource(R.string.vpn_age_days, (hours / 24).toInt())
    }
}

private fun formatRemaining(ms: Long): String {
    val hours = ms / 3_600_000
    val minutes = (ms / 60_000) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}