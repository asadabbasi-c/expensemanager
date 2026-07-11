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
    version = 7,
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Per-expense main-budget scope replaces the project-level flag.
                db.execSQL("ALTER TABLE expenses ADD COLUMN include_in_main INTEGER NOT NULL DEFAULT 1")
                // Backfill project-linked expenses from their project's old flag
                // so historical behavior intent is preserved.
                db.execSQL(
                    """
                    UPDATE expenses
                    SET include_in_main = COALESCE(
                        (SELECT sp.include_in_main FROM side_projects sp WHERE sp.id = expenses.project_id),
                        1
                    )
                    WHERE project_id IS NOT NULL
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE side_projects ADD COLUMN type TEXT NOT NULL DEFAULT 'other'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_manager_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
