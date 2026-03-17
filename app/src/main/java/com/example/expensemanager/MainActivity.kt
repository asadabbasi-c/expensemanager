package com.example.expensemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensemanager.ui.navigation.NavGraph
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.SmsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

                    NavGraph(
                        expenseViewModel = expenseViewModel,
                        dashboardViewModel = dashboardViewModel,
                        smsViewModel = smsViewModel
                    )
                }
            }
        }
    }
}
