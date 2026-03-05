package com.folio.ui.screens.wordtopdf

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
import com.folio.domain.usecase.convert.WordToPdfUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WordToPdfViewModel @Inject constructor(
    private val wordToPdfUseCase: WordToPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _uiState = MutableStateFlow<ConvertUiState>(ConvertUiState.Idle)
    val uiState: StateFlow<ConvertUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = wordToPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = ConvertUiState.Idle
    }

    fun clearAll() {
        _selectedFile.value = null
        _uiState.value = ConvertUiState.Idle
    }

    fun convert() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            _uiState.value = ConvertUiState.Processing
            when (val result = wordToPdfUseCase.execute(file.uri)) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = ConvertUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(
                            toolName = "Word → PDF",
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
                            mimeType = "application/pdf",
                            operationPerformed = "Word → PDF"
                        )
                    )

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = ConvertUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class ConvertUiState {
    data object Idle : ConvertUiState()
    data object Processing : ConvertUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : ConvertUiState()
    data class Error(val message: String) : ConvertUiState()
}
