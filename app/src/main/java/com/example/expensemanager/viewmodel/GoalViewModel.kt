package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Income
import com.example.expensemanager.data.model.SavingGoal
import com.example.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GoalViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val dateSdf  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeSdf  = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val labelSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    private val _selectedMonth = MutableStateFlow(monthSdf.format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val selectedMonthLabel: StateFlow<String> = _selectedMonth.map { month ->
        runCatching { labelSdf.format(monthSdf.parse(month)!!) }.getOrDefault(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val goalForMonth: StateFlow<SavingGoal?> = _selectedMonth.flatMapLatest { month ->
        repository.getGoalForMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val incomeForMonth: StateFlow<List<Income>> = _selectedMonth.flatMapLatest { month ->
        repository.getIncomeForMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExtraIncomeForMonth: StateFlow<Double> = _selectedMonth.flatMapLatest { month ->
        repository.getTotalIncomeForMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun navigateMonth(delta: Int) {
        val cal = Calendar.getInstance()
        cal.time = runCatching { monthSdf.parse(_selectedMonth.value)!! }.getOrDefault(Date())
        cal.add(Calendar.MONTH, delta)
        _selectedMonth.value = monthSdf.format(cal.time)
    }

    fun saveGoal(goalAmount: Double, incomeTarget: Double) {
        viewModelScope.launch {
            _isSaving.value = true
            repository.upsertGoal(
                SavingGoal(
                    month        = _selectedMonth.value,
                    goalAmount   = goalAmount,
                    incomeTarget = incomeTarget
                )
            )
            _isSaving.value = false
        }
    }

    fun addManualIncome(amount: Double, description: String) {
        viewModelScope.launch {
            val now = Date()
            repository.insertIncome(
                Income(
                    amount      = amount,
                    description = description,
                    date        = dateSdf.format(now),
                    time        = timeSdf.format(now),
                    source      = "manual",
                    month       = _selectedMonth.value,
                    smsHash     = null
                )
            )
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch { repository.deleteIncome(income) }
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GoalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GoalViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
