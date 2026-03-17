package com.example.expensemanager.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensemanager.ui.screens.AddExpenseScreen
import com.example.expensemanager.ui.screens.DashboardScreen
import com.example.expensemanager.ui.screens.ExpenseListScreen
import com.example.expensemanager.ui.screens.ManageCategoriesScreen
import com.example.expensemanager.ui.screens.SmsScreen
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.SmsViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Expenses : Screen("expenses", "Expenses", Icons.Filled.List)
    object Add : Screen("add", "Add", Icons.Filled.Add)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.BarChart)
    object Sms : Screen("sms", "SMS", Icons.Filled.Sms)
    object Categories : Screen("categories", "Categories", Icons.Filled.Label)
}

val bottomNavItems = listOf(
    Screen.Expenses,
    Screen.Add,
    Screen.Dashboard,
    Screen.Sms,
    Screen.Categories
)

@Composable
fun NavGraph(
    expenseViewModel: ExpenseViewModel,
    dashboardViewModel: DashboardViewModel,
    smsViewModel: SmsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 4.dp) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Expenses.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Expenses.route) {
                ExpenseListScreen(viewModel = expenseViewModel)
            }
            composable(Screen.Add.route) {
                AddExpenseScreen(
                    viewModel = expenseViewModel,
                    onSaved = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(Screen.Expenses.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Sms.route) {
                SmsScreen(
                    viewModel = smsViewModel,
                    expenseViewModel = expenseViewModel
                )
            }
            composable(Screen.Categories.route) {
                ManageCategoriesScreen(viewModel = expenseViewModel)
            }
        }
    }
}
