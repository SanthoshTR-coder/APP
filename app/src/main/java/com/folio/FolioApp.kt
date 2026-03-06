package com.folio

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FolioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob SDK early (safe to call before Activity)
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            Log.e("FolioApp", "AdMob initialization failed", e)
        }
    }
}
