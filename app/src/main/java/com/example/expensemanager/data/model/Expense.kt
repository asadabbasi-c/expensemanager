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
    val source: String = "manual",  // "manual", "sms", "voice", "receipt"
    @ColumnInfo(name = "bank_name")
    val bankName: String? = null,
    val merchant: String? = null,
    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,  // absolute path to saved receipt photo
    @ColumnInfo(name = "project_id")
    val projectId: Long? = null,    // optional link to a SideProject
    // Per-expense scope: true = counts in main monthly totals.
    // projectId == null → always true (main-only). projectId != null →
    // true = "both" (main + project), false = project-only.
    @ColumnInfo(name = "include_in_main", defaultValue = "1")
    val includeInMain: Boolean = true
)
