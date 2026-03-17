package com.example.expensemanager.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.repository.ExpenseRepository
import com.example.expensemanager.sms.ParsedSms
import com.example.expensemanager.sms.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SmsViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _parsedTransactions = MutableStateFlow<List<ParsedSms>>(emptyList())
    val parsedTransactions: StateFlow<List<ParsedSms>> = _parsedTransactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
    }

    fun loadSmsTransactions(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val transactions = withContext(Dispatchers.IO) {
                    readBankSms(context.contentResolver)
                }
                _parsedTransactions.value = transactions
            } catch (e: SecurityException) {
                _errorMessage.value = "SMS permission denied. Please grant READ_SMS permission."
            } catch (e: Throwable) {
                _errorMessage.value = "Error reading SMS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun readBankSms(contentResolver: ContentResolver): List<ParsedSms> {
        val results = mutableListOf<ParsedSms>()

        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "address", "body", "date")
            val sortOrder = "date DESC"

            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val addressIndex = it.getColumnIndexOrThrow("address")
                val bodyIndex = it.getColumnIndexOrThrow("body")
                val dateIndex = it.getColumnIndexOrThrow("date")

                var count = 0
                while (it.moveToNext() && count < 500 && results.size < 50) {
                    try {
                        val address = it.getString(addressIndex) ?: ""
                        val body = it.getString(bodyIndex) ?: ""
                        val timestamp = it.getLong(dateIndex)

                        if (SmsParser.isBankSms(address, body)) {
                            val parsed = SmsParser.parse(body, address, timestamp)
                            if (parsed != null) {
                                results.add(parsed)
                            }
                        }
                    } catch (_: Exception) { /* skip malformed row */ }
                    count++
                }
            }
        } catch (e: Exception) {
            throw e
        }

        return results
    }

    fun addParsedAsExpense(parsed: ParsedSms, categoryId: Long) {
        viewModelScope.launch {
            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val expense = Expense(
                amount = parsed.amount,
                categoryId = categoryId,
                description = "SMS: ${parsed.merchant}",
                location = "",
                address = "",
                date = parsed.date,
                time = timeSdf.format(Date()),
                source = "sms",
                bankName = parsed.bankName,
                merchant = parsed.merchant
            )
            repository.insertExpense(expense)

            // Remove from the list once added
            _parsedTransactions.value = _parsedTransactions.value.filter { it != parsed }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SmsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SmsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
