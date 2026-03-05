package com.folio.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.folio.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders a single PDF page thumbnail using Android's built-in PdfRenderer.
 * Runs on Dispatchers.IO — never blocks the main thread.
 */
@Composable
fun PageThumbnail(
    pdfFile: File,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    showPageNumber: Boolean = true
) {
    var bitmap by remember(pdfFile, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(pdfFile, pageIndex) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                if (pageIndex < renderer.pageCount) {
                    val page = renderer.openPage(pageIndex)
                    val bmp = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    fd.close()
                    bmp
                } else {
                    renderer.close()
                    fd.close()
                    null
                }
            } catch (e: Exception) {
                error = true
                null
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(FolioTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else if (error) {
            Text(
                text = "!",
                style = MaterialTheme.typography.titleMedium,
                color = FolioTheme.colors.onSurfaceVariant
            )
        }

        // Page number badge
        if (showPageNumber) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.xs)
                    .background(
                        color = FolioTheme.colors.onSurface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(Radius.sm)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${pageIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = FolioTheme.colors.cardSurface
                )
            }
        }
    }
}
