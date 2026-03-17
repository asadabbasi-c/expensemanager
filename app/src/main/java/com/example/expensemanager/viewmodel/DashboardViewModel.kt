package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Existing flows ────────────────────────────────────────────────────────

    val expensesByCategory: StateFlow<Map<String, Double>> = combine(
        allExpenses, allCategories
    ) { expenses, categories ->
        val categoryMap = categories.associateBy { it.id }
        expenses
            .groupBy { expense -> categoryMap[expense.categoryId]?.name ?: "Other" }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val monthlyTotals: StateFlow<Map<String, Double>> = allExpenses.map { expenses ->
        val result = mutableMapOf<String, Double>()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val labelSdf = SimpleDateFormat("MMM yy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val months = mutableListOf<Pair<String, String>>()
        repeat(6) {
            months.add(0, Pair(sdf.format(calendar.time), labelSdf.format(calendar.time)))
            calendar.add(Calendar.MONTH, -1)
        }
        months.forEach { (key, label) ->
            result[label] = expenses.filter { it.date.startsWith(key) }.sumOf { it.amount }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentMonthTotal: StateFlow<Double> = allExpenses.map { expenses ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        expenses.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoryColors: StateFlow<Map<String, String>> = allCategories.map { categories ->
        categories.associate { it.name to it.color }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ── Phase 2: new metrics ──────────────────────────────────────────────────

    /** Daily average spend this calendar month */
    val dailyAverage: StateFlow<Double> = allExpenses.map { expenses ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val total = expenses.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        total / day
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Day with the highest total spend this month — Pair(date string, amount) */
    val highestSpendingDay: StateFlow<Pair<String, Double>?> = allExpenses.map { expenses ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        expenses
            .filter { it.date.startsWith(currentMonth) }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Projected end-of-month total based on current daily pace */
    val spendingVelocity: StateFlow<Double> = allExpenses.map { expenses ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val total = expenses.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (total / day) * daysInMonth
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Top 5 merchants by total spend (all time) */
    val topMerchants: StateFlow<List<Pair<String, Double>>> = allExpenses.map { expenses ->
        expenses
            .filter { !it.merchant.isNullOrBlank() && it.merchant != "Unknown" }
            .groupBy { it.merchant!! }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Phase 2: drill-down ───────────────────────────────────────────────────

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val drillDownExpenses: StateFlow<List<Expense>> = combine(
        allExpenses, allCategories, _selectedCategory
    ) { expenses, categories, selected ->
        if (selected == null) emptyList()
        else {
            val categoryMap = categories.associateBy { it.id }
            expenses.filter { categoryMap[it.categoryId]?.name == selected }
                .sortedByDescending { it.date }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(name: String?) {
        _selectedCategory.value = name
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
