package com.folio.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.folio.ui.navigation.Routes
import com.folio.ui.theme.*

/**
 * Data model for a tool card on the home screen.
 */
data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val pastelColor: Color,
    val accentColor: Color,
    val route: String
)

/**
 * All tool definitions grouped by category.
 * Every tool has a unique icon, pastel, and accent color.
 */
object ToolDefinitions {

    val pdfTools = listOf(
        ToolItem(
            "Merge PDF", "Combine multiple files",
            Icons.Default.CallMerge, MergePastel, MergeAccent, Routes.MERGE
        ),
        ToolItem(
            "Split PDF", "Divide into parts",
            Icons.Default.CallSplit, SplitPastel, SplitAccent, Routes.SPLIT
        ),
        ToolItem(
            "Compress", "Shrink file size",
            Icons.Default.Compress, CompressPastel, CompressAccent, Routes.COMPRESS
        ),
        ToolItem(
            "Rotate Pages", "Rotate any direction",
            Icons.Default.RotateRight, RotatePastel, RotateAccent, Routes.ROTATE
        ),
        ToolItem(
            "Reorder Pages", "Drag to rearrange",
            Icons.Default.SwapVert, ReorderPastel, ReorderAccent, Routes.REORDER
        ),
        ToolItem(
            "Edit PDF", "Text, images & draw",
            Icons.Default.Edit, EditPastel, EditAccent, Routes.EDIT_PDF
        ),
    )

    val convertTools = listOf(
        ToolItem(
            "Image → PDF", "Photos to document",
            Icons.Default.Image, ImageToPdfPastel, ImageToPdfAccent, Routes.IMAGE_TO_PDF
        ),
        ToolItem(
            "PDF → Image", "Pages as pictures",
            Icons.Default.PhotoLibrary, PdfToImagePastel, PdfToImageAccent, Routes.PDF_TO_IMAGE
        ),
        ToolItem(
            "Word → PDF", "DOCX to PDF",
            Icons.Default.Description, WordPastel, WordAccent, Routes.WORD_TO_PDF
        ),
        ToolItem(
            "PDF → Word", "PDF to editable DOCX",
            Icons.Default.Article, WordPastel, WordAccent, Routes.PDF_TO_WORD
        ),
        ToolItem(
            "PPT → PDF", "Slides to PDF",
            Icons.Default.Slideshow, PptPastel, PptAccent, Routes.PPT_TO_PDF
        ),
        ToolItem(
            "PDF → PPT", "PDF to slides",
            Icons.Default.PresentToAll, PptPastel, PptAccent, Routes.PDF_TO_PPT
        ),
        ToolItem(
            "Excel → PDF", "Spreadsheet to PDF",
            Icons.Default.TableChart, ExcelPastel, ExcelAccent, Routes.EXCEL_TO_PDF
        ),
        ToolItem(
            "PDF → Text", "Extract readable text",
            Icons.Default.TextSnippet, PdfToTextPastel, PdfToTextAccent, Routes.PDF_TO_TEXT
        ),
    )

    val securityTools = listOf(
        ToolItem(
            "Protect PDF", "Add password lock",
            Icons.Default.Lock, SecurePastel, SecureAccent, Routes.PROTECT
        ),
        ToolItem(
            "Unlock PDF", "Remove password",
            Icons.Default.LockOpen, UnlockPastel, UnlockAccent, Routes.UNLOCK
        ),
        ToolItem(
            "Watermark", "Add text or image",
            Icons.Default.BrandingWatermark, WatermarkPastel, WatermarkAccent, Routes.WATERMARK
        ),
        ToolItem(
            "E-Sign", "Sign documents",
            Icons.Default.Draw, SignPastel, SignAccent, Routes.E_SIGN
        ),
    )

    val smartTools = listOf(
        ToolItem(
            "Health Check", "Diagnose PDF issues",
            Icons.Default.HealthAndSafety, HealthPastel, HealthAccent, Routes.HEALTH_CHECK
        ),
        ToolItem(
            "PDF Editor", "Full editing suite",
            Icons.Default.EditNote, EditPastel, EditAccent, Routes.EDIT_PDF
        ),
    )
}
