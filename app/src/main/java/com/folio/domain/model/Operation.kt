package com.folio.domain.model

/**
 * Represents a PDF operation result — success or failure.
 */
sealed class OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : OperationResult<Nothing>()
    data object Loading : OperationResult<Nothing>()
}

/**
 * Tracks operation progress.
 */
data class OperationProgress(
    val current: Int = 0,
    val total: Int = 0,
    val message: String = "",
    val isIndeterminate: Boolean = true
) {
    val fraction: Float
        get() = if (total > 0) current.toFloat() / total.toFloat() else 0f
}

/**
 * Metadata about a completed operation for history logging.
 */
data class Operation(
    val toolName: String,
    val inputFile: String,
    val inputSize: Long,
    val outputFile: String?,
    val outputSize: Long?,
    val outputPath: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true
)
