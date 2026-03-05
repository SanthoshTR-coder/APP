package com.folio.ui.screens.ppttopdf

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
import com.folio.domain.usecase.convert.PptToPdfUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PptToPdfViewModel @Inject constructor(
    private val pptToPdfUseCase: PptToPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _uiState = MutableStateFlow<PptConvertUiState>(PptConvertUiState.Idle)
    val uiState: StateFlow<PptConvertUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = pptToPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = PptConvertUiState.Idle
    }

    fun clearAll() {
        _selectedFile.value = null
        _uiState.value = PptConvertUiState.Idle
    }

    fun convert() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            _uiState.value = PptConvertUiState.Processing
            when (val result = pptToPdfUseCase.execute(file.uri)) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = PptConvertUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(toolName = "PPT → PDF", inputFile = file.name, inputSize = file.size, outputFile = output.name, outputSize = output.length(), outputPath = output.absolutePath)
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = output.name, filePath = output.absolutePath, fileSize = output.length(), mimeType = "application/pdf", operationPerformed = "PPT → PDF")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = PptConvertUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class PptConvertUiState {
    data object Idle : PptConvertUiState()
    data object Processing : PptConvertUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : PptConvertUiState()
    data class Error(val message: String) : PptConvertUiState()
}
