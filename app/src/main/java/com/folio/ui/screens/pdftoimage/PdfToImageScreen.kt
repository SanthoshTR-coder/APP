package com.folio.ui.screens.pdftoimage

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.domain.usecase.convert.PdfToImageUseCase.OutputFormat
import com.folio.domain.usecase.convert.PdfToImageUseCase.Quality
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImageScreen(
    onNavigateBack: () -> Unit,
    viewModel: PdfToImageViewModel = hiltViewModel()
) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val format by viewModel.format.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val adsRemoved by viewModel.adsRemoved.collectAsState()

    val accentColor = FolioTheme.colors.convertAccent
    val pastelColor = FolioTheme.colors.convertPastel

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    val context = LocalContext.current

    when (val state = uiState) {
        is PdfToImageUiState.Success -> {
            FolioSuccessScreen(
                outputFileName = "${state.outputFiles.size} ${format.name} files",
                originalSize = state.originalSize,
                outputSize = state.totalSize,
                operationLabel = "PDF → Images",
                onShare = {
                    state.outputFiles.firstOrNull()?.let { file ->
                        val intent = FileUtil.createShareIntent(context, file)
                        context.startActivity(Intent.createChooser(intent, "Share"))
                    }
                },
                onOpen = {
                    state.outputFiles.firstOrNull()?.let { file ->
                        val intent = FileUtil.createOpenIntent(context, file)
                        context.startActivity(intent)
                    }
                },
                onDone = {
                    viewModel.clearAll()
                    onNavigateBack()
                }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    FolioTopBar(
                        title = "PDF → Images",
                        onBackClick = onNavigateBack
                    )
                },
                bottomBar = {
                    Column {
                        if (!adsRemoved) AdBanner()
                        AnimatedVisibility(visible = selectedFile != null) {
                            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                                FolioButton(
                                    text = "Extract Images",
                                    onClick = { viewModel.convert() },
                                    accentColor = accentColor,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                if (selectedFile == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.BrokenImage, null,
                                modifier = Modifier.size(80.dp),
                                tint = accentColor.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Convert PDF pages to images", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(Modifier.height(8.dp))
                            Text("Each page becomes a separate image file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(Modifier.height(24.dp))
                            FolioButton(text = "Select PDF", onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, accentColor = accentColor)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Selected file card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null, tint = accentColor, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedFile!!.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text("${selectedFile!!.formattedSize} • ${selectedFile!!.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) {
                                    Icon(Icons.Default.SwapHoriz, "Change file")
                                }
                            }
                        }

                        // Format selector
                        Text("Output Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutputFormat.entries.forEach { f ->
                                FilterChip(
                                    selected = format == f,
                                    onClick = { viewModel.setFormat(f) },
                                    label = { Text(f.name) },
                                    leadingIcon = if (format == f) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pastelColor, selectedLabelColor = accentColor)
                                )
                            }
                        }

                        // Quality selector
                        Text("Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Quality.entries.forEach { q ->
                                val dpiLabel = when (q) {
                                    Quality.SCREEN -> "72 DPI • Smaller files"
                                    Quality.STANDARD -> "150 DPI • Balanced"
                                    Quality.PRINT -> "300 DPI • Best quality"
                                }
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (quality == q) pastelColor else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setQuality(q) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = quality == q, onClick = { viewModel.setQuality(q) }, colors = RadioButtonDefaults.colors(selectedColor = accentColor))
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(q.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            Text(dpiLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState is PdfToImageUiState.Processing) {
                FolioProgressSheet(fileName = selectedFile?.name ?: "", operationLabel = "Extracting images", progress = if (progress.isIndeterminate) null else progress.fraction)
            }
            if (uiState is PdfToImageUiState.Error) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearAll() },
                    confirmButton = { TextButton(onClick = { viewModel.clearAll() }) { Text("OK") } },
                    title = { Text("Error") },
                    text = { Text((uiState as PdfToImageUiState.Error).message) }
                )
            }
        }
    }
}
