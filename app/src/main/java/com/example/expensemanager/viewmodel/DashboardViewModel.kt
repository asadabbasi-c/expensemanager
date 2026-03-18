package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.Income
import com.example.expensemanager.data.model.SavingGoal
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

    private val allIncome: StateFlow<List<Income>> = repository.getAllIncome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allGoals: StateFlow<List<SavingGoal>> = repository.getAllGoals()
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
        val result   = mutableMapOf<String, Double>()
        val sdf      = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val labelSdf = SimpleDateFormat("MMM yy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val months   = mutableListOf<Pair<String, String>>()
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

    // ── Phase 2: metrics ──────────────────────────────────────────────────────

    val dailyAverage: StateFlow<Double> = allExpenses.map { expenses ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val total = expenses.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val day   = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        total / day
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val highestSpendingDay: StateFlow<Pair<String, Double>?> = allExpenses.map { expenses ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        expenses
            .filter { it.date.startsWith(currentMonth) }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val spendingVelocity: StateFlow<Double> = allExpenses.map { expenses ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val total        = expenses.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
        val cal          = Calendar.getInstance()
        val day          = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val daysInMonth  = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (total / day) * daysInMonth
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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

    // ── Phase 4: goal & income ────────────────────────────────────────────────

    /** Current month's saving goal */
    val currentMonthGoal: StateFlow<SavingGoal?> = allGoals.map { goals ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        goals.find { it.month == currentMonth }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Sum of extra income added for current month */
    val currentMonthExtraIncome: StateFlow<Double> = allIncome.map { incomeList ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        incomeList.filter { it.month == currentMonth }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    sealed class GoalStatus {
        object NoGoal : GoalStatus()
        data class OnTrack(val spent: Double, val budget: Double, val percent: Double) : GoalStatus()
        data class Warning(val spent: Double, val budget: Double, val percent: Double) : GoalStatus()
        data class OverBudget(val spent: Double, val budget: Double, val percent: Double) : GoalStatus()
    }

    val goalStatus: StateFlow<GoalStatus> = combine(
        currentMonthTotal, currentMonthGoal, currentMonthExtraIncome
    ) { spent, goal, extraIncome ->
        if (goal == null) {
            GoalStatus.NoGoal
        } else {
            val budget  = goal.incomeTarget + extraIncome - goal.goalAmount
            val percent = if (budget > 0) spent / budget else Double.MAX_VALUE
            when {
                percent > 1.0  -> GoalStatus.OverBudget(spent, budget, percent)
                percent >= 0.9 -> GoalStatus.Warning(spent, budget, percent)
                else           -> GoalStatus.OnTrack(spent, budget, percent)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalStatus.NoGoal)

    /** How much can be spent per day for rest of the month */
    val dailyBudget: StateFlow<Double> = combine(
        currentMonthTotal, currentMonthGoal, currentMonthExtraIncome
    ) { spent, goal, extraIncome ->
        if (goal == null) return@combine 0.0
        val budget    = goal.incomeTarget + extraIncome - goal.goalAmount
        val remaining = budget - spent
        val cal       = Calendar.getInstance()
        val today     = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMon = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft  = (daysInMon - today + 1).coerceAtLeast(1)
        remaining / daysLeft
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Monthly savings = total income (target + extra) − expenses, for last 6 months */
    val monthlySavings: StateFlow<Map<String, Double>> = combine(
        allExpenses, allIncome, allGoals
    ) { expenses, incomeList, goals ->
        val result   = mutableMapOf<String, Double>()
        val sdf      = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val labelSdf = SimpleDateFormat("MMM yy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val months   = mutableListOf<Pair<String, String>>()
        repeat(6) {
            months.add(0, Pair(sdf.format(calendar.time), labelSdf.format(calendar.time)))
            calendar.add(Calendar.MONTH, -1)
        }
        months.forEach { (key, label) ->
            val monthExpenses    = expenses.filter { it.date.startsWith(key) }.sumOf { it.amount }
            val monthGoal        = goals.find { it.month == key }
            val monthExtraIncome = incomeList.filter { it.month == key }.sumOf { it.amount }
            val monthIncome      = (monthGoal?.incomeTarget ?: 0.0) + monthExtraIncome
            result[label]        = monthIncome - monthExpenses
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Total savings across all months that have a goal set */
    val totalSavings: StateFlow<Double> = combine(
        allExpenses, allIncome, allGoals
    ) { expenses, incomeList, goals ->
        if (goals.isEmpty()) return@combine 0.0
        goals.sumOf { goal ->
            val monthExpenses    = expenses.filter { it.date.startsWith(goal.month) }.sumOf { it.amount }
            val monthExtraIncome = incomeList.filter { it.month == goal.month }.sumOf { it.amount }
            val monthIncome      = goal.incomeTarget + monthExtraIncome
            monthIncome - monthExpenses
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
