package com.raisoft.grabi.data.stream

import com.raisoft.grabi.domain.StreamOptionMapper
import com.raisoft.grabi.domain.model.StreamInfo
import com.raisoft.grabi.domain.model.StreamOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamExtractor

@Singleton
class StreamRepository @Inject constructor() {

    suspend fun resolve(url: String): StreamInfo = withContext(Dispatchers.IO) {
        when {
            isLocal(url) -> directInfo(url)
            url.contains("youtube.com") || url.contains("youtu.be") -> youtubeInfo(url)
            else -> directInfo(url)
        }
    }

    private fun isLocal(url: String): Boolean =
        url.startsWith("content://") || url.startsWith("file://") || url.startsWith("/")

    private fun directInfo(url: String): StreamInfo = StreamInfo(
        title = url.substringAfterLast('/').ifBlank { url },
        uploader = null,
        durationSeconds = 0,
        thumbnailUrl = null,
        videoOptions = listOf(
            StreamOption(
                id = "direct",
                label = "Original",
                videoUrl = url,
                audioUrl = null,
                height = 0,
                mimeType = null,
            ),
        ),
        audioOptions = emptyList(),
    )

    private fun youtubeInfo(url: String): StreamInfo {
        val extractor: StreamExtractor = ServiceList.YouTube.getStreamExtractor(url)
        extractor.fetchPage()
        val videos = (extractor.videoStreams + extractor.videoOnlyStreams).map {
            StreamOptionMapper.RawVideo(
                url = it.content,
                resolution = it.resolution,
                videoOnly = it.isVideoOnly,
                mimeType = it.format?.mimeType,
            )
        }
        val audios = extractor.audioStreams.map {
            StreamOptionMapper.RawAudio(
                url = it.content,
                bitrate = it.averageBitrate,
                mimeType = it.format?.mimeType,
            )
        }
        return StreamInfo(
            title = extractor.name,
            uploader = runCatching { extractor.uploaderName }.getOrNull(),
            durationSeconds = extractor.length,
            thumbnailUrl = runCatching { extractor.thumbnails.maxByOrNull { it.width }?.url }.getOrNull(),
            videoOptions = StreamOptionMapper.videoOptions(videos, audios),
            audioOptions = StreamOptionMapper.audioOptions(audios),
        )
    }
}