package com.folio.domain.usecase.security

import android.content.Context
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.itextpdf.kernel.pdf.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Protect a PDF with 128-bit AES encryption.
 * Supports user password + optional permission restrictions.
 */
@Singleton
class ProtectPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    /**
     * @param allowPrinting  Whether the protected PDF allows printing
     * @param allowCopying   Whether the protected PDF allows text copying
     */
    suspend fun execute(
        uri: Uri,
        userPassword: String,
        ownerPassword: String = userPassword,
        allowPrinting: Boolean = true,
        allowCopying: Boolean = false
    ): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(current = 0, total = 3, message = "Reading PDF…", isIndeterminate = false)

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext OperationResult.Error("Could not open file")

            val timestamp = System.currentTimeMillis()
            val outputFile = File(context.cacheDir, "Protected_$timestamp.pdf")

            _progress.value = OperationProgress(current = 1, total = 3, message = "Encrypting…", isIndeterminate = false)

            val reader = PdfReader(inputStream)
            val writer = PdfWriter(outputFile.outputStream(), WriterProperties().apply {
                setStandardEncryption(
                    userPassword.toByteArray(),
                    ownerPassword.toByteArray(),
                    buildPermissions(allowPrinting, allowCopying),
                    EncryptionConstants.ENCRYPTION_AES_128
                )
            })
            val pdfDoc = PdfDocument(reader, writer)

            _progress.value = OperationProgress(current = 2, total = 3, message = "Saving…", isIndeterminate = false)
            pdfDoc.close()

            _progress.value = OperationProgress(current = 3, total = 3, message = "Done", isIndeterminate = false)
            OperationResult.Success(outputFile)
        } catch (e: Exception) {
            OperationResult.Error("Failed to protect PDF: ${e.localizedMessage}")
        }
    }

    private fun buildPermissions(allowPrinting: Boolean, allowCopying: Boolean): Int {
        var permissions = 0
        if (allowPrinting) permissions = permissions or EncryptionConstants.ALLOW_PRINTING
        if (allowCopying) permissions = permissions or EncryptionConstants.ALLOW_COPY
        return permissions
    }

    companion object {
        fun getPasswordStrength(password: String): PasswordStrength {
            if (password.length < 4) return PasswordStrength.WEAK
            var score = 0
            if (password.length >= 8) score++
            if (password.length >= 12) score++
            if (password.any { it.isUpperCase() }) score++
            if (password.any { it.isLowerCase() }) score++
            if (password.any { it.isDigit() }) score++
            if (password.any { !it.isLetterOrDigit() }) score++
            return when {
                score <= 2 -> PasswordStrength.WEAK
                score <= 3 -> PasswordStrength.FAIR
                score <= 4 -> PasswordStrength.STRONG
                else -> PasswordStrength.VERY_STRONG
            }
        }
    }
}

enum class PasswordStrength(val label: String, val fraction: Float) {
    WEAK("Weak", 0.25f),
    FAIR("Fair", 0.5f),
    STRONG("Strong", 0.75f),
    VERY_STRONG("Very Strong", 1f)
}
