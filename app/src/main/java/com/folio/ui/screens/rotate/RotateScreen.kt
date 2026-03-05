package com.folio.ui.screens.rotate

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.domain.usecase.pdf.RotatePdfUseCase.RotationAngle
import com.folio.ui.components.*
import com.folio.ui.theme.*
import com.folio.util.FileUtil

/**
 * Rotate PDF Screen
 *
 * Flow:
 * 1. Pick a PDF
 * 2. See 3-column thumbnail grid of all pages
 * 3. Tap to select pages (multi-select), or "Select All"
 * 4. Choose rotation (90° CW, 90° CCW, 180°)
 * 5. Tap Rotate → progress sheet → success
 *
 * Accent: RotateAccent (#D4612A) / RotatePastel (#FFE4D4)
 */
@Composable
fun RotateScreen(
    onNavigateBack: () -> Unit,
    viewModel: RotateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsState()
    val tempFile by viewModel.tempFile.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val selectedPages by viewModel.selectedPages.collectAsState()
    val rotationAngle by viewModel.rotationAngle.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val filePicker = rememberSingleFilePicker(
        mimeTypes = PDF_MIME_TYPES,
        onFilePicked = { uri -> viewModel.selectFile(uri) }
    )

    // Success screen
    when (val state = uiState) {
        is RotateUiState.Success -> {
            FolioSuccessScreen(
                outputFileName = state.outputFile.name,
                originalSize = state.originalSize,
                outputSize = state.outputSize,
                operationLabel = "Rotated",
                onShare = {
                    val intent = FileUtil.createShareIntent(context, state.outputFile)
                    context.startActivity(Intent.createChooser(intent, "Share rotated PDF"))
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
    if (uiState is RotateUiState.Processing) {
        FolioProgressSheet(
            fileName = selectedFile?.name ?: "PDF",
            operationLabel = "Rotating",
            progress = if (progress.isIndeterminate) null else progress.fraction
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is RotateUiState.Error) {
            snackbarHostState.showSnackbar((uiState as RotateUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Rotate Pages",
                onBackClick = onNavigateBack,
                actions = {
                    if (pageCount > 0) {
                        TextButton(onClick = {
                            if (selectedPages.size == pageCount) viewModel.clearSelection()
                            else viewModel.selectAllPages()
                        }) {
                            Text(
                                text = if (selectedPages.size == pageCount) "Deselect All" else "Select All",
                                color = RotateAccent
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
                RotateBottomBar(
                    selectedCount = selectedPages.size,
                    rotationAngle = rotationAngle,
                    onAngleChange = { viewModel.setRotationAngle(it) },
                    isProcessing = uiState is RotateUiState.Processing,
                    onRotate = { viewModel.rotate() }
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
                EmptyRotateState(onPickFile = { filePicker.launch(PDF_MIME_TYPES) })
            } else if (tempFile != null && pageCount > 0) {
                // Page thumbnail grid
                PageThumbnailGrid(
                    pdfFile = tempFile!!,
                    pageCount = pageCount,
                    selectedPages = selectedPages,
                    onTogglePage = { viewModel.togglePageSelection(it) }
                )
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────

@Composable
private fun EmptyRotateState(onPickFile: () -> Unit) {
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
            color = RotatePastel
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = null,
                    tint = RotateAccent,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Rotate PDF pages",
            style = MaterialTheme.typography.headlineSmall,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W600
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Select pages and rotate them\nclockwise, counter-clockwise, or 180°.",
            style = MaterialTheme.typography.bodyMedium,
            color = FolioTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        FolioButton(
            text = "Select PDF",
            onClick = onPickFile,
            accentColor = RotateAccent
        )
    }
}

// ─── Page Thumbnail Grid ─────────────────────────────────────

@Composable
private fun PageThumbnailGrid(
    pdfFile: java.io.File,
    pageCount: Int,
    selectedPages: Set<Int>,
    onTogglePage: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pageCount) { pageIndex ->
            val isSelected = selectedPages.contains(pageIndex)
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) RotateAccent else Color.Transparent,
                label = "pageBorder"
            )

            Box(
                modifier = Modifier
                    .aspectRatio(0.707f) // A4 ratio
                    .clip(RoundedCornerShape(Radius.sm))
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(Radius.sm)
                    )
                    .clickable { onTogglePage(pageIndex) }
            ) {
                PageThumbnail(
                    pdfFile = pdfFile,
                    pageIndex = pageIndex,
                    showPageNumber = true,
                    modifier = Modifier.fillMaxSize()
                )

                // Selection checkmark overlay
                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.xs)
                            .size(24.dp),
                        shape = CircleShape,
                        color = RotateAccent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Bottom Bar ──────────────────────────────────────────────

@Composable
private fun RotateBottomBar(
    selectedCount: Int,
    rotationAngle: RotationAngle,
    onAngleChange: (RotationAngle) -> Unit,
    isProcessing: Boolean,
    onRotate: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = FolioTheme.colors.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md)
                .navigationBarsPadding()
        ) {
            // Rotation angle selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                RotationAngle.entries.forEach { angle ->
                    val isActive = rotationAngle == angle
                    val bgColor by animateColorAsState(
                        targetValue = if (isActive) RotatePastel else FolioTheme.colors.surfaceVariant,
                        label = "angleBg"
                    )

                    Surface(
                        onClick = { onAngleChange(angle) },
                        shape = RoundedCornerShape(Radius.sm),
                        color = bgColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (angle) {
                                    RotationAngle.CW_90 -> Icons.Default.RotateRight
                                    RotationAngle.CCW_90 -> Icons.Default.RotateLeft
                                    RotationAngle.ROTATE_180 -> Icons.Default.SyncAlt
                                },
                                contentDescription = angle.label,
                                tint = if (isActive) RotateAccent else FolioTheme.colors.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = angle.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) RotateAccent else FolioTheme.colors.onSurfaceVariant,
                                fontWeight = if (isActive) FontWeight.W600 else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Rotate button
            FolioButton(
                text = when {
                    isProcessing -> "Rotating…"
                    selectedCount == 0 -> "Rotate All Pages"
                    else -> "Rotate $selectedCount Page${if (selectedCount != 1) "s" else ""}"
                },
                onClick = onRotate,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                accentColor = RotateAccent
            )
        }
    }
}
