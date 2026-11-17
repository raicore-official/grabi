package com.raisoft.grabi.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionDisabled
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.raisoft.grabi.R
import com.raisoft.grabi.domain.CompactNumber
import com.raisoft.grabi.domain.model.DownloadStatus
import com.raisoft.grabi.domain.model.FeedItem
import com.raisoft.grabi.domain.model.StreamInfo
import com.raisoft.grabi.ui.components.EmptyState
import com.raisoft.grabi.ui.formatDuration
import com.raisoft.grabi.ui.theme.liveBadge
import com.raisoft.grabi.ui.theme.liveText
import com.raisoft.grabi.ui.theme.onLiveBadge
import com.raisoft.grabi.ui.theme.onVideo
import com.raisoft.grabi.ui.theme.videoScrim
import kotlinx.coroutines.delay

private val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onOpenPlayer: (url: String, title: String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val controller by viewModel.controller.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scheme = MaterialTheme.colorScheme
    val pipSupported = remember(activity) {
        activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true
    }
    var fullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var seekSide by remember { mutableIntStateOf(0) }
    var seekTick by remember { mutableIntStateOf(0) }
    var descriptionExpanded by remember { mutableStateOf(false) }
    val error = state.error

    BackHandler(enabled = fullscreen) { fullscreen = false }

    DisposableEffect(fullscreen, activity) {
        val window = activity?.window
        if (fullscreen && window != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            if (window != null) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(controlsVisible, state.isPlaying, state.buffering) {
        if (controlsVisible && state.isPlaying && !state.buffering) {
            delay(3_500)
            controlsVisible = false
        }
    }

    LaunchedEffect(seekTick) {
        if (seekTick > 0) {
            delay(650)
            seekSide = 0
        }
    }

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(4_000)
            viewModel.consumeNotice()
        }
    }

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        if (!fullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.player_back),
                    )
                }
                Text(
                    text = state.info?.title ?: stringResource(R.string.player_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val thumb = state.info?.thumbnailUrl ?: state.thumbnailUrl
                if (thumb != null) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(0.35f),
                    )
                }
                CircularProgressIndicator()
            }
            error != null -> Column(Modifier.fillMaxSize()) {
                EmptyState(
                    icon = Icons.Rounded.ErrorOutline,
                    title = stringResource(R.string.player_error_title),
                    message = error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = viewModel::retry,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            else -> {
                val info = state.info
                val stageModifier = if (fullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                }
                Box(modifier = stageModifier.background(scheme.videoScrim)) {
                    if (state.audioOnly) {
                        AudioStage(info)
                    } else {
                        AndroidView(
                            factory = { ctx -> PlayerView(ctx).apply { useController = false } },
                            update = { view -> view.player = controller },
                            onRelease = { view -> view.player = null },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Row(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { controlsVisible = !controlsVisible },
                                        onDoubleTap = {
                                            viewModel.seekBy(-10_000)
                                            seekSide = -1
                                            seekTick++
                                        },
                                    )
                                },
                        )
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                                },
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { controlsVisible = !controlsVisible },
                                        onDoubleTap = {
                                            viewModel.seekBy(10_000)
                                            seekSide = 1
                                            seekTick++
                                        },
                                    )
                                },
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = seekSide != 0,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(
                            if (seekSide < 0) Alignment.CenterStart else Alignment.CenterEnd,
                        ),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(scheme.onVideo.copy(alpha = 0.18f))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                imageVector = if (seekSide < 0) Icons.Rounded.Replay10 else Icons.Rounded.Forward10,
                                contentDescription = null,
                                tint = scheme.onVideo,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                "10s",
                                color = scheme.onVideo,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    if (info?.isLive == true) {
                        Surface(
                            color = scheme.liveBadge,
                            contentColor = scheme.onLiveBadge,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.player_live),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (state.buffering) {
                        CircularProgressIndicator(
                            color = scheme.onVideo,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible || !state.isPlaying,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                scheme.videoScrim.copy(alpha = 0.6f),
                                                scheme.videoScrim.copy(alpha = 0f),
                                            ),
                                        ),
                                    ),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                scheme.videoScrim.copy(alpha = 0f),
                                                scheme.videoScrim.copy(alpha = 0.7f),
                                            ),
                                        ),
                                    ),
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (info != null && !state.audioOnly && info.videoOptions.isNotEmpty()) {
                                    OverlayMenu(
                                        label = info.videoOptions
                                            .firstOrNull { it.id == state.selectedVideoId }?.label
                                            ?: stringResource(R.string.player_quality),
                                        options = info.videoOptions.map { it.id to it.label },
                                        onSelect = viewModel::selectVideo,
                                    )
                                }
                                if (info != null && state.audioOnly && info.audioOptions.isNotEmpty()) {
                                    OverlayMenu(
                                        label = stringResource(R.string.player_audio_quality),
                                        options = info.audioOptions.map { it.id to it.label },
                                        onSelect = viewModel::selectAudio,
                                    )
                                }
                                OverlayMenu(
                                    label = "${state.speed}×",
                                    options = SPEEDS.map { it.toString() to "${it}×" },
                                    onSelect = { viewModel.setSpeed(it.toFloat()) },
                                )
                                if (info?.subtitleUrl != null && !state.audioOnly) {
                                    IconButton(onClick = { viewModel.setCaptions(!state.captionsEnabled) }) {
                                        Icon(
                                            imageVector = if (state.captionsEnabled) {
                                                Icons.Rounded.ClosedCaption
                                            } else {
                                                Icons.Rounded.ClosedCaptionDisabled
                                            },
                                            contentDescription = stringResource(R.string.player_captions),
                                            tint = if (state.captionsEnabled) scheme.primary else scheme.onVideo,
                                        )
                                    }
                                }
                                IconButton(onClick = { fullscreen = !fullscreen }) {
                                    Icon(
                                        imageVector = if (fullscreen) {
                                            Icons.Rounded.FullscreenExit
                                        } else {
                                            Icons.Rounded.Fullscreen
                                        },
                                        contentDescription = stringResource(
                                            if (fullscreen) R.string.player_exit_fullscreen else R.string.player_fullscreen,
                                        ),
                                        tint = scheme.onVideo,
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                            ) {
                                IconButton(onClick = { viewModel.seekBy(-10_000) }) {
                                    Icon(
                                        Icons.Rounded.Replay10,
                                        contentDescription = stringResource(R.string.player_rewind),
                                        tint = scheme.onVideo,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                IconButton(
                                    onClick = viewModel::togglePlay,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(scheme.onVideo.copy(alpha = 0.16f)),
                                ) {
                                    Icon(
                                        imageVector = if (state.playWhenReady && state.isPlaying) {
                                            Icons.Rounded.Pause
                                        } else {
                                            Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = stringResource(
                                            if (state.playWhenReady) R.string.player_pause else R.string.player_play,
                                        ),
                                        tint = scheme.onVideo,
                                        modifier = Modifier.size(40.dp),
                                    )
                                }
                                IconButton(onClick = { viewModel.seekBy(10_000) }) {
                                    Icon(
                                        Icons.Rounded.Forward10,
                                        contentDescription = stringResource(R.string.player_forward),
                                        tint = scheme.onVideo,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                if (info?.isLive == true) {
                                    Text(
                                        text = stringResource(R.string.player_live),
                                        color = scheme.liveText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                } else {
                                    SeekBar(
                                        positionMs = state.positionMs,
                                        durationMs = state.durationMs,
                                        bufferedMs = state.bufferedMs,
                                        onSeek = viewModel::seekTo,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = formatDuration((state.positionMs / 1000).toInt()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.onVideo,
                                        )
                                        Text(
                                            text = formatDuration((state.durationMs / 1000).toInt()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.onVideo.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!fullscreen) {
                    androidx.compose.animation.AnimatedVisibility(visible = state.notice != null) {
                        Surface(
                            color = scheme.primaryContainer,
                            contentColor = scheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = state.notice.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = info?.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val meta = listOfNotNull(
                            info?.viewCount?.takeIf { it >= 0 }
                                ?.let { stringResource(R.string.home_views, CompactNumber.format(it)) },
                            info?.uploadDate,
                        ).joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        if (info?.uploader != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (info.uploaderAvatarUrl != null) {
                                    AsyncImage(
                                        model = info.uploaderAvatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(
                                    text = info.uploader,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                if (info.likeCount >= 0) {
                                    Icon(
                                        Icons.Rounded.ThumbUp,
                                        contentDescription = null,
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = CompactNumber.format(info.likeCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val dl by viewModel.downloadState.collectAsStateWithLifecycle()
                            val dlActive = dl != null && dl!!.status in setOf(
                                DownloadStatus.RUNNING.name,
                                DownloadStatus.QUEUED.name,
                                DownloadStatus.PAUSED.name,
                                DownloadStatus.WAITING_WIFI.name,
                            )
                            val dlDone = dl != null && dl!!.status == DownloadStatus.DONE.name
                            ActionChip(
                                icon = if (dlDone) Icons.Rounded.Check else Icons.Rounded.Download,
                                label = when {
                                    dlDone -> stringResource(R.string.player_downloaded)
                                    dlActive && dl!!.status == DownloadStatus.PAUSED.name ->
                                        stringResource(R.string.status_paused)
                                    dlActive ->
                                        "${((dl!!.progress) * 100).toInt()}%"
                                    else -> stringResource(R.string.player_download)
                                },
                                onClick = {
                                    when {
                                        dlDone -> viewModel.playOffline()
                                        dlActive -> viewModel.pauseResumeDownload()
                                        else -> viewModel.download()
                                    }
                                },
                            )
                            ActionChip(
                                icon = Icons.Rounded.MusicNote,
                                label = stringResource(R.string.player_audio_mode),
                                active = state.audioOnly,
                                onClick = { viewModel.setAudioOnly(!state.audioOnly) },
                            )
                            ActionChip(
                                icon = Icons.Rounded.Share,
                                label = stringResource(R.string.player_share),
                                onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${info?.title.orEmpty()}\n${viewModel.url}",
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(share, null))
                                },
                            )
                            ActionChip(
                                icon = if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                label = stringResource(
                                    if (state.isFavorite) R.string.player_favorited else R.string.player_favorite,
                                ),
                                active = state.isFavorite,
                                onClick = viewModel::toggleFavorite,
                            )
                            if (pipSupported && !state.audioOnly) {
                                ActionChip(
                                    icon = Icons.Rounded.PictureInPictureAlt,
                                    label = stringResource(R.string.player_pip),
                                    onClick = {
                                        activity?.enterPictureInPictureMode(
                                            PictureInPictureParams.Builder()
                                                .setAspectRatio(Rational(16, 9))
                                                .build(),
                                        )
                                    },
                                )
                            }
                        }
                        if (info?.description != null) {
                            Surface(
                                color = scheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .clickable { descriptionExpanded = !descriptionExpanded }
                                        .padding(12.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.player_description),
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Icon(
                                            imageVector = if (descriptionExpanded) {
                                                Icons.Rounded.ExpandLess
                                            } else {
                                                Icons.Rounded.ExpandMore
                                            },
                                            contentDescription = null,
                                            tint = scheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = info.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                        if (!info?.related.isNullOrEmpty()) {
                            Text(
                                text = stringResource(R.string.player_up_next),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            info!!.related.forEach { item ->
                                RelatedRow(item = item, onClick = { onOpenPlayer(item.url, item.title) })
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    onSeek: (Long) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onVideo = MaterialTheme.colorScheme.onVideo
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction
        ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val bufferedFraction =
        if (durationMs > 0) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        onSeek((offset.x / size.width * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragFraction?.let { onSeek((it * durationMs).toLong()) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                )
            },
    ) {
        val centerY = size.height / 2
        val trackHeight = 4.dp.toPx()
        val radius = CornerRadius(trackHeight / 2)
        drawRoundRect(
            color = onVideo.copy(alpha = 0.24f),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(size.width, trackHeight),
            cornerRadius = radius,
        )
        if (bufferedFraction > 0f) {
            drawRoundRect(
                color = onVideo.copy(alpha = 0.45f),
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = Size(size.width * bufferedFraction, trackHeight),
                cornerRadius = radius,
            )
        }
        if (fraction > 0f) {
            drawRoundRect(
                color = primary,
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = Size(size.width * fraction, trackHeight),
                cornerRadius = radius,
            )
        }
        val thumbX = size.width * fraction
        drawCircle(onVideo, radius = 7.dp.toPx(), center = Offset(thumbX, centerY))
        drawCircle(primary, radius = 5.dp.toPx(), center = Offset(thumbX, centerY))
    }
}

@Composable
private fun OverlayMenu(
    label: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onVideo,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) scheme.primaryContainer else scheme.surfaceContainerHigh,
        contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RelatedRow(item: FeedItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(128.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.surfaceVariant),
        ) {
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (item.durationSeconds > 0) {
                Surface(
                    color = scheme.videoScrim.copy(alpha = 0.75f),
                    contentColor = scheme.onVideo,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                ) {
                    Text(
                        text = formatDuration(item.durationSeconds.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(
                item.uploader,
                item.viewCount.takeIf { it >= 0 }
                    ?.let { stringResource(R.string.home_views, CompactNumber.format(it)) },
            ).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AudioStage(info: StreamInfo?) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (info?.thumbnailUrl != null) {
            AsyncImage(
                model = info.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.videoScrim.copy(alpha = 0.55f)),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.player_audio_stage),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onVideo,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Player parts - Light", showBackground = true)
@Composable
private fun PlayerPartsLightPreview() {
    com.raisoft.grabi.ui.theme.GrabiTheme(themeMode = com.raisoft.grabi.data.settings.ThemeMode.LIGHT) {
        PlayerPartsSample()
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Player parts - Dark", showBackground = true)
@Composable
private fun PlayerPartsDarkPreview() {
    com.raisoft.grabi.ui.theme.GrabiTheme(themeMode = com.raisoft.grabi.data.settings.ThemeMode.DARK) {
        PlayerPartsSample()
    }
}

@Composable
private fun PlayerPartsSample() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SeekBar(positionMs = 42_000, durationMs = 210_000, bufferedMs = 95_000, onSeek = {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip(icon = Icons.Rounded.Download, label = "Download", onClick = {})
            ActionChip(icon = Icons.Rounded.MusicNote, label = "Audio", active = true, onClick = {})
        }
        RelatedRow(
            item = FeedItem(
                url = "https://youtube.com/watch?v=preview",
                title = "Sample related video with a reasonably long title",
                uploader = "Raisoft Studio",
                uploaderAvatarUrl = null,
                thumbnailUrl = null,
                durationSeconds = 372,
                viewCount = 12_400,
            ),
            onClick = {},
        )
    }
}