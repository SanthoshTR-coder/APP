package com.folio.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.folio.ui.screens.compress.CompressScreen
import com.folio.ui.screens.converter.UniversalConverterScreen
import com.folio.ui.screens.exceltopdf.ExcelToPdfScreen
import com.folio.ui.screens.history.HistoryScreen
import com.folio.ui.screens.home.HomeScreen
import com.folio.ui.screens.imagetopdf.ImageToPdfScreen
import com.folio.ui.screens.merge.MergeScreen
import com.folio.ui.screens.pdftoppt.PdfToPptScreen
import com.folio.ui.screens.pdftoimage.PdfToImageScreen
import com.folio.ui.screens.pdftotext.PdfToTextScreen
import com.folio.ui.screens.pdftoword.PdfToWordScreen
import com.folio.ui.screens.ppttopdf.PptToPdfScreen
import com.folio.ui.screens.reorder.ReorderScreen
import com.folio.ui.screens.rotate.RotateScreen
import com.folio.ui.screens.split.SplitScreen
import com.folio.ui.screens.wordtopdf.WordToPdfScreen
import com.folio.ui.screens.protect.ProtectPdfScreen
import com.folio.ui.screens.unlock.UnlockPdfScreen
import com.folio.ui.screens.watermark.WatermarkPdfScreen
import com.folio.ui.screens.esign.ESignPdfScreen
import com.folio.ui.screens.editpdf.EditPdfScreen
import com.folio.ui.screens.healthcheck.HealthCheckScreen
import com.folio.ui.screens.whatsapp.WhatsAppShrinkerScreen
import com.folio.ui.screens.settings.SettingsScreen
import com.folio.ui.screens.onboarding.OnboardingScreen
import com.folio.ui.screens.privacy.PrivacyPolicyScreen
import com.folio.ui.theme.*

/**
 * Main navigation graph for Folio.
 *
 * All transitions use the slide+fade transitions defined in Motion.kt.
 * Every screen has proper enter/exit animations — no jarring cuts.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    showOnboarding: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) Routes.ONBOARDING else Routes.HOME,
        enterTransition = { folioEnterTransition() },
        exitTransition = { folioExitTransition() },
        popEnterTransition = { folioPopEnterTransition() },
        popExitTransition = { folioPopExitTransition() }
    ) {
        // ─── Home ────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToTool = { route -> navController.navigate(route) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        // ─── PDF Tools ───────────────────────────────
        composable(Routes.MERGE) {
            MergeScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SPLIT) {
            SplitScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.COMPRESS) {
            CompressScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.ROTATE) {
            RotateScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.REORDER) {
            ReorderScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.EDIT_PDF) {
            EditPdfScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PROTECT) {
            ProtectPdfScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.UNLOCK) {
            UnlockPdfScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.WATERMARK) {
            WatermarkPdfScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ─── Convert Tools ───────────────────────────
        composable(Routes.UNIVERSAL_CONVERTER) {
            UniversalConverterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTool = { route -> navController.navigate(route) }
            )
        }
        composable(Routes.IMAGE_TO_PDF) {
            ImageToPdfScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PDF_TO_IMAGE) {
            PdfToImageScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PDF_TO_TEXT) {
            PdfToTextScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.WORD_TO_PDF) {
            WordToPdfScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PDF_TO_WORD) {
            PdfToWordScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PPT_TO_PDF) {
            PptToPdfScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PDF_TO_PPT) {
            PdfToPptScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.EXCEL_TO_PDF) {
            ExcelToPdfScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ─── Security ────────────────────────────────
        composable(Routes.E_SIGN) {
            ESignPdfScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ─── Smart Tools ─────────────────────────────
        composable(Routes.HEALTH_CHECK) {
            HealthCheckScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.WHATSAPP_SHRINKER) {
            WhatsAppShrinkerScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ─── Utility ─────────────────────────────────
        composable(Routes.HISTORY) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) }
            )
        }
        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
    }
}


