package com.folio.ui.screens.exceltopdf

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelToPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExcelToPdfViewModel = hiltViewModel()
) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val sheetNames by viewModel.sheetNames.collectAsState()
    val selectedSheets by viewModel.selectedSheets.collectAsState()
    val showGridLines by viewModel.showGridLines.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val adsRemoved by viewModel.adsRemoved.collectAsState()

    val accentColor = FolioTheme.colors.convertAccent
    val pastelColor = FolioTheme.colors.convertPastel

    val xlsPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    val xlsMimes = arrayOf(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel"
    )

    val context = LocalContext.current

    when (val state = uiState) {
        is ExcelUiState.Success -> {
            FolioSuccessScreen(
                outputFileName = state.outputFile.name,
                originalSize = state.originalSize,
                outputSize = state.outputSize,
                operationLabel = "Excel → PDF",
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
                topBar = { FolioTopBar(title = "Excel → PDF", onBackClick = onNavigateBack) },
                bottomBar = {
                    Column {
                        if (!adsRemoved) AdBanner()
                        AnimatedVisibility(visible = selectedFile != null && selectedSheets.isNotEmpty()) {
                            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                                FolioButton(
                                    text = "Convert to PDF (${selectedSheets.size} sheet${if (selectedSheets.size != 1) "s" else ""})",
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
                            Icon(Icons.Default.TableChart, null, Modifier.size(80.dp), tint = accentColor.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text("Convert Excel spreadsheets to PDF", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(Modifier.height(8.dp))
                            Text("Supports .xlsx files • Select sheets to include", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(Modifier.height(24.dp))
                            FolioButton(text = "Select Excel File", onClick = { xlsPicker.launch(xlsMimes) }, accentColor = accentColor)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // File card
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.4f))) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TableChart, null, tint = accentColor, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(selectedFile!!.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text("${selectedFile!!.formattedSize} • ${sheetNames.size} sheet${if (sheetNames.size != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { xlsPicker.launch(xlsMimes) }) { Icon(Icons.Default.SwapHoriz, "Change") }
                            }
                        }

                        // Sheet selection
                        if (sheetNames.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sheets to Include", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Row {
                                    TextButton(onClick = { viewModel.selectAllSheets() }) { Text("All") }
                                    TextButton(onClick = { viewModel.deselectAllSheets() }) { Text("None") }
                                }
                            }
                            sheetNames.forEachIndexed { index, name ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedSheets.contains(index)) pastelColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleSheet(index) }
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedSheets.contains(index),
                                            onCheckedChange = { viewModel.toggleSheet(index) },
                                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(name, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        // Grid lines toggle
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Show Grid Lines", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("Draw cell borders in the PDF", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Switch(
                                    checked = showGridLines,
                                    onCheckedChange = { viewModel.setShowGridLines(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                                )
                            }
                        }
                    }
                }
            }
            if (uiState is ExcelUiState.Processing) {
                FolioProgressSheet(fileName = selectedFile?.name ?: "", operationLabel = "Converting to PDF", progress = if (progress.isIndeterminate) null else progress.fraction)
            }
            if (uiState is ExcelUiState.Error) {
                AlertDialog(onDismissRequest = { viewModel.clearAll() }, confirmButton = { TextButton(onClick = { viewModel.clearAll() }) { Text("OK") } }, title = { Text("Error") }, text = { Text((uiState as ExcelUiState.Error).message) })
            }
        }
    }
}
