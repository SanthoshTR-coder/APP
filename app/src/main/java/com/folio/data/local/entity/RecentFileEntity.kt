package com.folio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks last 20 files touched by the app.
 * Only metadata — never actual content.
 */
@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,             // bytes
    val mimeType: String,           // e.g. "application/pdf", "image/jpeg"
    val operationPerformed: String, // e.g. "Merged", "Compressed"
    val timestamp: Long = System.currentTimeMillis()
)
