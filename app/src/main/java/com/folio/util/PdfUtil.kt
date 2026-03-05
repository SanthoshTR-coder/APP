package com.folio.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF utility functions — thumbnail rendering, page counting, etc.
 * All operations run on Dispatchers.IO.
 */
object PdfUtil {

    /**
     * Get the page count of a PDF file.
     */
    suspend fun getPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext 0
            val tempFile = File(context.cacheDir, "temp_count_${System.currentTimeMillis()}.pdf")
            tempFile.outputStream().use { inputStream.copyTo(it) }
            inputStream.close()

            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            tempFile.delete()
            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Render a single page as a bitmap thumbnail.
     *
     * @param scale Render quality multiplier (1 = native, 2 = 2x quality)
     */
    suspend fun renderPage(
        pdfFile: File,
        pageIndex: Int,
        scale: Int = 2
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            if (pageIndex >= renderer.pageCount) {
                renderer.close()
                fd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(
                page.width * scale,
                page.height * scale,
                Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Render all pages as bitmap thumbnails.
     */
    suspend fun renderAllPages(
        pdfFile: File,
        scale: Int = 1
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }

            renderer.close()
            fd.close()
        } catch (e: Exception) {
            // Return whatever we managed to render
        }
        bitmaps
    }

    /**
     * Check if a PDF is password-protected.
     */
    suspend fun isPasswordProtected(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val tempFile = File(context.cacheDir, "temp_check_${System.currentTimeMillis()}.pdf")
            tempFile.outputStream().use { inputStream.copyTo(it) }
            inputStream.close()

            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                val renderer = PdfRenderer(fd)
                renderer.close()
                fd.close()
                tempFile.delete()
                false // Successfully opened = not protected
            } catch (e: SecurityException) {
                fd.close()
                tempFile.delete()
                true // SecurityException = password protected
            }
        } catch (e: Exception) {
            false
        }
    }
}
