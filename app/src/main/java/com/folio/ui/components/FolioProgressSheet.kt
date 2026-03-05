package com.folio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.folio.ui.theme.*

/**
 * Folio Processing Progress Sheet
 *
 * Shown as a ModalBottomSheet during file operations.
 * User can still see what's below — not a blocking dialog.
 *
 * Features:
 * - Animated tool icon (subtle pulse)
 * - Operation label + file name
 * - Linear progress indicator (animated, rounded)
 * - "This may take a few seconds..." hint
 * - Optional cancel button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolioProgressSheet(
    fileName: String,
    operationLabel: String,
    progress: Float?,           // null = indeterminate, 0f-1f = determinate
    onCancel: (() -> Unit)? = null,
    onDismiss: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FolioTheme.colors.cardSurface,
        shape = RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = Spacing.md)
                    .width(40.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(Radius.pill),
                color = FolioTheme.colors.divider
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Pulsing indicator
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            // Operation label
            Text(
                text = operationLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = FolioTheme.colors.onSurface,
                modifier = Modifier.alpha(pulseAlpha)
            )

            // File name
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = FolioTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Progress bar
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    trackColor = FolioTheme.colors.divider,
                    color = CompressAccent,
                    strokeCap = StrokeCap.Round,
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = FolioTheme.colors.onSurfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    trackColor = FolioTheme.colors.divider,
                    color = CompressAccent,
                    strokeCap = StrokeCap.Round,
                )
            }

            // Hint text
            Text(
                text = "This may take a few seconds…",
                style = MaterialTheme.typography.bodySmall,
                color = FolioTheme.colors.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // Cancel button
            if (onCancel != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                FolioTextButton(
                    text = "Cancel",
                    onClick = onCancel
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}
