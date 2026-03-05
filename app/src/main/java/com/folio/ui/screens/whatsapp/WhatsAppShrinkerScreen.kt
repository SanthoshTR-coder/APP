package com.folio.ui.screens.whatsapp

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.domain.usecase.smart.SizeTarget
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppShrinkerScreen(
    onNavigateBack: () -> Unit,
    viewModel: WhatsAppShrinkerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val selectedTarget by viewModel.selectedTarget.collectAsStateWithLifecycle()
    val customTargetMb by viewModel.customTargetMb.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()

    val accentColor = FolioTheme.colors.whatsAppAccent
    val pastelColor = FolioTheme.colors.whatsAppPastel

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "WhatsApp Shrinker",
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
                    label = "Select PDF to shrink",
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
                        Icon(Icons.Default.Compress, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
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

            // Target selector
            AnimatedVisibility(visible = selectedFile != null && uiState !is WhatsAppUiState.Success) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Size Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    val targets = listOf(
                        Triple(SizeTarget.WHATSAPP_OPTIMAL, "WhatsApp Optimal", "Under 16 MB — fastest send"),
                        Triple(SizeTarget.WHATSAPP_MAX, "WhatsApp Max", "Under 100 MB — file sharing limit"),
                        Triple(SizeTarget.EMAIL, "Email Friendly", "Under 10 MB — standard email limit"),
                        Triple(SizeTarget.GMAIL, "Gmail", "Under 25 MB — Gmail attachment limit"),
                        Triple(SizeTarget.CUSTOM, "Custom", "Set your own size target")
                    )

                    targets.forEach { (target, title, desc) ->
                        val isSelected = selectedTarget == target
                        Card(
                            onClick = { viewModel.setTarget(target) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(accentColor)
                            ) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setTarget(target) },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (target == SizeTarget.WHATSAPP_OPTIMAL || target == SizeTarget.WHATSAPP_MAX) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Custom MB slider
                    AnimatedVisibility(visible = selectedTarget == SizeTarget.CUSTOM) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Target Size", style = MaterialTheme.typography.bodyMedium)
                                Text("${customTargetMb.toInt()} MB", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Slider(
                                value = customTargetMb,
                                onValueChange = { viewModel.setCustomTargetMb(it) },
                                valueRange = 1f..200f,
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                            )
                        }
                    }

                    // Error state
                    if (uiState is WhatsAppUiState.Error) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text((uiState as WhatsAppUiState.Error).message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }

                    // Shrink button
                    FolioButton(
                        text = if (uiState is WhatsAppUiState.Processing) "Shrinking..." else "Shrink PDF",
                        onClick = { viewModel.shrink() },
                        enabled = uiState !is WhatsAppUiState.Processing,
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Success state
            if (uiState is WhatsAppUiState.Success) {
                val result = (uiState as WhatsAppUiState.Success).result

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (result.targetReached) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (result.targetReached) accentColor else Color(0xFFF9A825),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            if (result.targetReached) "Target Reached!" else "Best We Could Do",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (result.targetReached) accentColor else Color(0xFFF9A825)
                        )

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Original", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FileUtil.formatFileSize(result.originalSize), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Shrunk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FileUtil.formatFileSize(result.outputSize), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                        }

                        val savingsPercent = if (result.originalSize > 0) {
                            ((result.originalSize - result.outputSize) * 100f / result.originalSize).toInt()
                        } else 0
                        Text(
                            "$savingsPercent% smaller",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )

                        Text(
                            "Compression: ${result.compressionLevel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!result.targetReached) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "The PDF couldn't be compressed further. Consider removing images or splitting the document.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val shareUri = FileUtil.getShareableUri(context, result.outputFile)
                            FileUtil.shareFile(context, shareUri, "application/pdf")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = {
                            // Deep link to WhatsApp
                            val shareUri = FileUtil.getShareableUri(context, result.outputFile)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                setPackage("com.whatsapp")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {
                                // Fallback to generic share if WhatsApp not installed
                                FileUtil.shareFile(context, shareUri, "application/pdf")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp")
                    }
                }

                Button(
                    onClick = { viewModel.clearAll() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Shrink Another", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (!adsRemoved) {
                Spacer(modifier = Modifier.weight(1f))
                AdBanner(modifier = Modifier.fillMaxWidth())
            }
        }

        // Progress sheet
        if (uiState is WhatsAppUiState.Processing) {
            FolioProgressSheet(
                fileName = selectedFile?.name ?: "",
                operationLabel = "Shrinking for WhatsApp",
                progress = progress.fraction,
                onCancel = { },
                onDismiss = { }
            )
        }
    }
}
