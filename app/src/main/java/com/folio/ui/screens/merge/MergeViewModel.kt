package com.folio.ui.screens.merge

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
import com.folio.domain.usecase.pdf.MergePdfUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MergeViewModel @Inject constructor(
    private val mergePdfUseCase: MergePdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    // ─── State ───────────────────────────────────────────

    private val _selectedFiles = MutableStateFlow<List<DocumentFile>>(emptyList())
    val selectedFiles: StateFlow<List<DocumentFile>> = _selectedFiles.asStateFlow()

    private val _uiState = MutableStateFlow<MergeUiState>(MergeUiState.Idle)
    val uiState: StateFlow<MergeUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = mergePdfUseCase.progress

    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    // ─── Actions ─────────────────────────────────────────

    fun addFiles(uris: List<Uri>) {
        val current = _selectedFiles.value.toMutableList()
        uris.forEach { uri ->
            val doc = documentRepository.getDocumentFile(uri)
            if (doc != null && current.size < 15) {
                current.add(doc)
            }
        }
        _selectedFiles.value = current
    }

    fun removeFile(index: Int) {
        val current = _selectedFiles.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedFiles.value = current
        }
    }

    fun reorderFiles(fromIndex: Int, toIndex: Int) {
        val current = _selectedFiles.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _selectedFiles.value = current
        }
    }

    fun clearFiles() {
        _selectedFiles.value = emptyList()
        _uiState.value = MergeUiState.Idle
    }

    fun merge() {
        val files = _selectedFiles.value
        if (files.size < 2) {
            _uiState.value = MergeUiState.Error("Please select at least 2 files to merge.")
            return
        }

        viewModelScope.launch {
            _uiState.value = MergeUiState.Processing

            val uris = files.map { it.uri }
            val totalInputSize = files.sumOf { it.size }

            when (val result = mergePdfUseCase.execute(uris)) {
                is OperationResult.Success -> {
                    val outputFile = result.data
                    _uiState.value = MergeUiState.Success(
                        outputFile = outputFile,
                        outputSize = outputFile.length(),
                        originalTotalSize = totalInputSize
                    )

                    // Log to history
                    historyRepository.logOperation(
                        Operation(
                            toolName = "Merge PDF",
                            inputFile = "${files.size} files",
                            inputSize = totalInputSize,
                            outputFile = outputFile.name,
                            outputSize = outputFile.length(),
                            outputPath = outputFile.absolutePath
                        )
                    )

                    // Add to recent files
                    documentRepository.addRecentFile(
                        RecentFileEntity(
                            fileName = outputFile.name,
                            filePath = outputFile.absolutePath,
                            fileSize = outputFile.length(),
                            mimeType = "application/pdf",
                            operationPerformed = "Merged"
                        )
                    )

                    // Clean up temp files + increment stats
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> {
                    _uiState.value = MergeUiState.Error(result.message)
                }
                is OperationResult.Loading -> { /* handled by progress flow */ }
            }
        }
    }
}

sealed class MergeUiState {
    data object Idle : MergeUiState()
    data object Processing : MergeUiState()
    data class Success(
        val outputFile: File,
        val outputSize: Long,
        val originalTotalSize: Long
    ) : MergeUiState()
    data class Error(val message: String) : MergeUiState()
}
