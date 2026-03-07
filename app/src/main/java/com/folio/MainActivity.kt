package com.folio

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.folio.data.local.preferences.PreferencesManager
import com.folio.data.repository.BillingRepository
import com.folio.ui.navigation.AppNavGraph
import com.folio.ui.theme.FolioTheme
import com.folio.util.AdManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var billingRepository: BillingRepository

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.e("MainActivity", "enableEdgeToEdge failed", e)
        }

        // Preload interstitial ad (MobileAds already initialized in FolioApp)
        try {
            AdManager.preloadInterstitial(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Ad preload failed", e)
        }

        // Initialize billing
        try {
            billingRepository.initialize(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Billing initialization failed", e)
        }

        // Check if onboarding has been completed
        var showOnboarding by mutableStateOf<Boolean?>(null)
        lifecycleScope.launch {
            try {
                showOnboarding = !preferencesManager.onboardingCompletedFlow.first()
            } catch (e: Exception) {
                Log.e("MainActivity", "Preferences read failed", e)
                showOnboarding = true // Default to showing onboarding
            }
        }

        setContent {
            val darkMode by preferencesManager.darkModeFlow.collectAsState(initial = 0)

            FolioTheme(darkThemeOverride = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FolioTheme.colors.background
                ) {
                    // Wait until we know if onboarding is needed
                    val onboardingNeeded = showOnboarding
                    if (onboardingNeeded != null) {
                        AppNavGraph(showOnboarding = onboardingNeeded)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            billingRepository.cleanup()
        } catch (e: Exception) {
            Log.e("MainActivity", "Billing cleanup failed", e)
        }
    }
}
