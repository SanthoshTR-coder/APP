package com.folio.ui.screens.compress

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.domain.usecase.pdf.CompressPdfUseCase.CompressionLevel
import com.folio.domain.usecase.pdf.CompressPdfUseCase.SizeEstimate
import com.folio.ui.components.*
import com.folio.ui.theme.*
import com.folio.util.FileUtil

/**
 * Compress PDF Screen
 *
 * Flow:
 * 1. Pick a PDF file
 * 2. Instantly see estimated sizes for 3 compression levels
 * 3. Select a level → tap Compress
 * 4. Progress sheet → Success screen
 *
 * Accent: CompressAccent (#2A9B72) / CompressPastel (#D4F0E8)
 */
@Composable
fun CompressScreen(
    onNavigateBack: () -> Unit,
    viewModel: CompressViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val sizeEstimates by viewModel.sizeEstimates.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val filePicker = rememberSingleFilePicker(
        mimeTypes = PDF_MIME_TYPES,
        onFilePicked = { uri -> viewModel.selectFile(uri) }
    )

    // Success screen
    when (val state = uiState) {
        is CompressUiState.Success -> {
            FolioSuccessScreen(
                outputFileName = state.outputFile.name,
                originalSize = state.originalSize,
                outputSize = state.outputSize,
                operationLabel = "Compressed",
                onShare = {
                    val intent = FileUtil.createShareIntent(context, state.outputFile)
                    context.startActivity(Intent.createChooser(intent, "Share compressed PDF"))
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
        else -> { /* render main screen */ }
    }

    // Progress sheet
    if (uiState is CompressUiState.Processing) {
        FolioProgressSheet(
            fileName = selectedFile?.name ?: "PDF",
            operationLabel = "Compressing",
            progress = if (progress.isIndeterminate) null else progress.fraction
        )
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is CompressUiState.Error) {
            snackbarHostState.showSnackbar((uiState as CompressUiState.Error).message)
        }
    }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Compress PDF",
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FolioTheme.colors.background,
        bottomBar = {
            if (selectedFile != null) {
                CompressBottomBar(
                    isProcessing = uiState is CompressUiState.Processing,
                    onCompress = { viewModel.compress() }
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
                EmptyCompressState(onPickFile = { filePicker.launch(PDF_MIME_TYPES) })
            } else {
                // Selected file card
                SelectedFileCard(
                    fileName = selectedFile!!.name,
                    fileSize = selectedFile!!.sizeFormatted,
                    onRemove = { viewModel.clearFile() }
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Compression level selector
                Text(
                    text = "Compression Level",
                    style = MaterialTheme.typography.titleMedium,
                    color = FolioTheme.colors.onSurface,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Level cards with size estimates
                sizeEstimates.forEach { estimate ->
                    CompressionLevelCard(
                        estimate = estimate,
                        isSelected = selectedLevel == estimate.level,
                        originalSize = selectedFile!!.size,
                        onSelect = { viewModel.selectLevel(estimate.level) }
                    )
                }
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────

@Composable
private fun EmptyCompressState(onPickFile: () -> Unit) {
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
            color = CompressPastel
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Compress,
                    contentDescription = null,
                    tint = CompressAccent,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Shrink PDF size",
            style = MaterialTheme.typography.headlineSmall,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W600
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Choose a PDF and pick a compression level.\nSee estimated sizes instantly.",
            style = MaterialTheme.typography.bodyMedium,
            color = FolioTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        FolioButton(
            text = "Select PDF",
            onClick = onPickFile,
            accentColor = CompressAccent
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
                color = CompressPastel,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = CompressAccent,
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

// ─── Compression Level Card ──────────────────────────────────

@Composable
private fun CompressionLevelCard(
    estimate: SizeEstimate,
    isSelected: Boolean,
    originalSize: Long,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CompressAccent else Color.Transparent,
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CompressPastel.copy(alpha = 0.4f)
        else FolioTheme.colors.cardSurface,
        label = "bg"
    )

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(Radius.md),
        color = bgColor,
        border = if (isSelected) BorderStroke(2.dp, CompressAccent)
        else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Radio-like indicator
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CompressAccent,
                    unselectedColor = FolioTheme.colors.onSurfaceVariant
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = estimate.level.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = FolioTheme.colors.onSurface,
                        fontWeight = FontWeight.W600
                    )
                    // Quality hint
                    val qualityText = when (estimate.level) {
                        CompressionLevel.LIGHT -> "High quality"
                        CompressionLevel.BALANCED -> "Good quality"
                        CompressionLevel.MAXIMUM -> "Smallest size"
                    }
                    Text(
                        text = "· $qualityText",
                        style = MaterialTheme.typography.bodySmall,
                        color = FolioTheme.colors.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = FileUtil.formatFileSize(estimate.estimatedSize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CompressAccent,
                        fontWeight = FontWeight.W600
                    )
                    Surface(
                        shape = RoundedCornerShape(Radius.pill),
                        color = CompressPastel
                    ) {
                        Text(
                            text = "-${estimate.reductionPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = CompressAccent,
                            fontWeight = FontWeight.W700,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Bottom Bar ──────────────────────────────────────────────

@Composable
private fun CompressBottomBar(
    isProcessing: Boolean,
    onCompress: () -> Unit
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
                text = if (isProcessing) "Compressing…" else "Compress",
                onClick = onCompress,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                accentColor = CompressAccent
            )
        }
    }
}
