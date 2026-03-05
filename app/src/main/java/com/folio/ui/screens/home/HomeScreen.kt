package com.folio.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.data.local.entity.RecentFileEntity
import com.folio.ui.components.AdBanner
import com.folio.ui.components.FolioHeroCard
import com.folio.ui.components.FolioToolCard
import com.folio.ui.navigation.Routes
import com.folio.ui.theme.*

/**
 * Folio Home Screen — "The Dashboard"
 *
 * Premium first impression. Warm greeting, recent files strip,
 * hero feature card, staggered tool grid with pastel categories.
 */
@Composable
fun HomeScreen(
    onNavigateToTool: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()

    // Stagger animation state
    val allTools = remember {
        ToolDefinitions.pdfTools +
                ToolDefinitions.convertTools +
                ToolDefinitions.securityTools +
                ToolDefinitions.smartTools
    }
    val visibleCards = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) {
        allTools.indices.forEach { index ->
            kotlinx.coroutines.delay(index * STAGGER_DELAY_MS)
            visibleCards.add(index)
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = FolioTheme.colors.background,
        bottomBar = {
            AdBanner(adsRemoved = adsRemoved)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .statusBarsPadding()
        ) {
            // ─── Greeting Header ─────────────────────
            GreetingHeader(
                greeting = viewModel.greeting,
                onHistoryClick = onNavigateToHistory,
                onSettingsClick = onNavigateToSettings
            )

            // ─── Recent Files Strip ──────────────────
            if (recentFiles.isNotEmpty()) {
                RecentFilesStrip(
                    files = recentFiles,
                    onFileClick = { /* Re-open in correct tool */ }
                )
            }

            // ─── Hero Feature Card: WhatsApp Shrinker ─
            Spacer(modifier = Modifier.height(Spacing.md))
            FolioHeroCard(
                title = "WhatsApp Ready",
                description = "Make any file share-ready instantly",
                icon = Icons.Default.Bolt,
                pastelColor = FolioTheme.colors.whatsAppPastel,
                accentColor = FolioTheme.colors.whatsAppAccent,
                onClick = { onNavigateToTool(Routes.WHATSAPP_SHRINKER) },
                modifier = Modifier.padding(horizontal = Spacing.md)
            )

            // ─── PDF Tools Section ───────────────────
            Spacer(modifier = Modifier.height(Spacing.lg))
            ToolSectionGrid(
                title = "PDF TOOLS",
                tools = ToolDefinitions.pdfTools,
                visibleCards = visibleCards,
                startIndex = 0,
                onToolClick = onNavigateToTool
            )

            // ─── Convert Section ─────────────────────
            Spacer(modifier = Modifier.height(Spacing.lg))
            ToolSectionGrid(
                title = "CONVERT",
                tools = ToolDefinitions.convertTools,
                visibleCards = visibleCards,
                startIndex = ToolDefinitions.pdfTools.size,
                onToolClick = onNavigateToTool
            )

            // ─── Security Section ────────────────────
            Spacer(modifier = Modifier.height(Spacing.lg))
            ToolSectionGrid(
                title = "SECURITY",
                tools = ToolDefinitions.securityTools,
                visibleCards = visibleCards,
                startIndex = ToolDefinitions.pdfTools.size + ToolDefinitions.convertTools.size,
                onToolClick = onNavigateToTool
            )

            // ─── Smart Tools Section ─────────────────
            Spacer(modifier = Modifier.height(Spacing.lg))
            ToolSectionGrid(
                title = "SMART TOOLS",
                tools = ToolDefinitions.smartTools,
                visibleCards = visibleCards,
                startIndex = ToolDefinitions.pdfTools.size +
                        ToolDefinitions.convertTools.size +
                        ToolDefinitions.securityTools.size,
                onToolClick = onNavigateToTool
            )

            // Bottom padding for ad banner
            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

// ─── Greeting Header ─────────────────────────────────────

@Composable
private fun GreetingHeader(
    greeting: String,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = FolioTheme.colors.onSurface,
                fontWeight = FontWeight.W600
            )
            Text(
                text = "What would you like to do?",
                style = MaterialTheme.typography.bodyMedium,
                color = FolioTheme.colors.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "History",
                    tint = FolioTheme.colors.onSurfaceVariant
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = FolioTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Recent Files Horizontal Strip ───────────────────────

@Composable
private fun RecentFilesStrip(
    files: List<RecentFileEntity>,
    onFileClick: (RecentFileEntity) -> Unit
) {
    Column(modifier = Modifier.padding(top = Spacing.sm)) {
        SectionLabel(
            text = "RECENT FILES",
            modifier = Modifier.padding(horizontal = Spacing.md)
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            files.forEach { file ->
                RecentFileChip(
                    file = file,
                    onClick = { onFileClick(file) }
                )
            }
        }
    }
}

@Composable
private fun RecentFileChip(
    file: RecentFileEntity,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.md),
        color = FolioTheme.colors.cardSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // File type icon
            val icon = when {
                file.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                file.mimeType.contains("word") || file.mimeType.contains("document") -> Icons.Default.Description
                file.mimeType.contains("presentation") || file.mimeType.contains("powerpoint") -> Icons.Default.Slideshow
                file.mimeType.contains("sheet") || file.mimeType.contains("excel") -> Icons.Default.TableChart
                file.mimeType.startsWith("image") -> Icons.Default.Image
                else -> Icons.Default.InsertDriveFile
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FolioTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Column {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.labelMedium,
                    color = FolioTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.operationPerformed,
                    style = MaterialTheme.typography.labelSmall,
                    color = FolioTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Tool Section Grid ───────────────────────────────────

@Composable
private fun ToolSectionGrid(
    title: String,
    tools: List<ToolItem>,
    visibleCards: List<Int>,
    startIndex: Int,
    onToolClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
        SectionLabel(text = title)

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 2-column grid using Flow
        val rows = tools.chunked(2)
        rows.forEachIndexed { rowIndex, rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                rowTools.forEachIndexed { colIndex, tool ->
                    val globalIndex = startIndex + (rowIndex * 2) + colIndex

                    AnimatedVisibility(
                        visible = globalIndex in visibleCards,
                        enter = slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = tween(300, easing = EaseOutCubic)
                        ) + fadeIn(animationSpec = tween(200)),
                        modifier = Modifier.weight(1f)
                    ) {
                        FolioToolCard(
                            title = tool.title,
                            description = tool.description,
                            icon = tool.icon,
                            pastelColor = tool.pastelColor,
                            accentColor = tool.accentColor,
                            onClick = { onToolClick(tool.route) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // If odd number of tools, fill the empty space
                if (rowTools.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}

// ─── Section Label ───────────────────────────────────────

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = FolioTheme.colors.onSurfaceVariant,
        fontWeight = FontWeight.W600,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
        modifier = modifier
    )
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
