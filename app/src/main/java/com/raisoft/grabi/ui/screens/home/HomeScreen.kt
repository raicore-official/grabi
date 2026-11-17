package com.raisoft.grabi.ui.screens.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.CircularProgressIndicator
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.domain.VideoIds
import com.raisoft.grabi.domain.model.DownloadStatus
import com.raisoft.grabi.domain.model.MediaKind
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.raisoft.grabi.R
import com.raisoft.grabi.domain.CompactNumber
import com.raisoft.grabi.domain.model.FeedItem
import com.raisoft.grabi.ui.formatDuration
import com.raisoft.grabi.ui.theme.SiteBrand
import com.raisoft.grabi.ui.theme.onVideo
import com.raisoft.grabi.ui.theme.videoScrim

private data class QuickSite(val name: String, val url: String, val color: Color)

private val QUICK_SITES = listOf(
    QuickSite("YouTube", "https://www.youtube.com", SiteBrand.YouTube),
    QuickSite("Instagram", "https://www.instagram.com", SiteBrand.Instagram),
    QuickSite("Facebook", "https://www.facebook.com", SiteBrand.Facebook),
    QuickSite("TikTok", "https://www.tiktok.com", SiteBrand.TikTok),
    QuickSite("Pinterest", "https://www.pinterest.com", SiteBrand.Pinterest),
    QuickSite("X", "https://x.com", SiteBrand.X),
)

@Composable
fun HomeScreen(
    onOpenPlayer: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenBrowser: () -> Unit = {},
    onGrab: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    var searchInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                searchInput = spoken
                viewModel.search(spoken)
            }
        }
    }

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
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.home_search_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchInput.isNotBlank()) viewModel.search(searchInput.trim())
                        },
                    ),
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                        )
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(
                                            RecognizerIntent.EXTRA_PROMPT,
                                            context.getString(R.string.voice_search_prompt),
                                        )
                                    }
                                    runCatching { voiceLauncher.launch(intent) }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = stringResource(R.string.home_voice_search),
                                )
                            }
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { searchInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.action_clear),
                                    )
                                }
                            }
                        }
                    },
                )
                Image(
                    painter = painterResource(R.drawable.grabi_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
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
                        onClick = {
                            searchInput = ""
                            viewModel.clearSearch()
                        },
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
                                color = site.color,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = site.name.take(1),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (site.name == "TikTok" || site.name == "X") {
                                            SiteBrand.OnLight
                                        } else {
                                            SiteBrand.OnDark
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
                download = downloadStates["${VideoIds.of(item.url)}:${MediaKind.VIDEO.name}"],
                onOpen = { onOpenPlayer(item.url, item.title, item.thumbnailUrl.orEmpty()) },
                onDownload = { onGrab(item.url) },
                onPauseResume = { viewModel.pauseResumeDownload(it.id) },
                onDelete = { viewModel.deleteDownload(it.id) },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FeedCard(
    item: FeedItem,
    download: DownloadEntity?,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onPauseResume: (DownloadEntity) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
) {
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
                        Surface(
                            color = MaterialTheme.colorScheme.videoScrim.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.home_views,
                                    CompactNumber.format(item.viewCount),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onVideo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (item.durationSeconds > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.videoScrim.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = formatDuration(item.durationSeconds.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onVideo,
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
                    val active = download != null && download.status in setOf(
                        DownloadStatus.RUNNING.name,
                        DownloadStatus.QUEUED.name,
                        DownloadStatus.PAUSED.name,
                        DownloadStatus.WAITING_WIFI.name,
                    )
                    val done = download != null && download.status == DownloadStatus.DONE.name
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .combinedClickable(
                                onClick = {
                                    when {
                                        active -> onPauseResume(download!!)
                                        done -> onOpen()
                                        else -> onDownload()
                                    }
                                },
                                onLongClick = { if (done) onDelete(download!!) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            active -> CircularProgressIndicator(
                                progress = { download!!.progress },
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                            done -> Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.home_downloaded),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            else -> Icon(
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
}