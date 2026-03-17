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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expensesByCategory: StateFlow<Map<String, Double>> = combine(
        allExpenses, allCategories
    ) { expenses, categories ->
        val categoryMap = categories.associateBy { it.id }
        expenses
            .groupBy { expense ->
                categoryMap[expense.categoryId]?.name ?: "Other"
            }
            .mapValues { (_, expenseList) ->
                expenseList.sumOf { it.amount }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val monthlyTotals: StateFlow<Map<String, Double>> = allExpenses.map { expenses ->
        val result = mutableMapOf<String, Double>()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val labelSdf = SimpleDateFormat("MMM yy", Locale.getDefault())

        // Get last 6 months
        val calendar = Calendar.getInstance()
        val months = mutableListOf<Pair<String, String>>() // (key, label)
        repeat(6) {
            val key = sdf.format(calendar.time)
            val label = labelSdf.format(calendar.time)
            months.add(0, Pair(key, label))
            calendar.add(Calendar.MONTH, -1)
        }

        months.forEach { (key, label) ->
            val total = expenses
                .filter { it.date.startsWith(key) }
                .sumOf { it.amount }
            result[label] = total
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val currentMonthTotal: StateFlow<Double> = allExpenses.map { expenses ->
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonth = sdf.format(Date())
        expenses
            .filter { it.date.startsWith(currentMonth) }
            .sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val categoryColors: StateFlow<Map<String, String>> = allCategories.map { categories ->
        categories.associate { it.name to it.color }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

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
