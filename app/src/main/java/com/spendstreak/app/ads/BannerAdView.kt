package com.spendstreak.app.ads

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val BANNER_AD_UNIT_ID = "ca-app-pub-6165653121014687/2402284086"

// Placed only on secondary screens (Settings, Reports, History) — never on Dashboard or
// Add Expense, so the app's core "log something" flow is never interrupted by an ad.
// Renders nothing until the consent flow + SDK init (see AdsInitializer.kt) complete.
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var adsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        if (activity == null) {
            Log.w(ADS_LOG_TAG, "LocalContext.current is not an Activity (was: ${context::class.java.name}) — skipping ad init")
            return@LaunchedEffect
        }
        initializeAds(activity) { adsReady = true }
    }

    if (adsReady) {
        AndroidView(
            modifier = modifier,
            factory = { viewContext ->
                AdView(viewContext).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BANNER_AD_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d(ADS_LOG_TAG, "onAdLoaded — banner should now be visible")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(ADS_LOG_TAG, "onAdFailedToLoad — code=${error.code} domain=${error.domain} message=${error.message}")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    } else {
        Log.d(ADS_LOG_TAG, "adsReady=false — AdView not yet created")
    }
}
