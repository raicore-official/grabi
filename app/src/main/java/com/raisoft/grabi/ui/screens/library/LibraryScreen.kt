package com.raisoft.grabi.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.raisoft.grabi.R
import com.raisoft.grabi.ui.components.EmptyState
import com.raisoft.grabi.ui.formatDuration

@Composable
fun LibraryScreen(
    onOpenPlayer: (url: String, title: String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val section by viewModel.section.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (section == LibrarySection.HISTORY && history.isNotEmpty()) {
                TextButton(onClick = viewModel::clearHistory) {
                    Text(stringResource(R.string.library_clear_history))
                }
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibrarySection.entries.forEach { entry ->
                FilterChip(
                    selected = section == entry,
                    onClick = { viewModel.setSection(entry) },
                    label = { Text(stringResource(entry.labelRes())) },
                )
            }
        }
        when (section) {
            LibrarySection.HISTORY ->
                if (history.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.History,
                        title = stringResource(R.string.library_history),
                        message = stringResource(R.string.library_history_empty),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                    ) {
                        items(history, key = { it.url }) { entry ->
                            MediaRow(
                                thumbnailUrl = entry.thumbnailUrl,
                                title = entry.title,
                                subtitle = listOfNotNull(
                                    entry.uploader,
                                    entry.durationSeconds.takeIf { it > 0 }
                                        ?.let { formatDuration(it.toInt()) },
                                ).joinToString(" · "),
                                onClick = { onOpenPlayer(entry.url, entry.title) },
                                onRemove = { viewModel.removeHistory(entry) },
                            )
                        }
                    }
                }
            LibrarySection.FAVORITES ->
                if (favorites.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Favorite,
                        title = stringResource(R.string.library_favorites),
                        message = stringResource(R.string.library_favorites_empty),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                    ) {
                        items(favorites, key = { it.url }) { entry ->
                            MediaRow(
                                thumbnailUrl = entry.thumbnailUrl,
                                title = entry.title,
                                subtitle = listOfNotNull(
                                    entry.uploader,
                                    entry.durationSeconds.takeIf { it > 0 }
                                        ?.let { formatDuration(it.toInt()) },
                                ).joinToString(" · "),
                                onClick = { onOpenPlayer(entry.url, entry.title) },
                                onRemove = { viewModel.removeFavorite(entry) },
                            )
                        }
                    }
                }
            LibrarySection.DOWNLOADS ->
                if (downloads.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Download,
                        title = stringResource(R.string.library_downloads),
                        message = stringResource(R.string.library_downloads_empty),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                    ) {
                        items(downloads, key = { it.id }) { entry ->
                            MediaRow(
                                thumbnailUrl = null,
                                title = entry.title,
                                subtitle = entry.optionLabel,
                                onClick = {
                                    viewModel.playableTarget(entry)?.let { target ->
                                        onOpenPlayer(target, entry.title)
                                    }
                                },
                                onRemove = null,
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun MediaRow(
    thumbnailUrl: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_delete),
                    )
                }
            }
        }
    }
}

private fun LibrarySection.labelRes(): Int = when (this) {
    LibrarySection.HISTORY -> R.string.library_history
    LibrarySection.FAVORITES -> R.string.library_favorites
    LibrarySection.DOWNLOADS -> R.string.library_downloads
}