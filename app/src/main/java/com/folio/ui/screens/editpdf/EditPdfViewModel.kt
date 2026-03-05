package com.folio.ui.screens.editpdf

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
import com.folio.domain.usecase.edit.EditPdfUseCase
import com.folio.domain.usecase.edit.EditPdfUseCase.EditOperations
import com.folio.domain.usecase.edit.EditPdfUseCase.HighlightEdit
import com.folio.domain.usecase.edit.EditPdfUseCase.ImageEdit
import com.folio.domain.usecase.edit.EditPdfUseCase.TextEdit
import com.folio.util.OperationCleanup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditPdfViewModel @Inject constructor(
    private val editPdfUseCase: EditPdfUseCase,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val operationCleanup: OperationCleanup
) : ViewModel() {

    private val _selectedFile = MutableStateFlow<DocumentFile?>(null)
    val selectedFile: StateFlow<DocumentFile?> = _selectedFile.asStateFlow()

    private val _editorMode = MutableStateFlow(EditorMode.TEXT)
    val editorMode: StateFlow<EditorMode> = _editorMode.asStateFlow()

    // Text edits
    private val _textEdits = MutableStateFlow<List<TextEdit>>(emptyList())
    val textEdits: StateFlow<List<TextEdit>> = _textEdits.asStateFlow()

    // Image edits
    private val _imageEdits = MutableStateFlow<List<ImageEdit>>(emptyList())
    val imageEdits: StateFlow<List<ImageEdit>> = _imageEdits.asStateFlow()

    // Highlights
    private val _highlights = MutableStateFlow<List<HighlightEdit>>(emptyList())
    val highlights: StateFlow<List<HighlightEdit>> = _highlights.asStateFlow()

    // Pages to delete
    private val _pagesToDelete = MutableStateFlow<Set<Int>>(emptySet())
    val pagesToDelete: StateFlow<Set<Int>> = _pagesToDelete.asStateFlow()

    // Current page being edited
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // Pending text input
    private val _pendingText = MutableStateFlow("")
    val pendingText: StateFlow<String> = _pendingText.asStateFlow()

    private val _pendingFontSize = MutableStateFlow(12f)
    val pendingFontSize: StateFlow<Float> = _pendingFontSize.asStateFlow()

    private val _pendingColorHex = MutableStateFlow("#000000")
    val pendingColorHex: StateFlow<String> = _pendingColorHex.asStateFlow()

    private val _uiState = MutableStateFlow<EditPdfUiState>(EditPdfUiState.Idle)
    val uiState: StateFlow<EditPdfUiState> = _uiState.asStateFlow()

    val progress: StateFlow<OperationProgress> = editPdfUseCase.progress
    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    val hasEdits: StateFlow<Boolean> = combine(
        _textEdits, _imageEdits, _highlights, _pagesToDelete
    ) { texts, images, highlights, deletes ->
        texts.isNotEmpty() || images.isNotEmpty() || highlights.isNotEmpty() || deletes.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun selectFile(uri: Uri) {
        _selectedFile.value = documentRepository.getDocumentFile(uri)
        _uiState.value = EditPdfUiState.Idle
    }

    fun setEditorMode(mode: EditorMode) { _editorMode.value = mode }
    fun setCurrentPage(page: Int) { _currentPage.value = page.coerceAtLeast(1) }
    fun setPendingText(text: String) { _pendingText.value = text }
    fun setPendingFontSize(size: Float) { _pendingFontSize.value = size }
    fun setPendingColorHex(hex: String) { _pendingColorHex.value = hex }

    fun addTextEdit(x: Float = 50f, y: Float = 700f) {
        if (_pendingText.value.isBlank()) return
        val edit = TextEdit(
            pageNumber = _currentPage.value,
            x = x,
            y = y,
            text = _pendingText.value,
            fontSize = _pendingFontSize.value,
            colorHex = _pendingColorHex.value
        )
        _textEdits.value = _textEdits.value + edit
        _pendingText.value = ""
    }

    fun addImageEdit(bitmap: Bitmap, x: Float = 50f, y: Float = 500f) {
        val edit = ImageEdit(
            pageNumber = _currentPage.value,
            x = x,
            y = y,
            width = 150f,
            height = 150f,
            bitmap = bitmap
        )
        _imageEdits.value = _imageEdits.value + edit
    }

    fun addHighlight(x: Float = 50f, y: Float = 700f, width: Float = 200f, height: Float = 20f) {
        val highlight = HighlightEdit(
            pageNumber = _currentPage.value,
            x = x,
            y = y,
            width = width,
            height = height,
            colorHex = "#FFFF00",
            opacity = 0.4f
        )
        _highlights.value = _highlights.value + highlight
    }

    fun togglePageDelete(page: Int) {
        _pagesToDelete.value = if (page in _pagesToDelete.value) {
            _pagesToDelete.value - page
        } else {
            _pagesToDelete.value + page
        }
    }

    fun removeTextEdit(index: Int) {
        _textEdits.value = _textEdits.value.toMutableList().also { it.removeAt(index) }
    }

    fun removeHighlight(index: Int) {
        _highlights.value = _highlights.value.toMutableList().also { it.removeAt(index) }
    }

    fun clearAll() {
        _selectedFile.value = null
        _textEdits.value = emptyList()
        _imageEdits.value = emptyList()
        _highlights.value = emptyList()
        _pagesToDelete.value = emptySet()
        _pendingText.value = ""
        _uiState.value = EditPdfUiState.Idle
    }

    fun applyEdits() {
        val file = _selectedFile.value ?: return

        val operations = EditOperations(
            textEdits = _textEdits.value,
            imageEdits = _imageEdits.value,
            drawingOverlays = emptyList(),
            highlights = _highlights.value,
            pagesToDelete = _pagesToDelete.value.toList()
        )

        viewModelScope.launch {
            _uiState.value = EditPdfUiState.Processing
            when (val result = editPdfUseCase.execute(file.uri, operations)) {
                is OperationResult.Success -> {
                    val output = result.data
                    _uiState.value = EditPdfUiState.Success(output, output.length(), file.size)
                    historyRepository.logOperation(
                        Operation(toolName = "Edit PDF", inputFile = file.name, inputSize = file.size, outputFile = output.name, outputSize = output.length(), outputPath = output.absolutePath)
                    )
                    documentRepository.addRecentFile(
                        RecentFileEntity(fileName = output.name, filePath = output.absolutePath, fileSize = output.length(), mimeType = "application/pdf", operationPerformed = "Edit PDF")
                    )
                    operationCleanup.cleanup()
                }
                is OperationResult.Error -> _uiState.value = EditPdfUiState.Error(result.message)
                is OperationResult.Loading -> {}
            }
        }
    }
}

enum class EditorMode { TEXT, IMAGE, HIGHLIGHT, DELETE_PAGES }

sealed class EditPdfUiState {
    data object Idle : EditPdfUiState()
    data object Processing : EditPdfUiState()
    data class Success(val outputFile: File, val outputSize: Long, val originalSize: Long) : EditPdfUiState()
    data class Error(val message: String) : EditPdfUiState()
}
