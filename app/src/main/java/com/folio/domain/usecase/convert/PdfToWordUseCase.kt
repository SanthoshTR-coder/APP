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
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Converts PDF to Word (.docx) using iText7 (text extraction) + Apache POI (DOCX generation).
 *
 * Extracts text + basic layout and creates an editable Word document.
 * Complex PDF layouts may lose some formatting — this is expected for on-device conversion.
 */
class PdfToWordUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    suspend fun execute(
        uri: Uri,
        outputFileName: String = FileUtil.generateOutputFileName("PdfToWord", "docx")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading PDF...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "pdf_to_word_${System.currentTimeMillis()}.pdf"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val reader = PdfReader(tempFile)
            val pdfDoc = PdfDocument(reader)
            val totalPages = pdfDoc.numberOfPages

            _progress.value = OperationProgress(
                message = "Extracting text...",
                current = 0,
                total = totalPages,
                isIndeterminate = false
            )

            // Create DOCX
            val wordDoc = XWPFDocument()

            for (page in 1..totalPages) {
                _progress.value = OperationProgress(
                    current = page,
                    total = totalPages,
                    message = "Processing page $page of $totalPages...",
                    isIndeterminate = false
                )

                val text = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page))

                if (text.isNotBlank()) {
                    // Add page header
                    val headerParagraph = wordDoc.createParagraph()
                    val headerRun = headerParagraph.createRun()
                    headerRun.isBold = true
                    headerRun.fontSize = 10
                    headerRun.setText("── Page $page ──")

                    // Split text by lines and add as paragraphs
                    text.lines().forEach { line ->
                        if (line.isNotBlank()) {
                            val paragraph = wordDoc.createParagraph()
                            val run = paragraph.createRun()
                            run.setText(line)
                            run.fontSize = 11
                        }
                    }

                    // Page break between pages (except last)
                    if (page < totalPages) {
                        val breakParagraph = wordDoc.createParagraph()
                        breakParagraph.isPageBreak = true
                    }
                }
            }

            pdfDoc.close()

            _progress.value = OperationProgress(
                message = "Saving Word document...",
                current = totalPages,
                total = totalPages,
                isIndeterminate = false
            )

            val outputTempFile = File(context.cacheDir, outputFileName)
            FileOutputStream(outputTempFile).use { wordDoc.write(it) }
            wordDoc.close()

            val outputFile = FileUtil.copyToOutputDir(context, outputTempFile, outputFileName)
                ?: return@withContext OperationResult.Error(
                    "Your device is running out of space."
                )

            tempFile.delete()
            outputTempFile.delete()

            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error(
                "Something went wrong with the conversion. The file format may not be fully supported.",
                e
            )
        }
    }
}
