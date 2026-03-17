package com.example.expensemanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    val description: String = "",
    val location: String = "",
    val address: String = "",
    val date: String = "",
    val time: String = "",
    val source: String = "manual", // "manual" or "sms"
    @ColumnInfo(name = "bank_name")
    val bankName: String? = null,
    val merchant: String? = null
)
