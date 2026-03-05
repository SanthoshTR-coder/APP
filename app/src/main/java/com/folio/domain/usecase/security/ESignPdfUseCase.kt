package com.folio.domain.usecase.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
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

/**
 * E-Sign a PDF — place drawn, typed, or image signatures onto pages.
 * Supports signature placement at arbitrary coordinates with scaling.
 */
@Singleton
class ESignPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    data class SignaturePlacement(
        val pageNumber: Int,       // 1-indexed
        val x: Float,             // Points from left
        val y: Float,             // Points from bottom
        val width: Float,         // Width in points
        val height: Float,        // Height in points
        val signatureBitmap: Bitmap
    )

    data class DateStamp(
        val pageNumber: Int,
        val x: Float,
        val y: Float,
        val text: String          // e.g. "March 5, 2026"
    )

    suspend fun execute(
        pdfUri: Uri,
        signatures: List<SignaturePlacement>,
        dateStamps: List<DateStamp> = emptyList()
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            if (signatures.isEmpty()) {
                return@withContext OperationResult.Error("No signatures placed")
            }

            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext OperationResult.Error("Could not open file")

            _progress.value = OperationProgress(current = 0, total = signatures.size + 1, message = "Preparing…", isIndeterminate = false)

            val reader = PdfReader(inputStream)
            val timestamp = System.currentTimeMillis()
            val outputFile = File(context.cacheDir, "Signed_$timestamp.pdf")
            val writer = PdfWriter(outputFile.outputStream())
            val pdfDoc = PdfDocument(reader, writer)

            // Place each signature
            signatures.forEachIndexed { index, placement ->
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = signatures.size + 1,
                    message = "Placing signature ${index + 1}…",
                    isIndeterminate = false
                )

                if (placement.pageNumber < 1 || placement.pageNumber > pdfDoc.numberOfPages) return@forEachIndexed

                val page = pdfDoc.getPage(placement.pageNumber)
                val canvas = PdfCanvas(page)

                // Convert bitmap to iText image data
                val baos = ByteArrayOutputStream()
                placement.signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val imageData = ImageDataFactory.create(baos.toByteArray())

                canvas.addImageFittedIntoRectangle(
                    imageData,
                    Rectangle(placement.x, placement.y, placement.width, placement.height),
                    false
                )
            }

            // Place date stamps
            dateStamps.forEach { stamp ->
                if (stamp.pageNumber < 1 || stamp.pageNumber > pdfDoc.numberOfPages) return@forEach
                val page = pdfDoc.getPage(stamp.pageNumber)
                val canvas = PdfCanvas(page)
                val font = com.itextpdf.kernel.font.PdfFontFactory.createFont()

                canvas.beginText()
                canvas.setFontAndSize(font, 10f)
                canvas.setFillColor(com.itextpdf.kernel.colors.DeviceRgb(60, 60, 60))
                canvas.moveText(stamp.x.toDouble(), stamp.y.toDouble())
                canvas.showText(stamp.text)
                canvas.endText()
            }

            _progress.value = OperationProgress(
                current = signatures.size + 1,
                total = signatures.size + 1,
                message = "Saving…",
                isIndeterminate = false
            )

            pdfDoc.close()
            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error("Failed to sign PDF: ${e.localizedMessage}")
        }
    }
}
