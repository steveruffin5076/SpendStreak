package com.spendstreak.app.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

const val ADS_LOG_TAG = "SpendStreakAds"

// Safe to call more than once per process — both the UMP SDK's ConsentInformation and
// MobileAds.initialize() are idempotent, so mounting more than one BannerAdView (see
// BannerAdView.kt) never double-shows a consent form or double-initializes the SDK.
// Requests the UMP SDK's consent status, shows the consent form only if required (e.g.
// for EU/UK users), then initializes the Mobile Ads SDK only if canRequestAds() confirms
// consent is actually resolved — this final check is Google's own documented safeguard
// against requesting ads before consent genuinely permits it, not something invented here.
//
// Logs at every decision point (filter Logcat for tag "SpendStreakAds") — this is
// temporary debugging instrumentation added because banners weren't appearing on-device
// and the cause wasn't yet known: is consent info update failing, is canRequestAds()
// coming back false, or does MobileAds.initialize() never complete?
fun initializeAds(activity: Activity, onReady: () -> Unit) {
    val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    val params = ConsentRequestParameters.Builder().build()
    Log.d(ADS_LOG_TAG, "requestConsentInfoUpdate: starting")

    consentInformation.requestConsentInfoUpdate(
        activity,
        params,
        {
            Log.d(ADS_LOG_TAG, "requestConsentInfoUpdate: succeeded")
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                // A non-null formError here means only the form itself failed to show —
                // canRequestAds() below is still the authority on whether it's OK to
                // proceed, matching Google's own quickstart sample.
                Log.d(
                    ADS_LOG_TAG,
                    "loadAndShowConsentFormIfRequired: formError=${formError?.message}, " +
                        "canRequestAds=${consentInformation.canRequestAds()}"
                )
                if (consentInformation.canRequestAds()) {
                    MobileAds.initialize(activity) { status ->
                        Log.d(ADS_LOG_TAG, "MobileAds.initialize complete: $status")
                        onReady()
                    }
                } else {
                    Log.w(ADS_LOG_TAG, "canRequestAds() is false — MobileAds never initialized, banner will never show")
                }
            }
        },
        { requestConsentError ->
            // Consent info update itself failed (e.g. no network). canRequestAds() can
            // still be true here if consent was already resolved in an earlier session.
            Log.w(
                ADS_LOG_TAG,
                "requestConsentInfoUpdate: FAILED — ${requestConsentError.errorCode}: " +
                    "${requestConsentError.message}, canRequestAds=${consentInformation.canRequestAds()}"
            )
            if (consentInformation.canRequestAds()) {
                MobileAds.initialize(activity) { status ->
                    Log.d(ADS_LOG_TAG, "MobileAds.initialize complete: $status")
                    onReady()
                }
            } else {
                Log.w(ADS_LOG_TAG, "canRequestAds() is false after a failed consent update — banner will never show")
            }
        }
    )
}
