package com.raisoft.grabi.ui.screens.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.raisoft.grabi.data.db.FavoriteEntity
import com.raisoft.grabi.data.db.HistoryEntity
import com.raisoft.grabi.data.db.LibraryDao
import com.raisoft.grabi.data.download.DownloadManager
import com.raisoft.grabi.data.playback.PlaybackService
import com.raisoft.grabi.data.stream.StreamRepository
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.domain.model.MediaOption
import com.raisoft.grabi.domain.model.StreamInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val info: StreamInfo? = null,
    val selectedVideoId: String? = null,
    val selectedAudioId: String? = null,
    val audioOnly: Boolean = false,
    val speed: Float = 1f,
    val isFavorite: Boolean = false,
    val isPlaying: Boolean = false,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val streamRepository: StreamRepository,
    private val libraryDao: LibraryDao,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val url: String = savedStateHandle.get<String>("url").orEmpty()
    private val fallbackTitle: String = savedStateHandle.get<String>("title").orEmpty()

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui = _ui.asStateFlow()

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller = _controller.asStateFlow()

    init {
        viewModelScope.launch {
            libraryDao.observeFavorites().collect { favorites ->
                _ui.update { it.copy(isFavorite = favorites.any { entry -> entry.url == url }) }
            }
        }
        connectController()
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
                        controller.addListener(
                            object : Player.Listener {
                                override fun onIsPlayingChanged(isPlaying: Boolean) {
                                    _ui.update { it.copy(isPlaying = isPlaying) }
                                    if (!isPlaying) savePosition()
                                }
                            },
                        )
                        _controller.value = controller
                        loadStream()
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

    fun retry() {
        _ui.update { it.copy(loading = true, error = null) }
        if (_controller.value != null) loadStream() else connectController()
    }

    private fun loadStream() {
        viewModelScope.launch {
            val info = runCatching { streamRepository.resolve(url) }
                .getOrElse { error ->
                    _ui.update {
                        it.copy(loading = false, error = error.message ?: "Could not load this link")
                    }
                    return@launch
                }
            val resumePosition = libraryDao.historyByUrl(url)?.positionMs ?: 0L
            _ui.update {
                it.copy(
                    loading = false,
                    info = info,
                    selectedVideoId = it.selectedVideoId ?: info.videoOptions.firstOrNull()?.id,
                    selectedAudioId = it.selectedAudioId ?: info.audioOptions.firstOrNull()?.id,
                )
            }
            libraryDao.upsertHistory(
                HistoryEntity(
                    url = url,
                    title = info.title,
                    uploader = info.uploader,
                    thumbnailUrl = info.thumbnailUrl,
                    durationSeconds = info.durationSeconds,
                    positionMs = resumePosition,
                    watchedAt = System.currentTimeMillis(),
                ),
            )
            applyMedia(resumePosition)
        }
    }

    fun selectVideo(id: String) {
        _ui.update { it.copy(selectedVideoId = id, audioOnly = false) }
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
        val title = _ui.value.info?.title ?: fallbackTitle
        viewModelScope.launch {
            downloadManager.enqueue(
                url = url,
                title = title,
                option = MediaOption(
                    id = "best",
                    label = "Best",
                    kind = MediaKind.VIDEO,
                    formatSelector = "bestvideo+bestaudio/best",
                    audioFormat = null,
                    sizeBytes = null,
                ),
                subtitles = false,
            )
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
        val controller = _controller.value ?: return
        val state = _ui.value
        val info = state.info ?: return
        val option = if (state.audioOnly) {
            info.audioOptions.firstOrNull { it.id == state.selectedAudioId }
                ?: info.audioOptions.firstOrNull()
        } else {
            info.videoOptions.firstOrNull { it.id == state.selectedVideoId }
                ?: info.videoOptions.firstOrNull()
        } ?: return
        controller.sendCustomCommand(
            SessionCommand(PlaybackService.ACTION_SET_MEDIA, Bundle.EMPTY),
            bundleOf(
                PlaybackService.KEY_VIDEO_URL to option.videoUrl,
                PlaybackService.KEY_AUDIO_URL to option.audioUrl,
                PlaybackService.KEY_POSITION_MS to positionMs,
            ),
        )
        controller.playWhenReady = true
    }

    override fun onCleared() {
        _controller.value?.release()
        _controller.value = null
        super.onCleared()
    }
}