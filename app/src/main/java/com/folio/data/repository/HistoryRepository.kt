package com.folio.data.repository

import com.folio.data.local.dao.HistoryDao
import com.folio.data.local.entity.HistoryEntity
import com.folio.domain.model.Operation
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {

    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    fun getTodayHistory(): Flow<List<HistoryEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return historyDao.getHistoryForToday(calendar.timeInMillis)
    }

    suspend fun logOperation(operation: Operation) {
        val entity = HistoryEntity(
            toolName = operation.toolName,
            inputFileName = operation.inputFile,
            inputFileSize = operation.inputSize,
            outputFileName = operation.outputFile,
            outputFileSize = operation.outputSize,
            outputFilePath = operation.outputPath,
            timestamp = operation.timestamp,
            status = if (operation.success) "success" else "failed"
        )
        historyDao.insertHistory(entity)
    }

    suspend fun deleteEntry(entry: HistoryEntity) {
        historyDao.deleteHistory(entry)
    }

    suspend fun clearAll() {
        historyDao.clearAllHistory()
    }
}
