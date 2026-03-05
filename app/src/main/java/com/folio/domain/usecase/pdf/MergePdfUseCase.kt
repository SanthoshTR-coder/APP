package com.folio.domain.usecase.pdf

import android.content.Context
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.util.FileUtil
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.utils.PdfMerger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Merges 2–15 PDF files into a single document.
 * Runs on Dispatchers.IO — never blocks main thread.
 *
 * Supports: PDF files (other formats should be pre-converted).
 */
class MergePdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    /**
     * Merge multiple PDFs into one.
     *
     * @param uris List of content URIs for the PDF files to merge (in order)
     * @param outputFileName Desired output file name
     * @return OperationResult with the output File on success
     */
    suspend fun execute(
        uris: List<Uri>,
        outputFileName: String = FileUtil.generateOutputFileName("Merged", "pdf")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            if (uris.size < 2) {
                return@withContext OperationResult.Error("Please select at least 2 files to merge.")
            }
            if (uris.size > 15) {
                return@withContext OperationResult.Error("Maximum 15 files can be merged at once.")
            }

            _progress.value = OperationProgress(
                current = 0,
                total = uris.size,
                message = "Preparing files...",
                isIndeterminate = true
            )

            // Copy all URIs to temp files
            val tempFiles = mutableListOf<File>()
            uris.forEachIndexed { index, uri ->
                _progress.value = OperationProgress(
                    current = index,
                    total = uris.size,
                    message = "Reading file ${index + 1} of ${uris.size}...",
                    isIndeterminate = false
                )
                val tempFile = FileUtil.copyToTempFile(
                    context, uri, "merge_input_${index}_${System.currentTimeMillis()}.pdf"
                ) ?: return@withContext OperationResult.Error(
                    "We couldn't read file ${index + 1}. It may have been moved or deleted."
                )
                tempFiles.add(tempFile)
            }

            // Create output file in cache first
            val outputTempFile = File(context.cacheDir, outputFileName)

            _progress.value = OperationProgress(
                current = 0,
                total = uris.size,
                message = "Merging...",
                isIndeterminate = false
            )

            // Merge using iText7
            val writer = PdfWriter(outputTempFile)
            val pdfDoc = PdfDocument(writer)
            val merger = PdfMerger(pdfDoc)

            tempFiles.forEachIndexed { index, file ->
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = tempFiles.size,
                    message = "Merging file ${index + 1} of ${tempFiles.size}...",
                    isIndeterminate = false
                )

                val reader = PdfReader(file)
                val srcDoc = PdfDocument(reader)
                merger.merge(srcDoc, 1, srcDoc.numberOfPages)
                srcDoc.close()
            }

            pdfDoc.close()

            // Move to output directory
            val outputFile = FileUtil.copyToOutputDir(context, outputTempFile, outputFileName)
                ?: return@withContext OperationResult.Error(
                    "Your device is running out of space. Free up some storage and try again."
                )

            // Clean up temp files
            tempFiles.forEach { it.delete() }
            outputTempFile.delete()

            _progress.value = OperationProgress(
                current = uris.size,
                total = uris.size,
                message = "Done!",
                isIndeterminate = false
            )

            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error(
                "Something went wrong while merging. One of the files may be corrupted or password-protected.",
                e
            )
        }
    }
}
