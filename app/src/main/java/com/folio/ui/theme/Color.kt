package com.folio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Folio Design System — "Folio Pastel"
 *
 * Every color has a meaning. Every tool category has its own accent.
 * Warm whites, never pure white. Warm darks, never cold black.
 */

// ─── Base Surfaces ───────────────────────────────────────────
val Surface = Color(0xFFFAF9F7)             // Warm off-white, like paper
val SurfaceVariant = Color(0xFFF2F0EC)      // Slightly darker warm grey
val Background = Color(0xFFFAF9F7)

// ─── Text ────────────────────────────────────────────────────
val OnSurface = Color(0xFF1C1B1A)           // Warm near-black (not pure #000)
val OnSurfaceVariant = Color(0xFF6B6860)    // Muted warm grey for secondary text

// ─── Tool Category Accent Colors — pastel with meaning ──────

// Merge — "combining things"
val MergePastel = Color(0xFFD4E8FF)         // Soft sky blue
val MergeAccent = Color(0xFF3B82C4)         // Deep sky — action color

// Split — "dividing things"
val SplitPastel = Color(0xFFFFE4D4)         // Soft peach
val SplitAccent = Color(0xFFD4612A)

// Compress — "shrinking, efficient"
val CompressPastel = Color(0xFFD4F0E8)      // Soft mint
val CompressAccent = Color(0xFF2A9B72)

// Secure — "protection, trust"
val SecurePastel = Color(0xFFEAD4FF)        // Soft lavender
val SecureAccent = Color(0xFF7B4DB8)

// Convert — "transformation"
val ConvertPastel = Color(0xFFFFF3D4)       // Soft amber
val ConvertAccent = Color(0xFFB8860B)

// Edit — "creative, active"
val EditPastel = Color(0xFFFFD4E8)          // Soft rose
val EditAccent = Color(0xFFB8315A)

// Sign — "confirmed, done"
val SignPastel = Color(0xFFD4FFE4)          // Soft sage green
val SignAccent = Color(0xFF2A8B4A)

// Health — "diagnostic, insight"
val HealthPastel = Color(0xFFFFF8D4)        // Soft lemon
val HealthAccent = Color(0xFF8B7A00)

// WhatsApp — "sharing, sending"
val WhatsAppPastel = Color(0xFFD4FFD4)      // Soft green
val WhatsAppAccent = Color(0xFF128C3E)

// Rotate — reuse split tones for visual consistency
val RotatePastel = Color(0xFFFFE4D4)
val RotateAccent = Color(0xFFD4612A)

// Reorder — reuse merge tones
val ReorderPastel = Color(0xFFD4E8FF)
val ReorderAccent = Color(0xFF3B82C4)

// Unlock — reuse secure tones
val UnlockPastel = Color(0xFFEAD4FF)
val UnlockAccent = Color(0xFF7B4DB8)

// Watermark — reuse edit tones
val WatermarkPastel = Color(0xFFFFD4E8)
val WatermarkAccent = Color(0xFFB8315A)

// Image to PDF
val ImageToPdfPastel = Color(0xFFFFF3D4)
val ImageToPdfAccent = Color(0xFFB8860B)

// PDF to Image
val PdfToImagePastel = Color(0xFFFFF3D4)
val PdfToImageAccent = Color(0xFFB8860B)

// PDF to Text
val PdfToTextPastel = Color(0xFFFFF3D4)
val PdfToTextAccent = Color(0xFFB8860B)

// Word
val WordPastel = Color(0xFFD4E8FF)
val WordAccent = Color(0xFF3B82C4)

// PPT
val PptPastel = Color(0xFFFFE4D4)
val PptAccent = Color(0xFFD4612A)

// Excel
val ExcelPastel = Color(0xFFD4F0E8)
val ExcelAccent = Color(0xFF2A9B72)

// Universal converter
val UniversalPastel = Color(0xFFEAD4FF)
val UniversalAccent = Color(0xFF7B4DB8)

// ─── Neutral card background ─────────────────────────────────
val CardSurface = Color(0xFFFFFFFF)         // Pure white cards on warm surface
val DividerColor = Color(0xFFE8E5E0)

// ─── Dark Mode Equivalents ───────────────────────────────────
val SurfaceDark = Color(0xFF1A1917)         // Warm dark, not cold black
val SurfaceVariantDark = Color(0xFF242220)
val CardSurfaceDark = Color(0xFF2A2826)
val OnSurfaceDark = Color(0xFFF0EDE8)
val OnSurfaceVariantDark = Color(0xFF9A968E)
val BackgroundDark = Color(0xFF1A1917)
val DividerColorDark = Color(0xFF3A3735)
