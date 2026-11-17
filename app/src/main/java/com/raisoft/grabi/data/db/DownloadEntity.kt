package com.raisoft.grabi.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val optionLabel: String,
    val kind: String,
    val formatSelector: String,
    val audioFormat: String?,
    val subtitles: Boolean,
    val status: String,
    val progress: Float,
    val speed: String?,
    val etaSeconds: Long,
    val filePath: String?,
    val contentUri: String?,
    val error: String?,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "") val videoId: String = "",
    @ColumnInfo(defaultValue = "") val thumbnailUrl: String? = null,
)