package com.folio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.folio.ui.theme.FolioTheme
import com.folio.ui.theme.Spacing
import com.folio.util.AdManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * AdMob banner ad component.
 * Hidden for paid users who purchased "Remove Ads".
 *
 * Test ad unit ID: ca-app-pub-3940256099942544/6300978111
 * Replace with real ID before Play Store submission.
 */
@Composable
fun AdBanner(
    adsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (adsRemoved) return

    Column(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            factory = { context ->
                try {
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AdManager.BANNER_AD_UNIT_ID
                        loadAd(AdRequest.Builder().build())
                    }
                } catch (e: Exception) {
                    // If AdView fails to create, return an empty view
                    android.view.View(context)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
