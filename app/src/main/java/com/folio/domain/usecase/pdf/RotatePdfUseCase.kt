package com.folio.domain.usecase.pdf

import android.content.Context
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.util.FileUtil
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfPage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Rotates PDF pages by 90° CW, 90° CCW, or 180°.
 * Supports rotating selected pages or all pages.
 */
class RotatePdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    enum class RotationAngle(val degrees: Int, val label: String) {
        CW_90(90, "90° Clockwise"),
        CCW_90(270, "90° Counter-clockwise"),
        ROTATE_180(180, "180°")
    }

    /**
     * Rotate specified pages in a PDF.
     *
     * @param uri Source PDF content URI
     * @param pageIndices 0-based page indices to rotate (empty = all pages)
     * @param angle Rotation angle
     */
    suspend fun execute(
        uri: Uri,
        pageIndices: List<Int>,
        angle: RotationAngle,
        outputFileName: String = FileUtil.generateOutputFileName("Rotated", "pdf")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading file...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "rotate_input_${System.currentTimeMillis()}.pdf"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val outputTempFile = File(context.cacheDir, outputFileName)
            val reader = PdfReader(tempFile)
            val writer = PdfWriter(outputTempFile)
            val pdfDoc = PdfDocument(reader, writer)
            val totalPages = pdfDoc.numberOfPages

            // If no specific pages selected, rotate all
            val pagesToRotate = if (pageIndices.isEmpty()) {
                (0 until totalPages).toList()
            } else {
                pageIndices.filter { it in 0 until totalPages }
            }

            pagesToRotate.forEachIndexed { index, pageIdx ->
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = pagesToRotate.size,
                    message = "Rotating page ${pageIdx + 1}...",
                    isIndeterminate = false
                )

                val page: PdfPage = pdfDoc.getPage(pageIdx + 1) // 1-based
                val currentRotation = page.rotation
                val newRotation = (currentRotation + angle.degrees) % 360
                page.setRotation(newRotation)
            }

            pdfDoc.close()

            val outputFile = FileUtil.copyToOutputDir(context, outputTempFile, outputFileName)
                ?: return@withContext OperationResult.Error(
                    "Your device is running out of space. Free up some storage and try again."
                )

            tempFile.delete()
            outputTempFile.delete()

            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error(
                "Something went wrong while rotating. The file may be corrupted.",
                e
            )
        }
    }
}
