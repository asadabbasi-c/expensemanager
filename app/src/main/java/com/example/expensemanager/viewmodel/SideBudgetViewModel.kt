package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.SideBudget
import com.example.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SideBudgetViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val activeBudget: StateFlow<SideBudget?> = repository.getActiveSideBudget()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class DayStatus(
        val date: String,
        val spent: Double,
        val limit: Double,
        val isToday: Boolean,
        val isFuture: Boolean
    )

    data class SideBudgetSummary(
        val budget: SideBudget,
        val totalSpent: Double,
        val remaining: Double,
        val percentUsed: Double,
        val daysTotal: Int,
        val daysPassed: Int,
        val daysLeft: Int,
        val originalDailyLimit: Double,
        val adjustedDailyLimit: Double,
        val projectedTotal: Double,
        val dayStatuses: List<DayStatus>
    )

    val summary: StateFlow<SideBudgetSummary?> = combine(activeBudget, allExpenses) { budget, expenses ->
        if (budget == null) return@combine null

        val start = runCatching { dateSdf.parse(budget.startDate) }.getOrNull() ?: return@combine null
        val end   = runCatching { dateSdf.parse(budget.endDate) }.getOrNull() ?: return@combine null
        val today = runCatching { dateSdf.parse(dateSdf.format(Date())) }.getOrNull() ?: Date()

        val msPerDay = 86_400_000L
        val daysTotal = (((end.time - start.time) / msPerDay).toInt() + 1).coerceAtLeast(1)

        // Number of days strictly before "today" that fall within the budget period.
        val daysPassed = (((today.time - start.time) / msPerDay).toInt()).coerceIn(0, daysTotal)
        val daysLeft   = (daysTotal - daysPassed).coerceIn(0, daysTotal)

        val expensesByDate = expenses
            .filter { it.includeInMain && it.date >= budget.startDate && it.date <= budget.endDate }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val totalSpent = expensesByDate.values.sum()
        val remaining  = (budget.totalAmount - totalSpent).coerceAtLeast(0.0)
        val percentUsed = if (budget.totalAmount > 0) (totalSpent / budget.totalAmount).coerceIn(0.0, 1.5) else 0.0

        val originalDailyLimit = budget.totalAmount / daysTotal
        val adjustedDailyLimit = if (daysLeft > 0) remaining / daysLeft else 0.0

        val avgDailySpend = if (daysPassed > 0) totalSpent / daysPassed else 0.0
        val projectedTotal = if (daysPassed > 0) avgDailySpend * daysTotal else budget.totalAmount

        val cal = Calendar.getInstance()
        cal.time = start
        val dayStatuses = mutableListOf<DayStatus>()
        for (i in 0 until daysTotal) {
            val dateStr = dateSdf.format(cal.time)
            val spent   = expensesByDate[dateStr] ?: 0.0
            val isToday = dateStr == dateSdf.format(today)
            val isFuture = cal.time.after(today)
            dayStatuses.add(DayStatus(dateStr, spent, originalDailyLimit, isToday, isFuture))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        SideBudgetSummary(
            budget             = budget,
            totalSpent         = totalSpent,
            remaining          = remaining,
            percentUsed        = percentUsed,
            daysTotal          = daysTotal,
            daysPassed         = daysPassed,
            daysLeft           = daysLeft,
            originalDailyLimit = originalDailyLimit,
            adjustedDailyLimit = adjustedDailyLimit,
            projectedTotal     = projectedTotal,
            dayStatuses        = dayStatuses
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveSideBudget(name: String, totalAmount: Double, startDate: String, endDate: String) {
        viewModelScope.launch {
            val current = activeBudget.value
            repository.upsertSideBudget(
                SideBudget(
                    id          = current?.id ?: 0,
                    name        = name,
                    totalAmount = totalAmount,
                    startDate   = startDate,
                    endDate     = endDate,
                    isActive    = true
                )
            )
        }
    }

    fun endSideBudget(budget: SideBudget) {
        viewModelScope.launch {
            repository.updateSideBudget(budget.copy(isActive = false))
        }
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SideBudgetViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SideBudgetViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
