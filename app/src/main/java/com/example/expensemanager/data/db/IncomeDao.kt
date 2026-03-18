package com.example.expensemanager.data.db

import androidx.room.*
import com.example.expensemanager.data.model.Income
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Query("SELECT * FROM income ORDER BY date DESC, time DESC")
    fun getAllIncome(): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE month = :month ORDER BY date DESC")
    fun getIncomeForMonth(month: String): Flow<List<Income>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM income WHERE month = :month")
    fun getTotalIncomeForMonth(month: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income): Long

    @Delete
    suspend fun deleteIncome(income: Income)

    @Query("SELECT COUNT(*) FROM income WHERE sms_hash = :hash")
    suspend fun countByHash(hash: String): Int
}
