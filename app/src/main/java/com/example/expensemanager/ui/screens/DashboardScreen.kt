package com.example.expensemanager.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
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

    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 2
    }
    val shortFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }

    val surfaceArgb   = MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val primaryArgb   = MaterialTheme.colorScheme.primary.toArgb()
    val gradStart     = MaterialTheme.colorScheme.primary
    val gradEnd       = MaterialTheme.colorScheme.tertiary

    val totalAllTime = remember(expensesByCategory) { expensesByCategory.values.sum() }

    val categoryComposeColors = remember(categoryColors) {
        categoryColors.mapValues { (_, hex) ->
            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color(0xFF94A3B8))
        }
    }

    // Drill-down bottom sheet
    if (selectedCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectCategory(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            DrillDownSheet(
                categoryName = selectedCategory!!,
                expenses = drillDownExpenses,
                formatter = formatter,
                onClose = { viewModel.selectCategory(null) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Hero gradient card ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
                    .padding(24.dp)
            ) {
                Column {
                    Text("This Month's Spending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Text("PKR ${formatter.format(currentMonthTotal)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                    if (totalAllTime > 0) {
                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.25f), thickness = 1.dp)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("All Time", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f))
                                Text("PKR ${shortFormatter.format(totalAllTime)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Categories", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f))
                                Text("${expensesByCategory.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // ── Quick Metrics 2×2 grid ────────────────────────────────────
            if (currentMonthTotal > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Quick Stats", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricTile(
                                label = "Daily Avg",
                                value = "PKR ${shortFormatter.format(dailyAverage)}",
                                color = gradStart,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Projected",
                                value = "PKR ${shortFormatter.format(spendingVelocity)}",
                                color = gradEnd,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricTile(
                                label = "Peak Day",
                                value = highestSpendingDay?.let { shortFormatter.format(it.second) }
                                    ?.let { "PKR $it" } ?: "—",
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Categories",
                                value = "${expensesByCategory.size} active",
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Category breakdown + tappable pie chart ───────────────────
            if (expensesByCategory.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Spending by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("Tap a slice for details",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

                        AndroidView(
                            factory = { context ->
                                PieChart(context).apply {
                                    description.isEnabled = false
                                    setUsePercentValues(true)
                                    isDrawHoleEnabled = true
                                    setHoleColor(surfaceArgb)
                                    holeRadius = 52f
                                    transparentCircleRadius = 57f
                                    legend.isEnabled = false
                                    setEntryLabelTextSize(10f)
                                    setEntryLabelColor(AndroidColor.WHITE)
                                    animateY(700)
                                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                                            (e as? PieEntry)?.label?.let { viewModel.selectCategory(it) }
                                        }
                                        override fun onNothingSelected() { /* keep sheet open */ }
                                    })
                                }
                            },
                            update = { chart ->
                                if (expensesByCategory.isEmpty()) return@AndroidView
                                val sorted = expensesByCategory.entries.sortedByDescending { it.value }
                                val entries = sorted.map { PieEntry(it.value.toFloat(), it.key) }
                                val colors  = sorted.map { (name, _) ->
                                    categoryComposeColors[name]?.toArgb() ?: AndroidColor.GRAY
                                }
                                val dataSet = PieDataSet(entries, "").apply {
                                    this.colors = colors.toMutableList()
                                    valueTextColor = AndroidColor.WHITE
                                    valueTextSize = 10f
                                    valueFormatter = PercentFormatter(chart)
                                    sliceSpace = 3f
                                }
                                chart.data = PieData(dataSet)
                                chart.invalidate()
                            },
                            modifier = Modifier.fillMaxWidth().height(240.dp)
                        )

                        Spacer(Modifier.height(20.dp))

                        expensesByCategory.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
                            val barColor = categoryComposeColors[name] ?: MaterialTheme.colorScheme.primary
                            val pct = if (totalAllTime > 0) (amount / totalAllTime).toFloat() else 0f
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(barColor))
                                    Text(name, style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium)
                                }
                                Text("PKR ${formatter.format(amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            @Suppress("DEPRECATION")
                            LinearProgressIndicator(
                                progress = pct,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Top Merchants card ────────────────────────────────────────
            if (topMerchants.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Top Merchants", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        topMerchants.forEachIndexed { i, (merchant, amount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape)
                                            .background(gradStart.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${i + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = gradStart)
                                    }
                                    Text(merchant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 160.dp))
                                }
                                Text("PKR ${shortFormatter.format(amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            if (i < topMerchants.lastIndex)
                                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        }
                    }
                }
            }

            // ── Monthly Trends bar chart ──────────────────────────────────
            if (monthlyTotals.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Monthly Trends", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("Last 6 months",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
                        AndroidView(
                            factory = { context ->
                                BarChart(context).apply {
                                    description.isEnabled = false
                                    legend.isEnabled = false
                                    setDrawGridBackground(false)
                                    setScaleEnabled(false)
                                    setPinchZoom(false)
                                    setDrawBorders(false)
                                    xAxis.apply {
                                        position = XAxis.XAxisPosition.BOTTOM
                                        setDrawGridLines(false)
                                        setDrawAxisLine(false)
                                        granularity = 1f
                                        textColor = onSurfaceArgb
                                        textSize = 10f
                                    }
                                    axisLeft.apply {
                                        setDrawGridLines(true)
                                        gridColor = AndroidColor.parseColor("#1A000000")
                                        setDrawAxisLine(false)
                                        textColor = onSurfaceArgb
                                        textSize = 9f
                                        axisMinimum = 0f
                                    }
                                    axisRight.isEnabled = false
                                    animateY(700)
                                }
                            },
                            update = { chart ->
                                if (monthlyTotals.isEmpty()) return@AndroidView
                                val labels  = monthlyTotals.keys.toList()
                                val entries = monthlyTotals.entries.toList()
                                    .mapIndexed { i, e -> BarEntry(i.toFloat(), e.value.toFloat()) }
                                val dataSet = BarDataSet(entries, "").apply {
                                    color = primaryArgb
                                    valueTextColor = onSurfaceArgb
                                    valueTextSize = 9f
                                }
                                chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                                chart.xAxis.labelCount = labels.size
                                chart.data = BarData(dataSet).apply { barWidth = 0.55f }
                                chart.invalidate()
                            },
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────
            if (expensesByCategory.isEmpty() && monthlyTotals.values.all { it == 0.0 }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Text("📊", style = MaterialTheme.typography.displaySmall) }
                        Text("No data yet", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("Add some expenses to see your spending analytics",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Metric tile ───────────────────────────────────────────────────────────────

@Composable
private fun MetricTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold, color = color, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Drill-down bottom sheet content ──────────────────────────────────────────

@Composable
private fun DrillDownSheet(
    categoryName: String,
    expenses: List<Expense>,
    formatter: NumberFormat,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(categoryName, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text("${expenses.size} transaction${if (expenses.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
        Spacer(Modifier.height(4.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))

        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No expenses found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            expenses.forEach { expense ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.merchant?.takeIf { it.isNotBlank() }
                                ?: expense.description.takeIf { it.isNotBlank() }
                                ?: "Expense",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(expense.date, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!expense.bankName.isNullOrBlank()) {
                                Text("· ${expense.bankName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (expense.source == "sms") {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text("SMS", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                    Text("PKR ${formatter.format(expense.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error)
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
            }
        }
    }
}
