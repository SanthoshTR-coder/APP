package com.folio.domain.usecase.edit

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
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
 * PDF Editor use case — supports adding text, images, freehand drawings,
 * highlights, and deleting pages. All edits are applied as overlays
 * to keep the operation non-destructive.
 */
@Singleton
class EditPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    /** A text annotation to add on a page. */
    data class TextEdit(
        val pageNumber: Int,
        val x: Float,
        val y: Float,
        val text: String,
        val fontSize: Float = 12f,
        val colorHex: String = "#000000"
    )

    /** An image to overlay on a page. */
    data class ImageEdit(
        val pageNumber: Int,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val bitmap: Bitmap
    )

    /** A freehand drawing overlay rendered as a bitmap. */
    data class DrawingOverlay(
        val pageNumber: Int,
        val bitmap: Bitmap       // Full-page transparent bitmap with drawings
    )

    /** A highlight rectangle. */
    data class HighlightEdit(
        val pageNumber: Int,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val colorHex: String = "#FFFF00",
        val opacity: Float = 0.35f
    )

    data class EditOperations(
        val textEdits: List<TextEdit> = emptyList(),
        val imageEdits: List<ImageEdit> = emptyList(),
        val drawingOverlays: List<DrawingOverlay> = emptyList(),
        val highlights: List<HighlightEdit> = emptyList(),
        val pagesToDelete: List<Int> = emptyList()   // 1-indexed
    )

    suspend fun execute(
        pdfUri: Uri,
        operations: EditOperations
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext OperationResult.Error("Could not open file")

            val reader = PdfReader(inputStream)
            val timestamp = System.currentTimeMillis()
            val outputFile = File(context.cacheDir, "Edited_$timestamp.pdf")
            val writer = PdfWriter(outputFile.outputStream())
            val pdfDoc = PdfDocument(reader, writer)
            val totalSteps = operations.textEdits.size + operations.imageEdits.size +
                    operations.drawingOverlays.size + operations.highlights.size +
                    operations.pagesToDelete.size + 1
            var step = 0

            // Apply highlights first (they go behind text)
            operations.highlights.forEach { highlight ->
                step++
                _progress.value = OperationProgress(step, totalSteps, "Adding highlights…", false)
                if (highlight.pageNumber in 1..pdfDoc.numberOfPages) {
                    val page = pdfDoc.getPage(highlight.pageNumber)
                    val canvas = PdfCanvas(page.newContentStreamBefore(), page.resources, pdfDoc)
                    val gs = com.itextpdf.kernel.pdf.extgstate.PdfExtGState().setFillOpacity(highlight.opacity)
                    canvas.saveState()
                    canvas.setExtGState(gs)
                    canvas.setFillColor(parseColor(highlight.colorHex))
                    canvas.rectangle(highlight.x.toDouble(), highlight.y.toDouble(), highlight.width.toDouble(), highlight.height.toDouble())
                    canvas.fill()
                    canvas.restoreState()
                }
            }

            // Apply drawing overlays
            operations.drawingOverlays.forEach { drawing ->
                step++
                _progress.value = OperationProgress(step, totalSteps, "Adding drawings…", false)
                if (drawing.pageNumber in 1..pdfDoc.numberOfPages) {
                    val page = pdfDoc.getPage(drawing.pageNumber)
                    val canvas = PdfCanvas(page)
                    val baos = ByteArrayOutputStream()
                    drawing.bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val imageData = ImageDataFactory.create(baos.toByteArray())
                    val pageSize = page.pageSize
                    canvas.addImageFittedIntoRectangle(
                        imageData,
                        Rectangle(0f, 0f, pageSize.width, pageSize.height),
                        false
                    )
                }
            }

            // Apply text edits
            operations.textEdits.forEach { textEdit ->
                step++
                _progress.value = OperationProgress(step, totalSteps, "Adding text…", false)
                if (textEdit.pageNumber in 1..pdfDoc.numberOfPages) {
                    val page = pdfDoc.getPage(textEdit.pageNumber)
                    val canvas = PdfCanvas(page)
                    val font = PdfFontFactory.createFont()
                    canvas.beginText()
                    canvas.setFontAndSize(font, textEdit.fontSize)
                    canvas.setFillColor(parseColor(textEdit.colorHex))
                    canvas.moveText(textEdit.x.toDouble(), textEdit.y.toDouble())
                    canvas.showText(textEdit.text)
                    canvas.endText()
                }
            }

            // Apply image edits
            operations.imageEdits.forEach { imageEdit ->
                step++
                _progress.value = OperationProgress(step, totalSteps, "Adding images…", false)
                if (imageEdit.pageNumber in 1..pdfDoc.numberOfPages) {
                    val page = pdfDoc.getPage(imageEdit.pageNumber)
                    val canvas = PdfCanvas(page)
                    val baos = ByteArrayOutputStream()
                    imageEdit.bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val imageData = ImageDataFactory.create(baos.toByteArray())
                    canvas.addImageFittedIntoRectangle(
                        imageData,
                        Rectangle(imageEdit.x, imageEdit.y, imageEdit.width, imageEdit.height),
                        false
                    )
                }
            }

            // Delete pages (in reverse order to not shift indices)
            val sortedDeletes = operations.pagesToDelete.sortedDescending()
            sortedDeletes.forEach { pageNum ->
                step++
                _progress.value = OperationProgress(step, totalSteps, "Removing pages…", false)
                if (pageNum in 1..pdfDoc.numberOfPages && pdfDoc.numberOfPages > 1) {
                    pdfDoc.removePage(pageNum)
                }
            }

            _progress.value = OperationProgress(totalSteps, totalSteps, "Saving…", false)
            pdfDoc.close()
            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error("Failed to edit PDF: ${e.localizedMessage}")
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
}
