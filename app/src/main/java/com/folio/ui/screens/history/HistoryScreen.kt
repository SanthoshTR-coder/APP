package com.folio.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.folio.data.local.entity.HistoryEntity
import com.folio.ui.components.*
import com.folio.ui.theme.*
import com.folio.util.FileUtil
import java.text.SimpleDateFormat
import java.util.*

/**
 * History Screen
 *
 * Shows all past operations grouped by date.
 * Swipe‐to‐delete per entry, Clear All in action bar.
 */
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyItems by viewModel.historyItems.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("This will permanently delete your operation history. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) {
                    Text("Clear", color = EditAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "History",
                onBackClick = onNavigateBack,
                actions = {
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all",
                                tint = FolioTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        containerColor = FolioTheme.colors.background
    ) { paddingValues ->
        if (historyItems.isEmpty()) {
            EmptyHistoryState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            HistoryList(
                items = historyItems,
                onDelete = { viewModel.deleteEntry(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = FolioTheme.colors.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = FolioTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "No history yet",
            style = MaterialTheme.typography.headlineSmall,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W600
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Your operations will appear here\nonce you start using Folio.",
            style = MaterialTheme.typography.bodyMedium,
            color = FolioTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─── History List (grouped by date) ──────────────────────────

@Composable
private fun HistoryList(
    items: List<HistoryEntity>,
    onDelete: (HistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val grouped = remember(items) {
        items.groupBy { entity ->
            val cal = Calendar.getInstance().apply { timeInMillis = entity.timestamp }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            when {
                isSameDay(cal, today) -> "Today"
                isSameDay(cal, yesterday) -> "Yesterday"
                else -> dateFormat.format(Date(entity.timestamp))
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        grouped.forEach { (dateLabel, entries) ->
            item(key = "header_$dateLabel") {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = FolioTheme.colors.onSurfaceVariant,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.padding(vertical = Spacing.sm)
                )
            }

            items(entries, key = { it.id }) { entry ->
                HistoryItemCard(
                    entry = entry,
                    onDelete = { onDelete(entry) }
                )
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    entry: HistoryEntity,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val accentColor = toolAccentColor(entry.toolName)
    val pastelColor = toolPastelColor(entry.toolName)
    val icon = toolIcon(entry.toolName)

    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = FolioTheme.colors.cardSurface,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Tool icon
            Surface(
                shape = CircleShape,
                color = pastelColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.toolName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W600,
                    color = FolioTheme.colors.onSurface
                )
                Text(
                    text = entry.inputFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = FolioTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = FolioTheme.colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (entry.outputFileSize != null && entry.inputFileSize > 0) {
                        Text(
                            text = "· ${FileUtil.formatFileSize(entry.inputFileSize)} → ${FileUtil.formatFileSize(entry.outputFileSize)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = FolioTheme.colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Status + delete
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(Radius.pill),
                    color = if (entry.status == "success") CompressPastel else EditPastel
                ) {
                    Text(
                        text = if (entry.status == "success") "Done" else "Failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.status == "success") CompressAccent else EditAccent,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = FolioTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun toolAccentColor(toolName: String) = when {
    toolName.contains("Merge", true) -> MergeAccent
    toolName.contains("Split", true) -> SplitAccent
    toolName.contains("Compress", true) -> CompressAccent
    toolName.contains("Rotate", true) -> RotateAccent
    toolName.contains("Reorder", true) -> ReorderAccent
    toolName.contains("Protect", true) || toolName.contains("Lock", true) -> SecureAccent
    toolName.contains("Convert", true) -> ConvertAccent
    toolName.contains("Sign", true) -> SignAccent
    toolName.contains("Health", true) -> HealthAccent
    toolName.contains("WhatsApp", true) -> WhatsAppAccent
    else -> MergeAccent
}

private fun toolPastelColor(toolName: String) = when {
    toolName.contains("Merge", true) -> MergePastel
    toolName.contains("Split", true) -> SplitPastel
    toolName.contains("Compress", true) -> CompressPastel
    toolName.contains("Rotate", true) -> RotatePastel
    toolName.contains("Reorder", true) -> ReorderPastel
    toolName.contains("Protect", true) || toolName.contains("Lock", true) -> SecurePastel
    toolName.contains("Convert", true) -> ConvertPastel
    toolName.contains("Sign", true) -> SignPastel
    toolName.contains("Health", true) -> HealthPastel
    toolName.contains("WhatsApp", true) -> WhatsAppPastel
    else -> MergePastel
}

private fun toolIcon(toolName: String) = when {
    toolName.contains("Merge", true) -> Icons.Default.MergeType
    toolName.contains("Split", true) -> Icons.Default.CallSplit
    toolName.contains("Compress", true) -> Icons.Default.Compress
    toolName.contains("Rotate", true) -> Icons.Default.RotateRight
    toolName.contains("Reorder", true) -> Icons.Default.SwapVert
    toolName.contains("Protect", true) || toolName.contains("Lock", true) -> Icons.Default.Lock
    toolName.contains("Convert", true) -> Icons.Default.Transform
    toolName.contains("Sign", true) -> Icons.Default.Draw
    toolName.contains("Health", true) -> Icons.Default.HealthAndSafety
    toolName.contains("WhatsApp", true) -> Icons.Default.Share
    else -> Icons.Default.Description
}
