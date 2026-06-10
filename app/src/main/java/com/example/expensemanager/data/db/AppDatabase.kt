package com.example.expensemanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.Income
import com.example.expensemanager.data.model.RecurringExpense
import com.example.expensemanager.data.model.SavingGoal
import com.example.expensemanager.data.model.SideBudget
import com.example.expensemanager.data.model.SideProject

@Database(
    entities = [
        Expense::class, Category::class, Income::class, SavingGoal::class,
        RecurringExpense::class, SideBudget::class, SideProject::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun sideBudgetDao(): SideBudgetDao
    abstract fun sideProjectDao(): SideProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE income ADD COLUMN recurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expenses ADD COLUMN project_id INTEGER")

                db.execSQL("ALTER TABLE saving_goals ADD COLUMN budget_type TEXT NOT NULL DEFAULT 'monthly'")
                db.execSQL("ALTER TABLE saving_goals ADD COLUMN budget_mode TEXT NOT NULL DEFAULT 'fixed'")
                db.execSQL("ALTER TABLE saving_goals ADD COLUMN budget_amount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE saving_goals ADD COLUMN budget_percent REAL NOT NULL DEFAULT 20.0")
                db.execSQL("ALTER TABLE saving_goals ADD COLUMN savings_percent REAL NOT NULL DEFAULT 15.0")
                db.execSQL("ALTER TABLE saving_goals ADD COLUMN period_start TEXT")
                db.execSQL("ALTER TABLE saving_goals ADD COLUMN period_end TEXT")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `side_budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `total_amount` REAL NOT NULL,
                        `start_date` TEXT NOT NULL,
                        `end_date` TEXT NOT NULL,
                        `is_active` INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `side_projects` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL DEFAULT '📁',
                        `budget` REAL NOT NULL,
                        `color` TEXT NOT NULL DEFAULT '#60A5FA',
                        `include_in_main` INTEGER NOT NULL DEFAULT 0,
                        `is_active` INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_manager_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
