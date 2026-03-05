package com.folio.data.local.dao

import androidx.room.*
import com.folio.data.local.entity.RecentFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC LIMIT 20")
    fun getRecentFiles(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(file: RecentFileEntity)

    @Delete
    suspend fun deleteRecentFile(file: RecentFileEntity)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()

    /**
     * Keep only the most recent 20 entries.
     * Call after every insert.
     */
    @Query("""
        DELETE FROM recent_files WHERE id NOT IN (
            SELECT id FROM recent_files ORDER BY timestamp DESC LIMIT 20
        )
    """)
    suspend fun trimToLimit()
}
