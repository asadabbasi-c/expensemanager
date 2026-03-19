package com.example.expensemanager.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensemanager.monetization.ProGate
import com.example.expensemanager.monetization.ProManager
import com.example.expensemanager.ui.screens.AddExpenseScreen
import com.example.expensemanager.ui.screens.ProUpgradeScreen
import com.example.expensemanager.ui.screens.DashboardScreen
import com.example.expensemanager.ui.screens.ExportImportScreen
import com.example.expensemanager.ui.screens.ExpenseListScreen
import com.example.expensemanager.ui.screens.GoalSettingScreen
import com.example.expensemanager.ui.screens.ManageCategoriesScreen
import com.example.expensemanager.ui.screens.ReceiptScanScreen
import com.example.expensemanager.ui.screens.RecurringScreen
import com.example.expensemanager.ui.screens.SmsScreen
import com.example.expensemanager.ui.screens.VoiceExpenseScreen
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.example.expensemanager.viewmodel.ExportImportViewModel
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.GoalViewModel
import com.example.expensemanager.viewmodel.ReceiptViewModel
import com.example.expensemanager.viewmodel.RecurringViewModel
import com.example.expensemanager.viewmodel.SmsViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Expenses   : Screen("expenses",   "Expenses",   Icons.Filled.List)
    object Add        : Screen("add",        "Add",        Icons.Filled.Add)
    object Dashboard  : Screen("dashboard",  "Dashboard",  Icons.Filled.BarChart)
    object Sms        : Screen("sms",        "SMS",        Icons.Filled.Sms)
    object Categories : Screen("categories", "Categories", Icons.Filled.Label)
    object Files      : Screen("files",      "Files",      Icons.Filled.FolderOpen)
}

val bottomNavItems = listOf(
    Screen.Expenses,
    Screen.Add,
    Screen.Dashboard,
    Screen.Sms,
    Screen.Categories,
    Screen.Files
)

@Composable
fun NavGraph(
    expenseViewModel     : ExpenseViewModel,
    dashboardViewModel   : DashboardViewModel,
    smsViewModel         : SmsViewModel,
    exportImportViewModel: ExportImportViewModel,
    goalViewModel        : GoalViewModel,
    receiptViewModel     : ReceiptViewModel,
    recurringViewModel   : RecurringViewModel,
    proManager           : ProManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isPro by proManager.isPro.collectAsStateWithLifecycle()

    // Hide FAB on certain screens; also hide mic FAB for free users (voice is Pro)
    val showFab = isPro
        && currentRoute != "voice" && currentRoute != "goals"
        && currentRoute != "receipt" && currentRoute != "recurring"
        && currentRoute != "upgrade"

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 4.dp) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon     = { Icon(screen.icon, contentDescription = screen.label) },
                        label    = null,
                        selected = navBackStackEntry?.destination
                            ?.hierarchy?.any { it.route == screen.route } == true,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick        = { navController.navigate("voice") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice Expense")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Expenses.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Expenses.route) {
                ExpenseListScreen(viewModel = expenseViewModel, proManager = proManager)
            }
            composable(Screen.Add.route) {
                AddExpenseScreen(
                    viewModel     = expenseViewModel,
                    onSaved       = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(Screen.Expenses.route) { inclusive = true }
                        }
                    },
                    onScanReceipt = { navController.navigate("receipt") },
                    onRecurring   = { navController.navigate("recurring") }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel         = dashboardViewModel,
                    onNavigateToGoals = { navController.navigate("goals") }
                )
            }
            composable(Screen.Sms.route) {
                ProGate(
                    proManager  = proManager,
                    featureName = "SMS Auto-Import",
                    featureIcon = "📩",
                    description = "Automatically scan your bank messages and log debit transactions — no manual entry needed.",
                    onUpgrade   = { navController.navigate("upgrade") }
                ) {
                    SmsScreen(
                        viewModel        = smsViewModel,
                        expenseViewModel = expenseViewModel
                    )
                }
            }
            composable(Screen.Categories.route) {
                ManageCategoriesScreen(viewModel = expenseViewModel)
            }
            composable(Screen.Files.route) {
                ExportImportScreen(viewModel = exportImportViewModel)
            }
            composable("voice") {
                ProGate(
                    proManager  = proManager,
                    featureName = "Voice Entry",
                    featureIcon = "🎙️",
                    description = "Just say \"Spent 500 at KFC\" and it's logged instantly — completely hands-free.",
                    onUpgrade   = { navController.navigate("upgrade") }
                ) {
                    VoiceExpenseScreen(
                        expenseViewModel = expenseViewModel,
                        onSaved = {
                            navController.navigate(Screen.Expenses.route) {
                                popUpTo(Screen.Expenses.route) { inclusive = true }
                            }
                        },
                        onBack  = { navController.popBackStack() }
                    )
                }
            }
            composable("goals") {
                ProGate(
                    proManager  = proManager,
                    featureName = "Budget & Goals",
                    featureIcon = "🎯",
                    description = "Set monthly saving goals, track your daily spend budget, and get warnings before you overspend.",
                    onUpgrade   = { navController.navigate("upgrade") }
                ) {
                    GoalSettingScreen(
                        viewModel = goalViewModel,
                        onBack    = { navController.popBackStack() }
                    )
                }
            }
            composable("upgrade") {
                ProUpgradeScreen(
                    proManager = proManager,
                    onBack     = { navController.popBackStack() }
                )
            }
            composable("receipt") {
                ReceiptScanScreen(
                    receiptViewModel = receiptViewModel,
                    expenseViewModel = expenseViewModel,
                    onSaved = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(Screen.Expenses.route) { inclusive = true }
                        }
                    },
                    onBack  = { navController.popBackStack() }
                )
            }
            composable("recurring") {
                RecurringScreen(
                    recurringViewModel = recurringViewModel,
                    expenseViewModel   = expenseViewModel,
                    onBack             = { navController.popBackStack() }
                )
            }
        }
    }
}
