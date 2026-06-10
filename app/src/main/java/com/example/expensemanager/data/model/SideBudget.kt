package com.example.expensemanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "side_budgets")
data class SideBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    @ColumnInfo(name = "start_date") val startDate: String,   // yyyy-MM-dd
    @ColumnInfo(name = "end_date") val endDate: String,       // yyyy-MM-dd
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
