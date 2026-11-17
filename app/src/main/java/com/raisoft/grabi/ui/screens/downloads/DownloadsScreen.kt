package com.raisoft.grabi.ui.screens.downloads

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raisoft.grabi.R
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.domain.DownloadFilter
import com.raisoft.grabi.domain.model.DownloadStatus
import com.raisoft.grabi.ui.components.EmptyState
import com.raisoft.grabi.ui.formatDuration

@Composable
fun DownloadsScreen(
    onPlay: (DownloadEntity) -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val items by viewModel.downloads.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.downloads_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadFilter.entries.forEach { entry ->
                FilterChip(
                    selected = filter == entry,
                    onClick = { viewModel.setFilter(entry) },
                    label = { Text(stringResource(entry.labelRes())) },
                )
            }
        }
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Download,
                title = stringResource(R.string.downloads_title),
                message = stringResource(R.string.downloads_empty),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { entity ->
                    DownloadRow(entity, viewModel, onPlay)
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    entity: DownloadEntity,
    viewModel: DownloadsViewModel,
    onPlay: (DownloadEntity) -> Unit,
) {
    val status = DownloadStatus.valueOf(entity.status)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = entity.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entity.optionLabel + " · " + stringResource(status.labelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (status) {
                DownloadStatus.RUNNING -> {
                    LinearProgressIndicator(
                        progress = { entity.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = "${(entity.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.weight(1f))
                        val tail = listOfNotNull(
                            entity.speed,
                            entity.etaSeconds.takeIf { it >= 0 }
                                ?.let { stringResource(R.string.home_eta, formatDuration(it.toInt())) },
                        ).joinToString(" · ")
                        if (tail.isNotBlank()) {
                            Text(tail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    ActionRow {
                        TextButton(onClick = { viewModel.pause(entity.id) }) {
                            Text(stringResource(R.string.action_pause))
                        }
                        TextButton(onClick = { viewModel.cancel(entity.id) }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
                DownloadStatus.QUEUED, DownloadStatus.WAITING_WIFI -> ActionRow {
                    TextButton(onClick = { viewModel.cancel(entity.id) }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                DownloadStatus.PAUSED -> ActionRow {
                    TextButton(onClick = { viewModel.resume(entity.id) }) {
                        Text(stringResource(R.string.action_resume))
                    }
                    TextButton(onClick = { viewModel.cancel(entity.id) }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                DownloadStatus.DONE -> {
                    Text(
                        text = stringResource(
                            R.string.downloads_saved_to,
                            entity.contentUri ?: entity.filePath.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ActionRow {
                        TextButton(onClick = { onPlay(entity) }) {
                            Text(stringResource(R.string.action_play))
                        }
                        TextButton(onClick = { viewModel.open(entity) }) {
                            Text(stringResource(R.string.action_open))
                        }
                        TextButton(onClick = { viewModel.delete(entity.id) }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
                DownloadStatus.FAILED -> {
                    Text(
                        text = entity.error ?: stringResource(R.string.status_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ActionRow {
                        TextButton(onClick = { viewModel.retry(entity.id) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                        TextButton(onClick = { viewModel.delete(entity.id) }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
                DownloadStatus.CANCELED -> ActionRow {
                    TextButton(onClick = { viewModel.retry(entity.id) }) {
                        Text(stringResource(R.string.action_retry))
                    }
                    TextButton(onClick = { viewModel.delete(entity.id) }) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        content = content,
    )
}

private fun DownloadFilter.labelRes(): Int = when (this) {
    DownloadFilter.ALL -> R.string.filter_all
    DownloadFilter.ACTIVE -> R.string.filter_active
    DownloadFilter.DONE -> R.string.filter_done
    DownloadFilter.FAILED -> R.string.filter_failed
}

private fun DownloadStatus.labelRes(): Int = when (this) {
    DownloadStatus.QUEUED -> R.string.status_queued
    DownloadStatus.RUNNING -> R.string.status_running
    DownloadStatus.PAUSED -> R.string.status_paused
    DownloadStatus.WAITING_WIFI -> R.string.status_waiting_wifi
    DownloadStatus.DONE -> R.string.status_done
    DownloadStatus.FAILED -> R.string.status_failed
    DownloadStatus.CANCELED -> R.string.status_canceled
}