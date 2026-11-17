package com.raisoft.grabi.domain.model

data class FormatSpec(
    val formatId: String,
    val ext: String?,
    val height: Int,
    val vcodec: String?,
    val acodec: String?,
    val sizeBytes: Long,
)

enum class MediaKind { VIDEO, AUDIO }

data class MediaOption(
    val id: String,
    val label: String,
    val kind: MediaKind,
    val formatSelector: String,
    val audioFormat: String?,
    val sizeBytes: Long?,
)

data class MediaInfo(
    val title: String,
    val uploader: String?,
    val durationSeconds: Int,
    val thumbnailUrl: String?,
    val webpageUrl: String?,
    val options: List<MediaOption>,
)