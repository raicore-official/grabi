package com.raisoft.grabi.ui.screens.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.raisoft.grabi.data.db.DownloadEntity
import com.raisoft.grabi.data.db.FavoriteEntity
import com.raisoft.grabi.data.db.HistoryEntity
import com.raisoft.grabi.data.db.LibraryDao
import com.raisoft.grabi.data.download.DownloadManager
import com.raisoft.grabi.data.download.YtDlpEngine
import com.raisoft.grabi.data.playback.DebugLog
import com.raisoft.grabi.data.playback.PlaybackRetryPolicy
import com.raisoft.grabi.data.playback.PlaybackService
import com.raisoft.grabi.data.stream.StreamRepository
import com.raisoft.grabi.domain.MediaFormatMapper
import com.raisoft.grabi.domain.StreamOptionMapper
import com.raisoft.grabi.domain.VideoIds
import com.raisoft.grabi.domain.model.DownloadStatus
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.domain.model.MediaOption
import com.raisoft.grabi.domain.model.StreamInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class PlayerUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val info: StreamInfo? = null,
    val thumbnailUrl: String? = null,
    val selectedVideoId: String? = null,
    val selectedAudioId: String? = null,
    val audioOnly: Boolean = false,
    val speed: Float = 1f,
    val isFavorite: Boolean = false,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = true,
    val buffering: Boolean = false,
    val captionsEnabled: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val streamRepository: StreamRepository,
    private val libraryDao: LibraryDao,
    private val downloadManager: DownloadManager,
    private val ytDlpEngine: YtDlpEngine,
) : ViewModel() {

    val url: String = savedStateHandle.get<String>("url").orEmpty()
    private val fallbackTitle: String = savedStateHandle.get<String>("title").orEmpty()
    private val videoId: String = VideoIds.of(url)

    private val _ui = MutableStateFlow(PlayerUiState(thumbnailUrl = savedStateHandle.get<String>("thumb")))
    val ui = _ui.asStateFlow()

    val downloadState: StateFlow<DownloadEntity?> =
        downloadManager.statesForKey(videoId, MediaKind.VIDEO.name)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller = _controller.asStateFlow()

    private var recoveryAttempt = 0
    private var recovering = false
    private var lastKnownPositionMs = 0L

    init {
        viewModelScope.launch {
            libraryDao.observeFavorites().collect { favorites ->
                _ui.update { it.copy(isFavorite = favorites.any { entry -> entry.url == url }) }
            }
        }
        connectController()
        viewModelScope.launch { loadStream() }
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                val controller = _controller.value ?: continue
                val position = controller.currentPosition
                if (position > 0) lastKnownPositionMs = position
                _ui.update {
                    it.copy(
                        positionMs = position,
                        durationMs = controller.duration.coerceAtLeast(0L),
                        bufferedMs = controller.bufferedPosition,
                    )
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                savePosition()
            }
        }
    }

    private fun connectController() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { controller ->
                        controller.addListener(playerListener)
                        _controller.value = controller
                    }
                    .onFailure { error ->
                        _ui.update {
                            it.copy(loading = false, error = error.message ?: "Player unavailable")
                        }
                    }
            },
            { it.run() },
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _ui.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) savePosition()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _ui.update { it.copy(playWhenReady = playWhenReady) }
        }

        override fun onPlaybackStateChanged(state: Int) {
            DebugLog.line("player ui state=$state video=$videoId")
            _ui.update { it.copy(buffering = state == Player.STATE_BUFFERING) }
            if (state == Player.STATE_READY) {
                recoveryAttempt = 0
                recovering = false
            }
            if (state == Player.STATE_ENDED) savePosition()
        }

        override fun onPlayerError(error: PlaybackException) {
            DebugLog.line("player ui error ${error.errorCodeName}: ${error.message}")
            handlePlaybackError(error)
        }
    }

    private fun handlePlaybackError(error: PlaybackException) {
        if (recovering) return
        recovering = true
        val httpStatus = (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode
        val expirySuspected = PlaybackRetryPolicy.isExpirySuspected(httpStatus, error.errorCode)
        val position = maxOf(_controller.value?.currentPosition ?: 0L, lastKnownPositionMs)
        val action = PlaybackRetryPolicy.nextAction(recoveryAttempt, expirySuspected)
        recoveryAttempt++
        val raw = error.message ?: "Playback failed"
        when (action) {
            PlaybackRetryPolicy.Action.REEXTRACT -> reextract(position, raw)
            PlaybackRetryPolicy.Action.PROGRESSIVE_FALLBACK -> progressiveFallback(position, raw)
            PlaybackRetryPolicy.Action.YTDLP_FALLBACK -> ytDlpFallback(position, raw)
            PlaybackRetryPolicy.Action.FAIL -> fail(raw)
        }
    }

    private fun reextract(positionMs: Long, raw: String) {
        _ui.update { it.copy(notice = "Stream expired — refreshing links…", buffering = true) }
        viewModelScope.launch {
            val refreshed = runCatching { streamRepository.resolve(url, forceRefresh = true) }.getOrNull()
            if (refreshed != null && refreshed.videoOptions.isNotEmpty()) {
                _ui.update {
                    it.copy(
                        info = refreshed,
                        selectedVideoId = refreshed.videoOptions.firstOrNull()?.id,
                        selectedAudioId = refreshed.audioOptions.firstOrNull()?.id,
                    )
                }
                recovering = false
                applyMedia(positionMs)
            } else {
                progressiveFallback(positionMs, raw)
            }
        }
    }

    private fun progressiveFallback(positionMs: Long, raw: String) {
        val fallback = _ui.value.info?.let { StreamOptionMapper.progressiveFallback(it.videoOptions) }
        if (fallback != null) {
            _ui.update { it.copy(notice = "Switched to a compatible stream (${fallback.label})") }
            sendMedia(fallback.videoUrl, null, positionMs)
            recovering = false
        } else {
            ytDlpFallback(positionMs, raw)
        }
    }

    private fun ytDlpFallback(positionMs: Long, raw: String) {
        _ui.update { it.copy(notice = "Trying yt-dlp fallback…", buffering = true) }
        viewModelScope.launch {
            val urls = runCatching { ytDlpEngine.streamUrls(url, "best[height<=720]/best") }
                .getOrDefault(emptyList())
            val direct = urls.firstOrNull()
            if (direct != null) {
                _ui.update { it.copy(notice = "Playing via yt-dlp") }
                sendMedia(direct, null, positionMs)
                recovering = false
            } else {
                fail(raw)
            }
        }
    }

    private fun fail(raw: String) {
        recovering = false
        recoveryAttempt = 0
        _ui.update {
            it.copy(loading = false, buffering = false, error = PlaybackRetryPolicy.friendlyMessage(raw))
        }
    }

    fun retry() {
        recoveryAttempt = 0
        recovering = false
        _ui.update { it.copy(loading = true, error = null, notice = null) }
        if (_controller.value != null) loadStream(forceRefresh = true) else connectController()
    }

    fun consumeNotice() {
        _ui.update { it.copy(notice = null) }
    }

    private fun loadStream(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val local = downloadManager.localFileFor(videoId, MediaKind.VIDEO.name)
            val target = local ?: url
            val info = runCatching { streamRepository.resolve(target, forceRefresh) }
                .getOrElse { error ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            error = PlaybackRetryPolicy.friendlyMessage(
                                error.message ?: "Could not load this link",
                            ),
                        )
                    }
                    return@launch
                }
            val shown = if (local != null) {
                info.copy(title = fallbackTitle.ifBlank { info.title })
            } else {
                info
            }
            val resumePosition = libraryDao.historyByUrl(url)?.positionMs ?: 0L
            lastKnownPositionMs = resumePosition
            val fastStart = shown.videoOptions.firstOrNull {
                it.audioUrl == null && it.height in 1..720 && it.mimeType?.contains("mp4") == true
            } ?: shown.videoOptions.firstOrNull { it.audioUrl == null && it.height in 1..720 }
            _ui.update {
                it.copy(
                    loading = false,
                    info = shown,
                    thumbnailUrl = it.thumbnailUrl ?: shown.thumbnailUrl,
                    selectedVideoId = it.selectedVideoId ?: fastStart?.id
                        ?: shown.videoOptions.firstOrNull()?.id,
                    selectedAudioId = it.selectedAudioId ?: shown.audioOptions.firstOrNull()?.id,
                    captionsEnabled = shown.subtitleUrl != null,
                )
            }
            libraryDao.upsertHistory(
                HistoryEntity(
                    url = url,
                    title = shown.title,
                    uploader = shown.uploader,
                    thumbnailUrl = shown.thumbnailUrl,
                    durationSeconds = shown.durationSeconds,
                    positionMs = resumePosition,
                    watchedAt = System.currentTimeMillis(),
                ),
            )
            if (awaitController() != null) applyMedia(resumePosition)
        }
    }

    private suspend fun awaitController(): MediaController? = withTimeoutOrNull(10_000) {
        while (_controller.value == null) delay(50)
        _controller.value
    }

    fun togglePlay() {
        val controller = _controller.value ?: return
        if (controller.playbackState == Player.STATE_ENDED) {
            controller.seekTo(0)
            controller.play()
        } else {
            controller.playWhenReady = !controller.playWhenReady
        }
    }

    fun seekTo(positionMs: Long) {
        _controller.value?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val controller = _controller.value ?: return
        controller.seekTo((controller.currentPosition + deltaMs).coerceAtLeast(0L))
    }

    fun selectVideo(id: String) {
        recoveryAttempt = 0
        _ui.update { it.copy(selectedVideoId = id, audioOnly = false, error = null) }
        applyMedia(currentPosition())
    }

    fun selectAudio(id: String) {
        _ui.update { it.copy(selectedAudioId = id) }
        if (_ui.value.audioOnly) applyMedia(currentPosition())
    }

    fun setAudioOnly(enabled: Boolean) {
        _ui.update { it.copy(audioOnly = enabled) }
        applyMedia(currentPosition())
    }

    fun setSpeed(speed: Float) {
        _controller.value?.playbackParameters = PlaybackParameters(speed)
        _ui.update { it.copy(speed = speed) }
    }

    fun setCaptions(enabled: Boolean) {
        val controller = _controller.value ?: return
        controller.trackSelectionParameters = controller.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
        _ui.update { it.copy(captionsEnabled = enabled) }
    }

    fun toggleFavorite() {
        val info = _ui.value.info
        viewModelScope.launch {
            if (_ui.value.isFavorite) {
                libraryDao.deleteFavorite(url)
            } else {
                libraryDao.upsertFavorite(
                    FavoriteEntity(
                        url = url,
                        title = info?.title ?: fallbackTitle,
                        uploader = info?.uploader,
                        thumbnailUrl = info?.thumbnailUrl,
                        durationSeconds = info?.durationSeconds ?: 0L,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    fun download() {
        val existing = downloadState.value
        if (existing != null && (
            existing.status in setOf(
                DownloadStatus.QUEUED.name,
                DownloadStatus.RUNNING.name,
                DownloadStatus.PAUSED.name,
                DownloadStatus.WAITING_WIFI.name,
            ) || (existing.status == DownloadStatus.DONE.name &&
                downloadManager.fileExists(existing))
        )) {
            _ui.update {
                it.copy(
                    notice = if (existing.status == DownloadStatus.DONE.name) {
                        "Already downloaded"
                    } else {
                        "Download already in progress"
                    },
                )
            }
            return
        }
        val title = _ui.value.info?.title ?: fallbackTitle
        val label = _ui.value.info?.videoOptions
            ?.firstOrNull { it.id == _ui.value.selectedVideoId }?.label ?: "Best"
        viewModelScope.launch {
            downloadManager.enqueue(
                url = url,
                title = title,
                option = MediaOption(
                    id = "best",
                    label = label,
                    kind = MediaKind.VIDEO,
                    formatSelector = MediaFormatMapper.AVC_VIDEO_SELECTOR,
                    audioFormat = null,
                    sizeBytes = null,
                ),
                subtitles = false,
                videoId = videoId,
            )
            _ui.update { it.copy(notice = "Download queued") }
        }
    }

    fun pauseResumeDownload() {
        val entity = downloadState.value ?: return
        if (entity.status == DownloadStatus.PAUSED.name) {
            downloadManager.resume(entity.id)
        } else {
            downloadManager.pause(entity.id)
        }
    }

    fun playOffline() {
        viewModelScope.launch {
            val local = downloadManager.localFileFor(videoId, MediaKind.VIDEO.name)
            if (local != null) {
                sendMedia(local, null, currentPosition())
                _ui.update { it.copy(notice = "Playing downloaded file") }
            }
        }
    }

    fun savePosition() {
        val controller = _controller.value ?: return
        val position = controller.currentPosition
        if (position <= 0) return
        viewModelScope.launch {
            libraryDao.updatePosition(url, position, System.currentTimeMillis())
        }
    }

    private fun currentPosition(): Long = _controller.value?.currentPosition ?: 0L

    private fun applyMedia(positionMs: Long) {
        val state = _ui.value
        val info = state.info ?: return
        val option = if (state.audioOnly) {
            info.audioOptions.firstOrNull { it.id == state.selectedAudioId }
                ?: info.audioOptions.firstOrNull()
        } else {
            info.videoOptions.firstOrNull { it.id == state.selectedVideoId }
                ?: info.videoOptions.firstOrNull()
        } ?: return
        sendMedia(option.videoUrl, option.audioUrl, positionMs, info)
    }

    private fun sendMedia(
        videoUrl: String?,
        audioUrl: String?,
        positionMs: Long,
        info: StreamInfo? = _ui.value.info,
    ) {
        val controller = _controller.value ?: return
        runCatching {
            controller.sendCustomCommand(
                SessionCommand(PlaybackService.ACTION_SET_MEDIA, Bundle.EMPTY),
                bundleOf(
                    PlaybackService.KEY_VIDEO_URL to videoUrl,
                    PlaybackService.KEY_AUDIO_URL to audioUrl,
                    PlaybackService.KEY_POSITION_MS to positionMs,
                    PlaybackService.KEY_SUBTITLE_URL to info?.subtitleUrl,
                    PlaybackService.KEY_SUBTITLE_LANGUAGE to info?.subtitleLanguage,
                    PlaybackService.KEY_MEDIA_ID to "${videoUrl.orEmpty()}|${audioUrl.orEmpty()}",
                    PlaybackService.KEY_PAGE_URL to url,
                    PlaybackService.KEY_TITLE to (info?.title ?: fallbackTitle).toString(),
                ),
            )
            controller.playWhenReady = true
        }.onFailure { error ->
            DebugLog.line("media command failed ${error.message}")
            _ui.update {
                it.copy(
                    buffering = false,
                    error = error.message ?: "Unable to start playback",
                )
            }
        }
    }

    override fun onCleared() {
        _controller.value?.removeListener(playerListener)
        _controller.value?.release()
        _controller.value = null
        super.onCleared()
    }
}