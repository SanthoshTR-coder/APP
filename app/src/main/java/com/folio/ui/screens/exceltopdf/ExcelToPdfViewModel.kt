package com.folio.ui.screens.exceltopdf

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
import com.folio.domain.usecase.convert.ExcelToPdfUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExcelToPdfViewModel @Inject constructor(
    private val excelToPdfUseCase: ExcelToPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _sheetNames = MutableStateFlow<List<String>>(emptyList())
    val sheetNames: StateFlow<List<String>> = _sheetNames.asStateFlow()

    private val _selectedSheets = MutableStateFlow<Set<Int>>(emptySet())
    val selectedSheets: StateFlow<Set<Int>> = _selectedSheets.asStateFlow()

    private val _showGridLines = MutableStateFlow(true)
    val showGridLines: StateFlow<Boolean> = _showGridLines.asStateFlow()

    private val _uiState = MutableStateFlow<ExcelUiState>(ExcelUiState.Idle)
    val uiState: StateFlow<ExcelUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = excelToPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = ExcelUiState.Idle
        viewModelScope.launch {
            try {
                val names = excelToPdfUseCase.getSheetNames(uri)
                _sheetNames.value = names
                _selectedSheets.value = names.indices.toSet() // all selected by default
            } catch (e: Exception) {
                _sheetNames.value = emptyList()
            }
        }
    }

    fun toggleSheet(index: Int) {
        val current = _selectedSheets.value.toMutableSet()
        if (current.contains(index)) current.remove(index) else current.add(index)
        _selectedSheets.value = current
    }

    fun selectAllSheets() { _selectedSheets.value = _sheetNames.value.indices.toSet() }
    fun deselectAllSheets() { _selectedSheets.value = emptySet() }
    fun setShowGridLines(show: Boolean) { _showGridLines.value = show }

    fun clearAll() {
        _selectedFile.value = null
        _sheetNames.value = emptyList()
        _selectedSheets.value = emptySet()
        _uiState.value = ExcelUiState.Idle
    }

    fun convert() {
        val file = _selectedFile.value ?: return
        val sheets = _selectedSheets.value
        if (sheets.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = ExcelUiState.Processing
            when (val result = excelToPdfUseCase.execute(
                uri = file.uri,
                selectedSheetIndices = sheets.toList().sorted(),
                showGridLines = _showGridLines.value
            )) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = ExcelUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(toolName = "Excel → PDF", inputFile = file.name, inputSize = file.size, outputFile = output.name, outputSize = output.length(), outputPath = output.absolutePath)
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = output.name, filePath = output.absolutePath, fileSize = output.length(), mimeType = "application/pdf", operationPerformed = "Excel → PDF")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = ExcelUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class ExcelUiState {
    data object Idle : ExcelUiState()
    data object Processing : ExcelUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : ExcelUiState()
    data class Error(val message: String) : ExcelUiState()
}
