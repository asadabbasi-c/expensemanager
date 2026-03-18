package com.example.expensemanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saving_goals")
data class SavingGoal(
    @PrimaryKey val month: String,                                    // yyyy-MM
    @ColumnInfo(name = "goal_amount")   val goalAmount: Double,      // how much to save
    @ColumnInfo(name = "income_target") val incomeTarget: Double     // baseline monthly income
)
