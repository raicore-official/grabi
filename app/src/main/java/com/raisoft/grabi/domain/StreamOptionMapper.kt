package com.raisoft.grabi.domain

import com.raisoft.grabi.domain.model.StreamOption

object StreamOptionMapper {

    data class RawVideo(
        val url: String,
        val resolution: String,
        val videoOnly: Boolean,
        val mimeType: String?,
    )

    data class RawAudio(
        val url: String,
        val bitrate: Int,
        val mimeType: String?,
    )

    fun videoOptions(videos: List<RawVideo>, audios: List<RawAudio>): List<StreamOption> {
        val bestAudio = audios.maxByOrNull { it.bitrate }
        return videos.mapIndexed { index, video ->
            StreamOption(
                id = "v$index",
                label = video.resolution.ifBlank { "Video ${index + 1}" },
                videoUrl = video.url,
                audioUrl = if (video.videoOnly) bestAudio?.url else null,
                height = video.resolution.substringBefore('p').toIntOrNull() ?: 0,
                mimeType = video.mimeType,
            )
        }.sortedByDescending { it.height }
    }

    fun audioOptions(audios: List<RawAudio>): List<StreamOption> =
        audios.sortedByDescending { it.bitrate }.mapIndexed { index, audio ->
            StreamOption(
                id = "a$index",
                label = audioLabel(audio),
                videoUrl = null,
                audioUrl = audio.url,
                height = 0,
                mimeType = audio.mimeType,
            )
        }

    private fun audioLabel(audio: RawAudio): String {
        val codec = audio.mimeType?.substringAfter('/') ?: "audio"
        return if (audio.bitrate > 0) "$codec · ${audio.bitrate} kbps" else codec
    }
}