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
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Converts PDF to PowerPoint (.pptx).
 * Each PDF page → one slide with extracted text placed in a text box.
 */
class PdfToPptUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    suspend fun execute(
        uri: Uri,
        outputFileName: String = FileUtil.generateOutputFileName("PdfToPpt", "pptx")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading PDF...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "pdf_to_ppt_${System.currentTimeMillis()}.pdf"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val reader = PdfReader(tempFile)
            val pdfDoc = PdfDocument(reader)
            val totalPages = pdfDoc.numberOfPages

            val pptx = XMLSlideShow()
            val layout = pptx.slideMasters[0].slideLayouts[0] // Blank layout

            for (page in 1..totalPages) {
                _progress.value = OperationProgress(
                    current = page,
                    total = totalPages,
                    message = "Converting page $page of $totalPages...",
                    isIndeterminate = false
                )

                val text = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page))

                val slide = pptx.createSlide(layout)

                // Add text box to slide (using default positioning)
                val textBox = slide.createTextBox()

                if (text.isNotBlank()) {
                    val paragraph = textBox.addNewTextParagraph()
                    val run = paragraph.addNewTextRun()
                    run.setText(text)
                    run.fontSize = 12.0
                } else {
                    val paragraph = textBox.addNewTextParagraph()
                    val run = paragraph.addNewTextRun()
                    run.setText("[No text content on this page]")
                    run.isItalic = true
                    run.fontSize = 10.0
                }
            }

            pdfDoc.close()

            _progress.value = OperationProgress(
                message = "Saving presentation...",
                current = totalPages,
                total = totalPages,
                isIndeterminate = false
            )

            val outputTempFile = File(context.cacheDir, outputFileName)
            FileOutputStream(outputTempFile).use { pptx.write(it) }
            pptx.close()

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
