package com.raisoft.grabi.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val url: String,
    val title: String,
    val uploader: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val positionMs: Long,
    val watchedAt: Long,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val url: String,
    val title: String,
    val uploader: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val addedAt: Long,
)