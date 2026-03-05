package com.folio.di

import android.content.Context
import androidx.room.Room
import com.folio.data.local.db.FolioDatabase
import com.folio.data.local.dao.HistoryDao
import com.folio.data.local.dao.RecentFileDao
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
    fun provideFolioDatabase(@ApplicationContext context: Context): FolioDatabase {
        return Room.databaseBuilder(
            context,
            FolioDatabase::class.java,
            "folio_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideHistoryDao(database: FolioDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideRecentFileDao(database: FolioDatabase): RecentFileDao {
        return database.recentFileDao()
    }
}
