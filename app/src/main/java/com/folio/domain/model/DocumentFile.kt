package com.folio.domain.model

import android.net.Uri

/**
 * Represents a document file picked by the user.
 * Used across all tools for consistent file handling.
 */
data class DocumentFile(
    val uri: Uri,
    val name: String,
    val size: Long,             // bytes
    val mimeType: String,
    val pageCount: Int? = null, // null for non-PDF files
    val extension: String = name.substringAfterLast('.', "")
) {
    val sizeFormatted: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }

    val isPdf: Boolean get() = mimeType == "application/pdf" || extension.equals("pdf", true)
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isWord: Boolean get() = extension.equals("docx", true) || extension.equals("doc", true)
    val isPpt: Boolean get() = extension.equals("pptx", true) || extension.equals("ppt", true)
    val isExcel: Boolean get() = extension.equals("xlsx", true) || extension.equals("xls", true)
    val isText: Boolean get() = extension.equals("txt", true)
}
