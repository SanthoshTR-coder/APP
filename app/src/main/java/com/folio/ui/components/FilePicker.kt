package com.folio.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.folio.ui.theme.*

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

/**
 * A visual file-picker drop zone that shows a dashed border box with an upload icon.
 * When tapped, invokes [onPickFile] so callers can launch their own SAF launcher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePicker(
    label: String,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onPickFile,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tap to browse files",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
