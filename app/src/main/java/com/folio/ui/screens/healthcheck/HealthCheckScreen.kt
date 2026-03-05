package com.folio.ui.screens.healthcheck

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.domain.usecase.smart.HealthCheckUseCase.HealthResult
import com.folio.domain.usecase.smart.HealthCheckUseCase.Severity
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthCheckScreen(
    onNavigateBack: () -> Unit,
    viewModel: HealthCheckViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()

    val accentColor = FolioTheme.colors.healthAccent
    val pastelColor = FolioTheme.colors.healthPastel

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Health Check",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
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
                    label = "Select PDF to analyze",
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
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
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

            // Scanning indicator
            if (uiState is HealthCheckUiState.Scanning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = accentColor)
                        Text("Analyzing PDF...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Scan button (only show when idle with file)
            if (selectedFile != null && uiState is HealthCheckUiState.Idle) {
                FolioButton(
                    text = "Run Health Check",
                    onClick = { viewModel.runHealthCheck() },
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Results
            if (uiState is HealthCheckUiState.Result) {
                val result = (uiState as HealthCheckUiState.Result).healthResult
                HealthReportCard(result = result, accentColor = accentColor, pastelColor = pastelColor)

                // Share report button
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(viewModel.generateReportText()))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Report")
                    }
                    Button(
                        onClick = { viewModel.clearAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scan Another")
                    }
                }
            }

            // Error
            if (uiState is HealthCheckUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text((uiState as HealthCheckUiState.Error).message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            if (!adsRemoved) {
                Spacer(modifier = Modifier.weight(1f))
                AdBanner(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HealthReportCard(
    result: HealthResult,
    accentColor: Color,
    pastelColor: Color
) {
    val issueCount = result.issues.count { it.severity == Severity.ERROR }
    val warningCount = result.issues.count { it.severity == Severity.WARNING } + result.warnings.size
    val overallColor = when {
        issueCount > 0 -> MaterialTheme.colorScheme.error
        warningCount > 0 -> Color(0xFFF9A825)
        else -> Color(0xFF2E7D32)
    }
    val overallIcon = when {
        issueCount > 0 -> Icons.Default.Cancel
        warningCount > 0 -> Icons.Default.Warning
        else -> Icons.Default.CheckCircle
    }
    val overallLabel = when {
        issueCount > 0 -> "Issues Found"
        warningCount > 0 -> "Warnings"
        else -> "Healthy"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Overall status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(overallIcon, contentDescription = null, tint = overallColor, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
                Text(overallLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = overallColor)
            }

            HorizontalDivider()

            // Basic info grid
            ReportRow("File Name", result.fileName)
            ReportRow("File Size", formatBytes(result.fileSize))
            ReportRow("Pages", result.pageCount.toString())
            ReportRow("PDF Version", result.pdfVersion)
            ReportRow("Encrypted", if (result.isEncrypted) "Yes 🔒" else "No")
            ReportRow("Content Type", result.contentType.name)
            ReportRow("Has Text", if (result.hasText) "Yes" else "No")
            ReportRow("Has Images", if (result.hasImages) "Yes" else "No")

            if (result.embeddedFonts.isNotEmpty()) {
                HorizontalDivider()
                Text("Embedded Fonts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                result.embeddedFonts.forEach { font ->
                    Text("• $font", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Issues
            if (result.issues.isNotEmpty()) {
                HorizontalDivider()
                Text("Issues", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                result.issues.forEach { issue ->
                    val (icon, color) = when (issue.severity) {
                        Severity.ERROR -> Icons.Default.Cancel to MaterialTheme.colorScheme.error
                        Severity.WARNING -> Icons.Default.Warning to Color(0xFFF9A825)
                        Severity.OK -> Icons.Default.CheckCircle to Color(0xFF2E7D32)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                        Text(issue.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Warnings
            if (result.warnings.isNotEmpty()) {
                HorizontalDivider()
                Text("Warnings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                result.warnings.forEach { warning ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF9A825), modifier = Modifier.size(18.dp))
                        Text(warning, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
