package com.folio.ui.screens.reorder

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
import com.folio.domain.usecase.pdf.ReorderPdfUseCase
import com.folio.util.FileUtil
import com.folio.util.OperationCleanup
import com.folio.util.PdfUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reorderPdfUseCase: ReorderPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _tempFile = MutableStateFlow<File?>(null)
    val tempFile: StateFlow<File?> = _tempFile.asStateFlow()

    private val _pageOrder = MutableStateFlow<List<Int>>(emptyList())
    val pageOrder: StateFlow<List<Int>> = _pageOrder.asStateFlow()

    private val _uiState = MutableStateFlow<ReorderUiState>(ReorderUiState.Idle)
    val uiState: StateFlow<ReorderUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = reorderPdfUseCase.progress

    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        val doc = documentRepository.getDocumentFile(uri) ?: return
        _selectedFile.value = doc

        viewModelScope.launch {
            val count = PdfUtil.getPageCount(context, uri)
            _pageOrder.value = (0 until count).toList()

            val temp = FileUtil.copyToTempFile(
                context, uri, "reorder_preview_${System.currentTimeMillis()}.pdf"
            )
            _tempFile.value = temp
            _uiState.value = ReorderUiState.FileSelected
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val current = _pageOrder.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _pageOrder.value = current
        }
    }

    fun resetOrder() {
        val count = _pageOrder.value.size
        _pageOrder.value = (0 until count).toList()
    }

    fun clearFile() {
        _selectedFile.value = null
        _tempFile.value?.delete()
        _tempFile.value = null
        _pageOrder.value = emptyList()
        _uiState.value = ReorderUiState.Idle
    }

    fun reorder() {
        val file = _selectedFile.value ?: return
        val order = _pageOrder.value
        if (order.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = ReorderUiState.Processing

            when (val result = reorderPdfUseCase.execute(file.uri, order)) {
                is OperationResult.Success -> {
                    val outputFile = result.data
                    _uiState.value = ReorderUiState.Success(
                        outputFile = outputFile,
                        outputSize = outputFile.length(),
                        originalSize = file.size
                    )

                    historyRepository.logOperation(
                        Operation(
                            toolName = "Reorder PDF",
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
                            operationPerformed = "Reordered"
                        )
                    )

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> {
                    _uiState.value = ReorderUiState.Error(result.message)
                }
                is OperationResult.Loading -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _tempFile.value?.delete()
    }
}

sealed class ReorderUiState {
    data object Idle : ReorderUiState()
    data object FileSelected : ReorderUiState()
    data object Processing : ReorderUiState()
    data class Success(
        val outputFile: File,
        val outputSize: Long,
        val originalSize: Long
    ) : ReorderUiState()
    data class Error(val message: String) : ReorderUiState()
}
