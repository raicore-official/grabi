package com.raisoft.grabi.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.raisoft.grabi.R
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.domain.CompactNumber
import com.raisoft.grabi.domain.LinkValidator
import com.raisoft.grabi.domain.model.DownloadStatus
import com.raisoft.grabi.domain.model.FeedItem
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.ui.formatDuration
import com.raisoft.grabi.ui.formatSize

private data class QuickSite(val name: String, val url: String, val color: Long)

private val QUICK_SITES = listOf(
    QuickSite("YouTube", "https://www.youtube.com", 0xFFFF0000),
    QuickSite("Instagram", "https://www.instagram.com", 0xFFE1306C),
    QuickSite("Facebook", "https://www.facebook.com", 0xFF1877F2),
    QuickSite("TikTok", "https://www.tiktok.com", 0xFF25F4EE),
    QuickSite("Pinterest", "https://www.pinterest.com", 0xFFE60023),
    QuickSite("X", "https://x.com", 0xFFE7E9EA),
)

@Composable
fun HomeScreen(
    onOpenPlayer: (String, String) -> Unit = { _, _ -> },
    onOpenBrowser: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.grabi_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.home_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                            placeholder = { Text(stringResource(R.string.home_search_hint)) },
                            singleLine = true,
                            isError = ui.linkInvalid,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.submit(ui.link) }),
                            supportingText = if (ui.linkInvalid) {
                                { Text(stringResource(R.string.home_link_invalid)) }
                            } else {
                                null
                            },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
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
                            onClick = { viewModel.submit(ui.link) },
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
                                Text(
                                    stringResource(
                                        if (LinkValidator.isValidUrl(ui.link.trim())) {
                                            R.string.home_fetch_button
                                        } else {
                                            R.string.home_search_button
                                        },
                                    ),
                                )
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

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    feed.query?.let { query ->
                        FilterChip(
                            selected = true,
                            onClick = viewModel::clearSearch,
                            label = { Text("“$query”") },
                            trailingIcon = { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)) },
                        )
                    }
                    feed.kiosks.forEach { kiosk ->
                        FilterChip(
                            selected = feed.query == null && feed.kioskId == kiosk,
                            onClick = { viewModel.selectKiosk(kiosk) },
                            label = { Text(kiosk) },
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.home_sites_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QUICK_SITES.forEach { site ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Surface(
                                    onClick = {
                                        viewModel.openSite(site.url)
                                        onOpenBrowser()
                                    },
                                    shape = CircleShape,
                                    color = Color(site.color),
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = site.name.take(1),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (site.name == "TikTok" || site.name == "X") {
                                                Color(0xFF0F1115)
                                            } else {
                                                Color.White
                                            },
                                        )
                                    }
                                }
                                Text(
                                    text = site.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (feed.loading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            feed.error?.let { error ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(
                            onClick = {
                                feed.query?.let(viewModel::search)
                                    ?: feed.kioskId?.let(viewModel::selectKiosk)
                            },
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
            if (!feed.loading && feed.items.isEmpty() && feed.error == null) {
                item {
                    Text(
                        text = stringResource(R.string.home_feed_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(feed.items, key = { it.url }) { item ->
                FeedCard(
                    item = item,
                    onOpen = { onOpenPlayer(item.url, item.title) },
                    onDownload = { viewModel.fetch(item.url) },
                )
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FeedCard(item: FeedItem, onOpen: () -> Unit, onDownload: () -> Unit) {
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (item.thumbnailUrl != null) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.viewCount >= 0) {
                        Surface(color = Color(0xB3000000), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.home_views,
                                    CompactNumber.format(item.viewCount),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (item.durationSeconds > 0) {
                        Surface(color = Color(0xB3000000), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = formatDuration(item.durationSeconds.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AsyncImage(
                        model = item.uploaderAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Text(
                        text = item.uploader.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.home_download_button),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
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