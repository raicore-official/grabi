package com.raisoft.grabi.data.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.raisoft.grabi.MainActivity
import com.raisoft.grabi.data.db.LibraryDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var httpClient: OkHttpClient
    @Inject lateinit var libraryDao: LibraryDao

    private var session: MediaSession? = null
    private var currentMediaId: String? = null
    private var currentPageUrl: String? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15_000, 50_000, 1_000, 2_500)
                    .build(),
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .build()
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    DebugLog.line("player state=$state")
                    when (state) {
                        Player.STATE_ENDED -> persistPosition(reset = true)
                        Player.STATE_IDLE -> currentMediaId = null
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) persistPosition()
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    DebugLog.line("player error ${error.errorCodeName}: ${error.message}")
                }
            },
        )
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(SessionCallback(player))
            .build()
        addSession(session!!)
        serviceScope.launch {
            while (isActive) {
                delay(5_000)
                // ExoPlayer getters assert the application thread; read them on Main only.
                val snapshot = withContext(Dispatchers.Main) {
                    val page = currentPageUrl
                    if (page != null && player.isPlaying) page to player.currentPosition else null
                }
                if (snapshot != null && snapshot.second > 0) {
                    savePosition(snapshot.first, snapshot.second)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        val page = currentPageUrl
        val position = session?.player?.currentPosition ?: 0L
        if (page != null && position > 0) {
            runBlocking {
                runCatching { libraryDao.updatePosition(page, position, System.currentTimeMillis()) }
            }
        }
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private fun persistPosition(reset: Boolean = false) {
        val page = currentPageUrl ?: return
        val position = if (reset) 0L else session?.player?.currentPosition ?: return
        if (!reset && position <= 0) return
        savePosition(page, position)
    }

    private fun savePosition(page: String, position: Long) {
        serviceScope.launch {
            runCatching { libraryDao.updatePosition(page, position, System.currentTimeMillis()) }
        }
    }

    private inner class SessionCallback(private val player: ExoPlayer) : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_SET_MEDIA, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_SET_MEDIA) {
                val mediaId = args.getString(KEY_MEDIA_ID)
                val alreadyActive = mediaId != null &&
                    mediaId == currentMediaId &&
                    player.playbackState != Player.STATE_IDLE &&
                    player.playbackState != Player.STATE_ENDED
                if (!alreadyActive) {
                    val source = runCatching {
                        buildMediaSource(
                            videoUrl = args.getString(KEY_VIDEO_URL),
                            audioUrl = args.getString(KEY_AUDIO_URL),
                            subtitleUrl = args.getString(KEY_SUBTITLE_URL),
                            subtitleLanguage = args.getString(KEY_SUBTITLE_LANGUAGE),
                            title = args.getString(KEY_TITLE),
                        )
                    }.onFailure { error ->
                        DebugLog.line("source build failed ${error.message}")
                    }.getOrNull()
                    if (source != null) {
                        runCatching {
                            player.stop()
                            player.setMediaSource(source, args.getLong(KEY_POSITION_MS, 0L))
                            player.prepare()
                            currentMediaId = mediaId
                            currentPageUrl = args.getString(KEY_PAGE_URL)
                        }.onFailure { error ->
                            DebugLog.line("source prepare failed ${error.message}")
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun buildMediaSource(
        videoUrl: String?,
        audioUrl: String?,
        subtitleUrl: String?,
        subtitleLanguage: String?,
        title: String?,
    ): MediaSource? {
        val video = videoUrl?.let { mediaSource(it, subtitleUrl, subtitleLanguage, title) }
        val audio = audioUrl?.let { mediaSource(it, null, null, title) }
        return when {
            video != null && audio != null -> MergingMediaSource(video, audio)
            video != null -> video
            else -> audio
        }
    }

    private fun mediaSource(
        url: String,
        subtitleUrl: String?,
        subtitleLanguage: String?,
        title: String? = null,
    ): MediaSource {
        val uri = Uri.parse(url)
        val builder = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsPlayable(true)
                    .build(),
            )
        if (subtitleUrl != null) {
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(subtitleLanguage)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build(),
                ),
            )
        }
        val item = builder.build()
        val factory = dataSourceFactory(url)
        return when (Util.inferContentType(uri)) {
            C.CONTENT_TYPE_HLS -> HlsMediaSource.Factory(factory).createMediaSource(item)
            C.CONTENT_TYPE_DASH -> DashMediaSource.Factory(factory).createMediaSource(item)
            else -> ProgressiveMediaSource.Factory(factory).createMediaSource(item)
        }
    }

    private fun dataSourceFactory(url: String): DataSource.Factory =
        if (url.startsWith("http")) {
            OkHttpDataSource.Factory(httpClient)
                .setUserAgent(Util.getUserAgent(this, "Grabi"))
        } else {
            DefaultDataSource.Factory(this)
        }

    companion object {
        const val ACTION_SET_MEDIA = "com.raisoft.grabi.SET_MEDIA"
        const val KEY_VIDEO_URL = "video_url"
        const val KEY_AUDIO_URL = "audio_url"
        const val KEY_POSITION_MS = "position_ms"
        const val KEY_SUBTITLE_URL = "subtitle_url"
        const val KEY_SUBTITLE_LANGUAGE = "subtitle_language"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_PAGE_URL = "page_url"
        const val KEY_TITLE = "title"
    }
}