package com.example.expensemanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "side_projects")
data class SideProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "📁",
    val budget: Double,
    val color: String = "#60A5FA",
    @ColumnInfo(name = "include_in_main") val includeInMain: Boolean = false,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
