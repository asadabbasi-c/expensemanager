package com.example.expensemanager.data.settings

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Currency
import java.util.Locale

class AppSettings(private val context: Context) {
    private val prefs = context.getSharedPreferences("walletly_settings", Context.MODE_PRIVATE)

    fun getCurrencySymbol(): String {
        // If user has ever explicitly set a currency, use it
        if (prefs.contains("currency_symbol")) {
            return prefs.getString("currency_symbol", null) ?: detectDefault()
        }
        // First launch: auto-detect and persist so it doesn't re-detect every time
        val detected = detectDefault()
        prefs.edit().putString("currency_symbol", detected).apply()
        return detected
    }

    fun setCurrencySymbol(symbol: String) =
        prefs.edit().putString("currency_symbol", symbol).apply()

    /** Last yyyy-MM month for which recurring incomes were carried forward. */
    fun getLastRecurringIncomeMonth(): String? =
        prefs.getString("recurring_income_last_month", null)

    fun setLastRecurringIncomeMonth(month: String) =
        prefs.edit().putString("recurring_income_last_month", month).apply()

    private fun detectDefault(): String {
        val tm = runCatching {
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        }.getOrNull()

        // 1. Try SIM home country (most reliable — no permission needed)
        tm?.simCountryIso?.uppercase()?.takeIf { it.length == 2 }?.let { iso ->
            runCatching { Currency.getInstance(Locale("", iso)).currencyCode }
                .getOrNull()?.let { return it }
        }

        // 2. Try registered network country
        tm?.networkCountryIso?.uppercase()?.takeIf { it.length == 2 }?.let { iso ->
            runCatching { Currency.getInstance(Locale("", iso)).currencyCode }
                .getOrNull()?.let { return it }
        }

        // 3. Fall back to device locale
        return runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }
            .getOrDefault("PKR")
    }
}
