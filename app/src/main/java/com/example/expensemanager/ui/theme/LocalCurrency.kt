package com.example.expensemanager.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/** Provides the active currency symbol (e.g. "PKR", "USD") throughout the Compose tree. */
val LocalCurrency = staticCompositionLocalOf { "PKR" }
