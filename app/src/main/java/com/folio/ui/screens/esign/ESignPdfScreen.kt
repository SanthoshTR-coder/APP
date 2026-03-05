package com.folio.ui.screens.esign

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ESignPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: ESignViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val signatureMode by viewModel.signatureMode.collectAsStateWithLifecycle()
    val typedName by viewModel.typedName.collectAsStateWithLifecycle()
    val selectedFontIndex by viewModel.selectedFontIndex.collectAsStateWithLifecycle()
    val signatureBitmap by viewModel.signatureBitmap.collectAsStateWithLifecycle()
    val signatureImageUri by viewModel.signatureImageUri.collectAsStateWithLifecycle()
    val targetPage by viewModel.targetPage.collectAsStateWithLifecycle()
    val addDateStamp by viewModel.addDateStamp.collectAsStateWithLifecycle()
    val drawingPaths by viewModel.drawingPaths.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()

    val accentColor = FolioTheme.colors.signAccent
    val pastelColor = FolioTheme.colors.signPastel

    val signatureFonts = listOf(
        FontFamily.Cursive,
        FontFamily.Serif,
        FontFamily.SansSerif,
        FontFamily.Monospace,
        FontFamily.Default
    )
    val fontNames = listOf("Script", "Serif", "Sans-Serif", "Mono", "Default")

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.setSignatureImageUri(it) } }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "E-Sign PDF",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ESignUiState.Success -> {
                FolioSuccessScreen(
                    outputFileName = state.outputFile.name,
                    originalSize = state.originalSize,
                    outputSize = state.outputSize,
                    operationLabel = "Signature Applied",
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
                            label = "Select PDF to sign",
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
                                Icon(Icons.Default.Draw, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
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

                    // Signature Options
                    AnimatedVisibility(visible = selectedFile != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Mode selector
                            Text("Signature Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SignatureMode.entries.forEach { mode ->
                                    val (icon, label) = when (mode) {
                                        SignatureMode.DRAW -> Icons.Default.Draw to "Draw"
                                        SignatureMode.TYPE -> Icons.Default.TextFields to "Type"
                                        SignatureMode.IMAGE -> Icons.Default.Image to "Image"
                                    }
                                    FilterChip(
                                        selected = signatureMode == mode,
                                        onClick = { viewModel.setSignatureMode(mode) },
                                        label = { Text(label) },
                                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                            selectedLabelColor = accentColor
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Mode-specific content
                            when (signatureMode) {
                                SignatureMode.DRAW -> {
                                    DrawSignaturePanel(
                                        paths = drawingPaths,
                                        currentPath = currentPath,
                                        onDrawStart = { viewModel.onDrawStart(it) },
                                        onDrawMove = { viewModel.onDrawMove(it) },
                                        onDrawEnd = { viewModel.onDrawEnd() },
                                        onClear = { viewModel.clearDrawing() },
                                        onCapture = { bitmap -> viewModel.setSignatureBitmap(bitmap) },
                                        accentColor = accentColor
                                    )
                                }
                                SignatureMode.TYPE -> {
                                    TypeSignaturePanel(
                                        typedName = typedName,
                                        onNameChange = { viewModel.setTypedName(it) },
                                        selectedFontIndex = selectedFontIndex,
                                        onFontSelect = { viewModel.setSelectedFontIndex(it) },
                                        fonts = signatureFonts,
                                        fontNames = fontNames,
                                        onCapture = { bitmap -> viewModel.setSignatureBitmap(bitmap) },
                                        accentColor = accentColor
                                    )
                                }
                                SignatureMode.IMAGE -> {
                                    ImageSignaturePanel(
                                        imageUri = signatureImageUri,
                                        onPickImage = { imagePicker.launch(arrayOf("image/png", "image/jpeg")) },
                                        onRemove = { viewModel.setSignatureImageUri(null) },
                                        accentColor = accentColor
                                    )
                                }
                            }

                            // Page selector
                            OutlinedTextField(
                                value = targetPage.toString(),
                                onValueChange = { it.toIntOrNull()?.let { p -> viewModel.setTargetPage(p.coerceAtLeast(1)) } },
                                label = { Text("Place on Page") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor)
                            )

                            // Date stamp toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                    Text("Add Date Stamp", style = MaterialTheme.typography.bodyLarge)
                                }
                                Switch(
                                    checked = addDateStamp,
                                    onCheckedChange = { viewModel.setAddDateStamp(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                                )
                            }

                            // Error state
                            if (uiState is ESignUiState.Error) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Text((uiState as ESignUiState.Error).message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }

                            // Apply button
                            FolioButton(
                                text = if (uiState is ESignUiState.Processing) "Signing..." else "Apply Signature",
                                onClick = { viewModel.applySignature() },
                                enabled = signatureBitmap != null && uiState !is ESignUiState.Processing,
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

                if (uiState is ESignUiState.Processing) {
                    FolioProgressSheet(
                        fileName = selectedFile?.name ?: "",
                        operationLabel = "Applying Signature",
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
private fun DrawSignaturePanel(
    paths: List<List<Offset>>,
    currentPath: List<Offset>,
    onDrawStart: (Offset) -> Unit,
    onDrawMove: (Offset) -> Unit,
    onDrawEnd: () -> Unit,
    onClear: () -> Unit,
    onCapture: (Bitmap) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Draw your signature below", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> onDrawStart(offset) },
                            onDrag = { change, _ ->
                                change.consume()
                                onDrawMove(change.position)
                            },
                            onDragEnd = { onDrawEnd() }
                        )
                    }
            ) {
                // Draw completed paths
                paths.forEach { pathPoints ->
                    if (pathPoints.size > 1) {
                        val path = Path().apply {
                            moveTo(pathPoints.first().x, pathPoints.first().y)
                            for (i in 1 until pathPoints.size) {
                                val prev = pathPoints[i - 1]
                                val curr = pathPoints[i]
                                quadraticBezierTo(prev.x, prev.y, (prev.x + curr.x) / 2, (prev.y + curr.y) / 2)
                            }
                        }
                        drawPath(path, Color(0xFF1A1A2E), style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                // Draw current path
                if (currentPath.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPath.first().x, currentPath.first().y)
                        for (i in 1 until currentPath.size) {
                            val prev = currentPath[i - 1]
                            val curr = currentPath[i]
                            quadraticBezierTo(prev.x, prev.y, (prev.x + curr.x) / 2, (prev.y + curr.y) / 2)
                        }
                    }
                    drawPath(path, Color(0xFF1A1A2E), style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // Draw baseline
                drawLine(
                    Color.LightGray,
                    Offset(20f, size.height * 0.75f),
                    Offset(size.width - 20f, size.height * 0.75f),
                    strokeWidth = 1f
                )
            }

            // Placeholder text
            if (paths.isEmpty() && currentPath.isEmpty()) {
                Text(
                    "Sign here",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }
            Button(
                onClick = {
                    // Create bitmap from paths
                    val bitmap = Bitmap.createBitmap(400, 160, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#1A1A2E")
                        strokeWidth = 3f
                        style = android.graphics.Paint.Style.STROKE
                        isAntiAlias = true
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                    paths.forEach { pathPoints ->
                        if (pathPoints.size > 1) {
                            val p = android.graphics.Path()
                            p.moveTo(pathPoints.first().x, pathPoints.first().y)
                            for (i in 1 until pathPoints.size) {
                                val prev = pathPoints[i - 1]
                                val curr = pathPoints[i]
                                p.quadTo(prev.x, prev.y, (prev.x + curr.x) / 2, (prev.y + curr.y) / 2)
                            }
                            canvas.drawPath(p, paint)
                        }
                    }
                    onCapture(bitmap)
                },
                enabled = paths.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Confirm")
            }
        }
    }
}

@Composable
private fun TypeSignaturePanel(
    typedName: String,
    onNameChange: (String) -> Unit,
    selectedFontIndex: Int,
    onFontSelect: (Int) -> Unit,
    fonts: List<FontFamily>,
    fontNames: List<String>,
    onCapture: (Bitmap) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = typedName,
            onValueChange = onNameChange,
            label = { Text("Your Name") },
            placeholder = { Text("Enter your full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, cursorColor = accentColor)
        )

        Text("Font Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fontNames.forEachIndexed { index, name ->
                val isSelected = index == selectedFontIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { onFontSelect(index) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = typedName.ifEmpty { "Your Name" },
                                style = TextStyle(fontFamily = fonts[index], fontSize = 24.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentColor)
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (typedName.isNotBlank()) {
                    // Render typed signature as bitmap
                    val bitmap = Bitmap.createBitmap(400, 80, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#1A1A2E")
                        textSize = 40f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    canvas.drawText(typedName, 200f, 55f, paint)
                    onCapture(bitmap)
                }
            },
            enabled = typedName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Confirm Signature")
        }
    }
}

@Composable
private fun ImageSignaturePanel(
    imageUri: Uri?,
    onPickImage: () -> Unit,
    onRemove: () -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Upload a signature image (transparent PNG recommended)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (imageUri == null) {
            OutlinedButton(
                onClick = onPickImage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(32.dp), tint = accentColor)
                    Spacer(Modifier.height(8.dp))
                    Text("Upload Signature Image")
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentColor)
                    Text("Signature image selected", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}
