package com.raisoft.grabi.domain.model

data class FeedItem(
    val url: String,
    val title: String,
    val uploader: String?,
    val uploaderAvatarUrl: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val viewCount: Long,
)