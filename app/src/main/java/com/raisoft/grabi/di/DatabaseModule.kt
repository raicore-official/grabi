package com.raisoft.grabi.di

import android.content.Context
import androidx.room.Room
import com.raisoft.grabi.data.db.DownloadDao
import com.raisoft.grabi.data.db.GrabiDatabase
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
        Room.databaseBuilder(context, GrabiDatabase::class.java, "grabi.db").build()

    @Provides
    fun downloadDao(database: GrabiDatabase): DownloadDao = database.downloadDao()
}