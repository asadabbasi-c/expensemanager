package com.example.expensemanager.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.expensemanager.monetization.BillingManager
import com.example.expensemanager.monetization.InterstitialAdManager
import com.example.expensemanager.monetization.ProGate
import com.example.expensemanager.monetization.ProManager
import com.example.expensemanager.ui.screens.BadgesScreen
import com.example.expensemanager.ui.screens.DashboardScreen
import com.example.expensemanager.ui.screens.ExportImportScreen
import com.example.expensemanager.ui.screens.ExpenseListScreen
import com.example.expensemanager.ui.screens.GoalSettingScreen
import com.example.expensemanager.ui.screens.IncomeScreen
import com.example.expensemanager.ui.screens.ManageCategoriesScreen
import com.example.expensemanager.ui.screens.ProUpgradeScreen
import com.example.expensemanager.ui.screens.ReceiptScanScreen
import com.example.expensemanager.ui.screens.RecurringScreen
import com.example.expensemanager.ui.screens.SettingsScreen
import com.example.expensemanager.ui.screens.SideBudgetScreen
import com.example.expensemanager.ui.screens.SideProjectsScreen
import com.example.expensemanager.ui.screens.VoiceExpenseScreen
import com.example.expensemanager.ui.theme.LocalCurrency
import com.example.expensemanager.viewmodel.BadgeViewModel
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.example.expensemanager.viewmodel.ExportImportViewModel
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.GoalViewModel
import com.example.expensemanager.viewmodel.ReceiptViewModel
import com.example.expensemanager.viewmodel.RecurringViewModel
import com.example.expensemanager.viewmodel.SettingsViewModel
import com.example.expensemanager.viewmodel.SideBudgetViewModel
import com.example.expensemanager.viewmodel.SideProjectViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Expenses   : Screen("expenses",   "Expenses",  Icons.Filled.List)
    object Dashboard  : Screen("dashboard",  "Dashboard", Icons.Filled.BarChart)
    object Recurring  : Screen("recurring",  "Recurring", Icons.Filled.Repeat)
    object Settings   : Screen("settings",   "Settings",  Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Expenses,
    Screen.Dashboard,
    Screen.Recurring,
    Screen.Settings
)

@Composable
fun NavGraph(
    expenseViewModel     : ExpenseViewModel,
    dashboardViewModel   : DashboardViewModel,
    exportImportViewModel: ExportImportViewModel,
    goalViewModel        : GoalViewModel,
    receiptViewModel     : ReceiptViewModel,
    recurringViewModel   : RecurringViewModel,
    settingsViewModel    : SettingsViewModel,
    sideBudgetViewModel  : SideBudgetViewModel,
    sideProjectViewModel : SideProjectViewModel,
    badgeViewModel       : BadgeViewModel,
    proManager           : ProManager,
    billingManager       : BillingManager,
    interstitialAdManager: InterstitialAdManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currency by settingsViewModel.currencySymbol.collectAsStateWithLifecycle()
    val sideProjects by sideProjectViewModel.allProjects.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 4.dp) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon     = { Icon(screen.icon, contentDescription = screen.label) },
                        label    = { Text(screen.label) },
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
        }
    ) { innerPadding ->
        CompositionLocalProvider(LocalCurrency provides currency) {
            NavHost(
                navController    = navController,
                startDestination = Screen.Expenses.route,
                modifier         = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                composable(Screen.Expenses.route) {
                    ExpenseListScreen(
                        viewModel             = expenseViewModel,
                        proManager            = proManager,
                        interstitialAdManager = interstitialAdManager,
                        onNavigateToReceipt   = { navController.navigate("receipt") },
                        onNavigateToVoice     = { navController.navigate("voice") },
                        onNavigateToRecurring = { navController.navigate("recurring") },
                        onSaveRecurring       = { name, amount, catId, desc, freq, startDate ->
                            recurringViewModel.addRecurring(name, amount, catId, desc, freq, startDate, null)
                        },
                        projects              = sideProjects
                    )
                }
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel         = dashboardViewModel,
                        onNavigateToGoals = { navController.navigate("goals") },
                        proManager        = proManager
                    )
                }
                composable(Screen.Recurring.route) {
                    RecurringScreen(
                        recurringViewModel = recurringViewModel,
                        expenseViewModel   = expenseViewModel,
                        onBack             = { navController.navigate(Screen.Expenses.route) }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        settingsViewModel      = settingsViewModel,
                        proManager             = proManager,
                        onNavigateToGoals      = { navController.navigate("goals") },
                        onNavigateToCategories = { navController.navigate("categories") },
                        onNavigateToFiles      = { navController.navigate("files") },
                        onNavigateToUpgrade    = { navController.navigate("upgrade") },
                        onNavigateToSideBudget   = { navController.navigate("side_budget") },
                        onNavigateToIncome       = { navController.navigate("income") },
                        onNavigateToSideProjects = { navController.navigate("side_projects") },
                        onNavigateToBadges       = { navController.navigate("badges") }
                    )
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
                            viewModel          = goalViewModel,
                            onBack             = { navController.popBackStack() },
                            onNavigateToIncome = { navController.navigate("income") }
                        )
                    }
                }
                composable("income") {
                    IncomeScreen(
                        viewModel = goalViewModel,
                        onBack    = { navController.popBackStack() }
                    )
                }
                composable("side_budget") {
                    SideBudgetScreen(
                        viewModel = sideBudgetViewModel,
                        onBack    = { navController.popBackStack() }
                    )
                }
                composable("side_projects") {
                    ProGate(
                        proManager  = proManager,
                        featureName = "Side Projects",
                        featureIcon = "📁",
                        description = "Create named budget projects, track their own expenses and analytics, separate from your main budget.",
                        onUpgrade   = { navController.navigate("upgrade") }
                    ) {
                        SideProjectsScreen(
                            viewModel = sideProjectViewModel,
                            onBack    = { navController.popBackStack() }
                        )
                    }
                }
                composable("badges") {
                    ProGate(
                        proManager  = proManager,
                        featureName = "Badges",
                        featureIcon = "🏆",
                        description = "Get a monthly score based on your budget adherence, savings rate, daily discipline, and consistency.",
                        onUpgrade   = { navController.navigate("upgrade") }
                    ) {
                        BadgesScreen(
                            viewModel = badgeViewModel,
                            onBack    = { navController.popBackStack() }
                        )
                    }
                }
                composable("upgrade") {
                    val activity = LocalContext.current as Activity
                    ProUpgradeScreen(
                        proManager         = proManager,
                        billingManager     = billingManager,
                        onBack             = { navController.popBackStack() },
                        onPurchase         = { planId -> billingManager.launchBillingFlow(activity, planId) },
                        onRestore          = { billingManager.restorePurchases() },
                        onDebugActivatePro = { proManager.grantPro() }
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
                composable("categories") {
                    ManageCategoriesScreen(viewModel = expenseViewModel)
                }
                composable("files") {
                    ExportImportScreen(viewModel = exportImportViewModel)
                }
            }
        }
    }
}
