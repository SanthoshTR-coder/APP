package com.folio.ui.screens.pdftoppt

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
import com.folio.domain.usecase.convert.PdfToPptUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfToPptViewModel @Inject constructor(
    private val pdfToPptUseCase: PdfToPptUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _uiState = MutableStateFlow<PdfToPptUiState>(PdfToPptUiState.Idle)
    val uiState: StateFlow<PdfToPptUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = pdfToPptUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = PdfToPptUiState.Idle
    }

    fun clearAll() {
        _selectedFile.value = null
        _uiState.value = PdfToPptUiState.Idle
    }

    fun convert() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            _uiState.value = PdfToPptUiState.Processing
            when (val result = pdfToPptUseCase.execute(file.uri)) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = PdfToPptUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(toolName = "PDF → PPT", inputFile = file.name, inputSize = file.size, outputFile = output.name, outputSize = output.length(), outputPath = output.absolutePath)
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = output.name, filePath = output.absolutePath, fileSize = output.length(), mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation", operationPerformed = "PDF → PPT")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = PdfToPptUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class PdfToPptUiState {
    data object Idle : PdfToPptUiState()
    data object Processing : PdfToPptUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : PdfToPptUiState()
    data class Error(val message: String) : PdfToPptUiState()
}
