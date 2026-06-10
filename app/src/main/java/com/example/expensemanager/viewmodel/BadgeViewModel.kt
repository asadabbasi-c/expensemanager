package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.Income
import com.example.expensemanager.data.model.SavingGoal
import com.example.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

enum class BadgeTier(val emoji: String, val label: String, val minScore: Double) {
    EXTRAORDINARY("🏆", "Extraordinary", 90.0),
    GOOD("🥇", "Good", 75.0),
    NORMAL("🥈", "Normal", 50.0),
    BAD("🥉", "Bad", 25.0),
    WORST("💀", "Worst", 0.0);

    companion object {
        fun forScore(score: Double): BadgeTier =
            values().firstOrNull { score >= it.minScore } ?: WORST
    }
}

data class MonthScore(
    val month: String,
    val totalScore: Double,
    val budgetAdherence: Double,
    val savingsRate: Double,
    val dailyDiscipline: Double,
    val consistency: Double
) {
    val tier: BadgeTier get() = BadgeTier.forScore(totalScore)
}

class BadgeViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val labelSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    private val _selectedMonth = MutableStateFlow(monthSdf.format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val selectedMonthLabel: StateFlow<String> = _selectedMonth.map { month ->
        runCatching { labelSdf.format(monthSdf.parse(month)!!) }.getOrDefault(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allIncome: StateFlow<List<Income>> = repository.getAllIncome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allGoals: StateFlow<List<SavingGoal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val scoresByMonth: StateFlow<Map<String, MonthScore>> =
        combine(allExpenses, allIncome, allGoals) { expenses, income, goals ->
            val months = (expenses.map { monthOf(it.date) } + income.map { it.month } + goals.map { it.month })
                .filter { it.isNotBlank() }
                .toSortedSet()
            months.associateWith { month -> computeMonthScore(month, expenses, income, goals) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentScore: StateFlow<MonthScore?> = combine(scoresByMonth, _selectedMonth) { scores, month ->
        scores[month] ?: computeMonthScore(month, allExpenses.value, allIncome.value, allGoals.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val monthHistory: StateFlow<List<MonthScore>> = combine(scoresByMonth, _selectedMonth) { scores, month ->
        scores.values
            .filter { it.month <= month }
            .sortedByDescending { it.month }
            .take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateMonth(delta: Int) {
        val cal = Calendar.getInstance()
        cal.time = runCatching { monthSdf.parse(_selectedMonth.value)!! }.getOrDefault(Date())
        cal.add(Calendar.MONTH, delta)
        _selectedMonth.value = monthSdf.format(cal.time)
    }

    private fun monthOf(date: String): String = if (date.length >= 7) date.substring(0, 7) else ""

    private fun computeMonthScore(
        month: String,
        allExpenses: List<Expense>,
        allIncome: List<Income>,
        allGoals: List<SavingGoal>
    ): MonthScore {
        val goal = allGoals.firstOrNull { it.month == month }
        val monthExpenses = allExpenses.filter { monthOf(it.date) == month }
        val totalSpent = monthExpenses.sumOf { it.amount }
        val extraIncome = allIncome.filter { it.month == month }.sumOf { it.amount }

        val incomeTarget = goal?.incomeTarget ?: 0.0
        val goalAmount = goal?.goalAmount ?: 0.0
        val savingsPercentTarget = goal?.savingsPercent ?: 15.0
        val monthlyLimit = (incomeTarget - goalAmount).coerceAtLeast(0.0)
        val totalIncome = incomeTarget + extraIncome

        // ── Budget adherence (35%) ──────────────────────────────────────────────
        val budgetAdherence = if (monthlyLimit <= 0.0) {
            50.0 // no budget set — neutral score
        } else {
            val ratio = totalSpent / monthlyLimit
            ((1.0 - (ratio - 1.0).coerceAtLeast(0.0)) * 100.0).coerceIn(0.0, 100.0)
        }

        // ── Savings rate (30%) ───────────────────────────────────────────────────
        val savingsRate = if (totalIncome <= 0.0) {
            50.0 // no income data — neutral score
        } else {
            val actualRate = ((totalIncome - totalSpent) / totalIncome) * 100.0
            ((actualRate / savingsPercentTarget) * 100.0).coerceIn(0.0, 100.0)
        }

        // ── Daily discipline (25%) ──────────────────────────────────────────────
        val cal = Calendar.getInstance()
        runCatching { cal.time = monthSdf.parse(month)!! }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = monthSdf.format(Date())
        val daysToConsider = if (month == today) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        } else {
            daysInMonth
        }
        val dailyLimit = if (monthlyLimit > 0.0) monthlyLimit / daysInMonth else 0.0
        val spentByDay = monthExpenses.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.amount } }

        val dailyDiscipline = if (dailyLimit <= 0.0 || daysToConsider <= 0) {
            50.0
        } else {
            val daysWithinLimit = (1..daysToConsider).count { day ->
                val dateKey = "%s-%02d".format(month, day)
                (spentByDay[dateKey] ?: 0.0) <= dailyLimit
            }
            (daysWithinLimit.toDouble() / daysToConsider.toDouble() * 100.0).coerceIn(0.0, 100.0)
        }

        // ── Consistency (10%) ────────────────────────────────────────────────────
        val dailySpends = (1..daysToConsider).map { day ->
            val dateKey = "%s-%02d".format(month, day)
            spentByDay[dateKey] ?: 0.0
        }
        val consistency = if (dailySpends.size < 2) {
            100.0
        } else {
            val mean = dailySpends.average()
            if (mean <= 0.0) {
                100.0
            } else {
                val variance = dailySpends.sumOf { (it - mean) * (it - mean) } / dailySpends.size
                val stdDev = sqrt(variance)
                val coefficientOfVariation = stdDev / mean
                (100.0 - coefficientOfVariation * 100.0).coerceIn(0.0, 100.0)
            }
        }

        val total = budgetAdherence * 0.35 + savingsRate * 0.30 + dailyDiscipline * 0.25 + consistency * 0.10

        return MonthScore(
            month            = month,
            totalScore       = total,
            budgetAdherence  = budgetAdherence,
            savingsRate      = savingsRate,
            dailyDiscipline  = dailyDiscipline,
            consistency      = consistency
        )
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BadgeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BadgeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
