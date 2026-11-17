package com.raisoft.grabi.data.playback

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
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

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
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

    private fun buildMediaSource(videoUrl: String?, audioUrl: String?): MediaSource? {
        val video = videoUrl?.let { progressiveSource(it) }
        val audio = audioUrl?.let { progressiveSource(it) }
        return when {
            video != null && audio != null -> MergingMediaSource(video, audio)
            video != null -> video
            else -> audio
        }
    }

    private fun progressiveSource(url: String): MediaSource {
        val factory = if (url.startsWith("http")) {
            DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(this, "Grabi"))
                .setAllowCrossProtocolRedirects(true)
        } else {
            DefaultDataSource.Factory(this)
        }
        return ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(url))
    }

    companion object {
        const val ACTION_SET_MEDIA = "com.raisoft.grabi.SET_MEDIA"
        const val KEY_VIDEO_URL = "video_url"
        const val KEY_AUDIO_URL = "audio_url"
        const val KEY_POSITION_MS = "position_ms"
    }
}