package com.folio.ui.screens.merge

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.domain.model.DocumentFile
import com.folio.ui.components.*
import com.folio.ui.theme.*
import com.folio.util.FileUtil

/**
 * Merge PDF Screen
 *
 * Flow:
 * 1. User picks 2-15 PDFs via multi-file picker
 * 2. Draggable list shows selected files with reorder handles
 * 3. Tap "Merge" to start — shows progress bottom sheet
 * 4. On success — shows Success screen with Share / Open / Done
 *
 * Accent: MergeAccent (#3B82C4) / MergePastel (#D4E8FF)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeScreen(
    onNavigateBack: () -> Unit,
    viewModel: MergeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    // Multi-file picker
    val filePicker = rememberMultiFilePicker(
        mimeTypes = PDF_MIME_TYPES,
        onFilesPicked = { uris -> viewModel.addFiles(uris) }
    )

    // Handle UI states
    when (val state = uiState) {
        is MergeUiState.Success -> {
            FolioSuccessScreen(
                outputFileName = state.outputFile.name,
                originalSize = state.originalTotalSize,
                outputSize = state.outputSize,
                operationLabel = "Merged",
                onShare = {
                    val intent = FileUtil.createShareIntent(context, state.outputFile)
                    context.startActivity(Intent.createChooser(intent, "Share merged PDF"))
                },
                onOpen = {
                    val intent = FileUtil.createOpenIntent(context, state.outputFile)
                    context.startActivity(intent)
                },
                onDone = {
                    viewModel.clearFiles()
                    onNavigateBack()
                }
            )
            return
        }
        else -> { /* Continue rendering main screen */ }
    }

    // Progress sheet
    if (uiState is MergeUiState.Processing) {
        FolioProgressSheet(
            fileName = "${selectedFiles.size} files",
            operationLabel = "Merging PDFs",
            progress = if (progress.isIndeterminate) null else progress.fraction,
            onCancel = null,
            onDismiss = {}
        )
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is MergeUiState.Error) {
            snackbarHostState.showSnackbar((uiState as MergeUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Merge PDFs",
                onBackClick = onNavigateBack,
                actions = {
                    if (selectedFiles.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearFiles() }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear all",
                                tint = FolioTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FolioTheme.colors.background,
        bottomBar = {
            MergeBottomBar(
                fileCount = selectedFiles.size,
                isProcessing = uiState is MergeUiState.Processing,
                onAddFiles = { filePicker.launch(PDF_MIME_TYPES) },
                onMerge = { viewModel.merge() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedFiles.isEmpty()) {
                // Empty state
                EmptyMergeState(
                    onPickFiles = { filePicker.launch(PDF_MIME_TYPES) }
                )
            } else {
                // File list
                MergeFileList(
                    files = selectedFiles,
                    onRemove = { viewModel.removeFile(it) },
                    onMoveUp = { index ->
                        if (index > 0) viewModel.reorderFiles(index, index - 1)
                    },
                    onMoveDown = { index ->
                        if (index < selectedFiles.size - 1) viewModel.reorderFiles(index, index + 1)
                    }
                )
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────

@Composable
private fun EmptyMergeState(onPickFiles: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon
        val infiniteTransition = rememberInfiniteTransition(label = "mergeIcon")
        val iconAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "iconAlpha"
        )

        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MergePastel
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MergeType,
                    contentDescription = null,
                    tint = MergeAccent.copy(alpha = iconAlpha),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Combine PDFs into one",
            style = MaterialTheme.typography.headlineSmall,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W600
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Select 2–15 PDF files to merge them\ninto a single document.",
            style = MaterialTheme.typography.bodyMedium,
            color = FolioTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        FolioButton(
            text = "Select PDFs",
            onClick = onPickFiles,
            accentColor = MergeAccent
        )
    }
}

// ─── File List ───────────────────────────────────────────────

@Composable
private fun MergeFileList(
    files: List<DocumentFile>,
    onRemove: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Info header
        item {
            Surface(
                shape = RoundedCornerShape(Radius.sm),
                color = MergePastel.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MergeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${files.size} file${if (files.size != 1) "s" else ""} selected · Use arrows to reorder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MergeAccent
                    )
                }
            }
        }

        // File items
        itemsIndexed(files, key = { index, file -> "${file.uri}_$index" }) { index, file ->
            MergeFileItem(
                file = file,
                position = index + 1,
                isFirst = index == 0,
                isLast = index == files.lastIndex,
                onRemove = { onRemove(index) },
                onMoveUp = { onMoveUp(index) },
                onMoveDown = { onMoveDown(index) }
            )
        }
    }
}

@Composable
private fun MergeFileItem(
    file: DocumentFile,
    position: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = FolioTheme.colors.cardSurface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Position badge
            Surface(
                shape = CircleShape,
                color = MergePastel,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography.labelLarge,
                        color = MergeAccent,
                        fontWeight = FontWeight.W700
                    )
                }
            }

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FolioTheme.colors.onSurface,
                    fontWeight = FontWeight.W500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.sizeFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = FolioTheme.colors.onSurfaceVariant
                )
            }

            // Reorder buttons
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (!isFirst) MergeAccent else FolioTheme.colors.divider,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (!isLast) MergeAccent else FolioTheme.colors.divider,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
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

// ─── Bottom Bar ──────────────────────────────────────────────

@Composable
private fun MergeBottomBar(
    fileCount: Int,
    isProcessing: Boolean,
    onAddFiles: () -> Unit,
    onMerge: () -> Unit
) {
    if (fileCount == 0) return

    Surface(
        shadowElevation = 8.dp,
        color = FolioTheme.colors.cardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add more button
            if (fileCount < 15) {
                FolioOutlinedButton(
                    text = "Add More",
                    onClick = onAddFiles,
                    modifier = Modifier.weight(1f),
                    accentColor = MergeAccent
                )
            }

            // Merge button
            FolioButton(
                text = if (isProcessing) "Merging…" else "Merge ($fileCount)",
                onClick = onMerge,
                modifier = Modifier.weight(1f),
                enabled = fileCount >= 2 && !isProcessing,
                accentColor = MergeAccent
            )
        }
    }
}
