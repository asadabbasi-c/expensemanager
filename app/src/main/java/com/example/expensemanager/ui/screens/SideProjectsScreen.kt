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

private val ProjectColors = listOf("#60A5FA", "#A78BFA", "#FB923C", "#F472B6", "#22D3EE", "#FBBF24", "#4ADE80")
private val ProjectIcons = listOf("📁", "🚀", "🏠", "🚗", "🎓", "💼", "🎉", "🛠️")

@Composable
fun SideProjectsScreen(
    viewModel: SideProjectViewModel,
    onBack: () -> Unit
) {
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    if (selectedProjectId == null) {
        SideProjectsListScreen(viewModel = viewModel, onBack = onBack)
    } else {
        SideProjectDetailScreen(viewModel = viewModel, onBack = { viewModel.selectProject(null) })
    }
}

@Composable
private fun SideProjectsListScreen(
    viewModel: SideProjectViewModel,
    onBack: () -> Unit
) {
    val summaries by viewModel.projectSummaries.collectAsStateWithLifecycle()
    val currency = LocalCurrency.current
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }
    var showAddSheet by remember { mutableStateOf(false) }

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
                        .clickable { showAddSheet = true }
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
                                .clickable { showAddSheet = true }
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
                            onClick   = { viewModel.selectProject(summary.project.id) },
                            onToggleInclude = { viewModel.toggleIncludeInMain(summary.project) }
                        )
                    }
                    item { Spacer(Modifier.height(60.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        AddProjectSheet(
            onSave = { name, icon, budget, color, include ->
                viewModel.addProject(name, icon, budget, color, include)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
private fun ProjectCard(
    summary: SideProjectViewModel.ProjectSummary,
    currency: String,
    formatter: NumberFormat,
    onClick: () -> Unit,
    onToggleInclude: () -> Unit
) {
    val project = summary.project
    val color = remember(project.color) {
        runCatching { Color(AndroidColor.parseColor(project.color)) }.getOrDefault(AccentBlue)
    }
    val overBudget = summary.spent > project.budget

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
                Text("${summary.expenseCount} expense${if (summary.expenseCount == 1) "" else "s"}", fontSize = 11.sp, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$currency ${formatter.format(summary.spent)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (overBudget) ErrorRed else TextPrimary)
                Text("of $currency ${formatter.format(project.budget)}", fontSize = 10.sp, color = TextMuted)
            }
        }

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

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggleInclude() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Include in main budget", fontSize = 12.sp, color = TextSecondary)
            Switch(checked = project.includeInMain, onCheckedChange = { onToggleInclude() })
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
                val overBudget = summary.spent > project.budget
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
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining", fontSize = 11.sp, color = TextMuted)
                                Text("$currency ${formatter.format(summary.remaining)}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Brand400)
                            }
                        }
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

// ── Add project sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProjectSheet(
    onSave: (name: String, icon: String, budget: Double, color: String, include: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val currency = LocalCurrency.current

    var name by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(ProjectIcons.first()) }
    var color by remember { mutableStateOf(ProjectColors.first()) }
    var include by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var budgetError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("New Side Project", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Project Name (e.g. App Marketing)") },
                isError = nameError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' }; budgetError = false },
                label = { Text("Budget ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = budgetError,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Icon", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectIcons.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (icon == i) Brand400.copy(alpha = 0.2f) else SurfaceEl)
                            .border(1.dp, if (icon == i) Brand400 else SurfaceVar, CircleShape)
                            .clickable { icon = i },
                        contentAlignment = Alignment.Center
                    ) { Text(i, fontSize = 18.sp) }
                }
            }

            Text("Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectColors.forEach { c ->
                    val parsed = remember(c) { Color(AndroidColor.parseColor(c)) }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parsed)
                            .border(2.dp, if (color == c) TextPrimary else Color.Transparent, CircleShape)
                            .clickable { color = c }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { include = !include }) {
                Switch(checked = include, onCheckedChange = { include = it })
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Include in main budget", fontSize = 14.sp, color = TextPrimary)
                    Text("Counts toward the dashboard's monthly spending", fontSize = 11.sp, color = TextMuted)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand400)
                    .clickable {
                        val budget = budgetText.toDoubleOrNull()
                        nameError = name.isBlank()
                        budgetError = budget == null || budget <= 0
                        if (nameError || budgetError) return@clickable
                        onSave(name, icon, budget!!, color, include)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Create Project", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
            }
        }
    }
}
