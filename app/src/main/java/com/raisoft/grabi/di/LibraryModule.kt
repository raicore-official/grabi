package com.raisoft.grabi.di

import android.content.Context
import androidx.room.Room
import com.raisoft.grabi.data.db.LibraryDao
import com.raisoft.grabi.data.db.LibraryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LibraryModule {

    @Provides
    @Singleton
    fun libraryDatabase(@ApplicationContext context: Context): LibraryDatabase =
        Room.databaseBuilder(context, LibraryDatabase::class.java, "grabi-library.db").build()

    @Provides
    fun libraryDao(database: LibraryDatabase): LibraryDao = database.libraryDao()
}