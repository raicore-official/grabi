package com.raisoft.grabi.ui.screens.autobot

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raisoft.grabi.R
import com.raisoft.grabi.domain.vpn.AutoBotPhase
import com.raisoft.grabi.domain.vpn.ExpiryCalculator
import com.raisoft.grabi.domain.vpn.ExpiryRecord
import java.util.Date

@Composable
fun AutoBotScreen(
    onBack: () -> Unit = {},
    onOpenHandoff: () -> Unit = {},
    viewModel: AutoBotViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val activity by viewModel.activity.collectAsStateWithLifecycle()
    val statuses by viewModel.providerStatuses.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val activeRecord by viewModel.activeRecord.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activeEntry = entries.firstOrNull { it.name == connection.activeName }
    val busy = phase in setOf(
        AutoBotPhase.DISCOVERING,
        AutoBotPhase.VALIDATING,
        AutoBotPhase.REPLACING,
        AutoBotPhase.VERIFYING,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
            Column {
                Text(stringResource(R.string.autobot_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.autobot_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.autobot_enable),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = settings.enabled, onCheckedChange = viewModel::setEnabled)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.autobot_full_auto),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.autobot_full_auto_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.fullAuto,
                        onCheckedChange = viewModel::setFullAuto,
                        enabled = settings.enabled,
                    )
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.autobot_phase, phase.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.autobot_current), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.autobot_status, connection.status.name),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.autobot_server, connection.activeName ?: "—"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.autobot_country, connection.activeCountry ?: "—"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.autobot_protocol, "WireGuard"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.autobot_provider, activeEntry?.source ?: "—"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.autobot_expiry,
                        expiryLabel(activeRecord),
                        ExpiryCalculator.status(activeRecord, System.currentTimeMillis()).name,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.autobot_providers), style = MaterialTheme.typography.titleMedium)
                viewModel.providers.forEach { provider ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = statuses[provider.id] ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(
                                if (provider.capabilities.automaticGeneration) {
                                    R.string.autobot_automated
                                } else {
                                    R.string.autobot_needs_verification
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (provider.id == "vpnbook") {
                            TextButton(onClick = onOpenHandoff) {
                                Text(stringResource(R.string.autobot_complete_verification))
                            }
                        }
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.autobot_schedule), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.autobot_last_discovery, timeLabel(settings.lastDiscovery)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.autobot_last_renewal, timeLabel(settings.lastRenewal)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.autobot_next_renewal, timeLabel(settings.nextRenewal)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.autobot_fallback,
                        stringResource(if (settings.autoFallback) R.string.onboarding_granted else R.string.onboarding_not_granted),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = viewModel::retry, enabled = settings.enabled && !busy) {
                    Text(stringResource(R.string.autobot_retry))
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.autobot_preferences), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.autobot_preferred_provider),
                    style = MaterialTheme.typography.bodyLarge,
                )
                val options = listOf("" to R.string.autobot_auto, "github" to R.string.autobot_github, "vpnbook" to R.string.autobot_vpnbook)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = settings.preferredProvider == value,
                            onClick = { viewModel.setPreferredProvider(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) {
                            Text(stringResource(label))
                        }
                    }
                }
                OutlinedTextField(
                    value = settings.countryPref,
                    onValueChange = viewModel::setCountryPref,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.autobot_country_pref)) },
                )
                SwitchRow(stringResource(R.string.autobot_fallback_title), settings.autoFallback, viewModel::setAutoFallback)
                SwitchRow(stringResource(R.string.autobot_auto_renew), settings.autoRenew, viewModel::setAutoRenew)
                SwitchRow(stringResource(R.string.autobot_expiry_protection), settings.expiryProtection, viewModel::setExpiryProtection)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.autobot_activity), style = MaterialTheme.typography.titleMedium)
                if (activity.isEmpty()) {
                    Text(
                        text = stringResource(R.string.autobot_no_activity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                activity.take(10).forEach { entry ->
                    Text(
                        text = "${timeLabel(entry.at)} · ${entry.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun timeLabel(timestamp: Long): String {
    if (timestamp <= 0L) return stringResource(R.string.autobot_never)
    val context = LocalContext.current
    val date = DateFormat.getDateFormat(context).format(Date(timestamp))
    val time = DateFormat.getTimeFormat(context).format(Date(timestamp))
    return "$date $time"
}

private fun expiryLabel(record: ExpiryRecord?): String {
    val remaining = ExpiryCalculator.remainingMs(record, System.currentTimeMillis()) ?: return "—"
    val days = remaining / 86_400_000
    val hours = (remaining % 86_400_000) / 3_600_000
    return "${days}d ${hours}h"
}