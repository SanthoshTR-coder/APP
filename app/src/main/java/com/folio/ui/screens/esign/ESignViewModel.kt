package com.folio.ui.screens.esign

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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
import com.folio.domain.usecase.security.ESignPdfUseCase
import com.folio.domain.usecase.security.ESignPdfUseCase.DateStamp
import com.folio.domain.usecase.security.ESignPdfUseCase.SignaturePlacement
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ESignViewModel @Inject constructor(
    private val eSignPdfUseCase: ESignPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _signatureMode = MutableStateFlow(SignatureMode.DRAW)
    val signatureMode: StateFlow<SignatureMode> = _signatureMode.asStateFlow()

    private val _signatureBitmap = MutableStateFlow<Bitmap?>(null)
    val signatureBitmap: StateFlow<Bitmap?> = _signatureBitmap.asStateFlow()

    private val _typedName = MutableStateFlow("")
    val typedName: StateFlow<String> = _typedName.asStateFlow()

    private val _selectedFontIndex = MutableStateFlow(0)
    val selectedFontIndex: StateFlow<Int> = _selectedFontIndex.asStateFlow()

    private val _signatureImageUri = MutableStateFlow<Uri?>(null)
    val signatureImageUri: StateFlow<Uri?> = _signatureImageUri.asStateFlow()

    private val _targetPage = MutableStateFlow(1)
    val targetPage: StateFlow<Int> = _targetPage.asStateFlow()

    private val _addDateStamp = MutableStateFlow(true)
    val addDateStamp: StateFlow<Boolean> = _addDateStamp.asStateFlow()

    // Drawing state
    private val _drawingPaths = MutableStateFlow<List<List<Offset>>>(emptyList())
    val drawingPaths: StateFlow<List<List<Offset>>> = _drawingPaths.asStateFlow()

    private val _currentPath = MutableStateFlow<List<Offset>>(emptyList())
    val currentPath: StateFlow<List<Offset>> = _currentPath.asStateFlow()

    private val _uiState = MutableStateFlow<ESignUiState>(ESignUiState.Idle)
    val uiState: StateFlow<ESignUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = eSignPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = ESignUiState.Idle
    }

    fun setSignatureMode(mode: SignatureMode) { _signatureMode.value = mode }
    fun setTypedName(name: String) { _typedName.value = name }
    fun setSelectedFontIndex(index: Int) { _selectedFontIndex.value = index }
    fun setSignatureImageUri(uri: Uri?) { _signatureImageUri.value = uri }
    fun setTargetPage(page: Int) { _targetPage.value = page }
    fun setAddDateStamp(add: Boolean) { _addDateStamp.value = add }

    fun setSignatureBitmap(bitmap: Bitmap?) { _signatureBitmap.value = bitmap }

    // Drawing methods
    fun onDrawStart(offset: Offset) {
        _currentPath.value = listOf(offset)
    }

    fun onDrawMove(offset: Offset) {
        _currentPath.value = _currentPath.value + offset
    }

    fun onDrawEnd() {
        if (_currentPath.value.isNotEmpty()) {
            _drawingPaths.value = _drawingPaths.value + listOf(_currentPath.value)
            _currentPath.value = emptyList()
        }
    }

    fun clearDrawing() {
        _drawingPaths.value = emptyList()
        _currentPath.value = emptyList()
        _signatureBitmap.value = null
    }

    fun clearAll() {
        _selectedFile.value = null
        _signatureBitmap.value = null
        _typedName.value = ""
        _signatureImageUri.value = null
        clearDrawing()
        _uiState.value = ESignUiState.Idle
    }

    fun applySignature() {
        val file = _selectedFile.value ?: return
        val bitmap = _signatureBitmap.value ?: return

        val placement = SignaturePlacement(
            pageNumber = _targetPage.value,
            x = 100f,
            y = 100f,
            width = 200f,
            height = 80f,
            signatureBitmap = bitmap
        )

        val dateStamp = if (_addDateStamp.value) {
            val dateText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            DateStamp(
                pageNumber = _targetPage.value,
                x = 100f,
                y = 80f,
                text = "Signed: $dateText"
            )
        } else null

        viewModelScope.launch {
            _uiState.value = ESignUiState.Processing
            when (val result = eSignPdfUseCase.execute(file.uri, listOf(placement), listOfNotNull(dateStamp))) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = ESignUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(toolName = "E-Sign PDF", inputFile = file.name, inputSize = file.size, outputFile = output.name, outputSize = output.length(), outputPath = output.absolutePath)
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = output.name, filePath = output.absolutePath, fileSize = output.length(), mimeType = "application/pdf", operationPerformed = "E-Sign PDF")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = ESignUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

enum class SignatureMode { DRAW, TYPE, IMAGE }

sealed class ESignUiState {
    data object Idle : ESignUiState()
    data object Processing : ESignUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : ESignUiState()
    data class Error(val message: String) : ESignUiState()
}
