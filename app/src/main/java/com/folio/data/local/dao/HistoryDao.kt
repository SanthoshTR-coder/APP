package com.folio.data.local.dao

import androidx.room.*
import com.folio.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getHistoryForToday(startOfDay: Long): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntity)

    @Delete
    suspend fun deleteHistory(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun getHistoryCount(): Int
}
