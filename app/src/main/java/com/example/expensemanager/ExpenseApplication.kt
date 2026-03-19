package com.example.expensemanager

import android.app.Application
import com.example.expensemanager.data.db.AppDatabase
import com.example.expensemanager.data.repository.ExpenseRepository

class ExpenseApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val repository: ExpenseRepository by lazy {
        ExpenseRepository(
            expenseDao           = database.expenseDao(),
            categoryDao          = database.categoryDao(),
            incomeDao            = database.incomeDao(),
            savingGoalDao        = database.savingGoalDao(),
            recurringExpenseDao  = database.recurringExpenseDao()
        )
    }
}
