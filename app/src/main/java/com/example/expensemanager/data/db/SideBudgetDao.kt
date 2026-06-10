package com.example.expensemanager.data.db

import androidx.room.*
import com.example.expensemanager.data.model.SideBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface SideBudgetDao {

    @Query("SELECT * FROM side_budgets WHERE is_active = 1 ORDER BY id DESC LIMIT 1")
    fun getActiveSideBudget(): Flow<SideBudget?>

    @Query("SELECT * FROM side_budgets ORDER BY id DESC")
    fun getAllSideBudgets(): Flow<List<SideBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSideBudget(budget: SideBudget): Long

    @Update
    suspend fun updateSideBudget(budget: SideBudget)

    @Delete
    suspend fun deleteSideBudget(budget: SideBudget)
}
