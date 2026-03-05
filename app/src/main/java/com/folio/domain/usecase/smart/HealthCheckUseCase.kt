package com.folio.domain.usecase.smart

import android.content.Context
import android.net.Uri
import com.folio.domain.model.HealthReport
import com.folio.domain.model.OperationResult
import com.itextpdf.kernel.pdf.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans a PDF and returns a detailed health report:
 * integrity, password status, page count, file size, PDF version,
 * content type, embedded fonts, and potential issues.
 */
@Singleton
class HealthCheckUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class HealthResult(
        val fileName: String,
        val fileSize: Long,
        val pageCount: Int,
        val pdfVersion: String,
        val isEncrypted: Boolean,
        val hasText: Boolean,
        val hasImages: Boolean,
        val contentType: ContentType,
        val embeddedFonts: List<String>,
        val issues: List<HealthIssue>,
        val warnings: List<String>
    )

    enum class ContentType { TEXT, SCANNED, MIXED, UNKNOWN }

    data class HealthIssue(
        val severity: Severity,
        val title: String,
        val description: String
    )

    enum class Severity { OK, WARNING, ERROR }

    suspend fun analyze(uri: Uri, fileName: String, fileSize: Long): OperationResult<HealthResult> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext OperationResult.Error("Could not open file")

                val reader = PdfReader(inputStream)
                val pdfDoc = PdfDocument(reader)
                val issues = mutableListOf<HealthIssue>()
                val warnings = mutableListOf<String>()

                val pageCount = pdfDoc.numberOfPages
                val pdfVersion = "PDF ${reader.pdfAConformanceLevel?.part ?: pdfDoc.pdfVersion ?: "Unknown"}"

                // Check for encryption
                val isEncrypted = reader.isEncrypted

                // Analyze content: check first few pages for text vs images
                var hasAnyText = false
                var hasAnyImages = false
                val fonts = mutableSetOf<String>()

                for (i in 1..minOf(pageCount, 5)) {
                    val page = pdfDoc.getPage(i)
                    val resources = page.resources

                    // Check for fonts (indicates text content)
                    val fontNames = resources.getPdfObject()?.getAsDictionary(PdfName.Font)
                    if (fontNames != null && fontNames.size() > 0) {
                        hasAnyText = true
                        fontNames.keySet().forEach { key ->
                            fonts.add(key.value)
                        }
                    }

                    // Check for images (XObject)
                    val xObjects = resources.getPdfObject()?.getAsDictionary(PdfName.XObject)
                    if (xObjects != null && xObjects.size() > 0) {
                        hasAnyImages = true
                    }
                }

                val contentType = when {
                    hasAnyText && hasAnyImages -> ContentType.MIXED
                    hasAnyText -> ContentType.TEXT
                    hasAnyImages -> ContentType.SCANNED
                    else -> ContentType.UNKNOWN
                }

                // Generate issues
                issues.add(
                    HealthIssue(
                        Severity.OK,
                        "File Integrity",
                        "PDF structure is valid and readable"
                    )
                )

                if (isEncrypted) {
                    issues.add(HealthIssue(Severity.WARNING, "Encrypted", "This PDF is password-protected"))
                } else {
                    issues.add(HealthIssue(Severity.OK, "Not Encrypted", "No password protection"))
                }

                if (contentType == ContentType.SCANNED) {
                    warnings.add("This appears to be a scanned PDF. Text extraction may not work.")
                    issues.add(HealthIssue(Severity.WARNING, "Scanned Content", "Pages contain images only — text search won't work"))
                } else {
                    issues.add(HealthIssue(Severity.OK, "Text Content", "PDF contains selectable text"))
                }

                if (fileSize > 50 * 1024 * 1024) {
                    warnings.add("Large file — may be slow to process")
                    issues.add(HealthIssue(Severity.WARNING, "Large File", "File exceeds 50 MB — consider compressing"))
                } else {
                    issues.add(HealthIssue(Severity.OK, "File Size", "Size is within normal range"))
                }

                if (pageCount > 500) {
                    warnings.add("Very large document")
                    issues.add(HealthIssue(Severity.WARNING, "Many Pages", "$pageCount pages — some operations may be slow"))
                } else {
                    issues.add(HealthIssue(Severity.OK, "Page Count", "$pageCount page${if (pageCount != 1) "s" else ""}"))
                }

                pdfDoc.close()

                OperationResult.Success(
                    HealthResult(
                        fileName = fileName,
                        fileSize = fileSize,
                        pageCount = pageCount,
                        pdfVersion = pdfVersion,
                        isEncrypted = isEncrypted,
                        hasText = hasAnyText,
                        hasImages = hasAnyImages,
                        contentType = contentType,
                        embeddedFonts = fonts.toList(),
                        issues = issues,
                        warnings = warnings
                    )
                )
            } catch (e: com.itextpdf.kernel.exceptions.BadPasswordException) {
                OperationResult.Error("This PDF is password-protected. Unlock it first to run a health check.")
            } catch (e: Exception) {
                OperationResult.Error("Failed to analyze PDF: ${e.localizedMessage}")
            }
        }
}
