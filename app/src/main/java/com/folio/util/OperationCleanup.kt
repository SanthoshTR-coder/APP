package com.folio.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized cleanup after every PDF/conversion operation.
 *
 * Responsibilities:
 * - Delete temp files from cacheDir after operation completes
 * - Increment usage statistics in PreferencesManager
 *
 * Injected in all ViewModels via Hilt; call [cleanup] after success/error.
 */
@Singleton
class OperationCleanup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: com.folio.data.local.preferences.PreferencesManager
) {
    /**
     * Clean up temp files and increment operation stats.
     * Safe to call from any coroutine scope.
     */
    suspend fun cleanup(incrementStats: Boolean = true) = withContext(Dispatchers.IO) {
        // Delete all temp operation files from cache
        FileUtil.clearOperationTempFiles(context)

        // Increment usage counters
        if (incrementStats) {
            preferencesManager.incrementTotalOperations()
            preferencesManager.incrementFilesProcessed()
        }
    }
}
