package com.folio

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var billingRepository: BillingRepository

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob SDK
        MobileAds.initialize(this) {}
        AdManager.preloadInterstitial(this)

        // Initialize billing
        billingRepository.initialize(this)

        // Check if onboarding has been completed
        var showOnboarding by mutableStateOf<Boolean?>(null)
        lifecycleScope.launch {
            showOnboarding = !preferencesManager.onboardingCompletedFlow.first()
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
        billingRepository.cleanup()
    }
}
