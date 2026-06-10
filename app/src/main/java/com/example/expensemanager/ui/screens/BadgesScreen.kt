package com.example.expensemanager.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.viewmodel.BadgeTier
import com.example.expensemanager.viewmodel.BadgeViewModel
import com.example.expensemanager.viewmodel.MonthScore
import kotlin.math.roundToInt

@Composable
fun BadgesScreen(
    viewModel: BadgeViewModel,
    onBack: () -> Unit
) {
    val score by viewModel.currentScore.collectAsStateWithLifecycle()
    val monthLabel by viewModel.selectedMonthLabel.collectAsStateWithLifecycle()
    val history by viewModel.monthHistory.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text("Badges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            // ── Month navigator ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(1.dp, SurfaceVar, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month", tint = TextSecondary)
                    }
                    Text(monthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    IconButton(onClick = { viewModel.navigateMonth(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next month", tint = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            val current = score
            if (current != null) {
                ScoreRingCard(score = current)
                Spacer(Modifier.height(20.dp))
                BreakdownCard(score = current)
                Spacer(Modifier.height(20.dp))
                TipsCard(score = current)
                Spacer(Modifier.height(20.dp))
                MonthHistoryCard(history = history, selectedMonth = current.month)
            }
        }
    }
}

private fun tierColor(tier: BadgeTier): Color = when (tier) {
    BadgeTier.EXTRAORDINARY -> Brand400
    BadgeTier.GOOD          -> AccentCyan
    BadgeTier.NORMAL        -> AccentBlue
    BadgeTier.BAD           -> WarnAmber
    BadgeTier.WORST         -> ErrorRed
}

@Composable
private fun ScoreRingCard(score: MonthScore) {
    val animatedScore by animateFloatAsState(
        targetValue   = score.totalScore.toFloat(),
        animationSpec = tween(durationMillis = 900),
        label         = "scoreAnim"
    )
    val color = tierColor(score.tier)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color    = SurfaceVar,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color    = color,
                        startAngle = -90f,
                        sweepAngle = 360f * (animatedScore / 100f).coerceIn(0f, 1f),
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(score.tier.emoji, fontSize = 36.sp)
                    Text(
                        "${animatedScore.roundToInt()}",
                        fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary
                    )
                    Text("/ 100", fontSize = 11.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
                Text(
                    score.tier.label,
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
        }
    }
}

@Composable
private fun BreakdownCard(score: MonthScore) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("SCORE BREAKDOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.07.sp)
            BreakdownBar("Budget Adherence", score.budgetAdherence, "35%", AccentBlue)
            BreakdownBar("Savings Rate", score.savingsRate, "30%", Brand400)
            BreakdownBar("Daily Discipline", score.dailyDiscipline, "25%", AccentPurple)
            BreakdownBar("Consistency", score.consistency, "10%", AccentAmber)
        }
    }
}

@Composable
private fun BreakdownBar(label: String, value: Double, weight: String, color: Color) {
    val animatedValue by animateFloatAsState(
        targetValue   = value.toFloat(),
        animationSpec = tween(durationMillis = 700),
        label         = "barAnim"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Surface(shape = RoundedCornerShape(4.dp), color = SurfaceEl) {
                    Text(weight, fontSize = 9.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
            Text("${value.roundToInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceVar)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (animatedValue / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun TipsCard(score: MonthScore) {
    val tips = buildList {
        if (score.budgetAdherence < 75.0) add("💡" to "You're spending more than your monthly budget — try trimming non-essential categories.")
        if (score.savingsRate < 75.0) add("💰" to "Boost your savings rate by setting aside money right after income arrives.")
        if (score.dailyDiscipline < 75.0) add("📅" to "Stay under your daily limit more consistently to lift your discipline score.")
        if (score.consistency < 75.0) add("📊" to "Smooth out big spending spikes for a steadier day-to-day pattern.")
        if (isEmpty()) add("🎉" to "Great job! Keep up your current habits to maintain this score.")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TIPS TO LEVEL UP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.07.sp)
            tips.forEach { (icon, text) ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(icon, fontSize = 16.sp)
                    Text(text, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MonthHistoryCard(history: List<MonthScore>, selectedMonth: String) {
    if (history.isEmpty()) return

    val labelSdf = remember { java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()) }
    val monthSdf = remember { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MONTH HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.07.sp)
            history.forEach { month ->
                val label = remember(month.month) {
                    runCatching { labelSdf.format(monthSdf.parse(month.month)!!) }.getOrDefault(month.month)
                }
                val isSelected = month.month == selectedMonth
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Brand400.copy(alpha = 0.07f) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(month.tier.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(month.tier.label, fontSize = 11.sp, color = tierColor(month.tier), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(10.dp))
                    Text("${month.totalScore.roundToInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}
