package com.folio.domain.usecase.convert

import android.content.Context
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.util.FileUtil
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import javax.inject.Inject

/**
 * Converts Word documents (.docx) to PDF using Apache POI + iText7.
 *
 * Preserves: text, basic formatting, tables, lists.
 * Note: Complex formatting (headers/footers, advanced styles) may not be 100% preserved.
 */
class WordToPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    suspend fun execute(
        uri: Uri,
        outputFileName: String = FileUtil.generateOutputFileName("WordToPdf", "pdf")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading document...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "word_input_${System.currentTimeMillis()}.docx"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val inputStream = tempFile.inputStream()
            val wordDoc = XWPFDocument(inputStream)

            _progress.value = OperationProgress(
                message = "Converting to PDF...",
                current = 1,
                total = 3,
                isIndeterminate = false
            )

            val outputTempFile = File(context.cacheDir, outputFileName)
            val writer = PdfWriter(outputTempFile)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc, PageSize.A4)
            document.setMargins(36f, 36f, 36f, 36f)

            _progress.value = OperationProgress(
                message = "Processing content...",
                current = 2,
                total = 3,
                isIndeterminate = false
            )

            // Process paragraphs
            for (paragraph in wordDoc.paragraphs) {
                val text = paragraph.text
                if (text.isNotBlank()) {
                    val pdfParagraph = Paragraph(text)

                    // Apply basic styling
                    val firstRun = paragraph.runs.firstOrNull()
                    if (firstRun != null) {
                        val fontSize = firstRun.fontSize
                        if (fontSize > 0) {
                            pdfParagraph.setFontSize(fontSize.toFloat())
                        }
                        if (firstRun.isBold) {
                            pdfParagraph.setBold()
                        }
                        if (firstRun.isItalic) {
                            pdfParagraph.setItalic()
                        }
                    }

                    document.add(pdfParagraph)
                } else {
                    // Empty paragraph — add spacing
                    document.add(Paragraph("\n"))
                }
            }

            // Process tables
            for (table in wordDoc.tables) {
                val numCols = table.rows.firstOrNull()?.tableCells?.size ?: continue
                val pdfTable = Table(UnitValue.createPercentArray(numCols)).useAllAvailableWidth()

                for (row in table.rows) {
                    for (cell in row.tableCells) {
                        val cellText = cell.text
                        pdfTable.addCell(Paragraph(cellText))
                    }
                }

                document.add(pdfTable)
            }

            _progress.value = OperationProgress(
                message = "Saving PDF...",
                current = 3,
                total = 3,
                isIndeterminate = false
            )

            document.close()
            wordDoc.close()
            inputStream.close()

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
