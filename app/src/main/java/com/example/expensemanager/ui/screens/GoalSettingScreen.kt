package com.example.expensemanager.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.ui.theme.LocalCurrency
import com.example.expensemanager.viewmodel.GoalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GoalSettingScreen(
    viewModel: GoalViewModel,
    onBack   : () -> Unit,
    onNavigateToIncome: () -> Unit = {}
) {
    val selectedMonth    by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthLabel       by viewModel.selectedMonthLabel.collectAsStateWithLifecycle()
    val goal             by viewModel.goalForMonth.collectAsStateWithLifecycle()
    val totalExtraIncome by viewModel.totalExtraIncomeForMonth.collectAsStateWithLifecycle()

    val currency  = LocalCurrency.current
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = remember { sdf.format(Date()) }

    // ── Local form state, seeded from the saved goal ────────────────────────────
    var incomeText     by remember(goal) { mutableStateOf(goal?.incomeTarget?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
    var budgetType     by remember(goal) { mutableStateOf(goal?.budgetType ?: "monthly") }
    var budgetMode     by remember(goal) { mutableStateOf(goal?.budgetMode ?: "fixed") }
    var budgetAmountText by remember(goal) { mutableStateOf(goal?.budgetAmount?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
    var budgetPercent  by remember(goal) { mutableStateOf(goal?.budgetPercent ?: 50.0) }
    var savingsPercent by remember(goal) { mutableStateOf(goal?.savingsPercent ?: 15.0) }
    var periodStart    by remember(goal) { mutableStateOf(goal?.periodStart ?: today) }
    var periodEnd      by remember(goal) { mutableStateOf(goal?.periodEnd ?: today) }

    val cal = Calendar.getInstance()
    val startPicker = DatePickerDialog(context,
        { _, y, m, d -> periodStart = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    val endPicker = DatePickerDialog(context,
        { _, y, m, d -> periodEnd = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

    // ── Live preview calculations ───────────────────────────────────────────────
    val income  = incomeText.toDoubleOrNull() ?: 0.0
    val budgetAmount = budgetAmountText.toDoubleOrNull() ?: 0.0

    val periodDays = remember(budgetType, periodStart, periodEnd) {
        when (budgetType) {
            "yearly" -> 365.0
            "custom" -> runCatching {
                val s = sdf.parse(periodStart)!!.time
                val e = sdf.parse(periodEnd)!!.time
                (((e - s) / 86_400_000L).toInt() + 1).coerceAtLeast(1).toDouble()
            }.getOrDefault(30.0)
            else -> 30.0
        }
    }

    // Resolve to a per-month spending limit
    val monthlyLimit = when (budgetMode) {
        "percent" -> income * (budgetPercent / 100.0)
        else -> when (budgetType) {
            "yearly" -> budgetAmount / 12.0
            "custom" -> budgetAmount / (periodDays / 30.0).coerceAtLeast(0.1)
            else -> budgetAmount
        }
    }

    val periodLimit = when (budgetMode) {
        "percent" -> when (budgetType) {
            "yearly" -> monthlyLimit * 12.0
            "custom" -> monthlyLimit * (periodDays / 30.0)
            else -> monthlyLimit
        }
        else -> if (budgetType == "monthly") monthlyLimit else budgetAmount
    }

    val dailyLimit = if (periodDays > 0) periodLimit / periodDays else 0.0
    val savingsTarget = income * (savingsPercent / 100.0)
    val freeSpending = (income - savingsTarget - monthlyLimit).coerceAtLeast(0.0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Budget Settings",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Month navigator ───────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .border(1.dp, SurfaceVar, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month",
                                tint = TextSecondary)
                        }
                        Text(
                            text       = monthLabel,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                        IconButton(onClick = { viewModel.navigateMonth(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month",
                                tint = TextSecondary)
                        }
                    }
                }

                // ── Period type ──────────────────────────────────────────────
                SettingsCard(title = "Budget Period") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("monthly" to "Monthly", "yearly" to "Yearly", "custom" to "Custom").forEach { (key, label) ->
                            SegmentChip(
                                label    = label,
                                selected = budgetType == key,
                                modifier = Modifier.weight(1f),
                                onClick  = { budgetType = key }
                            )
                        }
                    }

                    AnimatedVisibility(visible = budgetType == "custom") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = periodStart, onValueChange = {}, readOnly = true,
                                label = { Text("Start") },
                                trailingIcon = { IconButton(onClick = { startPicker.show() }) { Text("📅") } },
                                modifier = Modifier.weight(1f).clickable { startPicker.show() }
                            )
                            OutlinedTextField(
                                value = periodEnd, onValueChange = {}, readOnly = true,
                                label = { Text("End") },
                                trailingIcon = { IconButton(onClick = { endPicker.show() }) { Text("📅") } },
                                modifier = Modifier.weight(1f).clickable { endPicker.show() }
                            )
                        }
                    }
                }

                // ── Income & mode ────────────────────────────────────────────
                SettingsCard(title = "Income & Spending Mode") {
                    DarkInputField(
                        label    = "Monthly Income ($currency)",
                        value    = incomeText,
                        hint     = "e.g. 50000",
                        onChange = { incomeText = it.filter { c -> c.isDigit() || c == '.' } }
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SegmentChip(
                            label    = "Fixed Amount",
                            selected = budgetMode == "fixed",
                            modifier = Modifier.weight(1f),
                            onClick  = { budgetMode = "fixed" }
                        )
                        SegmentChip(
                            label    = "% of Income",
                            selected = budgetMode == "percent",
                            modifier = Modifier.weight(1f),
                            onClick  = { budgetMode = "percent" }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (budgetMode == "fixed") {
                        val periodLabel = when (budgetType) {
                            "yearly" -> "Yearly"
                            "custom" -> "Period"
                            else -> "Monthly"
                        }
                        DarkInputField(
                            label    = "$periodLabel Spending Limit ($currency)",
                            value    = budgetAmountText,
                            hint     = "e.g. 30000",
                            onChange = { budgetAmountText = it.filter { c -> c.isDigit() || c == '.' } }
                        )
                    } else {
                        Text("Spending Cap: ${budgetPercent.toInt()}% of income", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Slider(
                            value = budgetPercent.toFloat(),
                            onValueChange = { budgetPercent = it.toDouble() },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue, inactiveTrackColor = SurfaceVar)
                        )
                    }
                }

                // ── Savings goal slider ──────────────────────────────────────
                SettingsCard(title = "Savings Goal") {
                    Text("Save ${savingsPercent.toInt()}% of income", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Slider(
                        value = savingsPercent.toFloat(),
                        onValueChange = { savingsPercent = it.toDouble() },
                        valueRange = 5f..50f,
                        colors = SliderDefaults.colors(thumbColor = Brand400, activeTrackColor = Brand400, inactiveTrackColor = SurfaceVar)
                    )
                    Text("$currency ${formatter.format(savingsTarget)} per month", fontSize = 12.sp, color = TextMuted)
                }

                // ── Live preview ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenDim)
                        .border(1.dp, Brand400.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Live Preview", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            PreviewStat("Monthly Limit", "$currency ${formatter.format(monthlyLimit)}", Brand400)
                            PreviewStat("Daily Limit", "$currency ${formatter.format(dailyLimit)}", AccentBlue)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            PreviewStat("Savings Target", "$currency ${formatter.format(savingsTarget)}", AccentPurple)
                            PreviewStat("Free-Spending", "$currency ${formatter.format(freeSpending)}", WarnAmber)
                        }
                    }
                }

                // ── Save button ──────────────────────────────────────────────
                val canSave = incomeText.isNotBlank() && (budgetMode == "percent" || budgetAmountText.isNotBlank())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canSave) Brand400 else Brand400.copy(alpha = 0.35f))
                        .clickable(enabled = canSave) {
                            viewModel.saveBudgetSettings(
                                incomeTarget   = income,
                                monthlyLimit   = monthlyLimit,
                                budgetType     = budgetType,
                                budgetMode     = budgetMode,
                                budgetAmount   = budgetAmount,
                                budgetPercent  = budgetPercent,
                                savingsPercent = savingsPercent,
                                periodStart    = if (budgetType == "custom") periodStart else null,
                                periodEnd      = if (budgetType == "custom") periodEnd else null
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar(if (goal != null) "Budget updated!" else "Budget saved!")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (goal != null) "Update Budget" else "Save Budget",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF0A0A0A)
                    )
                }

                // ── Income sources link ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .border(1.dp, SurfaceVar, RoundedCornerShape(16.dp))
                        .clickable { onNavigateToIncome() }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("💰 Income Sources", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(
                                if (totalExtraIncome > 0) "+$currency ${formatter.format(totalExtraIncome)} this month" else "Add one-time or recurring income",
                                fontSize = 12.sp, color = TextMuted
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            content()
        }
    }
}

@Composable
private fun SegmentChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brand400 else SurfaceEl)
            .border(1.dp, if (selected) Brand400 else SurfaceVar, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color(0xFF0A0A0A) else TextSecondary)
    }
}

@Composable
private fun PreviewStat(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = TextMuted)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun DarkInputField(
    label   : String,
    value   : String,
    hint    : String,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceEl)
                .border(1.dp, SurfaceVar, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty()) {
                Text(hint, color = TextMuted, style = MaterialTheme.typography.bodyLarge)
            }
            androidx.compose.foundation.text.BasicTextField(
                value         = value,
                onValueChange = onChange,
                singleLine    = true,
                textStyle     = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}
