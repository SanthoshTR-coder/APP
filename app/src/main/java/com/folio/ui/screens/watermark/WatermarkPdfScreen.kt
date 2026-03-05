package com.folio.ui.screens.watermark

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: WatermarkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val watermarkText by viewModel.watermarkText.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val opacity by viewModel.opacity.collectAsStateWithLifecycle()
    val angleDegrees by viewModel.angleDegrees.collectAsStateWithLifecycle()
    val selectedColorHex by viewModel.selectedColorHex.collectAsStateWithLifecycle()
    val isImageWatermark by viewModel.isImageWatermark.collectAsStateWithLifecycle()
    val watermarkImageUri by viewModel.watermarkImageUri.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()

    val accentColor = FolioTheme.colors.editAccent
    val pastelColor = FolioTheme.colors.editPastel

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.setWatermarkImageUri(it) } }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Watermark PDF",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is WatermarkUiState.Success -> {
                FolioSuccessScreen(
                    outputFileName = state.outputFile.name,
                    originalSize = state.originalSize,
                    outputSize = state.outputSize,
                    operationLabel = "Watermark Applied",
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
                            label = "Select PDF to watermark",
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
                                Icon(Icons.Default.BrandingWatermark, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
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

                    // Watermark Options
                    AnimatedVisibility(visible = selectedFile != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Toggle: Text vs Image
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !isImageWatermark,
                                    onClick = { viewModel.setIsImageWatermark(false) },
                                    label = { Text("Text Watermark") },
                                    leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor.copy(alpha = 0.2f)),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = isImageWatermark,
                                    onClick = { viewModel.setIsImageWatermark(true) },
                                    label = { Text("Image Watermark") },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor.copy(alpha = 0.2f)),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!isImageWatermark) {
                                // Text input
                                OutlinedTextField(
                                    value = watermarkText,
                                    onValueChange = { viewModel.setWatermarkText(it) },
                                    label = { Text("Watermark Text") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor)
                                )

                                // Font Size slider
                                SliderOption(
                                    label = "Font Size",
                                    value = fontSize,
                                    valueRange = 12f..120f,
                                    valueLabel = "${fontSize.toInt()}pt",
                                    onValueChange = { viewModel.setFontSize(it) },
                                    accentColor = accentColor
                                )

                                // Color picker
                                Text("Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    viewModel.presetColors.forEach { hex ->
                                        val color = Color(android.graphics.Color.parseColor(hex))
                                        val isSelected = hex == selectedColorHex
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .then(
                                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                    else Modifier
                                                )
                                                .clickable { viewModel.setColorHex(hex) }
                                        )
                                    }
                                }
                            } else {
                                // Image picker
                                if (watermarkImageUri == null) {
                                    OutlinedButton(
                                        onClick = { imagePicker.launch(arrayOf("image/png", "image/jpeg")) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Select Watermark Image")
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentColor)
                                            Text("Image selected", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { viewModel.setWatermarkImageUri(null) }) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove image")
                                            }
                                        }
                                    }
                                }
                            }

                            // Common sliders
                            SliderOption(
                                label = "Opacity",
                                value = opacity,
                                valueRange = 0.1f..0.9f,
                                valueLabel = "${(opacity * 100).toInt()}%",
                                onValueChange = { viewModel.setOpacity(it) },
                                accentColor = accentColor
                            )

                            SliderOption(
                                label = "Angle",
                                value = angleDegrees,
                                valueRange = -90f..90f,
                                valueLabel = "${angleDegrees.toInt()}°",
                                onValueChange = { viewModel.setAngleDegrees(it) },
                                accentColor = accentColor
                            )

                            // Live Preview
                            Text("Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            WatermarkPreview(
                                text = watermarkText,
                                fontSize = fontSize,
                                opacity = opacity,
                                angleDegrees = angleDegrees,
                                colorHex = selectedColorHex,
                                isImage = isImageWatermark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )

                            // Error state
                            if (uiState is WatermarkUiState.Error) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Text((uiState as WatermarkUiState.Error).message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }

                            // Apply button
                            val canApply = if (isImageWatermark) watermarkImageUri != null else watermarkText.isNotBlank()
                            FolioButton(
                                text = if (uiState is WatermarkUiState.Processing) "Applying..." else "Apply Watermark",
                                onClick = { viewModel.applyWatermark() },
                                enabled = canApply && uiState !is WatermarkUiState.Processing,
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

                if (uiState is WatermarkUiState.Processing) {
                    FolioProgressSheet(
                        fileName = selectedFile?.name ?: "",
                        operationLabel = "Applying Watermark",
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
private fun SliderOption(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = accentColor, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = accentColor.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun WatermarkPreview(
    text: String,
    fontSize: Float,
    opacity: Float,
    angleDegrees: Float,
    colorHex: String,
    isImage: Boolean,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val watermarkColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Red
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isImage) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Image watermark preview\nnot available",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw page lines to simulate document
                val lineColor = surfaceColor.copy(alpha = 0.3f)
                for (i in 0..8) {
                    val y = 30f + (i * 20f)
                    drawLine(lineColor, Offset(20f, y), Offset(size.width - 20f, y), strokeWidth = 2f)
                }

                // Draw watermark text
                rotate(degrees = angleDegrees) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(colorHex)
                            alpha = (opacity * 255).toInt()
                            textSize = fontSize * 0.6f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText(text, size.width / 2, size.height / 2, paint)
                    }
                }
            }
        }
    }
}
