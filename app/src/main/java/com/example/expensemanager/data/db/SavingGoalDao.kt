package com.example.expensemanager.data.db

import androidx.room.*
import com.example.expensemanager.data.model.SavingGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingGoalDao {

    @Query("SELECT * FROM saving_goals WHERE month = :month")
    fun getGoalForMonth(month: String): Flow<SavingGoal?>

    @Query("SELECT * FROM saving_goals ORDER BY month DESC")
    fun getAllGoals(): Flow<List<SavingGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: SavingGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingGoal)
}
