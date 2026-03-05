package com.folio.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Composable file picker helpers.
 * Uses Android's modern SAF (Storage Access Framework).
 */

/**
 * Creates a launcher for picking a single file.
 */
@Composable
fun rememberSingleFilePicker(
    mimeTypes: Array<String> = arrayOf("application/pdf"),
    onFilePicked: (Uri) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { onFilePicked(it) }
}

/**
 * Creates a launcher for picking multiple files.
 */
@Composable
fun rememberMultiFilePicker(
    mimeTypes: Array<String> = arrayOf("application/pdf"),
    onFilesPicked: (List<Uri>) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
    if (uris.isNotEmpty()) {
        onFilesPicked(uris)
    }
}

/**
 * Creates a launcher for picking images.
 */
@Composable
fun rememberImagePicker(
    onImagePicked: (Uri) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { onImagePicked(it) }
}

/**
 * Creates a launcher for picking multiple images.
 */
@Composable
fun rememberMultiImagePicker(
    onImagesPicked: (List<Uri>) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
    if (uris.isNotEmpty()) {
        onImagesPicked(uris)
    }
}

/**
 * Supported MIME types for the universal converter.
 */
val UNIVERSAL_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain",
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/heic",
    "image/bmp"
)

val PDF_MIME_TYPES = arrayOf("application/pdf")

val IMAGE_MIME_TYPES = arrayOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/heic",
    "image/bmp"
)

val OFFICE_MIME_TYPES = arrayOf(
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
)
