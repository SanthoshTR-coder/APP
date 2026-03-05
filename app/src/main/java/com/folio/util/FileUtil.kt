package com.folio.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * File utility functions for Folio.
 * Handles temp files, copying, sharing, and cleanup.
 */
object FileUtil {

    /**
     * Get the default output directory.
     * Creates it if it doesn't exist.
     */
    fun getOutputDir(context: Context): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Folio"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Create a timestamped output file name.
     * e.g. "Merged_20260305_143022.pdf"
     */
    fun generateOutputFileName(prefix: String, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${prefix}_${timestamp}.$extension"
    }

    /**
     * Copy InputStream to a temp file in cacheDir.
     * Returns the temp file.
     */
    fun copyToTempFile(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copy a temp file to the permanent output directory.
     */
    fun copyToOutputDir(context: Context, tempFile: File, outputName: String): File? {
        return try {
            val outputDir = getOutputDir(context)
            val outputFile = File(outputDir, outputName)
            tempFile.copyTo(outputFile, overwrite = true)
            outputFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create a sharing intent using FileProvider (Android security model).
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = getMimeType(file.name) ?: "application/octet-stream"

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Create an open intent for a file.
     */
    fun createOpenIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = getMimeType(file.name) ?: "application/octet-stream"

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Clean up all temp files from operations.
     * Call after every completed operation.
     */
    fun clearOperationTempFiles(context: Context) {
        context.cacheDir.walkTopDown()
            .filter {
                it.isFile && it.extension in listOf(
                    "pdf", "jpg", "png", "docx", "pptx", "xlsx", "doc", "ppt", "xls", "txt"
                )
            }
            .forEach { it.delete() }
    }

    /**
     * Get MIME type from file extension.
     */
    fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    /**
     * Get a shareable content:// URI for a file via FileProvider.
     */
    fun getShareableUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Launch a share chooser for a file URI.
     */
    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    /**
     * Launch a viewer for a file URI.
     */
    fun openFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /**
     * Format file size into human-readable string.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
