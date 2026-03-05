package com.folio.ui.screens.imagetopdf

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
import com.folio.domain.usecase.convert.ImageToPdfUseCase
import com.folio.domain.usecase.convert.ImageToPdfUseCase.Margin
import com.folio.domain.usecase.convert.ImageToPdfUseCase.PageSize
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImageToPdfViewModel @Inject constructor(
    private val imageToPdfUseCase: ImageToPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedImages = MutableStateFlow<List<DocumentFile>>(emptyList())
    val selectedImages: StateFlow<List<DocumentFile>> = _selectedImages.asStateFlow()

    private val _pageSize = MutableStateFlow(PageSize.A4)
    val pageSize: StateFlow<PageSize> = _pageSize.asStateFlow()

    private val _margin = MutableStateFlow(Margin.SMALL)
    val margin: StateFlow<Margin> = _margin.asStateFlow()

    private val _uiState = MutableStateFlow<ImageToPdfUiState>(ImageToPdfUiState.Idle)
    val uiState: StateFlow<ImageToPdfUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = imageToPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun addImages(uris: List<Uri>) {
        val current = _selectedImages.value.toMutableList()
        uris.forEach { uri ->
            val doc = documentRepository.getDocumentFile(uri)
            if (doc != null && current.size < 30) {
                current.add(doc)
            }
        }
        _selectedImages.value = current
    }

    fun removeImage(index: Int) {
        val current = _selectedImages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedImages.value = current
        }
    }

    fun reorderImages(fromIndex: Int, toIndex: Int) {
        val current = _selectedImages.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _selectedImages.value = current
        }
    }

    fun setPageSize(size: PageSize) { _pageSize.value = size }
    fun setMargin(margin: Margin) { _margin.value = margin }

    fun clearAll() {
        _selectedImages.value = emptyList()
        _uiState.value = ImageToPdfUiState.Idle
    }

    fun convert() {
        val images = _selectedImages.value
        if (images.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = ImageToPdfUiState.Processing
            val uris = images.map { it.uri }
            val totalInput = images.sumOf { it.size }

            when (val result = imageToPdfUseCase.execute(uris, _pageSize.value, _margin.value)) {
                is OperationResult.Success -> {
                    val outputFile = result.data
                    _uiState.value = ImageToPdfUiState.Success(
                        outputFile = outputFile,
                        outputSize = outputFile.length(),
                        originalTotalSize = totalInput
                    )
                    historyRepository.logOperation(
                        Operation(
                            toolName = "Image → PDF",
                            inputFile = "${images.size} images",
                            inputSize = totalInput,
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
                            operationPerformed = "Image → PDF"
                        )
                    )

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = ImageToPdfUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class ImageToPdfUiState {
    data object Idle : ImageToPdfUiState()
    data object Processing : ImageToPdfUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalTotalSize: Long) : ImageToPdfUiState()
    data class Error(val message: String) : ImageToPdfUiState()
}
