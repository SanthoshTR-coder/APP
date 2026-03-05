package com.folio.ui.screens.pdftoword

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
import com.folio.domain.usecase.convert.PdfToWordUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfToWordViewModel @Inject constructor(
    private val pdfToWordUseCase: PdfToWordUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _uiState = MutableStateFlow<PdfToWordUiState>(PdfToWordUiState.Idle)
    val uiState: StateFlow<PdfToWordUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = pdfToWordUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = PdfToWordUiState.Idle
    }

    fun clearAll() {
        _selectedFile.value = null
        _uiState.value = PdfToWordUiState.Idle
    }

    fun convert() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            _uiState.value = PdfToWordUiState.Processing
            when (val result = pdfToWordUseCase.execute(file.uri)) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = PdfToWordUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(
                            toolName = "PDF → Word",
                            inputFile = file.name,
                            inputSize = file.size,
                            outputFile = output.name,
                            outputSize = output.length(),
                            outputPath = output.absolutePath
                        )
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(
                            fileName = output.name,
                            filePath = output.absolutePath,
                            fileSize = output.length(),
                            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            operationPerformed = "PDF → Word"
                        )
                    )

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = PdfToWordUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class PdfToWordUiState {
    data object Idle : PdfToWordUiState()
    data object Processing : PdfToWordUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : PdfToWordUiState()
    data class Error(val message: String) : PdfToWordUiState()
}
