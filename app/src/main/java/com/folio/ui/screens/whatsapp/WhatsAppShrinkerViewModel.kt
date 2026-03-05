package com.folio.ui.screens.whatsapp

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
import com.folio.domain.usecase.smart.WhatsAppShrinkerUseCase
import com.folio.domain.usecase.smart.WhatsAppShrinkerUseCase.ShrinkResult
import com.folio.domain.usecase.smart.WhatsAppShrinkerUseCase.SizeTarget
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsAppShrinkerViewModel @Inject constructor(
    private val whatsAppShrinkerUseCase: WhatsAppShrinkerUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _selectedTarget = MutableStateFlow(SizeTarget.WHATSAPP_OPTIMAL)
    val selectedTarget: StateFlow<SizeTarget> = _selectedTarget.asStateFlow()

    private val _customTargetMb = MutableStateFlow(10f)
    val customTargetMb: StateFlow<Float> = _customTargetMb.asStateFlow()

    private val _uiState = MutableStateFlow<WhatsAppUiState>(WhatsAppUiState.Idle)
    val uiState: StateFlow<WhatsAppUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = whatsAppShrinkerUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = WhatsAppUiState.Idle
    }

    fun setTarget(target: SizeTarget) { _selectedTarget.value = target }
    fun setCustomTargetMb(mb: Float) { _customTargetMb.value = mb }

    fun clearAll() {
        _selectedFile.value = null
        _uiState.value = WhatsAppUiState.Idle
    }

    fun shrink() {
        val file = _selectedFile.value ?: return

        val customMb = if (_selectedTarget.value == SizeTarget.CUSTOM) {
            _customTargetMb.value.toInt()
        } else null

        viewModelScope.launch {
            _uiState.value = WhatsAppUiState.Processing
            when (val result = whatsAppShrinkerUseCase.execute(file.uri, _selectedTarget.value, customMb)) {
                is OperationResult.Success -> {
                    val shrinkResult = result.data
                    _uiState.value = WhatsAppUiState.Success(shrinkResult)
                    historyRepository.logOperation(
                        Operation(
                            toolName = "WhatsApp Shrinker",
                            inputFile = file.name,
                            inputSize = shrinkResult.originalSize,
                            outputFile = shrinkResult.outputFile.name,
                            outputSize = shrinkResult.outputSize,
                            outputPath = shrinkResult.outputFile.absolutePath
                        )
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(
                            fileName = shrinkResult.outputFile.name,
                            filePath = shrinkResult.outputFile.absolutePath,
                            fileSize = shrinkResult.outputSize,
                            mimeType = "application/pdf",
                            operationPerformed = "WhatsApp Shrinker"
                        )
                    )

                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = WhatsAppUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

sealed class WhatsAppUiState {
    data object Idle : WhatsAppUiState()
    data object Processing : WhatsAppUiState()
    data class Success(val result: ShrinkResult) : WhatsAppUiState()
    data class Error(val message: String) : WhatsAppUiState()
}
