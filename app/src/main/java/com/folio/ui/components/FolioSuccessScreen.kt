package com.folio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.folio.R
import com.folio.ui.theme.*

/**
 * Folio Success Screen — shared across ALL tools.
 *
 * Features:
 * - Lottie checkmark animation (plays once, then loops idle)
 * - "Done! [operation] successfully." message
 * - Animated size counter (numbers count up like a slot machine)
 * - For compression: "Saved X MB (Y%)" in a mint pastel pill
 * - Share, Open, Done buttons
 */
@Composable
fun FolioSuccessScreen(
    outputFileName: String,
    originalSize: Long,
    outputSize: Long,
    operationLabel: String,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onDone: () -> Unit
) {
    // Lottie checkmark animation — plays once then idles
    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.success_checkmark)
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = 1,
        isPlaying = true,
        restartOnPlay = false
    )

    // Fallback scale animation if Lottie not yet loaded
    var showCheck by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (showCheck) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkScale"
    )

    // Size counter animation
    var animatedSize by remember { mutableStateOf(0L) }
    val sizeReduction = if (originalSize > 0 && outputSize < originalSize) {
        val saved = originalSize - outputSize
        val percent = (saved.toFloat() / originalSize.toFloat() * 100).toInt()
        Pair(saved, percent)
    } else null

    LaunchedEffect(Unit) {
        showCheck = true
        // Animate the output size counting up like a slot machine
        val steps = 40
        val increment = if (steps > 0) outputSize / steps else outputSize
        for (i in 1..steps) {
            animatedSize = (increment * i).coerceAtMost(outputSize)
            kotlinx.coroutines.delay(16L)
        }
        animatedSize = outputSize
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FolioTheme.colors.background)
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lottie checkmark with fallback to Material icon
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            if (lottieComposition != null) {
                LottieAnimation(
                    composition = lottieComposition,
                    progress = { lottieProgress },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback animated icon while Lottie loads
                Surface(
                    shape = CircleShape,
                    color = CompressPastel,
                    modifier = Modifier
                        .size(96.dp)
                        .scale(checkScale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = CompressAccent,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Success message
        Text(
            text = "Done!",
            style = MaterialTheme.typography.headlineLarge,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W600
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "$operationLabel successfully.",
            style = MaterialTheme.typography.bodyLarge,
            color = FolioTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // File name
        Text(
            text = outputFileName,
            style = MaterialTheme.typography.titleMedium,
            color = FolioTheme.colors.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Animated file size
        Text(
            text = formatFileSize(animatedSize),
            style = MaterialTheme.typography.headlineMedium,
            color = FolioTheme.colors.onSurface,
            fontWeight = FontWeight.W600
        )

        // Compression savings pill
        if (sizeReduction != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Surface(
                shape = RoundedCornerShape(Radius.pill),
                color = CompressPastel
            ) {
                Text(
                    text = "Saved ${formatFileSize(sizeReduction.first)} (${sizeReduction.second}%)",
                    style = MaterialTheme.typography.labelLarge,
                    color = CompressAccent,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xxl))

        // Primary actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.fillMaxWidth()
        ) {
            FolioButton(
                text = "Share",
                onClick = onShare,
                modifier = Modifier.weight(1f),
                accentColor = MergeAccent
            )
            FolioButton(
                text = "Open",
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                accentColor = CompressAccent
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Done text button
        FolioTextButton(
            text = "Done",
            onClick = onDone
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
