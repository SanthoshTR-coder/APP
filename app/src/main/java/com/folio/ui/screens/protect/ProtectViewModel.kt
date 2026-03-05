package com.folio.ui.screens.protect

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
import com.folio.domain.usecase.security.PasswordStrength
import com.folio.domain.usecase.security.ProtectPdfUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProtectViewModel @Inject constructor(
    private val protectPdfUseCase: ProtectPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _allowPrinting = MutableStateFlow(true)
    val allowPrinting: StateFlow<Boolean> = _allowPrinting.asStateFlow()

    private val _allowCopying = MutableStateFlow(false)
    val allowCopying: StateFlow<Boolean> = _allowCopying.asStateFlow()

    private val _uiState = MutableStateFlow<ProtectUiState>(ProtectUiState.Idle)
    val uiState: StateFlow<ProtectUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = protectPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    val passwordStrength: StateFlow<PasswordStrength?> = _password.map { pw ->
        if (pw.isEmpty()) null else ProtectPdfUseCase.getPasswordStrength(pw)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val passwordsMatch: StateFlow<Boolean> = combine(_password, _confirmPassword) { pw, cpw ->
        pw.isNotEmpty() && pw == cpw
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = ProtectUiState.Idle
    }

    fun setPassword(pw: String) { _password.value = pw }
    fun setConfirmPassword(pw: String) { _confirmPassword.value = pw }
    fun setAllowPrinting(allow: Boolean) { _allowPrinting.value = allow }
    fun setAllowCopying(allow: Boolean) { _allowCopying.value = allow }

    fun clearAll() {
        _selectedFile.value = null
        _password.value = ""
        _confirmPassword.value = ""
        _uiState.value = ProtectUiState.Idle
    }

    fun protect() {
        val file = _selectedFile.value ?: return
        if (!passwordsMatch.value) return

        viewModelScope.launch {
            _uiState.value = ProtectUiState.Processing
            when (val result = protectPdfUseCase.execute(
                uri = file.uri,
                userPassword = _password.value,
                allowPrinting = _allowPrinting.value,
                allowCopying = _allowCopying.value
            )) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = ProtectUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(toolName = "Protect PDF", inputFile = file.name, inputSize = file.size, outputFile = output.name, outputSize = output.length(), outputPath = output.absolutePath)
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = output.name, filePath = output.absolutePath, fileSize = output.length(), mimeType = "application/pdf", operationPerformed = "Protect PDF")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = ProtectUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class ProtectUiState {
    data object Idle : ProtectUiState()
    data object Processing : ProtectUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : ProtectUiState()
    data class Error(val message: String) : ProtectUiState()
}
