package com.folio.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Folio Theme — warm, human, premium.
 *
 * Never pure white, never cold black.
 * Custom FolioColors provided alongside Material3.
 */

// ─── Custom Folio Colors ─────────────────────────────────────

data class FolioColors(
    val surface: Color,
    val surfaceVariant: Color,
    val background: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val cardSurface: Color,
    val divider: Color,
    val mergePastel: Color,
    val mergeAccent: Color,
    val splitPastel: Color,
    val splitAccent: Color,
    val compressPastel: Color,
    val compressAccent: Color,
    val securePastel: Color,
    val secureAccent: Color,
    val convertPastel: Color,
    val convertAccent: Color,
    val editPastel: Color,
    val editAccent: Color,
    val signPastel: Color,
    val signAccent: Color,
    val healthPastel: Color,
    val healthAccent: Color,
    val whatsAppPastel: Color,
    val whatsAppAccent: Color,
)

val LightFolioColors = FolioColors(
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    background = Background,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    cardSurface = CardSurface,
    divider = DividerColor,
    mergePastel = MergePastel,
    mergeAccent = MergeAccent,
    splitPastel = SplitPastel,
    splitAccent = SplitAccent,
    compressPastel = CompressPastel,
    compressAccent = CompressAccent,
    securePastel = SecurePastel,
    secureAccent = SecureAccent,
    convertPastel = ConvertPastel,
    convertAccent = ConvertAccent,
    editPastel = EditPastel,
    editAccent = EditAccent,
    signPastel = SignPastel,
    signAccent = SignAccent,
    healthPastel = HealthPastel,
    healthAccent = HealthAccent,
    whatsAppPastel = WhatsAppPastel,
    whatsAppAccent = WhatsAppAccent,
)

val DarkFolioColors = FolioColors(
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    background = BackgroundDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    cardSurface = CardSurfaceDark,
    divider = DividerColorDark,
    // Pastels are slightly muted in dark mode
    mergePastel = MergeAccent.copy(alpha = 0.15f),
    mergeAccent = MergeAccent,
    splitPastel = SplitAccent.copy(alpha = 0.15f),
    splitAccent = SplitAccent,
    compressPastel = CompressAccent.copy(alpha = 0.15f),
    compressAccent = CompressAccent,
    securePastel = SecureAccent.copy(alpha = 0.15f),
    secureAccent = SecureAccent,
    convertPastel = ConvertAccent.copy(alpha = 0.15f),
    convertAccent = ConvertAccent,
    editPastel = EditAccent.copy(alpha = 0.15f),
    editAccent = EditAccent,
    signPastel = SignAccent.copy(alpha = 0.15f),
    signAccent = SignAccent,
    healthPastel = HealthAccent.copy(alpha = 0.15f),
    healthAccent = HealthAccent,
    whatsAppPastel = WhatsAppAccent.copy(alpha = 0.15f),
    whatsAppAccent = WhatsAppAccent,
)

val LocalFolioColors = staticCompositionLocalOf { LightFolioColors }

// ─── Material3 Color Schemes ─────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = MergeAccent,
    onPrimary = Color.White,
    primaryContainer = MergePastel,
    secondary = CompressAccent,
    onSecondary = Color.White,
    secondaryContainer = CompressPastel,
    tertiary = SecureAccent,
    onTertiary = Color.White,
    tertiaryContainer = SecurePastel,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = DividerColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = MergeAccent,
    onPrimary = Color.White,
    primaryContainer = MergeAccent.copy(alpha = 0.3f),
    secondary = CompressAccent,
    onSecondary = Color.White,
    secondaryContainer = CompressAccent.copy(alpha = 0.3f),
    tertiary = SecureAccent,
    onTertiary = Color.White,
    tertiaryContainer = SecureAccent.copy(alpha = 0.3f),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = DividerColorDark,
)

// ─── Theme Composable ────────────────────────────────────────

/**
 * @param darkThemeOverride 0 = follow system (default), 1 = force light, 2 = force dark.
 *                          Pass from PreferencesManager.darkMode flow.
 */
@Composable
fun FolioTheme(
    darkThemeOverride: Int = 0,
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkThemeOverride) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val folioColors = if (darkTheme) DarkFolioColors else LightFolioColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalFolioColors provides folioColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FolioTypography,
            content = content
        )
    }
}

/**
 * Access Folio custom colors from anywhere in Compose.
 *
 * Usage: FolioTheme.colors.mergePastel, FolioTheme.colors.onSurface, etc.
 */
object FolioTheme {
    val colors: FolioColors
        @Composable
        get() = LocalFolioColors.current
}
