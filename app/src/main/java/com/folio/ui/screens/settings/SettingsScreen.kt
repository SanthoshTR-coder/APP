package com.folio.ui.screens.settings

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.ui.components.FolioTopBar
import com.folio.ui.theme.FolioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val dynamicColors by viewModel.dynamicColors.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()
    val defaultCompressionLevel by viewModel.defaultCompressionLevel.collectAsStateWithLifecycle()
    val defaultImageQuality by viewModel.defaultImageQuality.collectAsStateWithLifecycle()
    val totalOperations by viewModel.totalOperations.collectAsStateWithLifecycle()
    val filesProcessed by viewModel.filesProcessed.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showCompressionDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Settings",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─── Appearance ──────────────────────
            SettingsSectionHeader("Appearance")

            SettingsClickItem(
                icon = Icons.Default.DarkMode,
                title = "Theme",
                subtitle = when (darkMode) {
                    0 -> "Follow system"
                    1 -> "Light"
                    2 -> "Dark"
                    else -> "Follow system"
                },
                onClick = { showThemeDialog = true }
            )

            SettingsToggleItem(
                icon = Icons.Default.Palette,
                title = "Dynamic Colors",
                subtitle = "Use Material You colors from wallpaper",
                checked = dynamicColors,
                onCheckedChange = { viewModel.setDynamicColors(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Output Defaults ─────────────────
            SettingsSectionHeader("Output Defaults")

            SettingsClickItem(
                icon = Icons.Default.Compress,
                title = "Default Compression",
                subtitle = when (defaultCompressionLevel) {
                    0 -> "Light — best quality"
                    1 -> "Balanced — recommended"
                    2 -> "Maximum — smallest file"
                    else -> "Balanced"
                },
                onClick = { showCompressionDialog = true }
            )

            SettingsClickItem(
                icon = Icons.Default.HighQuality,
                title = "Image Export Quality",
                subtitle = "$defaultImageQuality DPI",
                onClick = { showQualityDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Premium ─────────────────────────
            SettingsSectionHeader("Premium")

            if (!adsRemoved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FolioTheme.colors.mergeAccent.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    onClick = {
                        (context as? Activity)?.let { viewModel.purchaseRemoveAds(it) }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = FolioTheme.colors.mergeAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Remove Ads",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "One-time purchase · \$4.99",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Enjoy a completely ad-free experience forever",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FolioTheme.colors.signAccent.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FolioTheme.colors.signAccent, modifier = Modifier.size(24.dp))
                        Text("Ads removed — thank you for your support!", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }

            SettingsClickItem(
                icon = Icons.Default.Restore,
                title = "Restore Purchases",
                subtitle = "Re-check your previous purchases",
                onClick = { viewModel.restorePurchases() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Stats ───────────────────────────
            SettingsSectionHeader("Your Stats")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = totalOperations.toString(), label = "Operations")
                    StatItem(value = filesProcessed.toString(), label = "Files Processed")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── About ───────────────────────────
            SettingsSectionHeader("About")

            SettingsClickItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "100% offline — your data never leaves your device",
                onClick = onNavigateToPrivacyPolicy
            )

            SettingsClickItem(
                icon = Icons.Default.Info,
                title = "About Folio",
                subtitle = "Version 1.0.0",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "Made with care for Thejas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ─── Dialogs ─────────────────────────────

    if (showThemeDialog) {
        ThemeDialog(
            currentMode = darkMode,
            onSelect = { viewModel.setDarkMode(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showCompressionDialog) {
        CompressionDialog(
            currentLevel = defaultCompressionLevel,
            onSelect = { viewModel.setDefaultCompressionLevel(it); showCompressionDialog = false },
            onDismiss = { showCompressionDialog = false }
        )
    }

    if (showQualityDialog) {
        QualityDialog(
            currentDpi = defaultImageQuality,
            onSelect = { viewModel.setDefaultImageQuality(it); showQualityDialog = false },
            onDismiss = { showQualityDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThemeDialog(currentMode: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf("Follow System" to 0, "Light" to 1, "Dark" to 2)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                options.forEach { (label, mode) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = currentMode == mode, onClick = { onSelect(mode) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CompressionDialog(currentLevel: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf("Light — best quality" to 0, "Balanced — recommended" to 1, "Maximum — smallest file" to 2)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Compression") },
        text = {
            Column {
                options.forEach { (label, level) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = currentLevel == level, onClick = { onSelect(level) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun QualityDialog(currentDpi: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf("72 DPI — small files" to 72, "150 DPI — balanced" to 150, "300 DPI — high quality" to 300)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Image Export Quality") },
        text = {
            Column {
                options.forEach { (label, dpi) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = currentDpi == dpi, onClick = { onSelect(dpi) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
