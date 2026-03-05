package com.folio.domain.usecase.smart

import android.content.Context
import android.net.Uri
import com.folio.domain.model.OperationProgress
import com.folio.domain.model.OperationResult
import com.folio.domain.usecase.pdf.CompressPdfUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart compression targeting specific platform size limits.
 * Tries compression levels until the target size is reached.
 */
@Singleton
class WhatsAppShrinkerUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compressPdfUseCase: CompressPdfUseCase
) {
    private val _progress = MutableStateFlow(OperationProgress())
    val progress: StateFlow<OperationProgress> = _progress.asStateFlow()

    enum class SizeTarget(val label: String, val maxBytes: Long) {
        WHATSAPP_OPTIMAL("WhatsApp Optimal", 16L * 1024 * 1024),       // 16 MB
        WHATSAPP_MAX("WhatsApp Max", 100L * 1024 * 1024),               // 100 MB
        EMAIL("Email", 10L * 1024 * 1024),                              // 10 MB
        GMAIL("Gmail", 25L * 1024 * 1024),                              // 25 MB
        CUSTOM("Custom", 0)                                              // User-defined
    }

    data class ShrinkResult(
        val outputFile: File,
        val originalSize: Long,
        val outputSize: Long,
        val targetReached: Boolean,
        val compressionLevel: CompressPdfUseCase.CompressionLevel
    )

    suspend fun execute(
        uri: Uri,
        target: SizeTarget,
        customTargetMb: Int? = null
    ): OperationResult<ShrinkResult> = withContext(Dispatchers.IO) {
        try {
            val targetBytes = if (target == SizeTarget.CUSTOM) {
                (customTargetMb ?: 10) * 1024L * 1024L
            } else {
                target.maxBytes
            }

            // Get original file size
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val originalSize = cursor?.use {
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                it.moveToFirst()
                if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
            } ?: 0L

            if (originalSize <= targetBytes) {
                // Already under target — light compress just in case
                _progress.value = OperationProgress(1, 2, "File already under target, applying light compression…", false)
                return@withContext when (val result = compressPdfUseCase.execute(uri, CompressPdfUseCase.CompressionLevel.LIGHT)) {
                    is OperationResult.Success -> OperationResult.Success(
                        ShrinkResult(result.data, originalSize, result.data.length(), true, CompressPdfUseCase.CompressionLevel.LIGHT)
                    )
                    is OperationResult.Error -> result
                    is OperationResult.Loading -> OperationResult.Error("Unexpected state")
                }
            }

            // Try compression levels from light to maximum
            val levels = listOf(
                CompressPdfUseCase.CompressionLevel.LIGHT,
                CompressPdfUseCase.CompressionLevel.BALANCED,
                CompressPdfUseCase.CompressionLevel.MAXIMUM
            )

            for ((index, level) in levels.withIndex()) {
                _progress.value = OperationProgress(
                    current = index + 1,
                    total = levels.size,
                    message = "Trying ${level.name.lowercase()} compression…",
                    isIndeterminate = false
                )

                when (val result = compressPdfUseCase.execute(uri, level)) {
                    is OperationResult.Success -> {
                        val outputSize = result.data.length()
                        if (outputSize <= targetBytes) {
                            _progress.value = OperationProgress(levels.size, levels.size, "Target reached!", false)
                            return@withContext OperationResult.Success(
                                ShrinkResult(result.data, originalSize, outputSize, true, level)
                            )
                        }
                        // If this wasn't the last level, delete the temp file and try harder
                        if (index < levels.size - 1) {
                            result.data.delete()
                        } else {
                            // Maximum compression result — couldn't reach target
                            return@withContext OperationResult.Success(
                                ShrinkResult(result.data, originalSize, outputSize, false, level)
                            )
                        }
                    }
                    is OperationResult.Error -> return@withContext result
                    is OperationResult.Loading -> {}
                }
            }

            OperationResult.Error("Compression failed unexpectedly")
        } catch (e: Exception) {
            OperationResult.Error("Failed to shrink PDF: ${e.localizedMessage}")
        }
    }
}
