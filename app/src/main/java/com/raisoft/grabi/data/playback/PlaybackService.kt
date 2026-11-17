package com.raisoft.grabi.data.playback

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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var httpClient: OkHttpClient

    private var session: MediaSession? = null

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
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    DebugLog.line("player error ${error.errorCodeName}: ${error.message}")
                }
            },
        )
        session = MediaSession.Builder(this, player)
            .setCallback(SessionCallback(player))
            .build()
        addSession(session!!)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
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
                val source = buildMediaSource(
                    videoUrl = args.getString(KEY_VIDEO_URL),
                    audioUrl = args.getString(KEY_AUDIO_URL),
                    subtitleUrl = args.getString(KEY_SUBTITLE_URL),
                    subtitleLanguage = args.getString(KEY_SUBTITLE_LANGUAGE),
                )
                if (source != null) {
                    player.setMediaSource(source, args.getLong(KEY_POSITION_MS, 0L))
                    player.prepare()
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
    ): MediaSource? {
        val video = videoUrl?.let { mediaSource(it, subtitleUrl, subtitleLanguage) }
        val audio = audioUrl?.let { mediaSource(it, null, null) }
        return when {
            video != null && audio != null -> MergingMediaSource(video, audio)
            video != null -> video
            else -> audio
        }
    }

    private fun mediaSource(url: String, subtitleUrl: String?, subtitleLanguage: String?): MediaSource {
        val uri = Uri.parse(url)
        val builder = MediaItem.Builder().setUri(uri)
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
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)
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
    }
}