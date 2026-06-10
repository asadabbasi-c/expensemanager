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
    private val ONE_DAY_MS = 24 * 60 * 60 * 1000L

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
     * Shows an interstitial ad at most once per day, then calls [onComplete].
     * Falls back to [onComplete] immediately if the daily cap is hit or no ad is ready.
     */
    fun showAdThenRun(activity: Activity, onComplete: () -> Unit) {
        val lastShown = prefs.getLong("interstitial_last_shown", 0L)
        if (System.currentTimeMillis() - lastShown < ONE_DAY_MS) {
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
}
