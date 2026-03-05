package com.folio.ui.screens.reorder

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.ui.components.*
import com.folio.ui.theme.*
import com.folio.util.FileUtil

/**
 * Reorder PDF Screen
 *
 * Flow:
 * 1. Pick a PDF
 * 2. See page thumbnails in a grid
 * 3. Use up/down arrows to reorder pages
 * 4. Tap "Reorder" → progress → success
 *
 * Accent: ReorderAccent (#3B82C4) / ReorderPastel (#D4E8FF)
 */
@Composable
fun ReorderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReorderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsState()
    val tempFile by viewModel.tempFile.collectAsState()
    val pageOrder by viewModel.pageOrder.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val filePicker = rememberSingleFilePicker(
        mimeTypes = PDF_MIME_TYPES,
        onFilePicked = { uri -> viewModel.selectFile(uri) }
    )

    // Success screen
    when (val state = uiState) {
        is ReorderUiState.Success -> {
            FolioSuccessScreen(
                outputFileName = state.outputFile.name,
                originalSize = state.originalSize,
                outputSize = state.outputSize,
                operationLabel = "Reordered",
                onShare = {
                    val intent = FileUtil.createShareIntent(context, state.outputFile)
                    context.startActivity(Intent.createChooser(intent, "Share reordered PDF"))
                },
                onOpen = {
                    val intent = FileUtil.createOpenIntent(context, state.outputFile)
                    context.startActivity(intent)
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
    if (uiState is ReorderUiState.Processing) {
        FolioProgressSheet(
            fileName = selectedFile?.name ?: "PDF",
            operationLabel = "Reordering",
            progress = if (progress.isIndeterminate) null else progress.fraction
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is ReorderUiState.Error) {
            snackbarHostState.showSnackbar((uiState as ReorderUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Reorder Pages",
                onBackClick = onNavigateBack,
                actions = {
                    if (pageOrder.isNotEmpty()) {
                        IconButton(onClick = { viewModel.resetOrder() }) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset order",
                                tint = ReorderAccent
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FolioTheme.colors.background,
        bottomBar = {
            if (selectedFile != null) {
                ReorderBottomBar(
                    isProcessing = uiState is ReorderUiState.Processing,
                    onReorder = { viewModel.reorder() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedFile == null) {
                EmptyReorderState(onPickFile = { filePicker.launch(PDF_MIME_TYPES) })
            } else if (tempFile != null && pageOrder.isNotEmpty()) {
                // Info bar
                Surface(
                    shape = RoundedCornerShape(Radius.sm),
                    color = ReorderPastel.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ReorderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${pageOrder.size} pages · Use arrows to reorder",
                            style = MaterialTheme.typography.bodySmall,
                            color = ReorderAccent
                        )
                    }
                }

                // Page grid with reorder controls
                ReorderPageGrid(
                    pdfFile = tempFile!!,
                    pageOrder = pageOrder,
                    onMoveUp = { index ->
                        if (index > 0) viewModel.movePage(index, index - 1)
                    },
                    onMoveDown = { index ->
                        if (index < pageOrder.size - 1) viewModel.movePage(index, index + 1)
                    }
                )
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────

@Composable
private fun EmptyReorderState(onPickFile: () -> Unit) {
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
            color = ReorderPastel
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = ReorderAccent,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Reorder PDF pages",
            style = MaterialTheme.typography.headlineSmall,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W600
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Rearrange pages in any order you like.\nSee thumbnails and move pages freely.",
            style = MaterialTheme.typography.bodyMedium,
            color = FolioTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        FolioButton(
            text = "Select PDF",
            onClick = onPickFile,
            accentColor = ReorderAccent
        )
    }
}

// ─── Page Grid ───────────────────────────────────────────────

@Composable
private fun ReorderPageGrid(
    pdfFile: java.io.File,
    pageOrder: List<Int>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(pageOrder, key = { index, pageIdx -> "${pageIdx}_$index" }) { index, originalPageIndex ->
            ReorderPageItem(
                pdfFile = pdfFile,
                originalPageIndex = originalPageIndex,
                currentPosition = index + 1,
                isFirst = index == 0,
                isLast = index == pageOrder.lastIndex,
                onMoveUp = { onMoveUp(index) },
                onMoveDown = { onMoveDown(index) }
            )
        }
    }
}

@Composable
private fun ReorderPageItem(
    pdfFile: java.io.File,
    originalPageIndex: Int,
    currentPosition: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = FolioTheme.colors.cardSurface,
        shadowElevation = 1.dp
    ) {
        Column {
            // Thumbnail
            PageThumbnail(
                pdfFile = pdfFile,
                pageIndex = originalPageIndex,
                showPageNumber = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f)
            )

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Position badge
                Surface(
                    shape = CircleShape,
                    color = ReorderPastel,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$currentPosition",
                            style = MaterialTheme.typography.labelSmall,
                            color = ReorderAccent,
                            fontWeight = FontWeight.W700
                        )
                    }
                }

                // Original page label
                Text(
                    text = "Page ${originalPageIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = FolioTheme.colors.onSurfaceVariant
                )

                // Reorder arrows
                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move up",
                            tint = if (!isFirst) ReorderAccent else FolioTheme.colors.divider,
                            modifier = Modifier.size(18.dp)
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
                            tint = if (!isLast) ReorderAccent else FolioTheme.colors.divider,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Bottom Bar ──────────────────────────────────────────────

@Composable
private fun ReorderBottomBar(
    isProcessing: Boolean,
    onReorder: () -> Unit
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
                text = if (isProcessing) "Reordering…" else "Save New Order",
                onClick = onReorder,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                accentColor = ReorderAccent
            )
        }
    }
}
