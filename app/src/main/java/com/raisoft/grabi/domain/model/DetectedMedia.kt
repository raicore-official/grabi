package com.raisoft.grabi.domain.model

data class DetectedMedia(
    val url: String,
    val kind: MediaKind,
    val mimeType: String?,
    val sizeBytes: Long?,
)