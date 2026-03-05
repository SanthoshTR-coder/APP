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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.File
import javax.inject.Inject

/**
 * Converts PowerPoint presentations (.pptx) to PDF.
 * Each slide → one PDF page.
 *
 * Preserves text content from slides.
 * Complex shapes, transitions, and animations are not preserved.
 */
class PptToPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    suspend fun execute(
        uri: Uri,
        outputFileName: String = FileUtil.generateOutputFileName("PptToPdf", "pdf")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading presentation...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "ppt_input_${System.currentTimeMillis()}.pptx"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val inputStream = tempFile.inputStream()
            val pptx = XMLSlideShow(inputStream)
            val slides = pptx.slides
            val totalSlides = slides.size

            if (totalSlides == 0) {
                pptx.close()
                inputStream.close()
                tempFile.delete()
                return@withContext OperationResult.Error("This presentation has no slides.")
            }

            val outputTempFile = File(context.cacheDir, outputFileName)

            // Use landscape A4 for slides (avoids java.awt.Dimension from pptx.pageSize on Android)
            val pageSize = PageSize.A4.rotate()

            val writer = PdfWriter(outputTempFile)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc, pageSize)
            document.setMargins(36f, 36f, 36f, 36f)

            slides.forEachIndexed { index, slide ->
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = totalSlides,
                    message = "Converting slide ${index + 1} of $totalSlides...",
                    isIndeterminate = false
                )

                // Extract text from all shapes on the slide
                val slideText = StringBuilder()
                slide.shapes.forEach { shape ->
                    if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                        shape.textParagraphs.forEach { para ->
                            val text = para.text
                            if (text.isNotBlank()) {
                                slideText.appendLine(text)
                            }
                        }
                    }
                }

                // Slide number header
                val header = Paragraph("Slide ${index + 1}")
                    .setBold()
                    .setFontSize(14f)
                document.add(header)

                // Slide content
                if (slideText.isNotBlank()) {
                    val content = Paragraph(slideText.toString())
                        .setFontSize(12f)
                    document.add(content)
                } else {
                    document.add(Paragraph("[No text content on this slide]")
                        .setFontSize(10f)
                        .setItalic())
                }

                // Page break between slides
                if (index < totalSlides - 1) {
                    document.add(com.itextpdf.layout.element.AreaBreak())
                }
            }

            document.close()
            pptx.close()
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
