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
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.example.expensemanager.viewmodel.ExportImportViewModel
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.GoalViewModel
import com.example.expensemanager.viewmodel.ReceiptViewModel
import com.example.expensemanager.monetization.ProManager
import com.example.expensemanager.viewmodel.RecurringViewModel
import com.example.expensemanager.viewmodel.SmsViewModel
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // must be called before super + setContent
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val app = application as ExpenseApplication

                    val expenseViewModel: ExpenseViewModel = viewModel(
                        factory = ExpenseViewModel.Factory(app.repository)
                    )
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = DashboardViewModel.Factory(app.repository)
                    )
                    val smsViewModel: SmsViewModel = viewModel(
                        factory = SmsViewModel.Factory(app.repository)
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
                        factory = RecurringViewModel.Factory(app.repository)
                    )

                    // Auto-generate any overdue recurring expenses on every app open
                    LaunchedEffect(Unit) {
                        recurringViewModel.processRecurring()
                    }

                    NavGraph(
                        expenseViewModel      = expenseViewModel,
                        dashboardViewModel    = dashboardViewModel,
                        smsViewModel          = smsViewModel,
                        exportImportViewModel = exportImportViewModel,
                        goalViewModel         = goalViewModel,
                        receiptViewModel      = receiptViewModel,
                        recurringViewModel    = recurringViewModel,
                        proManager            = app.proManager
                    )
                }
            }
        }
    }
}
