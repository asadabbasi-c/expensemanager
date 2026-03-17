package com.example.expensemanager.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.viewmodel.DashboardViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val expensesByCategory by viewModel.expensesByCategory.collectAsStateWithLifecycle()
    val monthlyTotals      by viewModel.monthlyTotals.collectAsStateWithLifecycle()
    val currentMonthTotal  by viewModel.currentMonthTotal.collectAsStateWithLifecycle()
    val categoryColors     by viewModel.categoryColors.collectAsStateWithLifecycle()

    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 2
    }

    val surfaceArgb    = MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceArgb  = MaterialTheme.colorScheme.onSurface.toArgb()
    val primaryArgb    = MaterialTheme.colorScheme.primary.toArgb()
    val gradStart      = MaterialTheme.colorScheme.primary
    val gradEnd        = MaterialTheme.colorScheme.tertiary

    val totalAllTime = remember(expensesByCategory) { expensesByCategory.values.sum() }

    // Pre-parse hex → Compose Color for category bars
    val categoryComposeColors = remember(categoryColors) {
        categoryColors.mapValues { (_, hex) ->
            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color(0xFF94A3B8))
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

            // ── Hero gradient card ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "This Month's Spending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "PKR ${formatter.format(currentMonthTotal)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (totalAllTime > 0) {
                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.25f), thickness = 1.dp)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "All Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "PKR ${formatter.format(totalAllTime)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Categories",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${expensesByCategory.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ── Category breakdown card ───────────────────────────────────────
            if (expensesByCategory.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Spending by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))

                        // Pie chart using category colours
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
                                }
                            },
                            update = { chart ->
                                if (expensesByCategory.isEmpty()) return@AndroidView
                                val sorted = expensesByCategory.entries.sortedByDescending { it.value }
                                val entries = sorted.map { PieEntry(it.value.toFloat(), it.key) }
                                val colors = sorted.map { (name, _) ->
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )

                        Spacer(Modifier.height(20.dp))

                        // Progress-bar breakdown
                        expensesByCategory.entries
                            .sortedByDescending { it.value }
                            .forEach { (name, amount) ->
                                val barColor = categoryComposeColors[name]
                                    ?: MaterialTheme.colorScheme.primary
                                val pct = if (totalAllTime > 0) (amount / totalAllTime).toFloat() else 0f

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(barColor)
                                        )
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "PKR ${formatter.format(amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                @Suppress("DEPRECATION")
                                LinearProgressIndicator(
                                    progress = pct,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                    }
                }
            }

            // ── Monthly trends card ───────────────────────────────────────────
            if (monthlyTotals.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Monthly Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Last 6 months",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        AndroidView(
                            factory = { context ->
                                BarChart(context).apply {
                                    description.isEnabled = false
                                    legend.isEnabled = false
                                    setDrawGridBackground(false)
                                    setDrawBarShadow(false)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (expensesByCategory.isEmpty() && monthlyTotals.values.all { it == 0.0 }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Text("📊", style = MaterialTheme.typography.displaySmall) }
                        Text(
                            "No data yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Add some expenses to see your spending analytics",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
