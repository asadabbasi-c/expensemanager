package com.example.expensemanager.monetization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the user's Pro entitlement.
 *
 * Current implementation persists the flag in SharedPreferences.
 * When you integrate Google Play Billing, call [grantPro] from your
 * PurchasesUpdatedListener after a successful acknowledgement, and
 * [revokePro] when a subscription expires (queryPurchasesAsync returns
 * no active subscription on app resume).
 *
 * Play Store product ID to create in Play Console → Monetize → Subscriptions:
 *   smartspend_pro_monthly
 */
class ProManager(context: Context) {

    private val prefs = context.getSharedPreferences("smartspend_entitlement", Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    /** Call after Play Billing confirms a valid active subscription. */
    fun grantPro() {
        prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
        _isPro.value = true
    }

    /** Call when subscription lapses, is cancelled, or is refunded. */
    fun revokePro() {
        prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
        _isPro.value = false
    }

    // ── Dev / testing helper ──────────────────────────────────────────────────
    /** Toggle pro state without going through billing — for internal testing only. */
    fun debugToggle() { if (_isPro.value) revokePro() else grantPro() }

    companion object {
        private const val KEY_IS_PRO = "is_pro"
    }
}
