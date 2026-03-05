package com.folio.domain.usecase.convert

import android.content.Context
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.util.FileUtil
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Extracts all selectable text from a text-based PDF.
 * If the PDF is scanned (image-based), returns an informative error.
 */
class PdfToTextUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    /**
     * Extract text and return it as a String.
     */
    suspend fun extractText(uri: Uri): OperationResult<String> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading PDF...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "pdf_to_text_${System.currentTimeMillis()}.pdf"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val reader = PdfReader(tempFile)
            val pdfDoc = PdfDocument(reader)
            val totalPages = pdfDoc.numberOfPages

            val textBuilder = StringBuilder()
            var hasText = false

            for (page in 1..totalPages) {
                _progress.value = OperationProgress(
                    current = page,
                    total = totalPages,
                    message = "Extracting page $page of $totalPages...",
                    isIndeterminate = false
                )

                val pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page))
                if (pageText.isNotBlank()) {
                    hasText = true
                    textBuilder.appendLine("── Page $page ──")
                    textBuilder.appendLine(pageText)
                    textBuilder.appendLine()
                }
            }

            pdfDoc.close()
            tempFile.delete()

            if (!hasText) {
                return@withContext OperationResult.Error(
                    "This PDF contains scanned images. Use an OCR tool to extract text."
                )
            }

            OperationResult.Success(textBuilder.toString())
        } catch (e: Exception) {
            OperationResult.Error(
                "Something went wrong while extracting text. The PDF may be corrupted.",
                e
            )
        }
    }

    /**
     * Extract text and save as .txt file.
     */
    suspend fun extractToFile(
        uri: Uri,
        outputFileName: String = FileUtil.generateOutputFileName("Extracted", "txt")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        when (val result = extractText(uri)) {
            is OperationResult.Success -> {
                val outputTempFile = File(context.cacheDir, outputFileName)
                outputTempFile.writeText(result.data)

                val outputFile = FileUtil.copyToOutputDir(context, outputTempFile, outputFileName)
                    ?: return@withContext OperationResult.Error(
                        "Your device is running out of space."
                    )

                outputTempFile.delete()
                OperationResult.Success(outputFile)
            }
            is OperationResult.Error -> OperationResult.Error(result.message, result.cause)
            is OperationResult.Loading -> OperationResult.Loading
        }
    }
}
