package com.example.expensemanager.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.repository.ExpenseRepository
import com.example.expensemanager.receipt.ParsedReceipt
import com.example.expensemanager.receipt.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ReceiptViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val _imagePath = MutableStateFlow<String?>(null)
    val imagePath: StateFlow<String?> = _imagePath.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _parsedReceipt = MutableStateFlow<ParsedReceipt?>(null)
    val parsedReceipt: StateFlow<ParsedReceipt?> = _parsedReceipt.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun setImageAndProcess(path: String) {
        _imagePath.value = path
        _parsedReceipt.value = null
        _errorMessage.value = null
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val text = withContext(Dispatchers.IO) { runOcr(path) }
                _parsedReceipt.value = ReceiptParser.parse(text)
            } catch (e: Exception) {
                _errorMessage.value = "Could not read receipt: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private suspend fun runOcr(imagePath: String): String {
        val bitmap   = BitmapFactory.decodeFile(imagePath)
            ?: throw Exception("Failed to decode image")
        val inputImg = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(inputImg)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    fun saveExpense(
        amount: Double,
        categoryId: Long,
        description: String,
        merchant: String,
        date: String
    ) {
        viewModelScope.launch {
            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            repository.insertExpense(
                Expense(
                    amount      = amount,
                    categoryId  = categoryId,
                    description = description.trim(),
                    merchant    = merchant.trim().ifBlank { null },
                    date        = date,
                    time        = timeSdf.format(Date()),
                    source      = "receipt",
                    imagePath   = _imagePath.value
                )
            )
            _isSaved.value = true
        }
    }

    fun reset() {
        _imagePath.value    = null
        _parsedReceipt.value = null
        _errorMessage.value  = null
        _isProcessing.value  = false
        _isSaved.value       = false
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReceiptViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
