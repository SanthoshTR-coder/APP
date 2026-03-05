package com.folio.ui.screens.editpdf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditPdfViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val editorMode by viewModel.editorMode.collectAsStateWithLifecycle()
    val textEdits by viewModel.textEdits.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val pagesToDelete by viewModel.pagesToDelete.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val pendingText by viewModel.pendingText.collectAsStateWithLifecycle()
    val pendingFontSize by viewModel.pendingFontSize.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val hasEdits by viewModel.hasEdits.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()

    val accentColor = FolioTheme.colors.editAccent
    val pastelColor = FolioTheme.colors.editPastel

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Edit PDF",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is EditPdfUiState.Success -> {
                FolioSuccessScreen(
                    outputFileName = state.outputFile.name,
                    originalSize = state.originalSize,
                    outputSize = state.outputSize,
                    operationLabel = "PDF Edited",
                    onShare = {
                        val shareUri = FileUtil.getShareableUri(context, state.outputFile)
                        FileUtil.shareFile(context, shareUri, "application/pdf")
                    },
                    onOpen = {
                        val openUri = FileUtil.getShareableUri(context, state.outputFile)
                        FileUtil.openFile(context, openUri, "application/pdf")
                    },
                    onDone = { viewModel.clearAll() }
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // File Picker
                    if (selectedFile == null) {
                        FilePicker(
                            label = "Select PDF to edit",
                            onPickFile = { filePicker.launch(arrayOf("application/pdf")) }
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedFile!!.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(FileUtil.formatFileSize(selectedFile!!.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.clearAll() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove file")
                                }
                            }
                        }
                    }

                    // Editor UI
                    AnimatedVisibility(visible = selectedFile != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Page selector
                            OutlinedTextField(
                                value = currentPage.toString(),
                                onValueChange = { it.toIntOrNull()?.let { p -> viewModel.setCurrentPage(p) } },
                                label = { Text("Current Page") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor)
                            )

                            // Mode selector tabs
                            Text("Edit Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EditorMode.entries.forEach { mode ->
                                    val (icon, label) = when (mode) {
                                        EditorMode.TEXT -> Icons.Default.TextFields to "Text"
                                        EditorMode.IMAGE -> Icons.Default.Image to "Image"
                                        EditorMode.HIGHLIGHT -> Icons.Default.Highlight to "Highlight"
                                        EditorMode.DELETE_PAGES -> Icons.Default.DeleteSweep to "Delete"
                                    }
                                    FilterChip(
                                        selected = editorMode == mode,
                                        onClick = { viewModel.setEditorMode(mode) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                            selectedLabelColor = accentColor
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Mode-specific panels
                            when (editorMode) {
                                EditorMode.TEXT -> TextEditPanel(
                                    pendingText = pendingText,
                                    fontSize = pendingFontSize,
                                    onTextChange = { viewModel.setPendingText(it) },
                                    onFontSizeChange = { viewModel.setPendingFontSize(it) },
                                    onAdd = { viewModel.addTextEdit() },
                                    textEdits = textEdits,
                                    onRemove = { viewModel.removeTextEdit(it) },
                                    accentColor = accentColor
                                )
                                EditorMode.IMAGE -> ImageEditPanel(accentColor = accentColor)
                                EditorMode.HIGHLIGHT -> HighlightEditPanel(
                                    highlights = highlights,
                                    onAdd = { viewModel.addHighlight() },
                                    onRemove = { viewModel.removeHighlight(it) },
                                    accentColor = accentColor,
                                    currentPage = currentPage
                                )
                                EditorMode.DELETE_PAGES -> DeletePagesPanel(
                                    pagesToDelete = pagesToDelete,
                                    onToggle = { viewModel.togglePageDelete(it) },
                                    accentColor = accentColor
                                )
                            }

                            // Edit summary
                            if (hasEdits) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Pending Edits", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
                                        if (textEdits.isNotEmpty()) Text("${textEdits.size} text addition(s)", style = MaterialTheme.typography.bodySmall)
                                        if (highlights.isNotEmpty()) Text("${highlights.size} highlight(s)", style = MaterialTheme.typography.bodySmall)
                                        if (pagesToDelete.isNotEmpty()) Text("${pagesToDelete.size} page(s) to delete", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            // Error state
                            if (uiState is EditPdfUiState.Error) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Text((uiState as EditPdfUiState.Error).message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }

                            // Apply button
                            FolioButton(
                                text = if (uiState is EditPdfUiState.Processing) "Applying..." else "Apply Edits",
                                onClick = { viewModel.applyEdits() },
                                enabled = hasEdits && uiState !is EditPdfUiState.Processing,
                                accentColor = accentColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (!adsRemoved) {
                        Spacer(modifier = Modifier.weight(1f))
                        AdBanner(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (uiState is EditPdfUiState.Processing) {
                    FolioProgressSheet(
                        fileName = selectedFile?.name ?: "",
                        operationLabel = "Applying Edits",
                        progress = progress.fraction,
                        onCancel = { },
                        onDismiss = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextEditPanel(
    pendingText: String,
    fontSize: Float,
    onTextChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onAdd: () -> Unit,
    textEdits: List<com.folio.domain.usecase.edit.TextEdit>,
    onRemove: (Int) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = pendingText,
            onValueChange = onTextChange,
            label = { Text("Text to add") },
            placeholder = { Text("Enter text to place on page") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Font Size: ${fontSize.toInt()}pt", style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = fontSize,
            onValueChange = onFontSizeChange,
            valueRange = 8f..72f,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
        )

        Button(
            onClick = onAdd,
            enabled = pendingText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Text")
        }

        // List of added text edits
        textEdits.forEachIndexed { index, edit ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("\"${edit.text}\"", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("Page ${edit.page} · ${edit.fontSize.toInt()}pt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageEditPanel(accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Image, contentDescription = null, tint = accentColor, modifier = Modifier.size(40.dp))
            Text("Image placement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Image overlay placement with drag-and-drop positioning will be available in a future update.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun HighlightEditPanel(
    highlights: List<com.folio.domain.usecase.edit.HighlightEdit>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    accentColor: Color,
    currentPage: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add highlight regions to the current page", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Highlight to Page $currentPage")
        }

        highlights.forEachIndexed { index, hl ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Highlight, contentDescription = null, tint = Color(0xFFF9A825), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Highlight on page ${hl.page}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletePagesPanel(
    pagesToDelete: Set<Int>,
    onToggle: (Int) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Enter page numbers to delete", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        var pageInput by remember { mutableStateOf("") }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = pageInput,
                onValueChange = { pageInput = it },
                label = { Text("Page Number") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor)
            )
            Button(
                onClick = {
                    pageInput.toIntOrNull()?.let {
                        onToggle(it)
                        pageInput = ""
                    }
                },
                enabled = pageInput.toIntOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        if (pagesToDelete.isNotEmpty()) {
            Text("Pages marked for deletion:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pagesToDelete.sorted().forEach { page ->
                    InputChip(
                        selected = true,
                        onClick = { onToggle(page) },
                        label = { Text("Page $page") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Unmark", modifier = Modifier.size(14.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }
        }
    }
}
