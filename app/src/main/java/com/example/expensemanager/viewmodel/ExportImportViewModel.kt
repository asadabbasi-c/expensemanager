package com.example.expensemanager.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.repository.ExpenseRepository
import com.example.expensemanager.data.service.DataTransferService
import com.example.expensemanager.data.service.DataTransferService.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportImportViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val allExpenses  = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val allCategories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCount: StateFlow<Int> = allExpenses.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    // ── Export ────────────────────────────────────────────────────────────────

    fun exportToXlsx(uri: Uri, context: Context) = export(uri, context, "Excel") { out ->
        DataTransferService.exportToXlsx(allExpenses.value, allCategories.value, out)
    }

    fun exportToPdf(uri: Uri, context: Context) = export(uri, context, "PDF") { out ->
        DataTransferService.exportToPdf(allExpenses.value, allCategories.value, out)
    }

    fun exportToCsv(uri: Uri, context: Context) = export(uri, context, "CSV") { out ->
        DataTransferService.exportToCsv(allExpenses.value, allCategories.value, out)
    }

    private fun export(uri: Uri, context: Context, label: String, block: (java.io.OutputStream) -> Unit) {
        viewModelScope.launch {
            _isWorking.value = true
            _exportResult.value = null
            try {
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri)
                        ?: throw Exception("Could not open output file")
                    out.use { block(it) }
                }
                _exportResult.value = ExportResult.Success(
                    "$label exported successfully — ${allExpenses.value.size} expenses"
                )
            } catch (e: Exception) {
                _exportResult.value = ExportResult.Error("$label export failed: ${e.message}")
            } finally {
                _isWorking.value = false
            }
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    fun importFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isWorking.value = true
            _importResult.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val fileName = getFileName(context, uri) ?: ""
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("Could not open file")

                    stream.use {
                        when {
                            fileName.endsWith(".xlsx", ignoreCase = true) ->
                                DataTransferService.importFromXlsx(it)
                            fileName.endsWith(".csv", ignoreCase = true) ->
                                DataTransferService.importFromCsv(it)
                            else ->
                                throw Exception("Unsupported format. Please use .xlsx or .csv")
                        }
                    }
                }

                // Persist valid rows
                if (result.rows.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val cats = allCategories.value
                        result.rows.forEach { row ->
                            val catId = cats.find { it.name.equals(row.category, ignoreCase = true) }?.id
                                ?: cats.find { it.name.equals("Other", ignoreCase = true) }?.id
                                ?: cats.firstOrNull()?.id ?: 1L
                            repository.insertExpense(
                                Expense(
                                    amount      = row.amount,
                                    categoryId  = catId,
                                    description = row.description ?: "",
                                    location    = row.location ?: "",
                                    date        = row.date,
                                    time        = row.time,
                                    source      = row.source ?: "manual",
                                    bankName    = row.bankName,
                                    merchant    = row.merchant
                                )
                            )
                        }
                    }
                }
                _importResult.value = result
            } catch (e: Exception) {
                _importResult.value = ImportResult(
                    rows          = emptyList(),
                    errors        = listOf(e.message ?: "Unknown error"),
                    totalRowsRead = 0
                )
            } finally {
                _isWorking.value = false
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
        } ?: uri.lastPathSegment
    }

    fun clearExportResult() { _exportResult.value = null }
    fun clearImportResult() { _importResult.value = null }

    // ── Result sealed class ───────────────────────────────────────────────────

    sealed class ExportResult {
        data class Success(val message: String) : ExportResult()
        data class Error(val message: String)   : ExportResult()
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExportImportViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExportImportViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
