package com.folio.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Folio Motion System
 *
 * Every transition must be animated. No jarring cuts.
 * Buttery smooth 320ms transitions with cubic easing.
 */

// ─── Spacing Tokens ──────────────────────────────────────────
object Spacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
}

// ─── Radius Tokens ───────────────────────────────────────────
object Radius {
    val sm   = 8.dp
    val md   = 16.dp
    val lg   = 24.dp
    val xl   = 32.dp
    val pill = 100.dp  // for chips and badges
}

// ─── Navigation Transitions ─────────────────────────────────

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInCubic = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

fun folioEnterTransition(): EnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(durationMillis = 320, easing = EaseOutCubic)
) + fadeIn(animationSpec = tween(200))

fun folioExitTransition(): ExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth / 4 },
    animationSpec = tween(320, easing = EaseInCubic)
) + fadeOut(animationSpec = tween(200))

fun folioPopEnterTransition(): EnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth / 4 },
    animationSpec = tween(320, easing = EaseOutCubic)
) + fadeIn(animationSpec = tween(200))

fun folioPopExitTransition(): ExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(320, easing = EaseInCubic)
) + fadeOut(animationSpec = tween(200))

// ─── Card Press Animation ────────────────────────────────────

/**
 * Scale down slightly on tap press for tactile feedback.
 * Apply to any clickable card or button.
 */
fun Modifier.pressEffect(): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale"
    )
    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

// ─── Stagger Animation Helper ────────────────────────────────

/**
 * Delays staggered card appearances by [delayPerItem] ms.
 */
const val STAGGER_DELAY_MS = 40L
