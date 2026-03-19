package com.example.expensemanager

import android.app.Application
import com.example.expensemanager.data.db.AppDatabase
import com.example.expensemanager.data.repository.ExpenseRepository
import com.example.expensemanager.monetization.ProManager
import com.google.android.gms.ads.MobileAds

class ExpenseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialise AdMob once on app start (required before loading any ad)
        MobileAds.initialize(this)
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val proManager: ProManager by lazy { ProManager(this) }

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
