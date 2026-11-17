package com.raisoft.grabi.data.playback

import androidx.media3.common.PlaybackException

object PlaybackRetryPolicy {

    enum class Action { REEXTRACT, PROGRESSIVE_FALLBACK, YTDLP_FALLBACK, FAIL }

    const val MAX_ATTEMPTS = 3

    fun isExpirySuspected(httpStatus: Int?, media3ErrorCode: Int): Boolean =
        httpStatus == 403 || httpStatus == 410 ||
            media3ErrorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
            media3ErrorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED

    fun nextAction(attempt: Int, expirySuspected: Boolean): Action = when {
        attempt >= MAX_ATTEMPTS -> Action.FAIL
        attempt == 0 -> if (expirySuspected) Action.REEXTRACT else Action.PROGRESSIVE_FALLBACK
        attempt == 1 -> if (expirySuspected) Action.PROGRESSIVE_FALLBACK else Action.REEXTRACT
        else -> Action.YTDLP_FALLBACK
    }

    fun friendlyMessage(raw: String?): String {
        val message = raw.orEmpty().lowercase()
        return when {
            message.contains("age") -> "This video is age-restricted and cannot be played without a signed-in account."
            message.contains("region") || message.contains("country") -> "This video is not available in your region."
            message.contains("private") -> "This video is private."
            message.contains("unavailable") || message.contains("removed") -> "This video is unavailable or was removed."
            message.contains("live") -> "Live streams can be unstable; try again in a moment."
            else -> raw?.take(300) ?: "Playback failed."
        }
    }
}