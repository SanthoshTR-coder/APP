package com.folio.domain.usecase.convert

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.util.FileUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Converts PDF pages to images (JPG or PNG).
 *
 * Quality levels:
 * - Screen (72 DPI) — smallest, good for screen viewing
 * - Standard (150 DPI) — good for sharing
 * - Print (300 DPI) — highest quality
 */
class PdfToImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    enum class OutputFormat(val label: String, val extension: String) {
        JPG("JPEG", "jpg"),
        PNG("PNG", "png")
    }

    enum class Quality(val label: String, val dpiScale: Int) {
        SCREEN("72 DPI (Screen)", 1),
        STANDARD("150 DPI (Standard)", 2),
        PRINT("300 DPI (Print)", 4)
    }

    suspend fun execute(
        uri: Uri,
        format: OutputFormat = OutputFormat.JPG,
        quality: Quality = Quality.STANDARD
    ): OperationResult<List<File>> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading PDF...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "pdf_to_image_input_${System.currentTimeMillis()}.pdf"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val totalPages = renderer.pageCount

            // Create output directory
            val outputDir = File(
                FileUtil.getOutputDir(context),
                "PDF_Images_${System.currentTimeMillis()}"
            )
            outputDir.mkdirs()

            val outputFiles = mutableListOf<File>()

            for (pageIndex in 0 until totalPages) {
                _progress.value = OperationProgress(
                    current = pageIndex + 1,
                    total = totalPages,
                    message = "Converting page ${pageIndex + 1} of $totalPages...",
                    isIndeterminate = false
                )

                val page = renderer.openPage(pageIndex)
                val scale = quality.dpiScale
                val bitmap = Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val outputFile = File(
                    outputDir,
                    "Page_${pageIndex + 1}.${format.extension}"
                )

                FileOutputStream(outputFile).use { fos ->
                    val compressFormat = when (format) {
                        OutputFormat.JPG -> Bitmap.CompressFormat.JPEG
                        OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                    }
                    val compressQuality = when (quality) {
                        Quality.SCREEN -> 70
                        Quality.STANDARD -> 85
                        Quality.PRINT -> 95
                    }
                    bitmap.compress(compressFormat, compressQuality, fos)
                }

                bitmap.recycle()
                outputFiles.add(outputFile)
            }

            renderer.close()
            fd.close()
            tempFile.delete()

            OperationResult.Success(outputFiles)
        } catch (e: Exception) {
            OperationResult.Error(
                "Something went wrong while extracting images. The PDF may be corrupted.",
                e
            )
        }
    }
}
