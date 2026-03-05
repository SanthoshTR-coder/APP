package com.folio.ui.screens.compress

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
import com.folio.domain.usecase.pdf.CompressPdfUseCase
import com.folio.domain.usecase.pdf.CompressPdfUseCase.CompressionLevel
import com.folio.domain.usecase.pdf.CompressPdfUseCase.SizeEstimate
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CompressViewModel @Inject constructor(
    private val compressPdfUseCase: CompressPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _selectedLevel = MutableStateFlow(CompressionLevel.BALANCED)
    val selectedLevel: StateFlow<CompressionLevel> = _selectedLevel.asStateFlow()

    private val _sizeEstimates = MutableStateFlow<List<SizeEstimate>>(emptyList())
    val sizeEstimates: StateFlow<List<SizeEstimate>> = _sizeEstimates.asStateFlow()

    private val _uiState = MutableStateFlow<CompressUiState>(CompressUiState.Idle)
    val uiState: StateFlow<CompressUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = compressPdfUseCase.progress

    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        val doc = documentRepository.getDocumentFile(uri)
        if (doc != null) {
            _selectedFile.value = doc
            _sizeEstimates.value = compressPdfUseCase.estimateSizes(doc.size)
            _uiState.value = CompressUiState.FileSelected
        }
    }

    fun selectLevel(level: CompressionLevel) {
        _selectedLevel.value = level
    }

    fun clearFile() {
        _selectedFile.value = null
        _sizeEstimates.value = emptyList()
        _uiState.value = CompressUiState.Idle
    }

    fun compress() {
        val file = _selectedFile.value ?: return
        val level = _selectedLevel.value

        viewModelScope.launch {
            _uiState.value = CompressUiState.Processing

            when (val result = compressPdfUseCase.execute(file.uri, level)) {
                is OperationResult.Success -> {
                    val outputFile = result.data
                    _uiState.value = CompressUiState.Success(
                        outputFile = outputFile,
                        outputSize = outputFile.length(),
                        originalSize = file.size
                    )

                    historyRepository.logOperation(
                        Operation(
                            toolName = "Compress PDF",
                            inputFile = file.name,
                            inputSize = file.size,
                            outputFile = outputFile.name,
                            outputSize = outputFile.length(),
                            outputPath = outputFile.absolutePath
                        )
                    )

                    documentRepository.addRecentFile(
                        RecentFileEntity(
                            fileName = outputFile.name,
                            filePath = outputFile.absolutePath,
                            fileSize = outputFile.length(),
                            mimeType = "application/pdf",
                            operationPerformed = "Compressed"
                        )
                    )

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> {
                    _uiState.value = CompressUiState.Error(result.message)
                }
                is OperationResult.Loading -> { /* progress flow handles this */ }
            }
        }
    }
}

sealed class CompressUiState {
    data object Idle : CompressUiState()
    data object FileSelected : CompressUiState()
    data object Processing : CompressUiState()
    data class Success(
        val outputFile: File,
        val outputSize: Long,
        val originalSize: Long
    ) : CompressUiState()
    data class Error(val message: String) : CompressUiState()
}
