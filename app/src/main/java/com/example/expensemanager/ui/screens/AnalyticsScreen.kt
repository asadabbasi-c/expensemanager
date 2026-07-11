package com.example.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.expensemanager.data.model.ProjectTypes
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.viewmodel.AnalyticsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.NumberFormat
import java.util.*
import android.graphics.Color as AndroidColor

/**
 * In-depth analytics body — hosted inside the Dashboard's "Analytics" tab
 * (Pro). Scrolls on its own; no header of its own.
 */
@Composable
fun AnalyticsContent(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val monthMetrics    by viewModel.monthMetrics.collectAsStateWithLifecycle()
    val savingsProgress by viewModel.savingsGoalProgress.collectAsStateWithLifecycle()
    val forecast        by viewModel.forecast.collectAsStateWithLifecycle()
    val categoryTrends  by viewModel.categoryTrends.collectAsStateWithLifecycle()
    val spendingHistory by viewModel.spendingHistory.collectAsStateWithLifecycle()
    val subscriptions   by viewModel.subscriptionStats.collectAsStateWithLifecycle()
    val heatmap         by viewModel.dailyHeatmap.collectAsStateWithLifecycle()
    val projectMetrics  by viewModel.projectMetrics.collectAsStateWithLifecycle()
    val emergencies     by viewModel.emergencyMetrics.collectAsStateWithLifecycle()
    val incomeStats     by viewModel.incomeStats.collectAsStateWithLifecycle()
    val insights        by viewModel.insights.collectAsStateWithLifecycle()

    val currency  = LocalCurrency.current
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }

    val current = monthMetrics.lastOrNull()
    val savingsRate = current?.let { if (it.income > 0) (it.savings / it.income * 100) else null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(0.dp))

        // ── Headline stat tiles ───────────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val saved = current?.savings ?: 0.0
                AnalyticsStatTile(
                    "Saved This Month",
                    "$currency ${formatter.format(saved)}",
                    if (saved >= 0) Brand400 else ErrorRed
                )
            }
            item {
                AnalyticsStatTile(
                    "Savings Rate",
                    savingsRate?.let { "${it.toInt()}%" } ?: "—",
                    AccentPurple,
                    sub = "of income kept"
                )
            }
            item {
                AnalyticsStatTile(
                    "Income",
                    "$currency ${formatter.format(current?.income ?: 0.0)}",
                    AccentBlue,
                    sub = "${incomeStats.sources} source${if (incomeStats.sources == 1) "" else "s"}"
                )
            }
            item {
                AnalyticsStatTile(
                    "Avg / Expense",
                    "$currency ${formatter.format(insights.avgTransaction)}",
                    TextPrimary,
                    sub = "${insights.transactionCount} this month"
                )
            }
        }

        // ── End-of-month forecast ─────────────────────────────────────────────
        forecast?.let { fc ->
            AnalyticsCard(title = "End-of-Month Forecast") {
                val over = fc.budget > 0 && fc.overBy > 0
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Projected spend at this pace", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "$currency ${formatter.format(fc.projected)}",
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (over) ErrorRed else TextPrimary
                        )
                    }
                    if (fc.budget > 0) {
                        Text(
                            if (over) "▲ $currency${formatter.format(fc.overBy)} over budget"
                            else "▼ $currency${formatter.format(-fc.overBy)} under budget",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (over) ErrorRed else Brand400,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                if (fc.budget > 0) {
                    Spacer(Modifier.height(10.dp))
                    val fraction = (fc.projected / fc.budget).toFloat()
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = if (over) ErrorRed else Brand400,
                        trackColor = SurfaceVar
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("vs monthly budget of $currency ${formatter.format(fc.budget)}", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        // ── Savings goal progress ─────────────────────────────────────────────
        savingsProgress?.let { progress ->
            AnalyticsCard(title = "Savings Goal") {
                val onTrack = progress.actual >= progress.target
                val color = when {
                    onTrack                  -> Brand400
                    progress.percent >= 0.5  -> WarnAmber
                    else                     -> ErrorRed
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$currency${formatter.format(progress.actual)} / $currency${formatter.format(progress.target)}",
                        fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary
                    )
                    Text(
                        if (onTrack) "Goal reached 🎉" else "${(progress.percent * 100).toInt()}% there",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress.percent.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
            }
        }

        // ── Earning vs spending (cashflow), month by month ────────────────────
        if (monthMetrics.count { it.income > 0 || it.spent > 0 } >= 2) {
            AnalyticsCard(title = "Earning vs Spending") {
                val onSurfaceArgb = TextPrimary.toArgb()
                val dividerArgb   = Divider.toArgb()
                val incomeArgb    = Brand400.toArgb()
                val spentArgb     = ErrorRed.toArgb()
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
                                setCenterAxisLabels(true)
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
                        if (monthMetrics.isEmpty()) return@AndroidView
                        val incomeEntries = monthMetrics.indices.map { BarEntry(it.toFloat(), monthMetrics[it].income.toFloat()) }
                        val spentEntries  = monthMetrics.indices.map { BarEntry(it.toFloat(), monthMetrics[it].spent.toFloat()) }
                        val incomeSet = BarDataSet(incomeEntries, "Income").apply {
                            color = incomeArgb; setDrawValues(false); highLightAlpha = 0
                        }
                        val spentSet = BarDataSet(spentEntries, "Spent").apply {
                            color = spentArgb; setDrawValues(false); highLightAlpha = 0
                        }
                        val groupSpace = 0.3f
                        val barSpace   = 0.05f
                        val barWidth   = 0.3f   // (0.3 + 0.05) * 2 + 0.3 = 1.0
                        val data = BarData(incomeSet, spentSet).apply { this.barWidth = barWidth }
                        chart.data = data
                        chart.xAxis.axisMinimum = 0f
                        chart.xAxis.axisMaximum = monthMetrics.size.toFloat()
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(monthMetrics.map { it.label })
                        data.groupBars(0f, groupSpace, barSpace)
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(Brand400, "Income")
                    LegendDot(ErrorRed, "Spent")
                }
            }
        }

        // ── Savings trend chart ───────────────────────────────────────────────
        if (monthMetrics.count { it.income > 0 || it.spent > 0 } >= 2) {
            AnalyticsCard(title = "Savings Trend") {
                val onSurfaceArgb = TextPrimary.toArgb()
                val dividerArgb   = Divider.toArgb()
                val positiveArgb  = Brand400.toArgb()
                val negativeArgb  = ErrorRed.toArgb()
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
                            }
                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        if (monthMetrics.isEmpty()) return@AndroidView
                        val entries = monthMetrics.indices.map {
                            BarEntry(it.toFloat(), monthMetrics[it].savings.toFloat())
                        }
                        val dataSet = BarDataSet(entries, "").apply {
                            colors = monthMetrics.map { if (it.savings >= 0) positiveArgb else negativeArgb }
                            setDrawValues(false)
                            highLightAlpha = 0
                        }
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(monthMetrics.map { it.label })
                        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(Brand400, "Saved")
                    LegendDot(ErrorRed, "Overspent")
                }
            }
        }

        // ── 12-month spending history ─────────────────────────────────────────
        if (spendingHistory.count { it.second > 0 } >= 3) {
            AnalyticsCard(title = "12-Month Spending History") {
                val onSurfaceArgb = TextPrimary.toArgb()
                val dividerArgb   = Divider.toArgb()
                val barArgb       = AccentBlue.toArgb()
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
                                textSize = 8f
                                granularity = 1f
                                labelRotationAngle = -45f
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
                        if (spendingHistory.isEmpty()) return@AndroidView
                        val entries = spendingHistory.indices.map {
                            BarEntry(it.toFloat(), spendingHistory[it].second.toFloat())
                        }
                        val dataSet = BarDataSet(entries, "").apply {
                            color = barArgb
                            setDrawValues(false)
                            highLightAlpha = 0
                        }
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(spendingHistory.map { it.first })
                        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }

        // ── Category trends ───────────────────────────────────────────────────
        if (categoryTrends.isNotEmpty()) {
            AnalyticsCard(title = "Category Trends (6 months)") {
                categoryTrends.forEachIndexed { index, trend ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divider, thickness = 0.5.dp)
                    val catColor = remember(trend.color) {
                        runCatching { Color(AndroidColor.parseColor(trend.color)) }.getOrDefault(AccentBlue)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(catColor))
                        Spacer(Modifier.width(8.dp))
                        Text(trend.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))

                        // Mini 6-month bar strip
                        val maxVal = trend.monthly.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            trend.monthly.forEach { value ->
                                val h = ((value / maxVal) * 22).toInt().coerceAtLeast(2)
                                Box(
                                    Modifier
                                        .width(6.dp)
                                        .height(h.dp)
                                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                        .background(if (value > 0) catColor.copy(alpha = 0.85f) else SurfaceVar)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))
                        // Rising spend = warning-red, falling = green
                        val delta = trend.thisMonth - trend.lastMonth
                        val badgeText = when {
                            trend.lastMonth <= 0 && trend.thisMonth > 0 -> "new"
                            trend.lastMonth <= 0                        -> "—"
                            else -> "${if (delta >= 0) "▲" else "▼"}${(kotlin.math.abs(delta) / trend.lastMonth * 100).toInt()}%"
                        }
                        Text(
                            badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = if (delta > 0) ErrorRed else Brand400,
                            modifier = Modifier.widthIn(min = 38.dp), textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        // ── Daily spending heatmap ────────────────────────────────────────────
        heatmap?.let { map ->
            AnalyticsCard(title = "This Month, Day by Day") {
                val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(Modifier.fillMaxWidth()) {
                    weekdays.forEach { d ->
                        Text(d, fontSize = 9.sp, color = TextMuted, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                val totalCells = map.firstWeekdayOffset + map.daysInMonth
                val rows = (totalCells + 6) / 7
                for (row in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - map.firstWeekdayOffset + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            day < 1 || day > map.daysInMonth -> Color.Transparent
                                            else -> {
                                                val spent = map.spendByDay[day] ?: 0.0
                                                if (spent <= 0 || map.maxSpend <= 0) SurfaceVar.copy(alpha = 0.5f)
                                                else ErrorRed.copy(alpha = (0.15f + 0.75f * (spent / map.maxSpend)).toFloat())
                                            }
                                        }
                                    )
                                    .then(
                                        if (day == map.today) Modifier.border(1.dp, TextPrimary, RoundedCornerShape(6.dp))
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day in 1..map.daysInMonth) {
                                    Text("$day", fontSize = 9.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("Darker = more spent that day", fontSize = 10.sp, color = TextMuted)
            }
        }

        // ── Project goals & budgets ───────────────────────────────────────────
        if (projectMetrics.isNotEmpty()) {
            AnalyticsCard(title = "Projects") {
                projectMetrics.forEachIndexed { index, metric ->
                    if (index > 0) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)
                    }
                    val projectColor = remember(metric.project.color) {
                        runCatching { Color(AndroidColor.parseColor(metric.project.color)) }.getOrDefault(AccentBlue)
                    }
                    val over = metric.project.hasBudget && metric.percentUsed > 1.0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(metric.project.icon, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(metric.project.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${ProjectTypes.info(metric.project.type).label} • ${metric.expenseCount} expense${if (metric.expenseCount == 1) "" else "s"}" +
                                    if (metric.sharedWithMain > 0)
                                        " • $currency${formatter.format(metric.sharedWithMain)} also in main"
                                    else "",
                                fontSize = 10.sp, color = TextMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$currency${formatter.format(metric.spent)}",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (over) ErrorRed else TextPrimary
                            )
                            Text(
                                if (metric.project.hasBudget) "of $currency${formatter.format(metric.project.budget)}" else "no budget",
                                fontSize = 10.sp, color = TextMuted
                            )
                        }
                    }
                    if (metric.project.hasBudget) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { metric.percentUsed.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = if (over) ErrorRed else projectColor,
                            trackColor = SurfaceVar
                        )
                        metric.burnDaysLeft?.let { days ->
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "🔥 At the current pace, budget runs out in ~$days day${if (days == 1) "" else "s"}",
                                fontSize = 10.sp,
                                color = if (days <= 7) ErrorRed else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // ── Emergency budgets ─────────────────────────────────────────────────
        if (emergencies.isNotEmpty()) {
            AnalyticsCard(title = "Emergency Budgets") {
                emergencies.take(4).forEachIndexed { index, metric ->
                    if (index > 0) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)
                    }
                    val color = when {
                        metric.percentUsed > 1.0  -> ErrorRed
                        metric.percentUsed >= 0.8 -> WarnAmber
                        else                      -> AccentBlue
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(metric.budget.name.ifBlank { "Emergency Budget" }, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = TextPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (metric.isRunning) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Brand400.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text("Active", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Brand400)
                                    }
                                }
                            }
                            Text("${metric.budget.startDate} → ${metric.budget.endDate}", fontSize = 10.sp, color = TextMuted)
                        }
                        Text(
                            "$currency${formatter.format(metric.spent)} / $currency${formatter.format(metric.budget.totalAmount)}",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (metric.percentUsed > 1.0) ErrorRed else TextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { metric.percentUsed.toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = color,
                        trackColor = SurfaceVar
                    )
                }
            }
        }

        // ── Monthly subscription analysis ─────────────────────────────────────
        subscriptions?.let { subs ->
            AnalyticsCard(title = "Subscriptions & Recurring") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Monthly cost of ${subs.count} recurring payment${if (subs.count == 1) "" else "s"}", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "$currency ${formatter.format(subs.monthlyTotal)}/mo",
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AccentPurple
                        )
                    }
                    subs.shareOfBudget?.let { share ->
                        Text(
                            "${(share * 100).toInt()}% of budget",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (share > 0.3) WarnAmber else TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                subs.topItems.forEachIndexed { index, (name, cost) ->
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, fontSize = 12.sp, color = TextPrimary, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("$currency${formatter.format(cost)}/mo", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        // ── Income breakdown ──────────────────────────────────────────────────
        if (incomeStats.total > 0) {
            AnalyticsCard(title = "Income This Month") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Recurring", fontSize = 11.sp, color = TextMuted)
                        Text("$currency ${formatter.format(incomeStats.recurring)}",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand400)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("One-time", fontSize = 11.sp, color = TextMuted)
                        Text("$currency ${formatter.format(incomeStats.oneTime)}",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
                Spacer(Modifier.height(10.dp))
                val recurringFraction = (incomeStats.recurring / incomeStats.total).toFloat()
                Row(Modifier.fillMaxWidth().height(8.dp)) {
                    if (recurringFraction > 0f) {
                        Box(
                            Modifier
                                .weight(recurringFraction.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Brand400)
                        )
                    }
                    if (recurringFraction < 1f) {
                        Spacer(Modifier.width(2.dp))
                        Box(
                            Modifier
                                .weight((1f - recurringFraction).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(AccentBlue)
                        )
                    }
                }
            }
        }

        // ── Insights ──────────────────────────────────────────────────────────
        AnalyticsCard(title = "Insights") {
            var hasRow = false
            insights.largestExpense?.let { largest ->
                hasRow = true
                InsightLine(
                    "Largest expense",
                    "${largest.description.ifBlank { insights.largestCategoryName ?: "Expense" }} — $currency${formatter.format(largest.amount)}"
                )
            }
            insights.busiestWeekday?.let { (day, avg) ->
                if (hasRow) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divider, thickness = 0.5.dp)
                hasRow = true
                InsightLine("Highest-spend weekday", "$day (avg $currency${formatter.format(avg)})")
            }
            insights.topCategoryShift?.let { (name, thisM, lastM) ->
                if (hasRow) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divider, thickness = 0.5.dp)
                hasRow = true
                val delta = thisM - lastM
                InsightLine(
                    "Biggest category increase",
                    if (lastM > 0) "$name (+$currency${formatter.format(delta)} vs last month)"
                    else "$name ($currency${formatter.format(thisM)} this month)"
                )
            }
            if (!hasRow) {
                Text("Add a few expenses to unlock insights", fontSize = 12.sp, color = TextMuted)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun AnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceEl)
            .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun AnalyticsStatTile(label: String, value: String, color: Color, sub: String? = null) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceEl)
            .border(1.dp, SurfaceVar, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, color = TextMuted)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) {
                Text(sub, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun InsightLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.4f), textAlign = TextAlign.End)
    }
}
