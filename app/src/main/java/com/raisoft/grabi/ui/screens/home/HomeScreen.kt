package com.raisoft.grabi.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raisoft.grabi.R
import com.raisoft.grabi.data.download.DownloadJob
import com.raisoft.grabi.data.download.DownloadStatus
import com.raisoft.grabi.domain.model.MediaKind
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val job by viewModel.download.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) {
        ui.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.grabi_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_paste_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = ui.link,
                        onValueChange = viewModel::onLinkChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.home_link_label)) },
                        placeholder = { Text(stringResource(R.string.home_link_placeholder)) },
                        singleLine = true,
                        isError = ui.linkInvalid,
                        supportingText = if (ui.linkInvalid) {
                            { Text(stringResource(R.string.home_link_invalid)) }
                        } else {
                            null
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    clipboard.getText()?.text?.let(viewModel::onLinkChange)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = stringResource(R.string.home_paste_action),
                                )
                            }
                        },
                    )
                    Button(
                        onClick = { viewModel.fetch() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = ui.link.isNotBlank() && !ui.fetching,
                    ) {
                        if (ui.fetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.home_fetching))
                        } else {
                            Text(stringResource(R.string.home_fetch_button))
                        }
                    }
                }
            }

            val info = ui.info
            if (info != null) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val meta = listOfNotNull(
                            info.uploader,
                            formatDuration(info.durationSeconds).takeIf { info.durationSeconds > 0 },
                        ).joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(R.string.home_format_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        info.options.forEach { option ->
                            val selected = ui.selectedOptionId == option.id
                            Surface(
                                onClick = { viewModel.selectOption(option.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { viewModel.selectOption(option.id) },
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                        val kind = stringResource(
                                            if (option.kind == MediaKind.VIDEO) {
                                                R.string.home_format_video
                                            } else {
                                                R.string.home_format_audio
                                            },
                                        )
                                        val size = option.sizeBytes?.let { " · ~${formatSize(it)}" }.orEmpty()
                                        Text(
                                            text = kind + size,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.home_subtitles),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = ui.subtitles,
                                onCheckedChange = viewModel::setSubtitles,
                            )
                        }
                        Button(
                            onClick = viewModel::startDownload,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = job?.status != DownloadStatus.RUNNING,
                        ) {
                            Text(stringResource(R.string.home_download_button))
                        }
                    }
                }
            }

            val currentJob = job
            if (currentJob != null) {
                DownloadCard(
                    job = currentJob,
                    onCancel = viewModel::cancelDownload,
                    onDismiss = viewModel::dismissDownload,
                )
            }

            if (info == null) {
                Text(
                    text = stringResource(R.string.home_supported_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("YouTube", "Facebook", "Instagram", "TikTok", "X", "Vimeo", "SoundCloud", "+1000 more")
                        .forEach { site ->
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(
                                    text = site,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                }

                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_how_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(stringResource(R.string.home_step_1), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.home_step_2), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.home_step_3), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DownloadCard(job: DownloadJob, onCancel: () -> Unit, onDismiss: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = job.optionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (job.status) {
                DownloadStatus.RUNNING -> {
                    LinearProgressIndicator(
                        progress = { job.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = "${(job.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.weight(1f))
                        if (job.etaSeconds >= 0) {
                            Text(
                                text = stringResource(
                                    R.string.home_eta,
                                    formatDuration(job.etaSeconds.toInt()),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_download_cancel))
                    }
                }
                DownloadStatus.DONE -> {
                    Text(
                        text = stringResource(R.string.home_download_done, job.filePath.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_dismiss))
                    }
                }
                DownloadStatus.FAILED -> {
                    Text(
                        text = job.error ?: stringResource(R.string.home_download_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_dismiss))
                    }
                }
                DownloadStatus.CANCELED -> {
                    Text(
                        text = stringResource(R.string.home_download_canceled),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_dismiss))
                    }
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    else -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
}