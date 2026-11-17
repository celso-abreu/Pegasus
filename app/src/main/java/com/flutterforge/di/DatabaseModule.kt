package com.flutterforge.di

import android.content.Context
import androidx.room.Room
import com.flutterforge.data.local.AppDatabase
import com.flutterforge.data.local.dao.ProjectDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "flutterforge.db"
        ).fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()
}
