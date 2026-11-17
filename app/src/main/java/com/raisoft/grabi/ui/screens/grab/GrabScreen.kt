package com.raisoft.grabi.ui.screens.grab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.raisoft.grabi.R
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.domain.model.DownloadStatus
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.ui.formatDuration
import com.raisoft.grabi.ui.formatSize

@Composable
fun GrabScreen(viewModel: GrabViewModel = hiltViewModel()) {
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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.grab_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = ui.link,
                            onValueChange = viewModel::onLinkChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.home_link_label)) },
                            placeholder = { Text(stringResource(R.string.home_link_placeholder)) },
                            singleLine = true,
                            isError = ui.linkInvalid,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { viewModel.fetch() }),
                            supportingText = if (ui.linkInvalid) {
                                { Text(stringResource(R.string.home_link_invalid)) }
                            } else {
                                null
                            },
                            leadingIcon = { Icon(Icons.Rounded.Link, null) },
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
                        Text(
                            text = stringResource(R.string.home_universal_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            val info = ui.info
            if (info != null) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val selectedKind = info.options
                                .firstOrNull { it.id == ui.selectedOptionId }?.kind
                            val previewUrl = if (selectedKind == MediaKind.IMAGE) {
                                ui.link
                            } else {
                                info.thumbnailUrl
                            }
                            if (!previewUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = previewUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                            }
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
                                enabled = job?.status != DownloadStatus.RUNNING.name,
                            ) {
                                Text(stringResource(R.string.home_download_button))
                            }
                        }
                    }
                }
            }

            val currentJob = job
            if (currentJob != null) {
                item {
                    DownloadCard(
                        job = currentJob,
                        onPause = viewModel::pauseDownload,
                        onResume = viewModel::resumeDownload,
                        onRetry = viewModel::retryDownload,
                        onCancel = viewModel::cancelDownload,
                        onDismiss = viewModel::dismissDownload,
                    )
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DownloadCard(
    job: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val status = DownloadStatus.valueOf(job.status)
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
            when (status) {
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
                        val tail = listOfNotNull(
                            job.speed,
                            job.etaSeconds.takeIf { it >= 0 }
                                ?.let { stringResource(R.string.home_eta, formatDuration(it.toInt())) },
                        ).joinToString(" · ")
                        if (tail.isNotBlank()) {
                            Text(tail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_pause))
                        }
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
                DownloadStatus.QUEUED, DownloadStatus.WAITING_WIFI -> {
                    Text(
                        text = stringResource(
                            if (status == DownloadStatus.QUEUED) {
                                R.string.status_queued
                            } else {
                                R.string.status_waiting_wifi
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                DownloadStatus.PAUSED -> {
                    Text(
                        text = stringResource(R.string.status_paused),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onResume, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_resume))
                        }
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
                DownloadStatus.DONE -> {
                    Text(
                        text = stringResource(
                            R.string.downloads_saved_to,
                            job.contentUri ?: job.filePath.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_dismiss))
                    }
                }
                DownloadStatus.FAILED -> {
                    Text(
                        text = job.error ?: stringResource(R.string.status_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_retry))
                        }
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                }
                DownloadStatus.CANCELED -> {
                    Text(
                        text = stringResource(R.string.status_canceled),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_retry))
                        }
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                }
            }
        }
    }
}