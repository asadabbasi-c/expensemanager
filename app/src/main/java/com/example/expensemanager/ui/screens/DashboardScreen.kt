package com.example.expensemanager.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.monetization.BannerAd
import com.example.expensemanager.monetization.ProManager
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.example.expensemanager.viewmodel.DashboardViewModel.GoalStatus
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel       : DashboardViewModel,
    onNavigateToGoals : () -> Unit = {},
    proManager      : ProManager? = null
) {
    val expensesByCategory by viewModel.expensesByCategory.collectAsStateWithLifecycle()
    val monthlyTotals      by viewModel.monthlyTotals.collectAsStateWithLifecycle()
    val currentMonthTotal  by viewModel.currentMonthTotal.collectAsStateWithLifecycle()
    val categoryColors     by viewModel.categoryColors.collectAsStateWithLifecycle()
    val dailyAverage       by viewModel.dailyAverage.collectAsStateWithLifecycle()
    val highestSpendingDay by viewModel.highestSpendingDay.collectAsStateWithLifecycle()
    val spendingVelocity   by viewModel.spendingVelocity.collectAsStateWithLifecycle()
    val topMerchants       by viewModel.topMerchants.collectAsStateWithLifecycle()
    val selectedCategory   by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val drillDownExpenses  by viewModel.drillDownExpenses.collectAsStateWithLifecycle()
    val goalStatus         by viewModel.goalStatus.collectAsStateWithLifecycle()
    val dailyBudget        by viewModel.dailyBudget.collectAsStateWithLifecycle()
    val monthlySavings     by viewModel.monthlySavings.collectAsStateWithLifecycle()
    val totalSavings       by viewModel.totalSavings.collectAsStateWithLifecycle()

    val isPro by (proManager?.isPro ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) })
        .collectAsStateWithLifecycle()

    val currency = com.example.expensemanager.ui.theme.LocalCurrency.current

    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }

    val surfaceArgb   = Surface.toArgb()
    val onSurfaceArgb = TextPrimary.toArgb()
    val primaryArgb   = Brand400.toArgb()
    val darkGreenArgb = Color(0xFF1E3A28).toArgb()
    val dividerArgb   = Divider.toArgb()

    val totalAllTime = remember(expensesByCategory) { expensesByCategory.values.sum() }

    val categoryComposeColors = remember(categoryColors) {
        categoryColors.mapValues { (_, hex) ->
            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color(0xFF888888))
        }
    }

    // Drill-down sheet
    if (selectedCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectCategory(null) },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = Surface,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            DrillDownSheet(
                categoryName = selectedCategory!!,
                expenses     = drillDownExpenses,
                formatter    = formatter,
                onClose      = { viewModel.selectCategory(null) }
            )
        }
    }

    Scaffold(containerColor = Bg, bottomBar = { if (!isPro) BannerAd() }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Bg)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Custom header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                val currentMonthLabel = remember {
                    val cal = Calendar.getInstance()
                    val m = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
                    "$m ${cal.get(Calendar.YEAR)}"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceEl)
                        .border(1.dp, SurfaceVar, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("$currentMonthLabel ▾", fontSize = 12.sp, color = TextSecondary)
                }
            }

            // ── Horizontal stat cards ─────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                item {
                    DashStatCard("Spent", "$currency ${formatter.format(currentMonthTotal)}", color = ErrorRed)
                }
                item {
                    DashStatCard("Daily Avg", "$currency ${formatter.format(dailyAverage)}", color = TextPrimary)
                }
                if (expensesByCategory.isNotEmpty()) {
                    val top = expensesByCategory.maxByOrNull { it.value }
                    if (top != null) {
                        item {
                            val pct = if (totalAllTime > 0) (top.value / totalAllTime * 100).toInt() else 0
                            DashStatCard(
                                label = "Top Category",
                                value = top.key,
                                sub   = "$pct% of total",
                                color = categoryComposeColors[top.key] ?: Brand400
                            )
                        }
                    }
                }
                item {
                    DashStatCard("All Time", "$currency ${formatter.format(totalAllTime)}", color = Brand400)
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Goal / Budget card ────────────────────────────────────────────
            when (val status = goalStatus) {
                is GoalStatus.NoGoal -> DashCard { SetGoalCtaCard(onClick = onNavigateToGoals) }
                is GoalStatus.OnTrack -> DashCard {
                    GoalProgressSection(status.spent, status.budget, status.percent, dailyBudget,
                        Brand400, "On Track 🎯", formatter, currency, onNavigateToGoals)
                }
                is GoalStatus.Warning -> DashCard {
                    GoalProgressSection(status.spent, status.budget, status.percent, dailyBudget,
                        WarnAmber, "Near Limit ⚠️", formatter, currency, onNavigateToGoals)
                }
                is GoalStatus.OverBudget -> DashCard {
                    GoalProgressSection(status.spent, status.budget, status.percent, dailyBudget,
                        ErrorRed, "Over Budget 🚨", formatter, currency, onNavigateToGoals)
                }
            }

            // ── Category donut + legend ───────────────────────────────────────
            if (expensesByCategory.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                DashCard {
                    Column {
                        Text("Spending by Category", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Tap a slice for drilldown", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                        legend.isEnabled = false
                                        setEntryLabelTextSize(0f)
                                        animateY(700)
                                        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                                (e as? PieEntry)?.label?.let { viewModel.selectCategory(it) }
                                            }
                                            override fun onNothingSelected() {}
                                        })
                                    }
                                },
                                update = { chart ->
                                    if (expensesByCategory.isEmpty()) return@AndroidView
                                    val sorted  = expensesByCategory.entries.sortedByDescending { it.value }
                                    val entries = sorted.map { PieEntry(it.value.toFloat(), it.key) }
                                    val colors  = sorted.map { (name, _) ->
                                        categoryComposeColors[name]?.toArgb() ?: AndroidColor.GRAY
                                    }
                                    val dataSet = PieDataSet(entries, "").apply {
                                        this.colors = colors.toMutableList()
                                        valueTextColor = 0x00000000  // transparent
                                        valueTextSize = 0f
                                    }
                                    chart.data = PieData(dataSet).apply {
                                        setValueFormatter(PercentFormatter(chart))
                                    }
                                    chart.invalidate()
                                },
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(8.dp)
                            )

                            // Center Total
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", fontSize = 11.sp, color = TextMuted)
                                Text("$currency${formatter.format(totalAllTime)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Custom Legend
                        val sorted = expensesByCategory.entries.sortedByDescending { it.value }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sorted.chunked(2).forEach { rowItems ->
                                Row(Modifier.fillMaxWidth()) {
                                    rowItems.forEach { (cat, amt) ->
                                        val pct = if (totalAllTime > 0) (amt / totalAllTime * 100).toInt() else 0
                                        Row(
                                            Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(categoryComposeColors[cat] ?: Brand400)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(cat, fontSize = 12.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("$pct% • $currency${formatter.format(amt)}", fontSize = 10.sp, color = TextMuted)
                                            }
                                        }
                                    }
                                    if (rowItems.size < 2) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ── Monthly Trends ────────────────────────────────────────────────
            if (monthlyTotals.size >= 2) {
                Spacer(Modifier.height(16.dp))
                DashCard {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Monthly Trends", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            val currentMonthlySavings = monthlySavings.values.lastOrNull() ?: 0.0
                            if (currentMonthlySavings > 0) {
                                Badge(containerColor = Brand400.copy(alpha = 0.1f), contentColor = Brand400) {
                                    Text("Saved $currency${formatter.format(currentMonthlySavings)} this month", modifier = Modifier.padding(4.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        AndroidView(
                            factory = { context ->
                                BarChart(context).apply {
                                    description.isEnabled = false
                                    legend.isEnabled = false
                                    setTouchEnabled(false)
                                    setScaleEnabled(false)
                                    setPinchZoom(false)
                                    setDrawGridBackground(false)
                                    setDrawBarShadow(false)

                                    xAxis.apply {
                                        position = XAxis.XAxisPosition.BOTTOM
                                        setDrawGridLines(false)
                                        textColor = onSurfaceArgb
                                        textSize = 10f
                                        granularity = 1f
                                    }
                                    axisLeft.apply {
                                        setDrawGridLines(true)
                                        gridColor = dividerArgb
                                        textColor = onSurfaceArgb
                                        textSize = 10f
                                        axisMinimum = 0f
                                    }
                                    axisRight.isEnabled = false
                                }
                            },
                            update = { chart ->
                                val entries = monthlyTotals.entries.sortedBy { it.key }.takeLast(6)
                                if (entries.isEmpty()) return@AndroidView

                                val barEntries = entries.indices.map { BarEntry(it.toFloat(), entries[it].value.toFloat()) }
                                val dataSet = BarDataSet(barEntries, "").apply {
                                    color = primaryArgb
                                    setDrawValues(false)
                                    highLightAlpha = 0
                                }

                                chart.xAxis.valueFormatter = IndexAxisValueFormatter(entries.map { it.key })
                                chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                                chart.invalidate()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }
            }

            // ── Extra Insights ────────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            DashCard {
                Column {
                    Text("Financial Insights", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))

                    val highestSpendingDayText = highestSpendingDay?.let { "${it.first}: $currency${formatter.format(it.second)}" } ?: "None"
                    InsightRow(Icons.AutoMirrored.Filled.TrendingUp, "Highest Spending Day", highestSpendingDayText)
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divider, thickness = 0.5.dp)
                    
                    val velocityText = when {
                        spendingVelocity > 1.1f -> "Spending faster than usual"
                        spendingVelocity < 0.9f -> "Spending less than usual"
                        else -> "Spending at normal rate"
                    }
                    val velocityColor = if (spendingVelocity > 1.1f) ErrorRed else if (spendingVelocity < 0.9f) Brand400 else TextSecondary
                    InsightRow(Icons.Default.Speed, "Spending Velocity", velocityText, velocityColor)
                    
                    if (totalSavings > 0) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divider, thickness = 0.5.dp)
                        InsightRow(Icons.Default.Savings, "Total Estimated Savings", "$currency${formatter.format(totalSavings)}", Brand400)
                    }

                    if (topMerchants.isNotEmpty() && isPro) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divider, thickness = 0.5.dp)
                        Text("Top Merchants", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp))
                        topMerchants.take(3).forEach { (merchant, count) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(merchant, fontSize = 12.sp, color = TextPrimary)
                                Text("${count.toInt()} spent", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    } else if (!isPro && topMerchants.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divider, thickness = 0.5.dp)
                        Text("Upgrade to Pro to see merchant insights", fontSize = 11.sp, color = Brand400, modifier = Modifier.clickable { /* TODO */ })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun DashCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = SurfaceEl,
        shape = RoundedCornerShape(20.dp),
        border = borderStroke()
    ) {
        Column(Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun DashStatCard(label: String, value: String, sub: String? = null, color: Color) {
    Surface(
        modifier = Modifier.width(140.dp),
        color = SurfaceEl,
        shape = RoundedCornerShape(16.dp),
        border = borderStroke()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 11.sp, color = TextMuted)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) {
                Text(sub, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun InsightRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, valueColor: Color = TextPrimary) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = TextMuted)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
fun SetGoalCtaCard(onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("No Monthly Goal Set", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Track your progress by setting a budget.", fontSize = 12.sp, color = TextSecondary)
        }
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand400),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text("Set Goal", fontSize = 12.sp)
        }
    }
}

@Composable
fun GoalProgressSection(
    spent: Double, budget: Double, percent: Double, dailyBudget: Double,
    color: Color, statusText: String, formatter: NumberFormat, currency: String,
    onNavigate: () -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("Monthly Budget", fontSize = 11.sp, color = TextMuted)
                Text("$currency${formatter.format(spent)} / $currency${formatter.format(budget)}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            }
            Text(statusText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(bottom = 2.dp))
        }
        
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { percent.toFloat().coerceAtMost(1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.05f))
                .clickable { onNavigate() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, null, Modifier.size(14.dp), tint = color)
            Spacer(Modifier.width(8.dp))
            Text(
                "Daily allowance: $currency${formatter.format(dailyBudget)}",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(14.dp), tint = TextMuted)
        }
    }
}

@Composable
fun DrillDownSheet(
    categoryName: String,
    expenses: List<Expense>,
    formatter: NumberFormat,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(categoryName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${expenses.size} transactions", fontSize = 13.sp, color = TextMuted)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, null, tint = TextPrimary)
            }
        }

        Spacer(Modifier.height(20.dp))

        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions found", color = TextMuted)
            }
        } else {
            val currency = com.example.expensemanager.ui.theme.LocalCurrency.current
            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            
            Column(Modifier.verticalScroll(rememberScrollState())) {
                expenses.sortedByDescending { it.date }.forEach { exp ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(exp.description.ifBlank { categoryName }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val dateStr = remember(exp.date) {
                                try {
                                    val date = dateFormat.parse(exp.date)
                                    if (date != null) {
                                        val cal = Calendar.getInstance().apply { time = date }
                                        "${cal.get(Calendar.DAY_OF_MONTH)} ${cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())}"
                                    } else {
                                        exp.date
                                    }
                                } catch (e: Exception) {
                                    exp.date
                                }
                            }
                            Text(dateStr, fontSize = 12.sp, color = TextMuted)
                        }
                        Text("$currency${formatter.format(exp.amount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    HorizontalDivider(color = Divider, thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVar)
