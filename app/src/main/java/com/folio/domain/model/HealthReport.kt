package com.folio.domain.model

/**
 * PDF Health Check report model.
 *
 * Shows: integrity, password status, page count, file size,
 * PDF version, content type, embedded fonts, broken references.
 */
data class HealthReport(
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val pdfVersion: String,          // e.g. "1.7", "2.0"
    val isEncrypted: Boolean,
    val isValid: Boolean,
    val contentType: ContentType,
    val embeddedFonts: List<String>,
    val hasBrokenReferences: Boolean,
    val warnings: List<String>,
    val errors: List<String>
) {
    enum class ContentType(val label: String) {
        TEXT("Text-based"),
        SCANNED("Scanned (Image-based)"),
        MIXED("Mixed (Text + Scanned)")
    }

    val overallStatus: Status
        get() = when {
            errors.isNotEmpty() -> Status.ERROR
            warnings.isNotEmpty() -> Status.WARNING
            else -> Status.GOOD
        }

    enum class Status { GOOD, WARNING, ERROR }
}
