package com.raisoft.grabi.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}