package com.folio.ui.screens.watermark

import android.graphics.Bitmap
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
import com.folio.domain.usecase.security.WatermarkConfig
import com.folio.domain.usecase.security.WatermarkPdfUseCase
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WatermarkViewModel @Inject constructor(
    private val watermarkPdfUseCase: WatermarkPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _watermarkText = MutableStateFlow("CONFIDENTIAL")
    val watermarkText: StateFlow<String> = _watermarkText.asStateFlow()

    private val _fontSize = MutableStateFlow(48f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _opacity = MutableStateFlow(0.3f)
    val opacity: StateFlow<Float> = _opacity.asStateFlow()

    private val _angleDegrees = MutableStateFlow(45f)
    val angleDegrees: StateFlow<Float> = _angleDegrees.asStateFlow()

    private val _selectedColorHex = MutableStateFlow("#FF0000")
    val selectedColorHex: StateFlow<String> = _selectedColorHex.asStateFlow()

    private val _isImageWatermark = MutableStateFlow(false)
    val isImageWatermark: StateFlow<Boolean> = _isImageWatermark.asStateFlow()

    private val _watermarkImageUri = MutableStateFlow<Uri?>(null)
    val watermarkImageUri: StateFlow<Uri?> = _watermarkImageUri.asStateFlow()

    private val _uiState = MutableStateFlow<WatermarkUiState>(WatermarkUiState.Idle)
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = watermarkPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    val presetColors = WatermarkPdfUseCase.PRESET_COLORS

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = WatermarkUiState.Idle
    }

    fun setWatermarkText(text: String) { _watermarkText.value = text }
    fun setFontSize(size: Float) { _fontSize.value = size }
    fun setOpacity(value: Float) { _opacity.value = value }
    fun setAngleDegrees(angle: Float) { _angleDegrees.value = angle }
    fun setColorHex(hex: String) { _selectedColorHex.value = hex }
    fun setIsImageWatermark(isImage: Boolean) { _isImageWatermark.value = isImage }
    fun setWatermarkImageUri(uri: Uri?) { _watermarkImageUri.value = uri }

    fun clearAll() {
        _selectedFile.value = null
        _watermarkText.value = "CONFIDENTIAL"
        _fontSize.value = 48f
        _opacity.value = 0.3f
        _angleDegrees.value = 45f
        _selectedColorHex.value = "#FF0000"
        _isImageWatermark.value = false
        _watermarkImageUri.value = null
        _uiState.value = WatermarkUiState.Idle
    }

    fun applyWatermark() {
        val file = _selectedFile.value ?: return

        val config = WatermarkConfig(
            text = _watermarkText.value,
            fontSize = _fontSize.value,
            opacity = _opacity.value,
            angleDegrees = _angleDegrees.value,
            colorHex = _selectedColorHex.value,
            imageUri = if (_isImageWatermark.value) _watermarkImageUri.value else null
        )

        viewModelScope.launch {
            _uiState.value = WatermarkUiState.Processing
            when (val result = watermarkPdfUseCase.execute(file.uri, config)) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = WatermarkUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(toolName = "Watermark PDF", inputFile = file.name, inputSize = file.size, outputFile = output.name, outputSize = output.length(), outputPath = output.absolutePath)
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = output.name, filePath = output.absolutePath, fileSize = output.length(), mimeType = "application/pdf", operationPerformed = "Watermark PDF")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = WatermarkUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class WatermarkUiState {
    data object Idle : WatermarkUiState()
    data object Processing : WatermarkUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : WatermarkUiState()
    data class Error(val message: String) : WatermarkUiState()
}
