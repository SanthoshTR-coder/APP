package com.folio.ui.screens.unlock

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
import com.folio.domain.usecase.security.UnlockPdfUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val unlockPdfUseCase: UnlockPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isProtected = MutableStateFlow<Boolean?>(null)
    val isProtected: StateFlow<Boolean?> = _isProtected.asStateFlow()

    private val _uiState = MutableStateFlow<UnlockUiState>(UnlockUiState.Idle)
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = unlockPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        val doc = documentRepository.getDocumentFile(uri)
        _selectedFile.value = doc
        _uiState.value = UnlockUiState.Idle
        _password.value = ""

        // Check if file is protected
        viewModelScope.launch {
            val protected = unlockPdfUseCase.isPasswordProtected(uri)
            _isProtected.value = protected
            if (!protected) {
                _uiState.value = UnlockUiState.NotProtected
            }
        }
    }

    fun setPassword(pw: String) { _password.value = pw }

    fun clearAll() {
        _selectedFile.value = null
        _password.value = ""
        _isProtected.value = null
        _uiState.value = UnlockUiState.Idle
    }

    fun unlock() {
        val file = _selectedFile.value ?: return
        val pw = _password.value
        if (pw.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = UnlockUiState.Processing
            when (val result = unlockPdfUseCase.execute(file.uri, pw)) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = UnlockUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(toolName = "Unlock PDF", inputFile = file.name, inputSize = file.size, outputFile = output.name, outputSize = output.length(), outputPath = output.absolutePath)
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = output.name, filePath = output.absolutePath, fileSize = output.length(), mimeType = "application/pdf", operationPerformed = "Unlock PDF")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = UnlockUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class UnlockUiState {
    data object Idle : UnlockUiState()
    data object NotProtected : UnlockUiState()
    data object Processing : UnlockUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : UnlockUiState()
    data class Error(val message: String) : UnlockUiState()
}
