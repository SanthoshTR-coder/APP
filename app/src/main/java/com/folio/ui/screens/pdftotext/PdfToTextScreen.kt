package com.folio.ui.screens.pdftotext

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToTextScreen(
    onNavigateBack: () -> Unit,
    viewModel: PdfToTextViewModel = hiltViewModel()
) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val extractedText by viewModel.extractedText.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val adsRemoved by viewModel.adsRemoved.collectAsState()

    val accentColor = FolioTheme.colors.convertAccent
    val pastelColor = FolioTheme.colors.convertPastel

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    var showSavedSnackbar by remember { mutableStateOf(false) }
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is PdfToTextUiState.FileSaved) {
            showSavedSnackbar = true
        }
    }

    Scaffold(
        topBar = { FolioTopBar(title = "PDF → Text", onBackClick = onNavigateBack) },
        bottomBar = {
            Column {
                if (!adsRemoved) AdBanner()
                when {
                    selectedFile != null && extractedText == null -> {
                        Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                            FolioButton(
                                text = "Extract Text",
                                onClick = { viewModel.extractText() },
                                accentColor = accentColor,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }
                    extractedText != null -> {
                        Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.copyAll()
                                        showCopiedSnackbar = true
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy All")
                                }
                                Button(
                                    onClick = { viewModel.saveAsFile() },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save as .txt")
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(remember { SnackbarHostState() }.also { host ->
                LaunchedEffect(showSavedSnackbar) {
                    if (showSavedSnackbar) {
                        host.showSnackbar("File saved successfully!")
                        showSavedSnackbar = false
                    }
                }
                LaunchedEffect(showCopiedSnackbar) {
                    if (showCopiedSnackbar) {
                        host.showSnackbar("Text copied to clipboard!")
                        showCopiedSnackbar = false
                    }
                }
            })
        }
    ) { padding ->
        when {
            selectedFile == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TextFields, null, Modifier.size(80.dp), tint = accentColor.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("Extract text from any PDF", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Text("Selectable text only • Copy or save as .txt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Spacer(Modifier.height(24.dp))
                        FolioButton(text = "Select PDF", onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, accentColor = accentColor)
                    }
                }
            }
            extractedText == null -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.4f))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = accentColor, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(selectedFile!!.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("${selectedFile!!.sizeFormatted} • ${selectedFile!!.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            IconButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) { Icon(Icons.Default.SwapHoriz, "Change") }
                        }
                    }
                }
            }
            else -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search in text…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Word count
                    val wordCount = extractedText!!.split("\\s+".toRegex()).count { it.isNotBlank() }
                    val charCount = extractedText!!.length
                    Text(
                        "$wordCount words • $charCount characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Text content
                    SelectionContainer {
                        val annotated = if (searchQuery.isNotBlank()) {
                            buildAnnotatedString {
                                val text = extractedText!!
                                val query = searchQuery
                                var startIdx = 0
                                val lowerText = text.lowercase()
                                val lowerQuery = query.lowercase()
                                while (true) {
                                    val found = lowerText.indexOf(lowerQuery, startIdx)
                                    if (found == -1) {
                                        append(text.substring(startIdx))
                                        break
                                    }
                                    append(text.substring(startIdx, found))
                                    withStyle(SpanStyle(background = pastelColor, fontWeight = FontWeight.Bold)) {
                                        append(text.substring(found, found + query.length))
                                    }
                                    startIdx = found + query.length
                                }
                            }
                        } else {
                            buildAnnotatedString { append(extractedText!!) }
                        }

                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (uiState is PdfToTextUiState.Processing) {
        FolioProgressSheet(fileName = selectedFile?.name ?: "", operationLabel = "Extracting text", progress = if (progress.isIndeterminate) null else progress.fraction)
    }
    if (uiState is PdfToTextUiState.Error) {
        AlertDialog(onDismissRequest = { viewModel.clearAll() }, confirmButton = { TextButton(onClick = { viewModel.clearAll() }) { Text("OK") } }, title = { Text("Error") }, text = { Text((uiState as PdfToTextUiState.Error).message) })
    }
}
