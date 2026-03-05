package com.folio.ui.screens.pdftotext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.data.local.entity.RecentFileEntity
import com.folio.data.repository.BillingRepository
import com.folio.data.repository.DocumentRepository
import com.folio.data.repository.HistoryRepository
import com.folio.domain.model.DocumentFile
import com.folio.domain.model.Operation
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.domain.usecase.convert.PdfToTextUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfToTextViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfToTextUseCase: PdfToTextUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _extractedText = MutableStateFlow<String?>(null)
    val extractedText: StateFlow<String?> = _extractedText.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<PdfToTextUiState>(PdfToTextUiState.Idle)
    val uiState: StateFlow<PdfToTextUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = pdfToTextUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _extractedText.value = null
        _uiState.value = PdfToTextUiState.Idle
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun clearAll() {
        _selectedFile.value = null
        _extractedText.value = null
        _searchQuery.value = ""
        _uiState.value = PdfToTextUiState.Idle
    }

    fun extractText() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            _uiState.value = PdfToTextUiState.Processing
            when (val result = pdfToTextUseCase.extractText(file.uri)) {
                is OperationResult.Success -> {
                    _extractedText.value = result.data
                    _uiState.value = PdfToTextUiState.TextReady(result.data)
                    historyRepository.logOperation(
                        Operation(toolName = "PDF → Text", inputFile = file.name, inputSize = file.size, outputFile = "Extracted text", outputSize = result.data.length.toLong(), outputPath = "")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = PdfToTextUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }

    fun copyAll() {
        val text = _extractedText.value ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Extracted PDF Text", text))
    }

    fun saveAsFile() {
        val file = _selectedFile.value ?: return
        val text = _extractedText.value ?: return
        viewModelScope.launch {
            when (val result = pdfToTextUseCase.extractToFile(file.uri)) {
                is OperationResult.Success -> {
                    val outputFile = result.data
                    _uiState.value = PdfToTextUiState.FileSaved(outputFile)
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = outputFile.name, filePath = outputFile.absolutePath, fileSize = outputFile.length(), mimeType = "text/plain", operationPerformed = "PDF → Text")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = PdfToTextUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class PdfToTextUiState {
    data object Idle : PdfToTextUiState()
    data object Processing : PdfToTextUiState()
    data class TextReady(val text: String) : PdfToTextUiState()
    data class FileSaved(val file: File) : PdfToTextUiState()
    data class Error(val message: String) : PdfToTextUiState()
}
