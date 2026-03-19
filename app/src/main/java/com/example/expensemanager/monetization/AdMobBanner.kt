package com.example.expensemanager.monetization

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Adaptive banner ad unit for the free tier.
 *
 * ⚠ Ad unit ID below is Google's TEST ID.
 * Replace with your real banner unit ID from AdMob dashboard
 * (Apps → Ad units → Add ad unit → Banner) before publishing.
 *
 * Test ID:  ca-app-pub-3940256099942544/6300978111
 * Real ID:  ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier.fillMaxWidth()) {
    AndroidView(
        modifier = modifier,
        factory  = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
