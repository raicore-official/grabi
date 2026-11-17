package com.raisoft.grabi.domain

import com.raisoft.grabi.domain.model.StreamOption

object StreamOptionMapper {

    data class RawVideo(
        val url: String,
        val resolution: String,
        val videoOnly: Boolean,
        val mimeType: String?,
        val bitrate: Int = 0,
    )

    data class RawAudio(
        val url: String,
        val bitrate: Int,
        val mimeType: String?,
    )

    fun videoOptions(
        videos: List<RawVideo>,
        audios: List<RawAudio>,
        durationSeconds: Long = 0,
    ): List<StreamOption> {
        val bestAudio = audios.maxByOrNull { it.bitrate }
        return videos.mapIndexed { index, video ->
            StreamOption(
                id = "v$index",
                label = video.resolution.ifBlank { "Video ${index + 1}" },
                videoUrl = video.url,
                audioUrl = if (video.videoOnly) bestAudio?.url else null,
                height = video.resolution.substringBefore('p').toIntOrNull() ?: 0,
                mimeType = video.mimeType,
                sizeBytes = estimateSize(video.bitrate, durationSeconds),
            )
        }.sortedWith(
            compareByDescending<StreamOption> { it.height }
                .thenBy { if (it.mimeType?.contains("mp4") == true) 0 else 1 },
        )
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

    fun progressiveFallback(options: List<StreamOption>): StreamOption? =
        options.filter { it.videoUrl != null && it.audioUrl == null }
            .maxByOrNull { it.height }

    private fun estimateSize(bitrate: Int, durationSeconds: Long): Long? =
        if (bitrate > 0 && durationSeconds > 0) bitrate * 1000L * durationSeconds / 8 else null

    private fun audioLabel(audio: RawAudio): String {
        val codec = audio.mimeType?.substringAfter('/') ?: "audio"
        return if (audio.bitrate > 0) "$codec · ${audio.bitrate} kbps" else codec
    }
}