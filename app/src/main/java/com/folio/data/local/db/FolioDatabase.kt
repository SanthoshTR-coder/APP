package com.folio.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.folio.data.local.dao.HistoryDao
import com.folio.data.local.dao.RecentFileDao
import com.folio.data.local.entity.HistoryEntity
import com.folio.data.local.entity.RecentFileEntity

@Database(
    entities = [HistoryEntity::class, RecentFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun recentFileDao(): RecentFileDao
}
