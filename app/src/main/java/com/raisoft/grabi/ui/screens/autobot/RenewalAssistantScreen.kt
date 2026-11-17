package com.raisoft.grabi.ui.screens.autobot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raisoft.grabi.data.vpn.autobot.GenerationMode
import com.raisoft.grabi.domain.vpn.ExpiryCalculator
import com.raisoft.grabi.domain.vpn.ExpiryRecord
import java.text.DateFormat
import java.util.Date

@Composable
fun RenewalAssistantScreen(
    providerId: String,
    expiryRecord: ExpiryRecord?,
    onBack: () -> Unit,
    onOpenBrowser: (String) -> Unit,
    onOpenVpn: () -> Unit,
    viewModel: RenewalAssistantViewModel = hiltViewModel(),
) {
    val provider = viewModel.provider(providerId)
    val message by viewModel.message.collectAsStateWithLifecycle()
    val currentExpiry by viewModel.activeExpiry.collectAsStateWithLifecycle()
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importFile(it, providerId) } }

    if (provider == null) {
        Text("Provider is unavailable", modifier = Modifier.padding(24.dp))
        return
    }
    val record = expiryRecord ?: currentExpiry
    val remaining = ExpiryCalculator.remainingMs(record, System.currentTimeMillis())
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Renew configuration", style = MaterialTheme.typography.headlineSmall)
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(provider.name, style = MaterialTheme.typography.titleLarge)
                Text("Protocol: ${provider.supportedProtocols.joinToString().ifBlank { "Not supported" }}")
                Text("Generation: ${provider.generationMode.name.replace('_', ' ')}")
                remaining?.let { Text("Remaining: ${formatRemaining(it)}") }
                record?.generatedAt?.takeIf { it > 0 }?.let {
                    Text("Generated: ${DateFormat.getDateTimeInstance().format(Date(it))}")
                }
                record?.expiresAt?.takeIf { it > 0 }?.let {
                    Text("Expires: ${DateFormat.getDateTimeInstance().format(Date(it))}")
                }
            }
        }
        Text(
            if (provider.requiresAccount) "You may need to sign in on the provider website."
            else "Complete the provider workflow normally. Grabi does not bypass verification or collect credentials.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        provider.instructions.forEachIndexed { index, instruction ->
            Text("${index + 1}. $instruction")
        }
        if (provider.generationMode == GenerationMode.APP_ONLY) {
            Text("A generic configuration import is not supported for this provider.")
        } else {
            provider.officialConfigUrl?.let { url ->
                Button(onClick = { onOpenBrowser(url) }, Modifier.fillMaxWidth()) {
                    Text("Open official generator")
                }
            }
            OutlinedButton(
                onClick = { fileLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*")) },
                Modifier.fillMaxWidth(),
            ) {
                Text("Import configuration")
            }
            TextButton(onClick = onOpenVpn, Modifier.fillMaxWidth()) {
                Text("Open VPN settings to review and connect")
            }
        }
    }
    message?.let {
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = { Text("Renewal") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage(); onOpenVpn() }) {
                    Text("Open VPN settings")
                }
            },
        )
    }
}

private fun formatRemaining(ms: Long): String {
    val hours = ms / 3_600_000
    val minutes = (ms / 60_000) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
