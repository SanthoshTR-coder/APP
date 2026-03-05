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
 * Remove password protection from a PDF.
 * Requires the correct user password to decrypt.
 */
@Singleton
class UnlockPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    /**
     * Validates whether the given password can open the PDF.
     */
    suspend fun validatePassword(uri: Uri, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val reader = PdfReader(inputStream, ReaderProperties().setPassword(password.toByteArray()))
            val pdfDoc = PdfDocument(reader)
            pdfDoc.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a PDF is password-protected.
     */
    suspend fun isPasswordProtected(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val reader = PdfReader(inputStream)
            val pdfDoc = PdfDocument(reader)
            pdfDoc.close()
            false // Opened without password — not protected
        } catch (e: com.itextpdf.kernel.exceptions.BadPasswordException) {
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun execute(uri: Uri, password: String): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            _progress.value = OperationProgress(current = 0, total = 3, message = "Reading PDF…", isIndeterminate = false)

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext OperationResult.Error("Could not open file")

            _progress.value = OperationProgress(current = 1, total = 3, message = "Decrypting…", isIndeterminate = false)

            val reader = PdfReader(inputStream, ReaderProperties().setPassword(password.toByteArray()))
            reader.setUnethicalReading(true)

            val timestamp = System.currentTimeMillis()
            val outputFile = File(context.cacheDir, "Unlocked_$timestamp.pdf")
            val writer = PdfWriter(outputFile.outputStream())
            val pdfDoc = PdfDocument(reader, writer)

            _progress.value = OperationProgress(current = 2, total = 3, message = "Saving…", isIndeterminate = false)
            pdfDoc.close()

            _progress.value = OperationProgress(current = 3, total = 3, message = "Done", isIndeterminate = false)
            OperationResult.Success(outputFile)
        } catch (e: com.itextpdf.kernel.exceptions.BadPasswordException) {
            OperationResult.Error("That password didn't work. Double-check and try again.")
        } catch (e: Exception) {
            OperationResult.Error("Failed to unlock PDF: ${e.localizedMessage}")
        }
    }
}
