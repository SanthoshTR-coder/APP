package com.folio.ui.navigation

/**
 * All navigation routes in Folio.
 * Every screen has a unique string route.
 */
object Routes {
    const val HOME = "home"

    // PDF Tools
    const val MERGE = "merge"
    const val SPLIT = "split"
    const val COMPRESS = "compress"
    const val ROTATE = "rotate"
    const val REORDER = "reorder"
    const val EDIT_PDF = "edit_pdf"
    const val PROTECT = "protect"
    const val UNLOCK = "unlock"
    const val WATERMARK = "watermark"

    // Convert Tools
    const val UNIVERSAL_CONVERTER = "universal_converter"
    const val IMAGE_TO_PDF = "image_to_pdf"
    const val PDF_TO_IMAGE = "pdf_to_image"
    const val PDF_TO_TEXT = "pdf_to_text"
    const val WORD_TO_PDF = "word_to_pdf"
    const val PDF_TO_WORD = "pdf_to_word"
    const val PPT_TO_PDF = "ppt_to_pdf"
    const val PDF_TO_PPT = "pdf_to_ppt"
    const val EXCEL_TO_PDF = "excel_to_pdf"

    // Security
    const val E_SIGN = "e_sign"

    // Smart Tools
    const val HEALTH_CHECK = "health_check"
    const val WHATSAPP_SHRINKER = "whatsapp_shrinker"

    // Utility
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PRIVACY_POLICY = "privacy_policy"
    const val ONBOARDING = "onboarding"
}
