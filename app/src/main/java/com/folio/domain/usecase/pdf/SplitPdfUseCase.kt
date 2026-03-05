package com.folio.domain.usecase.pdf

import android.content.Context
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.util.FileUtil
import com.itextpdf.kernel.pdf.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Splits a PDF by page range, every N pages, or individual pages.
 */
class SplitPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    enum class SplitMode {
        BY_RANGE,       // "1-3, 5, 8-12"
        EVERY_N_PAGES,  // Split into chunks of N
        INDIVIDUAL      // One PDF per page
    }

    /**
     * Split by page range string (e.g. "1-3, 5, 8-12").
     * Returns a list of output files.
     */
    suspend fun splitByRange(
        uri: Uri,
        rangeString: String
    ): OperationResult<List<File>> = withContext(Dispatchers.IO) {
        try {
            val tempFile = copyToTemp(uri) ?: return@withContext fileNotFoundError()
            val reader = PdfReader(tempFile)
            val srcDoc = PdfDocument(reader)
            val totalPages = srcDoc.numberOfPages

            // Parse ranges
            val ranges = parseRanges(rangeString, totalPages)
            if (ranges.isEmpty()) {
                srcDoc.close()
                tempFile.delete()
                return@withContext OperationResult.Error("Invalid page range. Use format like: 1-3, 5, 8-12")
            }

            val outputFiles = mutableListOf<File>()
            ranges.forEachIndexed { index, pageRange ->
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = ranges.size,
                    message = "Creating part ${index + 1} of ${ranges.size}...",
                    isIndeterminate = false
                )

                val outputName = FileUtil.generateOutputFileName("Split_Part${index + 1}", "pdf")
                val outputFile = File(context.cacheDir, outputName)
                val writer = PdfWriter(outputFile)
                val outDoc = PdfDocument(writer)

                for (page in pageRange) {
                    if (page in 1..totalPages) {
                        srcDoc.copyPagesTo(page, page, outDoc)
                    }
                }
                outDoc.close()

                val finalFile = FileUtil.copyToOutputDir(context, outputFile, outputName)
                if (finalFile != null) outputFiles.add(finalFile)
                outputFile.delete()
            }

            srcDoc.close()
            tempFile.delete()

            OperationResult.Success(outputFiles)
        } catch (e: Exception) {
            OperationResult.Error("Something went wrong while splitting. The file may be corrupted.", e)
        }
    }

    /**
     * Split into chunks of N pages each.
     */
    suspend fun splitEveryNPages(
        uri: Uri,
        pagesPerChunk: Int
    ): OperationResult<List<File>> = withContext(Dispatchers.IO) {
        try {
            if (pagesPerChunk < 1) {
                return@withContext OperationResult.Error("Pages per chunk must be at least 1.")
            }

            val tempFile = copyToTemp(uri) ?: return@withContext fileNotFoundError()
            val reader = PdfReader(tempFile)
            val srcDoc = PdfDocument(reader)
            val totalPages = srcDoc.numberOfPages
            val chunks = (1..totalPages).chunked(pagesPerChunk)

            val outputFiles = mutableListOf<File>()
            chunks.forEachIndexed { index, pages ->
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = chunks.size,
                    message = "Creating chunk ${index + 1} of ${chunks.size}...",
                    isIndeterminate = false
                )

                val outputName = FileUtil.generateOutputFileName("Split_Chunk${index + 1}", "pdf")
                val outputFile = File(context.cacheDir, outputName)
                val writer = PdfWriter(outputFile)
                val outDoc = PdfDocument(writer)

                for (page in pages) {
                    srcDoc.copyPagesTo(page, page, outDoc)
                }
                outDoc.close()

                val finalFile = FileUtil.copyToOutputDir(context, outputFile, outputName)
                if (finalFile != null) outputFiles.add(finalFile)
                outputFile.delete()
            }

            srcDoc.close()
            tempFile.delete()

            OperationResult.Success(outputFiles)
        } catch (e: Exception) {
            OperationResult.Error("Something went wrong while splitting. The file may be corrupted.", e)
        }
    }

    /**
     * Split into individual pages (one PDF per page).
     */
    suspend fun splitIndividual(
        uri: Uri
    ): OperationResult<List<File>> = withContext(Dispatchers.IO) {
        try {
            val tempFile = copyToTemp(uri) ?: return@withContext fileNotFoundError()
            val reader = PdfReader(tempFile)
            val srcDoc = PdfDocument(reader)
            val totalPages = srcDoc.numberOfPages

            val outputFiles = mutableListOf<File>()
            for (page in 1..totalPages) {
                _progress.value = OperationProgress(
                    current = page,
                    total = totalPages,
                    message = "Extracting page $page of $totalPages...",
                    isIndeterminate = false
                )

                val outputName = FileUtil.generateOutputFileName("Page_$page", "pdf")
                val outputFile = File(context.cacheDir, outputName)
                val writer = PdfWriter(outputFile)
                val outDoc = PdfDocument(writer)

                srcDoc.copyPagesTo(page, page, outDoc)
                outDoc.close()

                val finalFile = FileUtil.copyToOutputDir(context, outputFile, outputName)
                if (finalFile != null) outputFiles.add(finalFile)
                outputFile.delete()
            }

            srcDoc.close()
            tempFile.delete()

            OperationResult.Success(outputFiles)
        } catch (e: Exception) {
            OperationResult.Error("Something went wrong while splitting. The file may be corrupted.", e)
        }
    }

    private fun copyToTemp(uri: Uri): File? {
        return FileUtil.copyToTempFile(context, uri, "split_input_${System.currentTimeMillis()}.pdf")
    }

    private fun <T> fileNotFoundError(): OperationResult<T> {
        return OperationResult.Error("We couldn't find this file. It may have been moved or deleted.")
    }

    /**
     * Parse range string like "1-3, 5, 8-12" into list of page number lists.
     * Each element in the outer list becomes one output file.
     */
    private fun parseRanges(rangeString: String, maxPage: Int): List<List<Int>> {
        return try {
            val parts = rangeString.split(",").map { it.trim() }.filter { it.isNotBlank() }
            parts.map { part ->
                if (part.contains("-")) {
                    val bounds = part.split("-").map { it.trim().toInt() }
                    (bounds[0]..bounds[1]).filter { it in 1..maxPage }
                } else {
                    val page = part.toInt()
                    if (page in 1..maxPage) listOf(page) else emptyList()
                }
            }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
