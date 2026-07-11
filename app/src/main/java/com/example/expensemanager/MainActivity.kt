package com.example.expensemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensemanager.ui.navigation.NavGraph
import com.example.expensemanager.ui.screens.SplashScreen
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.example.expensemanager.viewmodel.ExportImportViewModel
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.GoalViewModel
import com.example.expensemanager.viewmodel.ReceiptViewModel
import com.example.expensemanager.monetization.BillingManager
import com.example.expensemanager.viewmodel.RecurringViewModel
import com.example.expensemanager.viewmodel.SettingsViewModel
import com.example.expensemanager.viewmodel.SideBudgetViewModel
import com.example.expensemanager.viewmodel.SideProjectViewModel
import com.example.expensemanager.viewmodel.AnalyticsViewModel
import com.example.expensemanager.viewmodel.BadgeViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // must be called before super + setContent
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ExpenseApplication
        billingManager = app.billingManager
        billingManager.connect()
        setContent {
            ExpenseManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                        return@Surface
                    }

                    val expenseViewModel: ExpenseViewModel = viewModel(
                        factory = ExpenseViewModel.Factory(app.repository)
                    )
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = DashboardViewModel.Factory(app.repository)
                    )
                    val exportImportViewModel: ExportImportViewModel = viewModel(
                        factory = ExportImportViewModel.Factory(app.repository)
                    )
                    val goalViewModel: GoalViewModel = viewModel(
                        factory = GoalViewModel.Factory(app.repository)
                    )
                    val receiptViewModel: ReceiptViewModel = viewModel(
                        factory = ReceiptViewModel.Factory(app.repository)
                    )
                    val recurringViewModel: RecurringViewModel = viewModel(
                        factory = RecurringViewModel.Factory(app.repository, app.appSettings)
                    )
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.Factory(app.appSettings)
                    )
                    val sideBudgetViewModel: SideBudgetViewModel = viewModel(
                        factory = SideBudgetViewModel.Factory(app.repository)
                    )
                    val sideProjectViewModel: SideProjectViewModel = viewModel(
                        factory = SideProjectViewModel.Factory(app.repository)
                    )
                    val badgeViewModel: BadgeViewModel = viewModel(
                        factory = BadgeViewModel.Factory(app.repository)
                    )
                    val analyticsViewModel: AnalyticsViewModel = viewModel(
                        factory = AnalyticsViewModel.Factory(app.repository)
                    )

                    // Auto-generate any overdue recurring expenses and carry
                    // recurring incomes into the new month on every app open
                    LaunchedEffect(Unit) {
                        recurringViewModel.processRecurring()
                        recurringViewModel.processRecurringIncome()
                    }

                    NavGraph(
                        expenseViewModel      = expenseViewModel,
                        dashboardViewModel    = dashboardViewModel,
                        exportImportViewModel = exportImportViewModel,
                        goalViewModel         = goalViewModel,
                        receiptViewModel      = receiptViewModel,
                        recurringViewModel    = recurringViewModel,
                        settingsViewModel     = settingsViewModel,
                        sideBudgetViewModel   = sideBudgetViewModel,
                        sideProjectViewModel  = sideProjectViewModel,
                        badgeViewModel        = badgeViewModel,
                        analyticsViewModel    = analyticsViewModel,
                        proManager            = app.proManager,
                        billingManager        = billingManager,
                        interstitialAdManager = app.interstitialAdManager
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check purchases on every resume to catch renewals and Play-side restores
        billingManager.queryPurchases()
    }
}
