package com.folio.domain.usecase.pdf

import android.content.Context
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.util.FileUtil
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Reorders PDF pages according to a user-specified page order.
 * Input is a list of 0-based page indices in the desired new order.
 */
class ReorderPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    /**
     * Reorder pages in a PDF.
     *
     * @param uri Source PDF content URI
     * @param newPageOrder List of 0-based page indices in the desired order
     */
    suspend fun execute(
        uri: Uri,
        newPageOrder: List<Int>,
        outputFileName: String = FileUtil.generateOutputFileName("Reordered", "pdf")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading file...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "reorder_input_${System.currentTimeMillis()}.pdf"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val reader = PdfReader(tempFile)
            val srcDoc = PdfDocument(reader)
            val totalPages = srcDoc.numberOfPages

            // Validate page order
            if (newPageOrder.size != totalPages) {
                srcDoc.close()
                tempFile.delete()
                return@withContext OperationResult.Error(
                    "Page order must include all $totalPages pages."
                )
            }
            if (newPageOrder.any { it < 0 || it >= totalPages }) {
                srcDoc.close()
                tempFile.delete()
                return@withContext OperationResult.Error("Invalid page numbers in the order.")
            }

            val outputTempFile = File(context.cacheDir, outputFileName)
            val writer = PdfWriter(outputTempFile)
            val outDoc = PdfDocument(writer)

            newPageOrder.forEachIndexed { index, pageIdx ->
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = totalPages,
                    message = "Reordering page ${index + 1} of $totalPages...",
                    isIndeterminate = false
                )
                srcDoc.copyPagesTo(pageIdx + 1, pageIdx + 1, outDoc) // 1-based
            }

            outDoc.close()
            srcDoc.close()

            val outputFile = FileUtil.copyToOutputDir(context, outputTempFile, outputFileName)
                ?: return@withContext OperationResult.Error(
                    "Your device is running out of space. Free up some storage and try again."
                )

            tempFile.delete()
            outputTempFile.delete()

            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error(
                "Something went wrong while reordering. The file may be corrupted.",
                e
            )
        }
    }
}
