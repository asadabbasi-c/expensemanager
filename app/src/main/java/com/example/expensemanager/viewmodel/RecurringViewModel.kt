package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.RecurringExpense
import com.example.expensemanager.data.repository.ExpenseRepository
import com.example.expensemanager.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RecurringViewModel(
    private val repository: ExpenseRepository,
    private val appSettings: AppSettings? = null
) : ViewModel() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val recurringExpenses: StateFlow<List<RecurringExpense>> =
        repository.getAllRecurring()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Number of expenses auto-generated on this launch (shown as snackbar). */
    private val _generatedCount = MutableStateFlow(0)
    val generatedCount: StateFlow<Int> = _generatedCount.asStateFlow()

    /** Called once on app start to generate any overdue recurring expenses. */
    fun processRecurring() {
        viewModelScope.launch {
            val today = sdf.format(Date())
            val due   = repository.getDueRecurring(today)
            var count = 0

            for (recurring in due) {
                var next = recurring.nextDueDate

                // Generate one entry per missed period up to and including today
                while (next <= today) {
                    // Skip if end date has passed
                    if (recurring.endDate != null && next > recurring.endDate) break

                    repository.insertExpense(
                        Expense(
                            amount      = recurring.amount,
                            categoryId  = recurring.categoryId,
                            description = recurring.name,
                            merchant    = recurring.name,
                            date        = next,
                            time        = "00:00",
                            source      = "recurring"
                        )
                    )
                    count++
                    next = advanceDate(next, recurring.frequency)
                }

                // Update next due date (or deactivate if past end date)
                val expired = recurring.endDate != null && next > recurring.endDate
                repository.updateRecurring(
                    recurring.copy(
                        nextDueDate = next,
                        isActive    = if (expired) false else recurring.isActive
                    )
                )
            }

            if (count > 0) _generatedCount.value = count
        }
    }

    fun clearGeneratedCount() { _generatedCount.value = 0 }

    /**
     * Carries recurring incomes forward into each new month, posting them on
     * the 1st. Runs once per app open; a SharedPreferences cursor
     * (lastRecurringIncomeMonth) guarantees each month is only processed once,
     * so a user deleting an auto-posted entry won't see it re-appear.
     */
    fun processRecurringIncome() {
        val settings = appSettings ?: return
        viewModelScope.launch {
            val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonth = monthSdf.format(Date())

            fun nextMonth(month: String): String {
                val parsed = runCatching { monthSdf.parse(month) }.getOrNull() ?: return month
                val cal = Calendar.getInstance().also { it.time = parsed }
                cal.add(Calendar.MONTH, 1)
                return monthSdf.format(cal.time)
            }

            // First run: start from last month so this month gets posted.
            val cursor = settings.getLastRecurringIncomeMonth()
                ?: monthSdf.format(Calendar.getInstance().also { it.add(Calendar.MONTH, -1) }.time)

            if (cursor < currentMonth) {
                val incomes = repository.getAllIncomeOnce().toMutableList()
                var month = cursor
                while (month < currentMonth) {
                    val next = nextMonth(month)
                    if (next == month) break
                    val templates = incomes
                        .filter { it.recurring && it.month == month }
                        .distinctBy { it.description.trim().lowercase() }
                    for (template in templates) {
                        val alreadyThere = incomes.any {
                            it.month == next &&
                                it.description.trim().equals(template.description.trim(), ignoreCase = true)
                        }
                        if (!alreadyThere) {
                            val posted = template.copy(
                                id      = 0,
                                date    = "$next-01",
                                time    = "00:00",
                                source  = "recurring",
                                month   = next,
                                smsHash = null
                            )
                            repository.insertIncome(posted)
                            incomes.add(posted)
                        }
                    }
                    month = next
                }
            }
            settings.setLastRecurringIncomeMonth(currentMonth)
        }
    }

    fun addRecurring(
        name: String,
        amount: Double,
        categoryId: Long,
        description: String,
        frequency: String,
        startDate: String,
        endDate: String?
    ) {
        viewModelScope.launch {
            repository.insertRecurring(
                RecurringExpense(
                    name        = name.trim(),
                    amount      = amount,
                    categoryId  = categoryId,
                    description = description.trim(),
                    frequency   = frequency,
                    startDate   = startDate,
                    endDate     = endDate,
                    nextDueDate = startDate
                )
            )
        }
    }

    fun updateRecurring(expense: RecurringExpense) {
        viewModelScope.launch { repository.updateRecurring(expense) }
    }

    fun toggleActive(expense: RecurringExpense) {
        viewModelScope.launch {
            repository.updateRecurring(expense.copy(isActive = !expense.isActive))
        }
    }

    fun deleteRecurring(expense: RecurringExpense) {
        viewModelScope.launch { repository.deleteRecurring(expense) }
    }

    /** Advance a yyyy-MM-dd date string by one period of [frequency]. */
    fun advanceDate(dateStr: String, frequency: String): String {
        val cal = Calendar.getInstance().also { it.time = sdf.parse(dateStr)!! }
        when (frequency) {
            "daily"   -> cal.add(Calendar.DAY_OF_MONTH, 1)
            "weekly"  -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> cal.add(Calendar.MONTH, 1)
            "yearly"  -> cal.add(Calendar.YEAR, 1)
        }
        return sdf.format(cal.time)
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val appSettings: AppSettings? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RecurringViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RecurringViewModel(repository, appSettings) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
