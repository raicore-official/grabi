package com.raisoft.grabi.ui.screens.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raisoft.grabi.R
import com.raisoft.grabi.data.browser.BrowserHistoryRepository
import com.raisoft.grabi.data.browser.TabManager
import com.raisoft.grabi.domain.model.DetectedMedia
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.ui.theme.resolveDark
import java.net.URI

private data class Site(val name: String, val url: String)

private val SITES = listOf(
    Site("YouTube", "https://www.youtube.com"),
    Site("Wikipedia", "https://www.wikipedia.org"),
    Site("Reddit", "https://www.reddit.com"),
    Site("DuckDuckGo", "https://duckduckgo.com"),
    Site("GitHub", "https://github.com"),
    Site("Vimeo", "https://vimeo.com"),
    Site("Dailymotion", "https://www.dailymotion.com"),
    Site("Twitch", "https://www.twitch.tv"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onDownloadPage: (String) -> Unit,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val active by viewModel.activeTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val adblockUpdating by viewModel.adblockUpdating.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    val dark = settings.themeMode.resolveDark(isSystemInDarkTheme())
    val browserBackground = MaterialTheme.colorScheme.background.toArgb()
    LaunchedEffect(dark, browserBackground) {
        viewModel.applyTheme(dark, browserBackground)
    }

    var urlInput by remember(active?.id) { mutableStateOf(active?.url ?: "") }
    var showTabs by remember { mutableStateOf(false) }
    var showDetected by remember { mutableStateOf(false) }
    var detectedSection by remember { mutableStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val activeHost = remember(active?.url) {
        active?.url?.let { runCatching { URI(it).host }.getOrNull() }?.lowercase()
    }
    val siteAllowlisted = activeHost != null && activeHost in settings.adblockAllowlist

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = viewModel::goBack,
                    enabled = active?.canGoBack == true,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.browser_back),
                    )
                }
                IconButton(
                    onClick = viewModel::goForward,
                    enabled = active?.canGoForward == true,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = stringResource(R.string.browser_forward),
                    )
                }
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    placeholder = { Text(stringResource(R.string.browser_url_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            viewModel.submitUrl(urlInput)
                            keyboard?.hide()
                        },
                    ),
                )
                IconButton(onClick = { showTabs = true }) {
                    BadgedBox(
                        badge = {
                            Badge { Text("${tabs.size}") }
                        },
                    ) {
                        Icon(
                            Icons.Rounded.Tab,
                            contentDescription = stringResource(R.string.browser_tabs),
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.browser_menu),
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.browser_new_tab)) },
                            onClick = {
                                showMenu = false
                                viewModel.openTab(incognito = false)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.browser_new_incognito)) },
                            leadingIcon = { Icon(Icons.Rounded.VisibilityOff, null) },
                            onClick = {
                                showMenu = false
                                viewModel.openTab(incognito = true)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.browser_reload)) },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                            onClick = {
                                showMenu = false
                                viewModel.reload()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.browser_history)) },
                            onClick = {
                                showMenu = false
                                showHistory = true
                            },
                        )
                        if (activeHost != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (siteAllowlisted) {
                                                R.string.browser_block_site
                                            } else {
                                                R.string.browser_allow_site
                                            },
                                        ),
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleSiteAllowlist()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (adblockUpdating) {
                                            R.string.browser_updating_filters
                                        } else {
                                            R.string.browser_update_filters
                                        },
                                    ),
                                )
                            },
                            enabled = !adblockUpdating,
                            onClick = {
                                showMenu = false
                                viewModel.updateAdblock()
                            },
                        )
                        if (active?.url?.startsWith("http") == true) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_download_page)) },
                                leadingIcon = { Icon(Icons.Rounded.Download, null) },
                                onClick = {
                                    showMenu = false
                                    onDownloadPage(active!!.url)
                                },
                            )
                        }
                    }
                }
            }
            val progress = active?.progress ?: 100
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val tab = active
            if (tab == null || tab.url.isBlank()) {
                SiteGrid(
                    incognito = tab?.incognito == true,
                    onOpen = { url ->
                        urlInput = url
                        viewModel.submitUrl(url)
                    },
                )
            } else {
                val engine = viewModel.engineFor(tab.id)
                if (engine != null) {
                    key(tab.id) {
                        AndroidView(
                            factory = { engine.view },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        if (active?.incognito == true) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.browser_incognito),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { showDetected = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        ) {
            BadgedBox(
                badge = {
                    val count = active?.detected?.size ?: 0
                    if (count > 0) Badge { Text("$count") }
                },
            ) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.browser_detected_title),
                )
            }
        }
    }

    if (showTabs) {
        ModalBottomSheet(onDismissRequest = { showTabs = false }) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.browser_tabs),
                    style = MaterialTheme.typography.titleMedium,
                )
                tabs.forEach { tab ->
                    Surface(
                        onClick = {
                            viewModel.switchTo(tab.id)
                            showTabs = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (tab.incognito) {
                                Icon(
                                    Icons.Rounded.VisibilityOff,
                                    contentDescription = stringResource(R.string.browser_incognito),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = tab.title.ifBlank {
                                        tab.url.ifBlank { stringResource(R.string.browser_new_tab) }
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (tab.url.isNotBlank()) {
                                    Text(
                                        text = tab.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.closeTab(tab.id) }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.browser_close_tab),
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.openTab(incognito = false)
                            showTabs = false
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.browser_new_tab))
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.openTab(incognito = true)
                            showTabs = false
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.browser_new_incognito))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDetected && active != null) {
        val tab = active!!
        val avMedia = tab.detected.filter { it.kind != MediaKind.IMAGE }
        val images = tab.detected.filter { it.kind == MediaKind.IMAGE }
        ModalBottomSheet(onDismissRequest = { showDetected = false }) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.browser_detected_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = detectedSection == 0,
                        onClick = { detectedSection = 0 },
                        label = { Text(stringResource(R.string.browser_section_video, avMedia.size)) },
                        leadingIcon = { Icon(Icons.Rounded.VideoFile, null, Modifier.size(16.dp)) },
                    )
                    FilterChip(
                        selected = detectedSection == 1,
                        onClick = { detectedSection = 1 },
                        label = { Text(stringResource(R.string.browser_section_images, images.size)) },
                        leadingIcon = { Icon(Icons.Rounded.Image, null, Modifier.size(16.dp)) },
                    )
                }
                if (detectedSection == 0) {
                    if (tab.url.startsWith("http")) {
                        OutlinedButton(
                            onClick = {
                                showDetected = false
                                onDownloadPage(tab.url)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Download, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.browser_download_page))
                        }
                        Text(
                            text = stringResource(R.string.browser_page_video_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (avMedia.isEmpty()) {
                        Text(
                            text = stringResource(R.string.browser_detected_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(avMedia.reversed(), key = { it.url }) { media ->
                                DetectedRow(media) { viewModel.downloadDetected(media) }
                            }
                        }
                    }
                } else if (images.isEmpty()) {
                    Text(
                        text = stringResource(R.string.browser_images_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(images.reversed(), key = { it.url }) { media ->
                            DetectedRow(media) { viewModel.downloadDetected(media) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.browser_history),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (history.isNotEmpty()) {
                        OutlinedButton(onClick = viewModel::clearHistory) {
                            Text(stringResource(R.string.browser_clear_history))
                        }
                    }
                }
                if (history.isEmpty()) {
                    Text(
                        text = stringResource(R.string.browser_history_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(history, key = { it.url + it.visitedAt }) { entry ->
                            HistoryRow(entry) {
                                showHistory = false
                                urlInput = entry.url
                                viewModel.submitUrl(entry.url)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SiteGrid(incognito: Boolean, onOpen: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (incognito) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.browser_incognito_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(R.string.browser_sites_title),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(SITES) { site ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onOpen(site.url) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = site.name.take(1),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.browser_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetectedRow(media: DetectedMedia, onDownload: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = when (media.kind) {
                    MediaKind.VIDEO -> Icons.Rounded.VideoFile
                    MediaKind.AUDIO -> Icons.Rounded.Audiotrack
                    MediaKind.IMAGE -> Icons.Rounded.Image
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = media.url.substringAfter("://"),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = media.kind.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.browser_download),
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: BrowserHistoryRepository.Entry, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = entry.title.ifBlank { entry.url },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}