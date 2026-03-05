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
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import javax.inject.Inject

/**
 * Converts Excel spreadsheets (.xlsx) to PDF.
 *
 * Options:
 * - All sheets or selected sheets
 * - Show/hide grid lines
 * - Auto-fit to page width
 */
class ExcelToPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    suspend fun execute(
        uri: Uri,
        selectedSheetIndices: List<Int>? = null, // null = all sheets
        showGridLines: Boolean = true,
        outputFileName: String = FileUtil.generateOutputFileName("ExcelToPdf", "pdf")
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(message = "Reading spreadsheet...", isIndeterminate = true)

            val tempFile = FileUtil.copyToTempFile(
                context, uri, "excel_input_${System.currentTimeMillis()}.xlsx"
            ) ?: return@withContext OperationResult.Error(
                "We couldn't find this file. It may have been moved or deleted."
            )

            val inputStream = tempFile.inputStream()
            val workbook = XSSFWorkbook(inputStream)
            val totalSheets = workbook.numberOfSheets

            val sheetsToProcess = selectedSheetIndices
                ?.filter { it in 0 until totalSheets }
                ?: (0 until totalSheets).toList()

            if (sheetsToProcess.isEmpty()) {
                workbook.close()
                inputStream.close()
                tempFile.delete()
                return@withContext OperationResult.Error("No valid sheets found in the workbook.")
            }

            val outputTempFile = File(context.cacheDir, outputFileName)
            val writer = PdfWriter(outputTempFile)
            val pdfDoc = PdfDocument(writer)
            // Use landscape A4 for better spreadsheet layout
            val document = Document(pdfDoc, PageSize.A4.rotate())
            document.setMargins(24f, 24f, 24f, 24f)

            sheetsToProcess.forEachIndexed { idx, sheetIndex ->
                val sheet = workbook.getSheetAt(sheetIndex)
                val sheetName = workbook.getSheetName(sheetIndex)

                _progress.value = OperationProgress(
                    current = idx + 1,
                    total = sheetsToProcess.size,
                    message = "Processing \"$sheetName\"...",
                    isIndeterminate = false
                )

                // Sheet title
                document.add(
                    Paragraph(sheetName)
                        .setBold()
                        .setFontSize(14f)
                )

                // Find max columns used
                var maxCols = 0
                for (rowIndex in 0..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    maxCols = maxOf(maxCols, row.lastCellNum.toInt())
                }

                if (maxCols == 0) {
                    document.add(
                        Paragraph("[Empty sheet]")
                            .setItalic()
                            .setFontSize(10f)
                    )
                } else {
                    // Create table
                    val table = Table(UnitValue.createPercentArray(maxCols)).useAllAvailableWidth()

                    if (!showGridLines) {
                        table.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    }

                    for (rowIndex in 0..sheet.lastRowNum) {
                        val row = sheet.getRow(rowIndex)
                        for (colIndex in 0 until maxCols) {
                            val cell = row?.getCell(colIndex)
                            val cellText = when {
                                cell == null -> ""
                                cell.cellType == CellType.NUMERIC -> {
                                    val num = cell.numericCellValue
                                    if (num == num.toLong().toDouble()) {
                                        num.toLong().toString()
                                    } else {
                                        String.format("%.2f", num)
                                    }
                                }
                                cell.cellType == CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                cell.cellType == CellType.FORMULA -> try {
                                    cell.stringCellValue
                                } catch (_: Exception) {
                                    cell.numericCellValue.toString()
                                }
                                else -> cell.stringCellValue ?: ""
                            }

                            val pdfCell = Cell().add(
                                Paragraph(cellText).setFontSize(9f)
                            )

                            if (!showGridLines) {
                                pdfCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                            }

                            table.addCell(pdfCell)
                        }
                    }

                    document.add(table)
                }

                // Page break between sheets
                if (idx < sheetsToProcess.lastIndex) {
                    document.add(com.itextpdf.layout.element.AreaBreak())
                }
            }

            document.close()
            workbook.close()
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

    /**
     * Get sheet names from an Excel file for the sheet picker UI.
     */
    suspend fun getSheetNames(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        try {
            val tempFile = FileUtil.copyToTempFile(
                context, uri, "excel_sheets_${System.currentTimeMillis()}.xlsx"
            ) ?: return@withContext emptyList()

            val inputStream = tempFile.inputStream()
            val workbook = XSSFWorkbook(inputStream)
            val names = (0 until workbook.numberOfSheets).map { workbook.getSheetName(it) }
            workbook.close()
            inputStream.close()
            tempFile.delete()
            names
        } catch (e: Exception) {
            emptyList()
        }
    }
}
