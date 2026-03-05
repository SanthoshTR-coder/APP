package com.folio.ui.screens.rotate

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
import com.folio.domain.usecase.pdf.RotatePdfUseCase
import com.folio.domain.usecase.pdf.RotatePdfUseCase.RotationAngle
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
class RotateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rotatePdfUseCase: RotatePdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _tempFile = MutableStateFlow<File?>(null)
    val tempFile: StateFlow<File?> = _tempFile.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _selectedPages = MutableStateFlow<Set<Int>>(emptySet())
    val selectedPages: StateFlow<Set<Int>> = _selectedPages.asStateFlow()

    private val _rotationAngle = MutableStateFlow(RotationAngle.CW_90)
    val rotationAngle: StateFlow<RotationAngle> = _rotationAngle.asStateFlow()

    private val _uiState = MutableStateFlow<RotateUiState>(RotateUiState.Idle)
    val uiState: StateFlow<RotateUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = rotatePdfUseCase.progress

    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        val doc = documentRepository.getDocumentFile(uri) ?: return
        _selectedFile.value = doc

        viewModelScope.launch {
            val count = PdfUtil.getPageCount(context, uri)
            _pageCount.value = count

            // Create temp file for thumbnail rendering
            val temp = FileUtil.copyToTempFile(
                context, uri, "rotate_preview_${System.currentTimeMillis()}.pdf"
            )
            _tempFile.value = temp
            _uiState.value = RotateUiState.FileSelected
        }
    }

    fun togglePageSelection(pageIndex: Int) {
        val current = _selectedPages.value.toMutableSet()
        if (current.contains(pageIndex)) {
            current.remove(pageIndex)
        } else {
            current.add(pageIndex)
        }
        _selectedPages.value = current
    }

    fun selectAllPages() {
        _selectedPages.value = (0 until _pageCount.value).toSet()
    }

    fun clearSelection() {
        _selectedPages.value = emptySet()
    }

    fun setRotationAngle(angle: RotationAngle) {
        _rotationAngle.value = angle
    }

    fun clearFile() {
        _selectedFile.value = null
        _tempFile.value?.delete()
        _tempFile.value = null
        _pageCount.value = 0
        _selectedPages.value = emptySet()
        _uiState.value = RotateUiState.Idle
    }

    fun rotate() {
        val file = _selectedFile.value ?: return
        val angle = _rotationAngle.value
        val pages = _selectedPages.value.toList()

        viewModelScope.launch {
            _uiState.value = RotateUiState.Processing

            when (val result = rotatePdfUseCase.execute(file.uri, pages, angle)) {
                is OperationResult.Success -> {
                    val outputFile = result.data
                    _uiState.value = RotateUiState.Success(
                        outputFile = outputFile,
                        outputSize = outputFile.length(),
                        originalSize = file.size
                    )

                    historyRepository.logOperation(
                        Operation(
                            toolName = "Rotate PDF",
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
                            operationPerformed = "Rotated"
                        )
                    )

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> {
                    _uiState.value = RotateUiState.Error(result.message)
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

sealed class RotateUiState {
    data object Idle : RotateUiState()
    data object FileSelected : RotateUiState()
    data object Processing : RotateUiState()
    data class Success(
        val outputFile: File,
        val outputSize: Long,
        val originalSize: Long
    ) : RotateUiState()
    data class Error(val message: String) : RotateUiState()
}
