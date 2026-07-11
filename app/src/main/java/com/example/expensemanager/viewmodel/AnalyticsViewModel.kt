package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.Income
import com.example.expensemanager.data.model.RecurringExpense
import com.example.expensemanager.data.model.SavingGoal
import com.example.expensemanager.data.model.SideBudget
import com.example.expensemanager.data.model.SideProject
import com.example.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pro in-depth analytics: savings, budget adherence, emergency budgets,
 * project metrics, income breakdown and spending insights.
 */
class AnalyticsViewModel(
    repository: ExpenseRepository
) : ViewModel() {

    private val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val labelSdf = SimpleDateFormat("MMM yy", Locale.getDefault())
    private val dateSdf  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Expenses that count toward main totals (excludes project-only). */
    private val mainExpenses: StateFlow<List<Expense>> = allExpenses.map { list ->
        list.filter { it.includeInMain }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allIncome: StateFlow<List<Income>> = repository.getAllIncome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allGoals: StateFlow<List<SavingGoal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allProjects: StateFlow<List<SideProject>> = repository.getAllSideProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allSideBudgets: StateFlow<List<SideBudget>> = repository.getAllSideBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allRecurring: StateFlow<List<RecurringExpense>> = repository.getAllRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun lastMonths(count: Int): List<Pair<String, String>> {
        val cal = Calendar.getInstance()
        val months = mutableListOf<Pair<String, String>>()
        repeat(count) {
            months.add(0, monthSdf.format(cal.time) to labelSdf.format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        return months
    }

    // ── Month-by-month savings & budget adherence ─────────────────────────────

    data class MonthMetric(
        val key: String,       // yyyy-MM
        val label: String,     // "Jul 26"
        val spent: Double,
        val budget: Double,    // 0.0 when no goal was set that month
        val income: Double,
        val savings: Double
    )

    val monthMetrics: StateFlow<List<MonthMetric>> = combine(
        mainExpenses, allIncome, allGoals
    ) { expenses, incomeList, goals ->
        lastMonths(6).map { (key, label) ->
            val spent  = expenses.filter { it.date.startsWith(key) }.sumOf { it.amount }
            val goal   = goals.find { it.month == key }
            val extra  = incomeList.filter { it.month == key }.sumOf { it.amount }
            val income = (goal?.incomeTarget ?: 0.0) + extra
            // Same budget formula the dashboard uses
            val budget = if (goal != null) goal.incomeTarget + extra - goal.goalAmount else 0.0
            MonthMetric(key, label, spent, budget, income, income - spent)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Savings goal progress (current month) ─────────────────────────────────

    data class SavingsGoalProgress(
        val target: Double,
        val actual: Double,     // income so far − spent so far
        val percent: Double
    )

    val savingsGoalProgress: StateFlow<SavingsGoalProgress?> = combine(
        monthMetrics, allGoals
    ) { metrics, goals ->
        val currentKey = monthSdf.format(Date())
        val goal = goals.find { it.month == currentKey } ?: return@combine null
        if (goal.goalAmount <= 0) return@combine null
        val current = metrics.find { it.key == currentKey } ?: return@combine null
        SavingsGoalProgress(
            target  = goal.goalAmount,
            actual  = current.savings,
            percent = (current.savings / goal.goalAmount).coerceIn(-1.0, 1.5)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Project metrics ───────────────────────────────────────────────────────

    data class ProjectMetric(
        val project: SideProject,
        val spent: Double,
        val remaining: Double,
        val percentUsed: Double,
        val expenseCount: Int,
        val sharedWithMain: Double,   // portion of spent that also counts in main
        val burnDaysLeft: Int?        // days until budget runs out at current pace
    )

    val projectMetrics: StateFlow<List<ProjectMetric>> = combine(
        allProjects, allExpenses
    ) { projects, expenses ->
        val msPerDay = 86_400_000L
        val today = Date()
        projects.map { project ->
            val projectExpenses = expenses.filter { it.projectId == project.id }
            val spent = projectExpenses.sumOf { it.amount }
            val remaining = (project.budget - spent).coerceAtLeast(0.0)

            // Burn rate: average daily spend since the project's first expense
            val burnDaysLeft = if (project.hasBudget && spent > 0 && remaining > 0) {
                val firstDate = projectExpenses.minOfOrNull { it.date }
                    ?.let { runCatching { dateSdf.parse(it) }.getOrNull() }
                if (firstDate != null) {
                    val daysActive = (((today.time - firstDate.time) / msPerDay).toInt() + 1).coerceAtLeast(1)
                    val dailyRate = spent / daysActive
                    if (dailyRate > 0) (remaining / dailyRate).toInt() else null
                } else null
            } else null

            ProjectMetric(
                project        = project,
                spent          = spent,
                remaining      = remaining,
                percentUsed    = if (project.budget > 0) spent / project.budget else 0.0,
                expenseCount   = projectExpenses.size,
                sharedWithMain = projectExpenses.filter { it.includeInMain }.sumOf { it.amount },
                burnDaysLeft   = burnDaysLeft
            )
        }.sortedByDescending { it.spent }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Emergency budget history ──────────────────────────────────────────────

    data class EmergencyMetric(
        val budget: SideBudget,
        val spent: Double,
        val percentUsed: Double,
        val isRunning: Boolean
    )

    val emergencyMetrics: StateFlow<List<EmergencyMetric>> = combine(
        allSideBudgets, mainExpenses
    ) { budgets, expenses ->
        val today = dateSdf.format(Date())
        budgets.map { budget ->
            val spent = expenses
                .filter { it.date >= budget.startDate && it.date <= budget.endDate }
                .sumOf { it.amount }
            EmergencyMetric(
                budget      = budget,
                spent       = spent,
                percentUsed = if (budget.totalAmount > 0) spent / budget.totalAmount else 0.0,
                isRunning   = budget.isActive && today >= budget.startDate && today <= budget.endDate
            )
        }.sortedByDescending { it.budget.startDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Income breakdown (current month) ──────────────────────────────────────

    data class IncomeStats(
        val total: Double,
        val recurring: Double,
        val oneTime: Double,
        val sources: Int
    )

    val incomeStats: StateFlow<IncomeStats> = allIncome.map { incomeList ->
        val currentKey = monthSdf.format(Date())
        val thisMonth  = incomeList.filter { it.month == currentKey }
        IncomeStats(
            total     = thisMonth.sumOf { it.amount },
            recurring = thisMonth.filter { it.recurring }.sumOf { it.amount },
            oneTime   = thisMonth.filter { !it.recurring }.sumOf { it.amount },
            sources   = thisMonth.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IncomeStats(0.0, 0.0, 0.0, 0))

    // ── End-of-month forecast ─────────────────────────────────────────────────

    data class Forecast(
        val projected: Double,   // spend at current pace by month end
        val budget: Double,      // 0.0 = no budget set this month
        val overBy: Double       // projected − budget (negative = under)
    )

    val forecast: StateFlow<Forecast?> = combine(
        mainExpenses, monthMetrics
    ) { expenses, metrics ->
        val currentKey = monthSdf.format(Date())
        val spent = expenses.filter { it.date.startsWith(currentKey) }.sumOf { it.amount }
        if (spent <= 0) return@combine null
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val projected = (spent / day) * daysInMonth
        val budget = metrics.find { it.key == currentKey }?.budget ?: 0.0
        Forecast(projected, budget, projected - budget)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Category trends (6 months) ────────────────────────────────────────────

    data class CategoryTrend(
        val name: String,
        val color: String,
        val monthly: List<Double>,       // aligned with the last 6 month labels
        val thisMonth: Double,
        val lastMonth: Double
    )

    val categoryTrends: StateFlow<List<CategoryTrend>> = combine(
        mainExpenses, allCategories
    ) { expenses, categories ->
        val months = lastMonths(6)
        val categoryMap = categories.associateBy { it.id }
        expenses
            .groupBy { categoryMap[it.categoryId]?.name ?: "Other" }
            .map { (name, list) ->
                val monthly = months.map { (key, _) ->
                    list.filter { it.date.startsWith(key) }.sumOf { it.amount }
                }
                CategoryTrend(
                    name      = name,
                    color     = categories.find { it.name == name }?.color ?: "#94A3B8",
                    monthly   = monthly,
                    thisMonth = monthly.last(),
                    lastMonth = monthly[monthly.size - 2]
                )
            }
            .filter { trend -> trend.monthly.any { it > 0 } }
            .sortedByDescending { it.thisMonth + it.lastMonth }
            .take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── 12-month spending history ─────────────────────────────────────────────

    val spendingHistory: StateFlow<List<Pair<String, Double>>> = mainExpenses.map { expenses ->
        lastMonths(12).map { (key, label) ->
            label to expenses.filter { it.date.startsWith(key) }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Subscription analysis (from recurring expenses, normalized monthly) ──

    data class SubscriptionStats(
        val monthlyTotal: Double,
        val count: Int,
        val topItems: List<Pair<String, Double>>,   // name → monthly cost
        val shareOfBudget: Double?                  // fraction of current budget, null when no budget
    )

    val subscriptionStats: StateFlow<SubscriptionStats?> = combine(
        allRecurring, monthMetrics
    ) { recurring, metrics ->
        val active = recurring.filter { it.isActive }
        if (active.isEmpty()) return@combine null
        fun monthlyCost(item: RecurringExpense): Double = when (item.frequency) {
            "daily"  -> item.amount * 30.44
            "weekly" -> item.amount * 4.35
            "yearly" -> item.amount / 12.0
            else     -> item.amount
        }
        val costs = active.map { it.name to monthlyCost(it) }
        val total = costs.sumOf { it.second }
        val budget = metrics.lastOrNull()?.budget ?: 0.0
        SubscriptionStats(
            monthlyTotal  = total,
            count         = active.size,
            topItems      = costs.sortedByDescending { it.second }.take(4),
            shareOfBudget = if (budget > 0) total / budget else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Daily spending heatmap (current month) ────────────────────────────────

    data class DailyHeatmap(
        val daysInMonth: Int,
        val firstWeekdayOffset: Int,     // 0 = week starts on that day (Mon-based)
        val today: Int,
        val spendByDay: Map<Int, Double>,
        val maxSpend: Double
    )

    val dailyHeatmap: StateFlow<DailyHeatmap?> = mainExpenses.map { expenses ->
        val currentKey = monthSdf.format(Date())
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        // Monday-first offset for the calendar grid
        val offset = ((cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY) + 7) % 7

        val spendByDay = expenses
            .filter { it.date.startsWith(currentKey) && it.date.length >= 10 }
            .groupBy { it.date.substring(8, 10).toIntOrNull() ?: 0 }
            .filterKeys { it in 1..daysInMonth }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        if (spendByDay.isEmpty()) null
        else DailyHeatmap(
            daysInMonth        = daysInMonth,
            firstWeekdayOffset = offset,
            today              = today,
            spendByDay         = spendByDay,
            maxSpend           = spendByDay.values.maxOrNull() ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Spending insights ─────────────────────────────────────────────────────

    data class Insights(
        val avgTransaction: Double,
        val transactionCount: Int,
        val largestExpense: Expense?,
        val largestCategoryName: String?,
        val busiestWeekday: Pair<String, Double>?,   // avg spend on that weekday
        val topCategoryShift: Triple<String, Double, Double>?  // name, thisMonth, lastMonth
    )

    val insights: StateFlow<Insights> = combine(
        mainExpenses, allCategories
    ) { expenses, categories ->
        val currentKey = monthSdf.format(Date())
        val lastKey = Calendar.getInstance().let {
            it.add(Calendar.MONTH, -1); monthSdf.format(it.time)
        }
        val thisMonth = expenses.filter { it.date.startsWith(currentKey) }
        val lastMonth = expenses.filter { it.date.startsWith(lastKey) }
        val categoryMap = categories.associateBy { it.id }

        val largest = thisMonth.maxByOrNull { it.amount }

        val weekdaySdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val busiest = expenses
            .mapNotNull { e ->
                runCatching { weekdaySdf.format(dateSdf.parse(e.date)!!) }.getOrNull()?.let { it to e.amount }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() / amounts.size }
            .maxByOrNull { it.value }
            ?.toPair()

        val byCatThis = thisMonth.groupBy { categoryMap[it.categoryId]?.name ?: "Other" }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
        val byCatLast = lastMonth.groupBy { categoryMap[it.categoryId]?.name ?: "Other" }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
        val shift = byCatThis
            .map { (name, amt) -> Triple(name, amt, byCatLast[name] ?: 0.0) }
            .maxByOrNull { it.second - it.third }

        Insights(
            avgTransaction      = if (thisMonth.isNotEmpty()) thisMonth.sumOf { it.amount } / thisMonth.size else 0.0,
            transactionCount    = thisMonth.size,
            largestExpense      = largest,
            largestCategoryName = largest?.let { categoryMap[it.categoryId]?.name },
            busiestWeekday      = busiest,
            topCategoryShift    = shift
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        Insights(0.0, 0, null, null, null, null)
    )

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AnalyticsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
