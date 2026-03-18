package com.example.expensemanager.data.repository

import com.example.expensemanager.data.db.CategoryDao
import com.example.expensemanager.data.db.ExpenseDao
import com.example.expensemanager.data.db.IncomeDao
import com.example.expensemanager.data.db.SavingGoalDao
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.Income
import com.example.expensemanager.data.model.SavingGoal
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val incomeDao: IncomeDao,
    private val savingGoalDao: SavingGoalDao
) {

    // ── Expense operations ────────────────────────────────────────────────────

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByMonth(monthPrefix: String): Flow<List<Expense>> =
        expenseDao.getExpensesByMonth(monthPrefix)

    fun getMonthlyTotal(monthPrefix: String): Flow<Double?> =
        expenseDao.getMonthlyTotal(monthPrefix)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    // ── Category operations ───────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun insertCategories(categories: List<Category>) =
        categoryDao.insertCategories(categories)

    suspend fun getCategoryCount(): Int = categoryDao.getCategoryCount()

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    suspend fun getCategoryByName(name: String): Category? = categoryDao.getCategoryByName(name)

    suspend fun insertDefaultCategories() {
        val count = getCategoryCount()
        if (count == 0) {
            val defaultCategories = listOf(
                Category(name = "Food",          icon = "🍔", color = "#FF5722"),
                Category(name = "Transport",     icon = "🚗", color = "#2196F3"),
                Category(name = "Shopping",      icon = "🛍️", color = "#9C27B0"),
                Category(name = "Bills",         icon = "📄", color = "#FF9800"),
                Category(name = "Health",        icon = "❤️", color = "#F44336"),
                Category(name = "Entertainment", icon = "🎬", color = "#3F51B5"),
                Category(name = "Other",         icon = "💰", color = "#607D8B")
            )
            insertCategories(defaultCategories)
        }
    }

    // ── Income operations ─────────────────────────────────────────────────────

    fun getAllIncome(): Flow<List<Income>> = incomeDao.getAllIncome()

    fun getIncomeForMonth(month: String): Flow<List<Income>> =
        incomeDao.getIncomeForMonth(month)

    fun getTotalIncomeForMonth(month: String): Flow<Double> =
        incomeDao.getTotalIncomeForMonth(month)

    suspend fun insertIncome(income: Income): Long = incomeDao.insertIncome(income)

    suspend fun deleteIncome(income: Income) = incomeDao.deleteIncome(income)

    suspend fun incomeExistsForHash(hash: String): Boolean =
        incomeDao.countByHash(hash) > 0

    // ── Saving Goal operations ────────────────────────────────────────────────

    fun getGoalForMonth(month: String): Flow<SavingGoal?> =
        savingGoalDao.getGoalForMonth(month)

    fun getAllGoals(): Flow<List<SavingGoal>> = savingGoalDao.getAllGoals()

    suspend fun upsertGoal(goal: SavingGoal) = savingGoalDao.upsertGoal(goal)

    suspend fun deleteGoal(goal: SavingGoal) = savingGoalDao.deleteGoal(goal)
}
