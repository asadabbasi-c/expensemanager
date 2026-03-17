package com.example.expensemanager.data.db

import androidx.room.*
import com.example.expensemanager.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC, time DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, time DESC")
    fun getExpensesByMonth(monthPrefix: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense): Int

    @Update
    suspend fun updateExpense(expense: Expense): Int

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT SUM(amount) FROM expenses WHERE date LIKE :monthPrefix || '%'")
    fun getMonthlyTotal(monthPrefix: String): Flow<Double?>

    @Query("SELECT * FROM expenses ORDER BY date DESC, time DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int = 50): Flow<List<Expense>>
}
