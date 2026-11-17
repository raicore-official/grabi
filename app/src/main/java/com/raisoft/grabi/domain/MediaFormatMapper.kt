package com.raisoft.grabi.domain

import com.raisoft.grabi.domain.model.FormatSpec
import com.raisoft.grabi.domain.model.MediaKind
import com.raisoft.grabi.domain.model.MediaOption

object MediaFormatMapper {

    const val AVC_VIDEO_SELECTOR = "bv*[vcodec^=avc1][ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b"

    fun options(formats: List<FormatSpec>): List<MediaOption> {
        val heights = formats
            .filter { it.vcodec != null && it.vcodec != "none" && it.height > 0 }
            .map { it.height }
            .distinct()
            .sortedDescending()

        val video = heights.map { height ->
            val candidates = formats.filter {
                it.height == height && it.vcodec != null && it.vcodec != "none"
            }
            MediaOption(
                id = "video-$height",
                label = "${height}p",
                kind = MediaKind.VIDEO,
                formatSelector =
                    "bv*[vcodec^=avc1][height<=$height][ext=mp4]+ba[ext=m4a]/" +
                    "b[height<=$height][ext=mp4]/b[height<=$height]",
                audioFormat = null,
                sizeBytes = candidates.maxOf { it.sizeBytes }.takeIf { it > 0 },
            )
        }

        val audio = listOf(
            MediaOption("audio-mp3", "MP3", MediaKind.AUDIO, "bestaudio/best", "mp3", null),
            MediaOption("audio-m4a", "M4A", MediaKind.AUDIO, "bestaudio[ext=m4a]/bestaudio", "m4a", null),
            MediaOption("audio-opus", "Opus", MediaKind.AUDIO, "bestaudio/best", "opus", null),
        )
        return video + audio
    }
}