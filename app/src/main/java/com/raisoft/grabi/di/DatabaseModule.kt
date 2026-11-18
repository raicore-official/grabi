package com.raisoft.grabi.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.raisoft.grabi.data.db.DownloadDao
import com.raisoft.grabi.data.db.GrabiDatabase
import com.raisoft.grabi.domain.VideoIds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): GrabiDatabase =
        Room.databaseBuilder(context, GrabiDatabase::class.java, "grabi.db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN videoId TEXT NOT NULL DEFAULT ''")
            db.query("SELECT id, url FROM downloads").use { cursor ->
                while (cursor.moveToNext()) {
                    db.execSQL(
                        "UPDATE downloads SET videoId = ? WHERE id = ?",
                        arrayOf(VideoIds.of(cursor.getString(1)), cursor.getString(0)),
                    )
                }
            }
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN thumbnailUrl TEXT DEFAULT ''")
        }
    }

    @Provides
    fun downloadDao(database: GrabiDatabase): DownloadDao = database.downloadDao()
}