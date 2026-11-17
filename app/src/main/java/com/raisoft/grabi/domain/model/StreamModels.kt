package com.raisoft.grabi.domain.model

data class StreamOption(
    val id: String,
    val label: String,
    val videoUrl: String?,
    val audioUrl: String?,
    val height: Int,
    val mimeType: String?,
    val sizeBytes: Long? = null,
)

data class StreamInfo(
    val title: String,
    val uploader: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val videoOptions: List<StreamOption>,
    val audioOptions: List<StreamOption>,
    val description: String? = null,
    val viewCount: Long = -1,
    val likeCount: Long = -1,
    val uploadDate: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatarUrl: String? = null,
    val isLive: Boolean = false,
    val hlsUrl: String? = null,
    val subtitleUrl: String? = null,
    val subtitleLanguage: String? = null,
    val related: List<FeedItem> = emptyList(),
)