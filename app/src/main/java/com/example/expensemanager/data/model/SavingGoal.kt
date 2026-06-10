package com.example.expensemanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saving_goals")
data class SavingGoal(
    @PrimaryKey val month: String,                                    // yyyy-MM
    @ColumnInfo(name = "goal_amount")   val goalAmount: Double,      // how much to save
    @ColumnInfo(name = "income_target") val incomeTarget: Double,    // baseline monthly income

    // ── Flexible budget settings ──────────────────────────────────────────────
    @ColumnInfo(name = "budget_type", defaultValue = "monthly")
    val budgetType: String = "monthly",        // "monthly" | "yearly" | "custom"
    @ColumnInfo(name = "budget_mode", defaultValue = "fixed")
    val budgetMode: String = "fixed",          // "fixed" | "percent"
    @ColumnInfo(name = "budget_amount", defaultValue = "0.0")
    val budgetAmount: Double = 0.0,            // spending limit for the period (fixed mode)
    @ColumnInfo(name = "budget_percent", defaultValue = "20.0")
    val budgetPercent: Double = 20.0,          // spending cap as % of income (percent mode)
    @ColumnInfo(name = "savings_percent", defaultValue = "15.0")
    val savingsPercent: Double = 15.0,         // savings goal as % of income
    @ColumnInfo(name = "period_start")
    val periodStart: String? = null,           // yyyy-MM-dd, for "custom" budgetType
    @ColumnInfo(name = "period_end")
    val periodEnd: String? = null              // yyyy-MM-dd, for "custom" budgetType
)
