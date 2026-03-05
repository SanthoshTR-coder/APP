package com.folio.ui.screens.pdftoword

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToWordScreen(
    onNavigateBack: () -> Unit,
    viewModel: PdfToWordViewModel = hiltViewModel()
) {
    val selectedFile by viewModel.selectedFile.collectAsState()
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
        is PdfToWordUiState.Success -> {
            FolioSuccessScreen(
                outputFileName = state.outputFile.name,
                originalSize = state.originalSize,
                outputSize = state.outputSize,
                operationLabel = "PDF → Word",
                onShare = {
                    val intent = FileUtil.createShareIntent(context, state.outputFile)
                    context.startActivity(Intent.createChooser(intent, "Share"))
                },
                onOpen = {
                    val intent = FileUtil.createOpenIntent(context, state.outputFile)
                    context.startActivity(intent)
                },
                onDone = { viewModel.clearAll(); onNavigateBack() }
            )
        }
        else -> {
            Scaffold(
                topBar = { FolioTopBar(title = "PDF → Word", onBackClick = onNavigateBack) },
                bottomBar = {
                    Column {
                        if (!adsRemoved) AdBanner()
                        AnimatedVisibility(visible = selectedFile != null) {
                            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                                FolioButton(
                                    text = "Convert to Word",
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
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Description, null, Modifier.size(80.dp), tint = accentColor.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text("Convert PDF to editable Word", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(Modifier.height(8.dp))
                            Text("Text content is extracted and formatted as .docx", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(Modifier.height(24.dp))
                            FolioButton(text = "Select PDF", onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, accentColor = accentColor)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.4f))) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PictureAsPdf, null, tint = accentColor, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(selectedFile!!.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text("${selectedFile!!.formattedSize} • ${selectedFile!!.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) { Icon(Icons.Default.SwapHoriz, "Change") }
                            }
                        }
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Text content is preserved. Scanned PDFs (image-only) cannot be converted — use an OCR tool first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
            if (uiState is PdfToWordUiState.Processing) {
                FolioProgressSheet(fileName = selectedFile?.name ?: "", operationLabel = "Converting to Word", progress = if (progress.isIndeterminate) null else progress.fraction)
            }
            if (uiState is PdfToWordUiState.Error) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearAll() },
                    confirmButton = { TextButton(onClick = { viewModel.clearAll() }) { Text("OK") } },
                    title = { Text("Error") },
                    text = { Text((uiState as PdfToWordUiState.Error).message) }
                )
            }
        }
    }
}
