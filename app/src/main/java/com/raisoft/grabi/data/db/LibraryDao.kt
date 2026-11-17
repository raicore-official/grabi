package com.raisoft.grabi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT 100")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM history WHERE url = :url")
    suspend fun historyByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entry: HistoryEntity)

    @Query("UPDATE history SET positionMs = :positionMs, watchedAt = :watchedAt WHERE url = :url")
    suspend fun updatePosition(url: String, positionMs: Long, watchedAt: Long)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteHistory(url: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entry: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE url = :url")
    suspend fun deleteFavorite(url: String)
}