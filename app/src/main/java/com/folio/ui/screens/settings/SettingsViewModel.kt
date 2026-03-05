package com.folio.ui.screens.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.data.local.preferences.PreferencesManager
import com.folio.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val billingRepository: BillingRepository
) : ViewModel() {

    val darkMode: StateFlow<Int> = preferencesManager.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dynamicColors: StateFlow<Boolean> = preferencesManager.dynamicColorsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    val defaultSaveLocation: StateFlow<String> = preferencesManager.defaultSaveLocationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Downloads/Folio")

    val defaultCompressionLevel: StateFlow<Int> = preferencesManager.defaultCompressionLevelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val defaultImageQuality: StateFlow<Int> = preferencesManager.defaultImageQualityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    val totalOperations: StateFlow<Int> = preferencesManager.totalOperationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val filesProcessed: StateFlow<Int> = preferencesManager.filesProcessedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setDarkMode(mode: Int) {
        viewModelScope.launch { preferencesManager.setDarkMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDynamicColors(enabled) }
    }

    fun setDefaultCompressionLevel(level: Int) {
        viewModelScope.launch { preferencesManager.setDefaultCompressionLevel(level) }
    }

    fun setDefaultImageQuality(dpi: Int) {
        viewModelScope.launch { preferencesManager.setDefaultImageQuality(dpi) }
    }

    fun purchaseRemoveAds(activity: Activity) {
        billingRepository.launchPurchaseFlow(activity)
    }

    fun restorePurchases() {
        billingRepository.restorePurchases()
    }
}
