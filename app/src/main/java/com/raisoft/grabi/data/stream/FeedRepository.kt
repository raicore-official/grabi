package com.raisoft.grabi.data.stream

import com.raisoft.grabi.domain.model.FeedItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

@Singleton
class FeedRepository @Inject constructor() {

    suspend fun kiosks(): List<String> = withContext(Dispatchers.IO) {
        runCatching { ServiceList.YouTube.kioskList.availableKiosks.toList() }.getOrDefault(emptyList())
    }

    suspend fun defaultKiosk(): String? = withContext(Dispatchers.IO) {
        runCatching { ServiceList.YouTube.kioskList.defaultKioskId }.getOrNull()
    }

    suspend fun kioskItems(kioskId: String): List<FeedItem> = withContext(Dispatchers.IO) {
        val extractor = ServiceList.YouTube.kioskList.getExtractorById(kioskId, null)
        extractor.fetchPage()
        KioskInfo.getInfo(extractor).relatedItems.map(::toFeedItem)
    }

    suspend fun search(query: String): List<FeedItem> = withContext(Dispatchers.IO) {
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(query)
        SearchInfo.getInfo(service, handler).relatedItems.map(::toFeedItem)
    }

    private fun toFeedItem(item: StreamInfoItem) = FeedItem(
        url = item.url,
        title = item.name,
        uploader = runCatching { item.uploaderName }.getOrNull(),
        uploaderAvatarUrl = runCatching { item.uploaderAvatars.maxByOrNull { it.width }?.url }.getOrNull(),
        thumbnailUrl = runCatching { item.thumbnails.maxByOrNull { it.width }?.url }.getOrNull(),
        durationSeconds = item.duration,
        viewCount = item.viewCount,
    )
}