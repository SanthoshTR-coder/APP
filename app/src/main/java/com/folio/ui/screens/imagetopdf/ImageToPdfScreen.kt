package com.folio.ui.screens.imagetopdf

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.folio.domain.usecase.convert.ImageToPdfUseCase.Margin
import com.folio.domain.usecase.convert.ImageToPdfUseCase.PageSize
import com.folio.ui.components.AdBanner
import com.folio.ui.components.FolioButton
import com.folio.ui.components.FolioProgressSheet
import com.folio.ui.components.FolioSuccessScreen
import com.folio.ui.components.FolioTopBar
import com.folio.ui.theme.FolioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImageToPdfViewModel = hiltViewModel()
) {
    val images by viewModel.selectedImages.collectAsState()
    val pageSize by viewModel.pageSize.collectAsState()
    val margin by viewModel.margin.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val adsRemoved by viewModel.adsRemoved.collectAsState()

    val accentColor = FolioTheme.colors.convertAccent
    val pastelColor = FolioTheme.colors.convertPastel

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        viewModel.addImages(uris)
    }

    when (val state = uiState) {
        is ImageToPdfUiState.Success -> {
            val context = LocalContext.current
            FolioSuccessScreen(
                outputFileName = state.outputFile.name,
                originalSize = state.originalTotalSize,
                outputSize = state.outputSize,
                operationLabel = "Image → PDF",
                onShare = {
                    val intent = com.folio.util.FileUtil.createShareIntent(context, state.outputFile)
                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                },
                onOpen = {
                    val intent = com.folio.util.FileUtil.createOpenIntent(context, state.outputFile)
                    context.startActivity(intent)
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
                        title = "Image → PDF",
                        onBackClick = onNavigateBack
                    )
                },
                bottomBar = {
                    Column {
                        if (!adsRemoved) {
                            AdBanner()
                        }
                        AnimatedVisibility(visible = images.isNotEmpty()) {
                            Surface(
                                tonalElevation = 4.dp,
                                shadowElevation = 8.dp
                            ) {
                                FolioButton(
                                    text = "Create PDF (${images.size} image${if (images.size != 1) "s" else ""})",
                                    onClick = { viewModel.convert() },
                                    accentColor = accentColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (images.isEmpty()) {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = accentColor.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Add images to create a PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Supports JPG, PNG, WEBP • Up to 30 images",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(24.dp))
                                FolioButton(
                                    text = "Select Images",
                                    onClick = {
                                        imagePickerLauncher.launch(arrayOf("image/*"))
                                    },
                                    accentColor = accentColor
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Page size selector
                            item {
                                Text(
                                    "Page Size",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    PageSize.entries.forEach { size ->
                                        FilterChip(
                                            selected = pageSize == size,
                                            onClick = { viewModel.setPageSize(size) },
                                            label = { Text(size.name.replace("_", " "), style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = pastelColor,
                                                selectedLabelColor = accentColor
                                            )
                                        )
                                    }
                                }
                            }

                            // Margin selector
                            item {
                                Text(
                                    "Margins",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Margin.entries.forEach { m ->
                                        FilterChip(
                                            selected = margin == m,
                                            onClick = { viewModel.setMargin(m) },
                                            label = { Text(m.name, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = pastelColor,
                                                selectedLabelColor = accentColor
                                            )
                                        )
                                    }
                                }
                            }

                            // Image list header
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${images.size} image${if (images.size != 1) "s" else ""} selected",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = {
                                            imagePickerLauncher.launch(arrayOf("image/*"))
                                        }) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Add More")
                                        }
                                    }
                                }
                            }

                            // Image items
                            itemsIndexed(images, key = { i, doc -> "${doc.uri}_$i" }) { index, doc ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Position badge
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(pastelColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        // Image thumbnail
                                        AsyncImage(
                                            model = doc.uri,
                                            contentDescription = doc.name,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        // Name & size
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                doc.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                doc.sizeFormatted,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }

                                        // Reorder arrows
                                        Column {
                                            if (index > 0) {
                                                IconButton(
                                                    onClick = { viewModel.reorderImages(index, index - 1) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            if (index < images.size - 1) {
                                                IconButton(
                                                    onClick = { viewModel.reorderImages(index, index + 1) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        // Remove button
                                        IconButton(
                                            onClick = { viewModel.removeImage(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Progress sheet
            if (uiState is ImageToPdfUiState.Processing) {
                FolioProgressSheet(
                    fileName = "${images.size} images",
                    operationLabel = "Creating PDF",
                    progress = if (progress.isIndeterminate) null else progress.fraction
                )
            }

            // Error dialog
            if (uiState is ImageToPdfUiState.Error) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearAll() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearAll() }) { Text("OK") }
                    },
                    title = { Text("Error") },
                    text = { Text((uiState as ImageToPdfUiState.Error).message) }
                )
            }
        }
    }
}
