package com.folio.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.folio.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * AdMob ad manager with cooldown logic and interstitial preloading.
 *
 * Rules:
 * - Banner: bottom of every screen (hidden after Remove Ads)
 * - Interstitial: ONLY after successful operation
 * - Minimum 4 minutes between interstitials
 * - NEVER during History or Settings
 * - NEVER mid-operation
 */
object AdManager {

    private const val TAG = "AdManager"

    private var lastInterstitialShownAt = 0L
    private val COOLDOWN_MS = 4 * 60 * 1000L  // 4 minutes

    // Ad unit IDs from BuildConfig — test IDs in debug, real IDs in release
    val BANNER_AD_UNIT_ID: String get() = BuildConfig.ADMOB_BANNER_ID
    val INTERSTITIAL_AD_UNIT_ID: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /**
     * Preload an interstitial ad so it's ready when needed.
     * Call this early (e.g., on app startup or after showing an ad).
     */
    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial ad preloaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Check if enough time has elapsed to show an interstitial.
     */
    fun shouldShowInterstitial(adsRemoved: Boolean): Boolean {
        if (adsRemoved) return false
        return (System.currentTimeMillis() - lastInterstitialShownAt) > COOLDOWN_MS
    }

    /**
     * Attempt to show an interstitial ad after a successful operation.
     * Returns true if the ad was shown, false if not available or on cooldown.
     */
    fun showInterstitialIfReady(
        activity: Activity,
        adsRemoved: Boolean,
        onAdDismissed: () -> Unit = {}
    ): Boolean {
        if (!shouldShowInterstitial(adsRemoved)) {
            onAdDismissed()
            return false
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Interstitial not loaded yet")
            preloadInterstitial(activity)
            onAdDismissed()
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                interstitialAd = null
                preloadInterstitial(activity) // Preload next one
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial shown")
                recordInterstitialShown()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.d(TAG, "Interstitial failed to show: ${adError.message}")
                interstitialAd = null
                preloadInterstitial(activity)
                onAdDismissed()
            }
        }

        ad.show(activity)
        return true
    }

    /**
     * Record that an interstitial was just shown.
     * Resets the cooldown timer.
     */
    fun recordInterstitialShown() {
        lastInterstitialShownAt = System.currentTimeMillis()
    }
}
