package com.folio.domain.usecase.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

/**
 * Add text or image watermarks to every page of a PDF.
 * Supports customizable font size, opacity, angle, and color.
 */
@Singleton
class WatermarkPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    data class WatermarkConfig(
        val text: String = "CONFIDENTIAL",
        val fontSize: Float = 48f,
        val opacity: Float = 0.3f,      // 0.0 to 1.0
        val angleDegrees: Float = 45f,
        val colorHex: String = "#888888",
        val imageUri: Uri? = null        // null = text watermark, non-null = image watermark
    )

    suspend fun execute(
        pdfUri: Uri,
        config: WatermarkConfig
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext OperationResult.Error("Could not open file")

            val reader = PdfReader(inputStream)
            val timestamp = System.currentTimeMillis()
            val outputFile = File(context.cacheDir, "Watermarked_$timestamp.pdf")
            val writer = PdfWriter(outputFile.outputStream())
            val pdfDoc = PdfDocument(reader, writer)
            val totalPages = pdfDoc.numberOfPages

            for (i in 1..totalPages) {
                _progress.value = OperationProgress(
                    current = i, total = totalPages,
                    message = "Watermarking page $i of $totalPages…",
                    isIndeterminate = false
                )

                val page = pdfDoc.getPage(i)
                val pageSize = page.pageSize
                val canvas = PdfCanvas(page)

                // Set transparency
                val gs = PdfExtGState().setFillOpacity(config.opacity)
                canvas.saveState()
                canvas.setExtGState(gs)

                if (config.imageUri != null) {
                    // Image watermark
                    addImageWatermark(canvas, config.imageUri, pageSize, config)
                } else {
                    // Text watermark
                    addTextWatermark(canvas, pageSize, config)
                }

                canvas.restoreState()
            }

            pdfDoc.close()
            _progress.value = OperationProgress(current = totalPages, total = totalPages, message = "Done", isIndeterminate = false)
            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error("Failed to add watermark: ${e.localizedMessage}")
        }
    }

    private fun addTextWatermark(
        canvas: PdfCanvas,
        pageSize: com.itextpdf.kernel.geom.Rectangle,
        config: WatermarkConfig
    ) {
        val font = PdfFontFactory.createFont()
        val color = parseColor(config.colorHex)
        val angleRad = Math.toRadians(config.angleDegrees.toDouble())

        val centerX = pageSize.width / 2
        val centerY = pageSize.height / 2

        canvas.beginText()
        canvas.setFontAndSize(font, config.fontSize)
        canvas.setFillColor(color)
        canvas.setTextMatrix(
            cos(angleRad).toFloat(), sin(angleRad).toFloat(),
            (-sin(angleRad)).toFloat(), cos(angleRad).toFloat(),
            centerX, centerY
        )
        // Center the text approximately
        val textWidth = font.getWidth(config.text, config.fontSize)
        canvas.moveText((-textWidth / 2).toDouble(), 0.0)
        canvas.showText(config.text)
        canvas.endText()
    }

    private fun addImageWatermark(
        canvas: PdfCanvas,
        imageUri: Uri,
        pageSize: com.itextpdf.kernel.geom.Rectangle,
        config: WatermarkConfig
    ) {
        try {
            val imageStream = context.contentResolver.openInputStream(imageUri) ?: return
            val bitmap = BitmapFactory.decodeStream(imageStream)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val imageData = ImageDataFactory.create(baos.toByteArray())

            // Scale image to 30% of page width
            val targetWidth = pageSize.width * 0.3f
            val scale = targetWidth / imageData.width
            val targetHeight = imageData.height * scale

            val x = (pageSize.width - targetWidth) / 2
            val y = (pageSize.height - targetHeight) / 2

            canvas.addImageFittedIntoRectangle(
                imageData,
                com.itextpdf.kernel.geom.Rectangle(x, y, targetWidth, targetHeight),
                false
            )
            bitmap.recycle()
        } catch (e: Exception) {
            // Silently skip image watermark on error
        }
    }

    private fun parseColor(hex: String): DeviceRgb {
        val colorInt = android.graphics.Color.parseColor(hex)
        return DeviceRgb(
            android.graphics.Color.red(colorInt),
            android.graphics.Color.green(colorInt),
            android.graphics.Color.blue(colorInt)
        )
    }

    companion object {
        val PRESET_COLORS = listOf(
            "#888888", "#CC0000", "#0000CC", "#006600",
            "#FF6600", "#660066", "#333333", "#003366"
        )
    }
}
