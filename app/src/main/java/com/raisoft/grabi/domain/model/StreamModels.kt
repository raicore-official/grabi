package com.raisoft.grabi.domain.model

data class StreamOption(
    val id: String,
    val label: String,
    val videoUrl: String?,
    val audioUrl: String?,
    val height: Int,
    val mimeType: String?,
)

data class StreamInfo(
    val title: String,
    val uploader: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val videoOptions: List<StreamOption>,
    val audioOptions: List<StreamOption>,
)