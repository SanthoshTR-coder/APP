package com.folio.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.folio.data.local.dao.RecentFileDao
import com.folio.data.local.entity.RecentFileEntity
import com.folio.domain.model.DocumentFile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recentFileDao: RecentFileDao
) {

    /**
     * Read file metadata from a content URI.
     */
    fun getDocumentFile(uri: Uri): DocumentFile? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                    val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                    DocumentFile(
                        uri = uri,
                        name = name,
                        size = size,
                        mimeType = mimeType
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getRecentFiles(): Flow<List<RecentFileEntity>> = recentFileDao.getRecentFiles()

    suspend fun addRecentFile(file: RecentFileEntity) {
        recentFileDao.insertRecentFile(file)
        recentFileDao.trimToLimit()
    }

    suspend fun removeRecentFile(file: RecentFileEntity) {
        recentFileDao.deleteRecentFile(file)
    }

    suspend fun clearRecents() {
        recentFileDao.clearAll()
    }
}
