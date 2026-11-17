package com.raisoft.grabi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun byId(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC")
    suspend fun pending(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status = 'WAITING_WIFI' ORDER BY createdAt ASC")
    suspend fun waitingWifi(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(id: String, status: String, error: String? = null)

    @Query("UPDATE downloads SET progress = :progress, speed = :speed, etaSeconds = :eta WHERE id = :id")
    suspend fun setProgress(id: String, progress: Float, speed: String?, eta: Long)

    @Query("UPDATE downloads SET status = :status, filePath = :filePath, contentUri = :contentUri, progress = 1.0 WHERE id = :id")
    suspend fun finish(id: String, status: String, filePath: String?, contentUri: String?)

    @Query("UPDATE downloads SET status = 'QUEUED' WHERE status = 'RUNNING'")
    suspend fun recoverStale(): Int

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)
}