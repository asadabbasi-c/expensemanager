package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensemanager.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _currencySymbol = MutableStateFlow(appSettings.getCurrencySymbol())
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    fun setCurrency(symbol: String) {
        appSettings.setCurrencySymbol(symbol)
        _currencySymbol.value = symbol
    }

    class Factory(private val appSettings: AppSettings) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(appSettings) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
