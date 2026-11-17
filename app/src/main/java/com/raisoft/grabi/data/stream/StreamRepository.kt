package com.raisoft.grabi.data.stream

import com.raisoft.grabi.domain.StreamOptionMapper
import com.raisoft.grabi.domain.model.FeedItem
import com.raisoft.grabi.domain.model.StreamInfo
import com.raisoft.grabi.domain.model.StreamOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

@Singleton
class StreamRepository @Inject constructor(
    private val cache: StreamCache,
) {

    suspend fun resolve(url: String, forceRefresh: Boolean = false): StreamInfo =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) cache.get(url)?.let { return@withContext it }
            val info = when {
                isLocal(url) -> directInfo(url)
                url.contains("youtube.com") || url.contains("youtu.be") -> youtubeInfo(url)
                else -> directInfo(url)
            }
            if (!isLocal(url)) cache.put(url, info)
            info
        }

    fun invalidate(url: String) {
        cache.invalidate(url)
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
        val live = runCatching {
            extractor.streamType == StreamType.LIVE_STREAM ||
                extractor.streamType == StreamType.AUDIO_LIVE_STREAM
        }.getOrDefault(false)
        val hls = runCatching { extractor.hlsUrl }.getOrNull()?.takeIf { it.isNotBlank() }
        if (live && hls != null) {
            return StreamInfo(
                title = extractor.name,
                uploader = runCatching { extractor.uploaderName }.getOrNull(),
                durationSeconds = 0,
                thumbnailUrl = bestThumbnail(extractor),
                videoOptions = listOf(
                    StreamOption(
                        id = "live",
                        label = "LIVE",
                        videoUrl = hls,
                        audioUrl = null,
                        height = 0,
                        mimeType = "application/x-mpegURL",
                    ),
                ),
                audioOptions = emptyList(),
                isLive = true,
                hlsUrl = hls,
                viewCount = runCatching { extractor.viewCount }.getOrDefault(-1),
                uploaderUrl = runCatching { extractor.uploaderUrl }.getOrNull(),
                uploaderAvatarUrl = runCatching {
                    extractor.uploaderAvatars.maxByOrNull { it.width }?.url
                }.getOrNull(),
                related = relatedItems(extractor),
            )
        }
        val videos = (extractor.videoStreams + extractor.videoOnlyStreams).map {
            StreamOptionMapper.RawVideo(
                url = it.content,
                resolution = it.resolution,
                videoOnly = it.isVideoOnly,
                mimeType = it.format?.mimeType,
                bitrate = runCatching { it.bitrate }.getOrDefault(0),
            )
        }
        val audios = extractor.audioStreams.map {
            StreamOptionMapper.RawAudio(
                url = it.content,
                bitrate = it.averageBitrate,
                mimeType = it.format?.mimeType,
            )
        }
        val subtitle = runCatching {
            extractor.subtitles.firstOrNull { it.languageCode == "en" }
                ?: extractor.subtitles.firstOrNull()
        }.getOrNull()
        return StreamInfo(
            title = extractor.name,
            uploader = runCatching { extractor.uploaderName }.getOrNull(),
            durationSeconds = extractor.length,
            thumbnailUrl = bestThumbnail(extractor),
            videoOptions = StreamOptionMapper.videoOptions(videos, audios, extractor.length),
            audioOptions = StreamOptionMapper.audioOptions(audios),
            description = runCatching { extractor.description }.getOrNull(),
            viewCount = runCatching { extractor.viewCount }.getOrDefault(-1),
            likeCount = runCatching { extractor.likeCount }.getOrDefault(-1),
            uploadDate = runCatching { extractor.textualUploadDate }.getOrNull(),
            uploaderUrl = runCatching { extractor.uploaderUrl }.getOrNull(),
            uploaderAvatarUrl = runCatching {
                extractor.uploaderAvatars.maxByOrNull { it.width }?.url
            }.getOrNull(),
            subtitleUrl = subtitle?.content,
            subtitleLanguage = subtitle?.languageCode,
            related = relatedItems(extractor),
        )
    }

    private fun bestThumbnail(extractor: StreamExtractor): String? =
        runCatching { extractor.thumbnails.maxByOrNull { it.width }?.url }.getOrNull()

    private fun relatedItems(extractor: StreamExtractor): List<FeedItem> =
        runCatching {
            extractor.relatedItems.filterIsInstance<StreamInfoItem>().take(20).map { item ->
                FeedItem(
                    url = item.url,
                    title = item.name,
                    uploader = runCatching { item.uploaderName }.getOrNull(),
                    uploaderAvatarUrl = runCatching {
                        item.uploaderAvatars.maxByOrNull { it.width }?.url
                    }.getOrNull(),
                    thumbnailUrl = runCatching {
                        item.thumbnails.maxByOrNull { it.width }?.url
                    }.getOrNull(),
                    durationSeconds = item.duration,
                    viewCount = item.viewCount,
                )
            }
        }.getOrDefault(emptyList())
}