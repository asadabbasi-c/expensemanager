package com.example.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.ProjectTypes
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.viewmodel.SideProjectViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.*
import android.graphics.Color as AndroidColor


@Composable
fun SideProjectsScreen(
    viewModel: SideProjectViewModel,
    onBack: () -> Unit,
    isPro: Boolean = true,
    onUpgrade: () -> Unit = {}
) {
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    if (selectedProjectId == null) {
        SideProjectsListScreen(viewModel = viewModel, onBack = onBack, isPro = isPro, onUpgrade = onUpgrade)
    } else {
        SideProjectDetailScreen(viewModel = viewModel, onBack = { viewModel.selectProject(null) })
    }
}

@Composable
private fun SideProjectsListScreen(
    viewModel: SideProjectViewModel,
    onBack: () -> Unit,
    isPro: Boolean,
    onUpgrade: () -> Unit
) {
    val summaries by viewModel.projectSummaries.collectAsStateWithLifecycle()
    val currency = LocalCurrency.current
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }
    var showAddSheet by remember { mutableStateOf(false) }
    var showProLimit by remember { mutableStateOf(false) }
    // Free tier: 1 project. Pro: unlimited.
    val requestAdd = { if (isPro || summaries.isEmpty()) showAddSheet = true else showProLimit = true }

    Box(modifier = Modifier.fillMaxSize().background(Bg).systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text("Side Projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Brand400)
                        .clickable { requestAdd() }
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add project", tint = Color(0xFF0A0A0A), modifier = Modifier.size(18.dp))
                }
            }

            if (summaries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
                        Text("📁", fontSize = 48.sp)
                        Text("No side projects yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            "Create named budgets like \"App Marketing\" or \"Home Renovation\" and track them separately.",
                            fontSize = 13.sp, color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brand400)
                                .clickable { requestAdd() }
                                .padding(horizontal = 28.dp, vertical = 14.dp)
                        ) {
                            Text("New Project", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(summaries, key = { it.project.id }) { summary ->
                        ProjectCard(
                            summary   = summary,
                            currency  = currency,
                            formatter = formatter,
                            onClick   = { viewModel.selectProject(summary.project.id) }
                        )
                    }
                    item { Spacer(Modifier.height(60.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        CreateProjectDialog(
            onCreate = { name, type, budget ->
                viewModel.addProject(name, type, budget)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }

    if (showProLimit) {
        AlertDialog(
            onDismissRequest = { showProLimit = false },
            containerColor   = Surface,
            title = { Text("Unlimited projects with Pro", color = TextPrimary) },
            text  = { Text("Free includes 1 project. Upgrade to SmartSpend Pro to create unlimited projects.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showProLimit = false; onUpgrade() }) {
                    Text("Upgrade", color = Brand400, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProLimit = false }) { Text("Not now", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    summary: SideProjectViewModel.ProjectSummary,
    currency: String,
    formatter: NumberFormat,
    onClick: () -> Unit
) {
    val project = summary.project
    val color = remember(project.color) {
        runCatching { Color(AndroidColor.parseColor(project.color)) }.getOrDefault(AccentBlue)
    }
    val overBudget = project.hasBudget && summary.spent > project.budget

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(project.icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(project.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "${ProjectTypes.info(project.type).label} • ${summary.expenseCount} expense${if (summary.expenseCount == 1) "" else "s"}",
                    fontSize = 11.sp, color = TextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$currency ${formatter.format(summary.spent)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (overBudget) ErrorRed else TextPrimary)
                Text(
                    if (project.hasBudget) "of $currency ${formatter.format(project.budget)}" else "no budget",
                    fontSize = 10.sp, color = TextMuted
                )
            }
        }

        if (project.hasBudget) {
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceVar)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(summary.percentUsed.toFloat().coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (overBudget) ErrorRed else color)
                )
            }
        }
    }
}

// ── Detail screen ──────────────────────────────────────────────────────────────

@Composable
private fun SideProjectDetailScreen(
    viewModel: SideProjectViewModel,
    onBack: () -> Unit
) {
    val summaries by viewModel.projectSummaries.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val expenses by viewModel.selectedProjectExpenses.collectAsStateWithLifecycle()
    val breakdown by viewModel.selectedProjectBreakdown.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    val summary = summaries.find { it.project.id == selectedId } ?: run {
        onBack(); return
    }
    val project = summary.project
    val currency = LocalCurrency.current
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }
    val categoryColors = remember(categories) {
        categories.associate { it.name to (runCatching { Color(AndroidColor.parseColor(it.color)) }.getOrDefault(AccentBlue)) }
    }
    val surfaceArgb = Surface.toArgb()
    val onSurfaceArgb = TextPrimary.toArgb()

    Box(modifier = Modifier.fillMaxSize().background(Bg).systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text("${project.icon} ${project.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary card
                val overBudget = project.hasBudget && summary.spent > project.budget
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)
                        .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp)).padding(20.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Spent", fontSize = 11.sp, color = TextMuted)
                                Text("$currency ${formatter.format(summary.spent)}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (overBudget) ErrorRed else TextPrimary)
                            }
                            if (project.hasBudget) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining", fontSize = 11.sp, color = TextMuted)
                                    Text("$currency ${formatter.format(summary.remaining)}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Brand400)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(ProjectTypes.info(project.type).label, fontSize = 11.sp, color = TextMuted)
                                    Text("No budget", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                        if (project.hasBudget) {
                            Spacer(Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(SurfaceVar)) {
                                Box(
                                    modifier = Modifier.fillMaxHeight().fillMaxWidth(summary.percentUsed.toFloat().coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(5.dp)).background(if (overBudget) ErrorRed else Brand400)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("of $currency ${formatter.format(project.budget)} budget", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }

                // Donut breakdown
                if (breakdown.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)
                            .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp)).padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Breakdown", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Box(contentAlignment = Alignment.Center) {
                                AndroidView(
                                    factory = { context ->
                                        PieChart(context).apply {
                                            description.isEnabled = false
                                            setUsePercentValues(true)
                                            isDrawHoleEnabled = true
                                            setHoleColor(surfaceArgb)
                                            holeRadius = 54f
                                            transparentCircleRadius = 58f
                                            setTransparentCircleColor(surfaceArgb)
                                            legend.textColor = onSurfaceArgb
                                            setEntryLabelTextSize(0f)
                                            animateY(700)
                                        }
                                    },
                                    update = { chart ->
                                        val sorted = breakdown.entries.sortedByDescending { it.value }
                                        val entries = sorted.map { PieEntry(it.value.toFloat(), it.key) }
                                        val colors = sorted.map { (name, _) -> categoryColors[name]?.toArgb() ?: AndroidColor.GRAY }
                                        val dataSet = PieDataSet(entries, "").apply {
                                            this.colors = colors.toMutableList()
                                            valueTextColor = 0x00000000
                                            valueTextSize = 0f
                                        }
                                        chart.data = PieData(dataSet).apply { setValueFormatter(PercentFormatter(chart)) }
                                        chart.invalidate()
                                    },
                                    modifier = Modifier.size(200.dp).padding(8.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", fontSize = 11.sp, color = TextMuted)
                                    Text("$currency ${formatter.format(summary.spent)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                // Expense list
                Text("Expenses", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (expenses.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No expenses linked to this project yet", fontSize = 13.sp, color = TextMuted)
                    }
                } else {
                    expenses.forEach { expense ->
                        ProjectExpenseRow(expense = expense, currency = currency, formatter = formatter, categoryName = categories.find { it.id == expense.categoryId }?.name)
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ProjectExpenseRow(expense: Expense, currency: String, formatter: NumberFormat, categoryName: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(expense.description.ifBlank { categoryName ?: "Expense" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(expense.date, fontSize = 11.sp, color = TextMuted)
        }
        Text("$currency ${formatter.format(expense.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
    }
}

