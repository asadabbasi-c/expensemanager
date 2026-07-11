package com.example.expensemanager.monetization

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null

    private val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)

    // Minimum gap between two interstitials so they never stack back-to-back.
    private val MIN_INTERVAL_MS = 30 * 1000L

    init {
        load()
    }

    private fun load() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            "ca-app-pub-4989909437303771/9128959719",
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Shows an interstitial ad (subject to [MIN_INTERVAL_MS] spacing), then calls [onComplete].
     * Falls back to [onComplete] immediately if the cooldown hasn't elapsed or no ad is ready.
     */
    fun showAdThenRun(activity: Activity, onComplete: () -> Unit) {
        val lastShown = prefs.getLong("interstitial_last_shown", 0L)
        if (System.currentTimeMillis() - lastShown < MIN_INTERVAL_MS) {
            onComplete()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onComplete()
            load()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                prefs.edit().putLong("interstitial_last_shown", System.currentTimeMillis()).apply()
                onComplete()
                load()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onComplete()
                load()
            }
        }
        ad.show(activity)
        interstitialAd = null
    }

    /**
     * Tracks a per-feature entry counter and shows an interstitial on every
     * 2nd entry for that [key], then calls [onComplete].
     */
    fun showAdEveryOtherThenRun(activity: Activity, key: String, onComplete: () -> Unit) {
        val countKey = "entry_count_$key"
        val count = prefs.getInt(countKey, 0) + 1
        prefs.edit().putInt(countKey, count).apply()

        if (count % 2 == 0) {
            showAdThenRun(activity, onComplete)
        } else {
            onComplete()
        }
    }
}
