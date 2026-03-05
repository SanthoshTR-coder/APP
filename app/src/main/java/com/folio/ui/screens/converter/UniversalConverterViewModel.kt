package com.folio.ui.screens.converter

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.folio.data.repository.BillingRepository
import com.folio.data.repository.DocumentRepository
import com.folio.domain.model.DocumentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class UniversalConverterViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _detectedFile = MutableStateFlow<DocumentFile?>(null)
    val detectedFile: StateFlow<DocumentFile?> = _detectedFile.asStateFlow()

    private val _detectedRoute = MutableStateFlow<String?>(null)
    val detectedRoute: StateFlow<String?> = _detectedRoute.asStateFlow()

    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun detectFile(uri: Uri) {
        val doc = documentRepository.getDocumentFile(uri) ?: return
        _detectedFile.value = doc

        val route = when {
            doc.mimeType.startsWith("image/") -> "image_to_pdf"
            doc.mimeType == "application/pdf" -> null // Show converter picker for PDFs
            doc.mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
            doc.mimeType == "application/msword" -> "word_to_pdf"
            doc.mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
            doc.mimeType == "application/vnd.ms-powerpoint" -> "ppt_to_pdf"
            doc.mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
            doc.mimeType == "application/vnd.ms-excel" -> "excel_to_pdf"
            else -> null
        }
        _detectedRoute.value = route
    }

    fun clear() {
        _detectedFile.value = null
        _detectedRoute.value = null
    }

    companion object {
        val PDF_OUTPUT_ROUTES = listOf(
            "pdf_to_images" to "PDF → Images",
            "pdf_to_word" to "PDF → Word",
            "pdf_to_ppt" to "PDF → PPT",
            "pdf_to_text" to "PDF → Text"
        )
    }
}
