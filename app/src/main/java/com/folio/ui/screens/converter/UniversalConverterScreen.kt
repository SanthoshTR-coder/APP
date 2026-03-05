package com.folio.ui.screens.converter

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalConverterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    viewModel: UniversalConverterViewModel = hiltViewModel()
) {
    val detectedFile by viewModel.detectedFile.collectAsState()
    val detectedRoute by viewModel.detectedRoute.collectAsState()
    val adsRemoved by viewModel.adsRemoved.collectAsState()

    val accentColor = FolioTheme.colors.convertAccent
    val pastelColor = FolioTheme.colors.convertPastel

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.detectFile(it) } }

    val allMimes = arrayOf(
        "application/pdf",
        "image/*",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel"
    )

    // Auto-route when non-PDF detected
    LaunchedEffect(detectedRoute) {
        detectedRoute?.let { route ->
            onNavigateToTool(route)
            viewModel.clear()
        }
    }

    Scaffold(
        topBar = { FolioTopBar(title = "Universal Converter", onBackClick = onNavigateBack) },
        bottomBar = {
            if (!adsRemoved) AdBanner()
        }
    ) { padding ->
        if (detectedFile == null) {
            // Empty state — drop zone
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SwapHoriz, null, Modifier.size(80.dp), tint = accentColor.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Drop any file to convert", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))
                    Text("PDF, Word, Excel, PPT, or Images", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.height(4.dp))
                    Text("We'll auto-detect the format and route you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.height(24.dp))
                    FolioButton(text = "Select File", onClick = { filePicker.launch(allMimes) }, accentColor = accentColor)
                }
            }
        } else if (detectedFile!!.mimeType == "application/pdf") {
            // PDF detected — show converter options
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File card
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.4f))) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = accentColor, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(detectedFile!!.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(detectedFile!!.formattedSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        IconButton(onClick = { filePicker.launch(allMimes) }) { Icon(Icons.Default.SwapHoriz, "Change") }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Convert to…", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                UniversalConverterViewModel.PDF_OUTPUT_ROUTES.forEach { (route, label) ->
                    val icon = when (route) {
                        "pdf_to_images" -> Icons.Default.Image
                        "pdf_to_word" -> Icons.Default.Description
                        "pdf_to_ppt" -> Icons.Default.Slideshow
                        "pdf_to_text" -> Icons.Default.TextFields
                        else -> Icons.Default.SwapHoriz
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            onNavigateToTool(route)
                            viewModel.clear()
                        }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}
