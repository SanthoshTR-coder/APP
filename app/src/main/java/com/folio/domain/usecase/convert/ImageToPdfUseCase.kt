package com.folio.domain.usecase.convert

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import android.net.Uri
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
 * Converts 1–30 images (JPG, PNG, WEBP, HEIC, BMP) into a single PDF.
 *
 * Options:
 * - Page size: A4, A3, Letter, Fit to Image, Square
 * - Margin: None, Small, Medium, Large
 */
class ImageToPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    enum class PageSize(val label: String, val widthPt: Int, val heightPt: Int) {
        A4("A4", 595, 842),
        A3("A3", 842, 1191),
        LETTER("Letter", 612, 792),
        FIT_TO_IMAGE("Fit to Image", 0, 0),
        SQUARE("Square", 612, 612)
    }

    enum class Margin(val label: String, val value: Int) {
        NONE("None", 0),
        SMALL("Small", 24),
        MEDIUM("Medium", 48),
        LARGE("Large", 72)
    }

    suspend fun execute(
        imageUris: List<Uri>,
        pageSize: PageSize = PageSize.A4,
        margin: Margin = Margin.SMALL,
        outputFileName: String = FileUtil.generateOutputFileName("ImageToPdf", "pdf")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            if (imageUris.isEmpty()) {
                return@withContext OperationResult.Error("Please select at least one image.")
            }
            if (imageUris.size > 30) {
                return@withContext OperationResult.Error("Maximum 30 images can be converted at once.")
            }

            _progress.value = OperationProgress(
                message = "Preparing images...",
                isIndeterminate = true
            )

            val pdfDocument = AndroidPdfDocument()

            imageUris.forEachIndexed { index, uri ->
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = imageUris.size,
                    message = "Processing image ${index + 1} of ${imageUris.size}...",
                    isIndeterminate = false
                )

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@forEachIndexed
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap != null) {
                    val m = margin.value

                    val (pageW, pageH) = when (pageSize) {
                        PageSize.FIT_TO_IMAGE -> Pair(bitmap.width + m * 2, bitmap.height + m * 2)
                        else -> Pair(pageSize.widthPt, pageSize.heightPt)
                    }

                    val pageInfo = AndroidPdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)

                    val canvas = page.canvas
                    val availW = pageW - m * 2
                    val availH = pageH - m * 2

                    // Scale image to fit within available area, maintaining aspect ratio
                    val scale = minOf(
                        availW.toFloat() / bitmap.width,
                        availH.toFloat() / bitmap.height
                    ).coerceAtMost(1f) // Don't upscale

                    val scaledW = (bitmap.width * scale).toInt()
                    val scaledH = (bitmap.height * scale).toInt()

                    // Center on page
                    val left = m + (availW - scaledW) / 2f
                    val top = m + (availH - scaledH) / 2f

                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
                    canvas.drawBitmap(scaledBitmap, left, top, null)

                    pdfDocument.finishPage(page)

                    if (scaledBitmap !== bitmap) scaledBitmap.recycle()
                    bitmap.recycle()
                }
            }

            _progress.value = OperationProgress(
                message = "Saving PDF...",
                current = imageUris.size,
                total = imageUris.size,
                isIndeterminate = false
            )

            val outputTempFile = File(context.cacheDir, outputFileName)
            FileOutputStream(outputTempFile).use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            val outputFile = FileUtil.copyToOutputDir(context, outputTempFile, outputFileName)
                ?: return@withContext OperationResult.Error(
                    "Your device is running out of space. Free up some storage and try again."
                )

            outputTempFile.delete()

            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error(
                "Something went wrong while converting images. One or more files may be unsupported.",
                e
            )
        }
    }
}
