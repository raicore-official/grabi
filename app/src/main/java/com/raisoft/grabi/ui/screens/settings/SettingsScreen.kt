package com.raisoft.grabi.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raisoft.grabi.R
import com.raisoft.grabi.data.settings.ThemeMode

@Composable
fun SettingsScreen(onOpenVpn: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val engineVersion by viewModel.engineVersion.collectAsStateWithLifecycle()
    val adblockUpdating by viewModel.adblockUpdating.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshEngineVersion() }
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_appearance),
                    style = MaterialTheme.typography.titleMedium,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                        ) {
                            Text(stringResource(mode.labelRes()))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_dynamic_color),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                stringResource(R.string.settings_dynamic_color_summary)
                            } else {
                                stringResource(R.string.settings_dynamic_color_unavailable)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    )
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_downloads),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_parallelism),
                    style = MaterialTheme.typography.bodyLarge,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(1, 2, 3).forEachIndexed { index, count ->
                        SegmentedButton(
                            selected = settings.downloadParallelism == count,
                            onClick = { viewModel.setDownloadParallelism(count) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        ) {
                            Text("$count")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_wifi_only),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.settings_wifi_only_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.wifiOnly,
                        onCheckedChange = viewModel::setWifiOnly,
                    )
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_browser),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_adblock),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.settings_adblock_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.adblockEnabled,
                        onCheckedChange = viewModel::setAdblockEnabled,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.settings_adblock_updated,
                        adblockAgeLabel(settings.adblockLastUpdated),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.settings_adblock_allowlist,
                        settings.adblockAllowlist.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = viewModel::updateAdblock,
                    enabled = !adblockUpdating,
                ) {
                    Text(
                        stringResource(
                            if (adblockUpdating) {
                                R.string.settings_adblock_updating
                            } else {
                                R.string.settings_adblock_update
                            },
                        ),
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
                    text = stringResource(R.string.settings_vpn),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_vpn_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenVpn) {
                    Text(stringResource(R.string.settings_vpn_open))
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.settings_engine,
                        engineVersion ?: stringResource(R.string.settings_engine_bundled),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.settings_license),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/raicore-official/grabi")),
                            )
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_source))
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_roadmap),
                    style = MaterialTheme.typography.titleMedium,
                )
                listOf(
                    R.string.roadmap_phase_1,
                    R.string.roadmap_phase_2,
                    R.string.roadmap_phase_3,
                    R.string.roadmap_phase_4,
                    R.string.roadmap_phase_5,
                    R.string.roadmap_phase_6,
                    R.string.roadmap_phase_7,
                ).forEach { phase ->
                    Text(
                        text = stringResource(phase),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun adblockAgeLabel(timestamp: Long): String {
    if (timestamp == 0L) return stringResource(R.string.settings_adblock_never)
    val ageMs = System.currentTimeMillis() - timestamp
    val hours = ageMs / 3_600_000
    return when {
        hours < 1 -> stringResource(R.string.settings_adblock_now)
        hours < 24 -> stringResource(R.string.settings_adblock_hours, hours.toInt())
        else -> stringResource(R.string.settings_adblock_days, (hours / 24).toInt())
    }
}