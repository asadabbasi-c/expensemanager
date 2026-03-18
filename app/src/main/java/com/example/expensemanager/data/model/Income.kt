package com.example.expensemanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val source: String = "manual",   // "manual" or "sms"
    val month: String = "",          // yyyy-MM
    @ColumnInfo(name = "sms_hash") val smsHash: String? = null
)
