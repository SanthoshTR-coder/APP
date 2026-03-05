package com.folio.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "folio_preferences")

/**
 * Central preferences manager backed by Jetpack DataStore.
 * Replaces any SharedPreferences usage across the app.
 *
 * Manages: dark mode, onboarding shown, ads removed, default save location,
 * output quality, interstitial tracking.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Theme
        val DARK_MODE_KEY = intPreferencesKey("dark_mode") // 0=system, 1=light, 2=dark
        val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")

        // Onboarding
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

        // Ads
        val ADS_REMOVED_KEY = booleanPreferencesKey("ads_removed")
        val LAST_INTERSTITIAL_KEY = longPreferencesKey("last_interstitial_shown")

        // Output
        val DEFAULT_SAVE_LOCATION_KEY = stringPreferencesKey("default_save_location")
        val DEFAULT_COMPRESSION_LEVEL_KEY = intPreferencesKey("default_compression_level")
        val DEFAULT_IMAGE_QUALITY_KEY = intPreferencesKey("default_image_quality")

        // Stats
        val TOTAL_OPERATIONS_KEY = intPreferencesKey("total_operations")
        val FILES_PROCESSED_KEY = intPreferencesKey("files_processed")
    }

    // ─── Theme ───────────────────────────────────

    val darkModeFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[DARK_MODE_KEY] ?: 0 }

    val dynamicColorsFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[DYNAMIC_COLORS_KEY] ?: false }

    suspend fun setDarkMode(mode: Int) {
        dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = mode }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DYNAMIC_COLORS_KEY] = enabled }
    }

    // ─── Onboarding ──────────────────────────────

    val onboardingCompletedFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[ONBOARDING_COMPLETED_KEY] ?: false }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETED_KEY] = true }
    }

    // ─── Ads ─────────────────────────────────────

    val adsRemovedFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[ADS_REMOVED_KEY] ?: false }

    suspend fun setAdsRemoved(removed: Boolean) {
        dataStore.edit { prefs -> prefs[ADS_REMOVED_KEY] = removed }
    }

    val lastInterstitialFlow: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[LAST_INTERSTITIAL_KEY] ?: 0L }

    suspend fun setLastInterstitialShown(timestamp: Long) {
        dataStore.edit { prefs -> prefs[LAST_INTERSTITIAL_KEY] = timestamp }
    }

    // ─── Output Settings ─────────────────────────

    val defaultSaveLocationFlow: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[DEFAULT_SAVE_LOCATION_KEY] ?: "Downloads/Folio" }

    suspend fun setDefaultSaveLocation(path: String) {
        dataStore.edit { prefs -> prefs[DEFAULT_SAVE_LOCATION_KEY] = path }
    }

    val defaultCompressionLevelFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[DEFAULT_COMPRESSION_LEVEL_KEY] ?: 1 } // 0=Light, 1=Balanced, 2=Maximum

    suspend fun setDefaultCompressionLevel(level: Int) {
        dataStore.edit { prefs -> prefs[DEFAULT_COMPRESSION_LEVEL_KEY] = level }
    }

    val defaultImageQualityFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[DEFAULT_IMAGE_QUALITY_KEY] ?: 150 } // DPI

    suspend fun setDefaultImageQuality(dpi: Int) {
        dataStore.edit { prefs -> prefs[DEFAULT_IMAGE_QUALITY_KEY] = dpi }
    }

    // ─── Stats ───────────────────────────────────

    val totalOperationsFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[TOTAL_OPERATIONS_KEY] ?: 0 }

    suspend fun incrementTotalOperations() {
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_OPERATIONS_KEY] ?: 0
            prefs[TOTAL_OPERATIONS_KEY] = current + 1
        }
    }

    val filesProcessedFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[FILES_PROCESSED_KEY] ?: 0 }

    suspend fun incrementFilesProcessed() {
        dataStore.edit { prefs ->
            val current = prefs[FILES_PROCESSED_KEY] ?: 0
            prefs[FILES_PROCESSED_KEY] = current + 1
        }
    }

    // ─── Clear All ───────────────────────────────

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
