package com.folio.ui.screens.pdftoimage

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
import com.folio.domain.usecase.convert.PdfToImageUseCase
import com.folio.domain.usecase.convert.PdfToImageUseCase.OutputFormat
import com.folio.domain.usecase.convert.PdfToImageUseCase.Quality
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfToImageViewModel @Inject constructor(
    private val pdfToImageUseCase: PdfToImageUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _format = MutableStateFlow(OutputFormat.JPG)
    val format: StateFlow<OutputFormat> = _format.asStateFlow()

    private val _quality = MutableStateFlow(Quality.STANDARD)
    val quality: StateFlow<Quality> = _quality.asStateFlow()

    private val _uiState = MutableStateFlow<PdfToImageUiState>(PdfToImageUiState.Idle)
    val uiState: StateFlow<PdfToImageUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = pdfToImageUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = PdfToImageUiState.Idle
    }

    fun setFormat(f: OutputFormat) { _format.value = f }
    fun setQuality(q: Quality) { _quality.value = q }

    fun clearAll() {
        _selectedFile.value = null
        _uiState.value = PdfToImageUiState.Idle
    }

    fun convert() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            _uiState.value = PdfToImageUiState.Processing
            when (val result = pdfToImageUseCase.execute(file.uri, _format.value, _quality.value)) {
                is OperationResult.Success -> {
                    val files = result.data
                    val totalSize = files.sumOf { it.length() }
                    _uiState.value = PdfToImageUiState.Success(
                        outputFiles = files,
                        totalSize = totalSize,
                        originalSize = file.size
                    )
                    historyRepository.logOperation(
                        Operation(
                            toolName = "PDF → Images",
                            inputFile = file.name,
                            inputSize = file.size,
                            outputFile = "${files.size} images",
                            outputSize = totalSize,
                            outputPath = files.firstOrNull()?.parent ?: ""
                        )
                    )

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = PdfToImageUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class PdfToImageUiState {
    data object Idle : PdfToImageUiState()
    data object Processing : PdfToImageUiState()
    data class Success(val outputFiles: List<File>, val totalSize: Long, val originalSize: Long) : PdfToImageUiState()
    data class Error(val message: String) : PdfToImageUiState()
}
