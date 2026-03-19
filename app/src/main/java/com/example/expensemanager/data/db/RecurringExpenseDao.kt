package com.example.expensemanager.data.db

import androidx.room.*
import com.example.expensemanager.data.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {

    @Query("SELECT * FROM recurring_expenses ORDER BY is_active DESC, next_due_date ASC")
    fun getAllRecurring(): Flow<List<RecurringExpense>>

    /** Returns active recurring entries whose next due date is on or before [today] (yyyy-MM-dd). */
    @Query("SELECT * FROM recurring_expenses WHERE is_active = 1 AND next_due_date <= :today")
    suspend fun getDueRecurring(today: String): List<RecurringExpense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(expense: RecurringExpense): Long

    @Update
    suspend fun updateRecurring(expense: RecurringExpense)

    @Delete
    suspend fun deleteRecurring(expense: RecurringExpense)
}
