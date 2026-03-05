package com.folio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records every operation performed by the user.
 * Only metadata is stored — never the actual file content.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolName: String,           // e.g. "Merge PDF", "Compress PDF"
    val inputFileName: String,
    val inputFileSize: Long,        // bytes
    val outputFileName: String?,
    val outputFileSize: Long?,      // bytes
    val outputFilePath: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "success"  // "success" | "failed"
)
