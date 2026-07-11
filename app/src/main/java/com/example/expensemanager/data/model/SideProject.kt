package com.example.expensemanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "side_projects")
data class SideProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "📁",
    val budget: Double = 0.0,       // 0.0 = no budget set (project just groups expenses)
    val color: String = "#60A5FA",
    // Deprecated: scope now lives on each Expense (Expense.includeInMain).
    // Kept only so the existing table schema stays valid; not used in logic/UI.
    @ColumnInfo(name = "include_in_main") val includeInMain: Boolean = false,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "type", defaultValue = "other")
    val type: String = ProjectTypes.OTHER   // "trip" | "side_expense" | "pet_project" | "other"
) {
    val hasBudget: Boolean get() = budget > 0.0
}

/** Fixed project types; each type carries its icon and color. */
object ProjectTypes {
    const val TRIP        = "trip"
    const val SIDE        = "side_expense"
    const val PET         = "pet_project"
    const val OTHER       = "other"

    data class TypeInfo(val key: String, val label: String, val icon: String, val color: String)

    val ALL = listOf(
        TypeInfo(TRIP,  "Trip",         "✈️", "#22D3EE"),
        TypeInfo(SIDE,  "Side Expense", "💼", "#60A5FA"),
        TypeInfo(PET,   "Pet Project",  "🐾", "#A78BFA"),
        TypeInfo(OTHER, "Other",        "📁", "#FBBF24")
    )

    fun info(key: String): TypeInfo = ALL.find { it.key == key } ?: ALL.last()
}
