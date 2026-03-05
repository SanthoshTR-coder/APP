package com.folio.ui.screens.split

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.domain.usecase.pdf.SplitPdfUseCase.SplitMode
import com.folio.ui.components.*
import com.folio.ui.theme.*
import com.folio.util.FileUtil

/**
 * Split PDF Screen
 *
 * Three modes:
 * 1. By Range — e.g. "1-3, 5, 8-12"
 * 2. Every N Pages — split into equal chunks
 * 3. Individual — one PDF per page
 *
 * Accent: SplitAccent (#D4612A) / SplitPastel (#FFE4D4)
 */
@Composable
fun SplitScreen(
    onNavigateBack: () -> Unit,
    viewModel: SplitViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsState()
    val splitMode by viewModel.splitMode.collectAsState()
    val rangeString by viewModel.rangeString.collectAsState()
    val pagesPerChunk by viewModel.pagesPerChunk.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val filePicker = rememberSingleFilePicker(
        mimeTypes = PDF_MIME_TYPES,
        onFilePicked = { uri -> viewModel.selectFile(uri) }
    )

    // Success screen
    when (val state = uiState) {
        is SplitUiState.Success -> {
            FolioSuccessScreen(
                outputFileName = "${state.outputFiles.size} PDF files created",
                originalSize = state.originalSize,
                outputSize = state.totalOutputSize,
                operationLabel = "Split",
                onShare = {
                    // Share first file
                    state.outputFiles.firstOrNull()?.let { file ->
                        val intent = FileUtil.createShareIntent(context, file)
                        context.startActivity(Intent.createChooser(intent, "Share split PDF"))
                    }
                },
                onOpen = {
                    state.outputFiles.firstOrNull()?.let { file ->
                        val intent = FileUtil.createOpenIntent(context, file)
                        context.startActivity(intent)
                    }
                },
                onDone = {
                    viewModel.clearFile()
                    onNavigateBack()
                }
            )
            return
        }
        else -> {}
    }

    // Progress sheet
    if (uiState is SplitUiState.Processing) {
        FolioProgressSheet(
            fileName = selectedFile?.name ?: "PDF",
            operationLabel = "Splitting",
            progress = if (progress.isIndeterminate) null else progress.fraction
        )
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is SplitUiState.Error) {
            snackbarHostState.showSnackbar((uiState as SplitUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Split PDF",
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FolioTheme.colors.background,
        bottomBar = {
            if (selectedFile != null) {
                SplitBottomBar(
                    isProcessing = uiState is SplitUiState.Processing,
                    onSplit = { viewModel.split() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (selectedFile == null) {
                EmptySplitState(onPickFile = { filePicker.launch(PDF_MIME_TYPES) })
            } else {
                // Selected file
                SelectedFileCard(
                    fileName = selectedFile!!.name,
                    fileSize = selectedFile!!.sizeFormatted,
                    onRemove = { viewModel.clearFile() }
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Split mode picker
                Text(
                    text = "Split Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = FolioTheme.colors.onSurface,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                SplitModeSelector(
                    selectedMode = splitMode,
                    onModeSelected = { viewModel.setSplitMode(it) }
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Mode-specific options
                when (splitMode) {
                    SplitMode.BY_RANGE -> {
                        RangeInput(
                            value = rangeString,
                            onValueChange = { viewModel.setRangeString(it) }
                        )
                    }
                    SplitMode.EVERY_N_PAGES -> {
                        ChunkSizeInput(
                            value = pagesPerChunk,
                            onValueChange = { viewModel.setPagesPerChunk(it) }
                        )
                    }
                    SplitMode.INDIVIDUAL -> {
                        Surface(
                            shape = RoundedCornerShape(Radius.sm),
                            color = SplitPastel.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md)
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = SplitAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Each page will be saved as a separate PDF file.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SplitAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────

@Composable
private fun EmptySplitState(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = SplitPastel
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CallSplit,
                    contentDescription = null,
                    tint = SplitAccent,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Split PDF into parts",
            style = MaterialTheme.typography.headlineSmall,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W600
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Extract specific pages, split into chunks,\nor create individual page PDFs.",
            style = MaterialTheme.typography.bodyMedium,
            color = FolioTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        FolioButton(
            text = "Select PDF",
            onClick = onPickFile,
            accentColor = SplitAccent
        )
    }
}

// ─── Selected File Card ──────────────────────────────────────

@Composable
private fun SelectedFileCard(
    fileName: String,
    fileSize: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = FolioTheme.colors.cardSurface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Surface(
                shape = CircleShape,
                color = SplitPastel,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = SplitAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W500,
                    color = FolioTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fileSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = FolioTheme.colors.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = FolioTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Split Mode Selector ─────────────────────────────────────

@Composable
private fun SplitModeSelector(
    selectedMode: SplitMode,
    onModeSelected: (SplitMode) -> Unit
) {
    val modes = listOf(
        Triple(SplitMode.BY_RANGE, "By Range", "e.g. 1-3, 5, 8-12"),
        Triple(SplitMode.EVERY_N_PAGES, "Every N Pages", "Equal chunks"),
        Triple(SplitMode.INDIVIDUAL, "Individual", "One per page")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        modes.forEach { (mode, label, desc) ->
            val isSelected = selectedMode == mode
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) SplitPastel else FolioTheme.colors.cardSurface,
                label = "modeBg"
            )

            Surface(
                onClick = { onModeSelected(mode) },
                shape = RoundedCornerShape(Radius.md),
                color = bgColor,
                border = if (isSelected) BorderStroke(1.5.dp, SplitAccent)
                else null,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) SplitAccent else FolioTheme.colors.onSurface,
                        fontWeight = FontWeight.W600
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = FolioTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Range Input ─────────────────────────────────────────────

@Composable
private fun RangeInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
        Text(
            text = "Page Range",
            style = MaterialTheme.typography.labelLarge,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W500
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("e.g. 1-3, 5, 8-12") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SplitAccent,
                cursorColor = SplitAccent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "Separate ranges with commas. Each range becomes a separate PDF.",
            style = MaterialTheme.typography.bodySmall,
            color = FolioTheme.colors.onSurfaceVariant
        )
    }
}

// ─── Chunk Size Input ────────────────────────────────────────

@Composable
private fun ChunkSizeInput(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
        Text(
            text = "Pages per chunk",
            style = MaterialTheme.typography.labelLarge,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W500
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            IconButton(
                onClick = { if (value > 1) onValueChange(value - 1) },
                enabled = value > 1
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = if (value > 1) SplitAccent else FolioTheme.colors.divider
                )
            }

            Surface(
                shape = RoundedCornerShape(Radius.md),
                color = SplitPastel,
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SplitAccent,
                    fontWeight = FontWeight.W700,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Spacing.sm)
                )
            }

            IconButton(onClick = { onValueChange(value + 1) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = SplitAccent
                )
            }
        }
    }
}

// ─── Bottom Bar ──────────────────────────────────────────────

@Composable
private fun SplitBottomBar(
    isProcessing: Boolean,
    onSplit: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = FolioTheme.colors.cardSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md)
                .navigationBarsPadding()
        ) {
            FolioButton(
                text = if (isProcessing) "Splitting…" else "Split PDF",
                onClick = onSplit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                accentColor = SplitAccent
            )
        }
    }
}
