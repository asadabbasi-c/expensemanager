package com.example.expensemanager.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.viewmodel.SideBudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideBudgetScreen(
    viewModel: SideBudgetViewModel,
    onBack: () -> Unit
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val activeBudget by viewModel.activeBudget.collectAsStateWithLifecycle()
    val currency = LocalCurrency.current

    var showSetup by remember { mutableStateOf(false) }

    Scaffold(containerColor = Bg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Bg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                }
                Text("Side Budget", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                if (summary != null) {
                    Text(
                        "Edit",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Brand400,
                        modifier = Modifier.clickable { showSetup = true }
                    )
                }
            }

            val current = summary
            if (current == null) {
                EmptySideBudgetState(onCreate = { showSetup = true })
            } else {
                SideBudgetDetail(summary = current, currency = currency, onEnd = { viewModel.endSideBudget(activeBudget!!) })
            }
        }
    }

    if (showSetup) {
        SideBudgetSetupSheet(
            initialName  = activeBudget?.name ?: "",
            initialTotal = activeBudget?.totalAmount,
            initialStart = activeBudget?.startDate,
            initialEnd   = activeBudget?.endDate,
            onSave = { name, total, start, end ->
                viewModel.saveSideBudget(name, total, start, end)
                showSetup = false
            },
            onDismiss = { showSetup = false }
        )
    }
}

@Composable
private fun EmptySideBudgetState(onCreate: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("⏱", fontSize = 48.sp)
            Text("No side budget set", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "Set a time-boxed budget — e.g. PKR 80,000 until July 1 — and track your daily spending pace.",
                fontSize = 13.sp, color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand400)
                    .clickable { onCreate() }
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text("Set Up Side Budget", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
            }
        }
    }
}

@Composable
private fun SideBudgetDetail(
    summary: SideBudgetViewModel.SideBudgetSummary,
    currency: String,
    onEnd: () -> Unit
) {
    val budget = summary.budget
    val displaySdf = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val parseSdf   = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val rangeLabel = remember(budget.startDate, budget.endDate) {
        runCatching {
            "${displaySdf.format(parseSdf.parse(budget.startDate)!!)} – ${displaySdf.format(parseSdf.parse(budget.endDate)!!)}"
        }.getOrDefault("${budget.startDate} – ${budget.endDate}")
    }

    var showEndDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(budget.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(rangeLabel, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
            }
        }

        // ── Hero card ────────────────────────────────────────────────────────
        item {
            val overBudget = summary.totalSpent > budget.totalAmount
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface)
                    .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Spent", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "$currency ${"%,.0f".format(summary.totalSpent)}",
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (overBudget) ErrorRed else TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remaining", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "$currency ${"%,.0f".format(summary.remaining)}",
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Brand400
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(SurfaceVar)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(summary.percentUsed.toFloat().coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (overBudget) ErrorRed else Brand400)
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("of $currency ${"%,.0f".format(budget.totalAmount)} total", fontSize = 11.sp, color = TextMuted)
                    Text("${(summary.percentUsed * 100).toInt()}% used", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (overBudget) ErrorRed else TextSecondary)
                }
            }
        }

        // ── Daily limit cards ────────────────────────────────────────────────
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DailyLimitCard(
                    modifier = Modifier.weight(1f),
                    label    = "Original Daily",
                    amount   = summary.originalDailyLimit,
                    currency = currency,
                    color    = AccentBlue
                )
                val adjustedColor = if (summary.adjustedDailyLimit < summary.originalDailyLimit) ErrorRed else Brand400
                DailyLimitCard(
                    modifier = Modifier.weight(1f),
                    label    = "Adjusted Daily",
                    amount   = summary.adjustedDailyLimit,
                    currency = currency,
                    color    = adjustedColor
                )
            }
        }

        // ── Projection card ──────────────────────────────────────────────────
        item {
            val overProjected = summary.projectedTotal > budget.totalAmount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (overProjected) ErrorRed.copy(alpha = 0.1f) else GreenDim)
                    .border(1.dp, if (overProjected) ErrorRed.copy(alpha = 0.3f) else Brand400.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Projected End Total", fontSize = 11.sp, color = TextMuted)
                    Text(
                        "$currency ${"%,.0f".format(summary.projectedTotal)}",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = if (overProjected) ErrorRed else Brand400
                    )
                }
                Text(
                    if (overProjected) "⚠️ Over budget" else "✅ On track",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (overProjected) ErrorRed else Brand400
                )
            }
        }

        // ── Daily tracker ─────────────────────────────────────────────────────
        item {
            Text("Daily Tracker", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 6.dp))
        }
        items(summary.dayStatuses, key = { it.date }) { day ->
            DayTrackerRow(day = day, currency = currency)
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { showEndDialog = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("End Side Budget", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed)
            }
        }

        item { Spacer(Modifier.height(60.dp)) }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            containerColor   = Surface,
            title  = { Text("End side budget?", color = TextPrimary) },
            text   = { Text("This will stop tracking \"${budget.name}\". Past expenses are not affected.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onEnd(); showEndDialog = false }) {
                    Text("End", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun DailyLimitCard(modifier: Modifier, label: String, amount: Double, currency: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(label, fontSize = 11.sp, color = TextMuted)
        Text(
            "$currency ${"%,.0f".format(amount)}",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text("per day", fontSize = 10.sp, color = TextMuted)
    }
}

@Composable
private fun DayTrackerRow(day: SideBudgetViewModel.DayStatus, currency: String) {
    val displaySdf = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val parseSdf   = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val label = remember(day.date) {
        runCatching { displaySdf.format(parseSdf.parse(day.date)!!) }.getOrDefault(day.date)
    }
    val withinLimit = day.spent <= day.limit

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (day.isToday) Brand400.copy(alpha = 0.08f) else Surface)
            .border(1.dp, if (day.isToday) Brand400.copy(alpha = 0.3f) else SurfaceVar, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (day.isFuture) {
                Text("Limit: $currency ${"%,.0f".format(day.limit)}", fontSize = 11.sp, color = TextMuted)
            } else {
                Text("Spent: $currency ${"%,.0f".format(day.spent)} / $currency ${"%,.0f".format(day.limit)}", fontSize = 11.sp, color = TextMuted)
            }
        }

        if (!day.isFuture) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (withinLimit) Brand400.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (withinLimit) "✓" else "✗", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (withinLimit) Brand400 else ErrorRed)
            }
        }
    }
}

// ── Setup / Edit sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SideBudgetSetupSheet(
    initialName: String,
    initialTotal: Double?,
    initialStart: String?,
    initialEnd: String?,
    onSave: (name: String, total: Double, start: String, end: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context  = LocalContext.current
    val currency = LocalCurrency.current
    val sdf      = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today    = remember { sdf.format(Date()) }

    var name      by remember { mutableStateOf(initialName) }
    var totalText by remember { mutableStateOf(initialTotal?.let { "%.0f".format(it) } ?: "") }
    var startDate by remember { mutableStateOf(initialStart ?: today) }
    var endDate   by remember { mutableStateOf(initialEnd ?: today) }

    var nameError by remember { mutableStateOf(false) }
    var totalError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()
    val startPicker = DatePickerDialog(context,
        { _, y, m, d -> startDate = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    val endPicker = DatePickerDialog(context,
        { _, y, m, d -> endDate = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

    // Live preview of the daily limit
    val daysTotal = remember(startDate, endDate) {
        runCatching {
            val s = sdf.parse(startDate)!!.time
            val e = sdf.parse(endDate)!!.time
            (((e - s) / 86_400_000L).toInt() + 1).coerceAtLeast(1)
        }.getOrDefault(1)
    }
    val previewDailyLimit = remember(totalText, daysTotal) {
        val total = totalText.toDoubleOrNull() ?: 0.0
        if (daysTotal > 0) total / daysTotal else 0.0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Set Up Side Budget", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Name (e.g. June Spending)") },
                isError = nameError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = totalText,
                onValueChange = { totalText = it.filter { c -> c.isDigit() || c == '.' }; totalError = false },
                label = { Text("Total Budget ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = totalError,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate, onValueChange = {}, readOnly = true,
                    label = { Text("Start Date") },
                    trailingIcon = { IconButton(onClick = { startPicker.show() }) { Text("📅") } },
                    isError = dateError,
                    modifier = Modifier.weight(1f).clickable { startPicker.show() }
                )
                OutlinedTextField(
                    value = endDate, onValueChange = {}, readOnly = true,
                    label = { Text("End Date") },
                    trailingIcon = { IconButton(onClick = { endPicker.show() }) { Text("📅") } },
                    isError = dateError,
                    modifier = Modifier.weight(1f).clickable { endPicker.show() }
                )
            }

            if (totalText.toDoubleOrNull() != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GreenDim)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Daily Limit", fontSize = 11.sp, color = TextMuted)
                        Text("$currency ${"%,.0f".format(previewDailyLimit)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brand400)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Duration", fontSize = 11.sp, color = TextMuted)
                        Text("$daysTotal days", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand400)
                    .clickable {
                        val total = totalText.toDoubleOrNull()
                        nameError  = name.isBlank()
                        totalError = total == null || total <= 0
                        dateError  = endDate < startDate
                        if (nameError || totalError || dateError) return@clickable
                        onSave(name, total!!, startDate, endDate)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Save Side Budget", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
            }
        }
    }
}
