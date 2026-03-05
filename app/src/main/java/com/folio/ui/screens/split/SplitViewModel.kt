package com.folio.ui.screens.split

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
import com.folio.domain.usecase.pdf.SplitPdfUseCase
import com.folio.domain.usecase.pdf.SplitPdfUseCase.SplitMode
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SplitViewModel @Inject constructor(
    private val splitPdfUseCase: SplitPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _splitMode = MutableStateFlow(SplitMode.BY_RANGE)
    val splitMode: StateFlow<SplitMode> = _splitMode.asStateFlow()

    private val _rangeString = MutableStateFlow("")
    val rangeString: StateFlow<String> = _rangeString.asStateFlow()

    private val _pagesPerChunk = MutableStateFlow(1)
    val pagesPerChunk: StateFlow<Int> = _pagesPerChunk.asStateFlow()

    private val _uiState = MutableStateFlow<SplitUiState>(SplitUiState.Idle)
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = splitPdfUseCase.progress

    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        val doc = documentRepository.getDocumentFile(uri)
        if (doc != null) {
            _selectedFile.value = doc
            _uiState.value = SplitUiState.FileSelected
        }
    }

    fun setSplitMode(mode: SplitMode) {
        _splitMode.value = mode
    }

    fun setRangeString(range: String) {
        _rangeString.value = range
    }

    fun setPagesPerChunk(n: Int) {
        _pagesPerChunk.value = n.coerceAtLeast(1)
    }

    fun clearFile() {
        _selectedFile.value = null
        _rangeString.value = ""
        _pagesPerChunk.value = 1
        _uiState.value = SplitUiState.Idle
    }

    fun split() {
        val file = _selectedFile.value ?: return
        val mode = _splitMode.value

        viewModelScope.launch {
            _uiState.value = SplitUiState.Processing

            val result: OperationResult<List<File>> = when (mode) {
                SplitMode.BY_RANGE -> {
                    val range = _rangeString.value
                    if (range.isBlank()) {
                        _uiState.value = SplitUiState.Error("Please enter a page range.")
                        return@launch
                    }
                    splitPdfUseCase.splitByRange(file.uri, range)
                }
                SplitMode.EVERY_N_PAGES -> {
                    splitPdfUseCase.splitEveryNPages(file.uri, _pagesPerChunk.value)
                }
                SplitMode.INDIVIDUAL -> {
                    splitPdfUseCase.splitIndividual(file.uri)
                }
            }

            when (result) {
                is OperationResult.Success -> {
                    val files = result.data
                    _uiState.value = SplitUiState.Success(
                        outputFiles = files,
                        totalOutputSize = files.sumOf { it.length() },
                        originalSize = file.size
                    )

                    historyRepository.logOperation(
                        Operation(
                            toolName = "Split PDF",
                            inputFile = file.name,
                            inputSize = file.size,
                            outputFile = "${files.size} parts",
                            outputSize = files.sumOf { it.length() },
                            outputPath = files.firstOrNull()?.parent
                        )
                    )

                    files.forEach { outputFile ->
                        documentRepository.addRecentFile(
                            RecentFileEntity(
                                fileName = outputFile.name,
                                filePath = outputFile.absolutePath,
                                fileSize = outputFile.length(),
                                mimeType = "application/pdf",
                                operationPerformed = "Split"
                            )
                        )
                    }

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> {
                    _uiState.value = SplitUiState.Error(result.message)
                }
                is OperationResult.Loading -> { /* progress flow handles this */ }
            }
        }
    }
}

sealed class SplitUiState {
    data object Idle : SplitUiState()
    data object FileSelected : SplitUiState()
    data object Processing : SplitUiState()
    data class Success(
        val outputFiles: List<File>,
        val totalOutputSize: Long,
        val originalSize: Long
    ) : SplitUiState()
    data class Error(val message: String) : SplitUiState()
}
