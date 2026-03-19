package com.example.expensemanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val description: String = "",
    val frequency: String,                       // "daily" | "weekly" | "monthly" | "yearly"
    @ColumnInfo(name = "start_date") val startDate: String,          // yyyy-MM-dd
    @ColumnInfo(name = "end_date") val endDate: String? = null,      // yyyy-MM-dd, optional
    @ColumnInfo(name = "next_due_date") val nextDueDate: String,     // yyyy-MM-dd
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
