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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Income
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.viewmodel.GoalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IncomeScreen(
    viewModel: GoalViewModel,
    onBack: () -> Unit,
    proManager: com.example.expensemanager.monetization.ProManager? = null,
    interstitialAdManager: com.example.expensemanager.monetization.InterstitialAdManager? = null,
    onUpgrade: () -> Unit = {}
) {
    val monthLabel by viewModel.selectedMonthLabel.collectAsStateWithLifecycle()
    val incomeList by viewModel.incomeForMonth.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalExtraIncomeForMonth.collectAsStateWithLifecycle()
    val recurringSourceCount by viewModel.recurringSourceCount.collectAsStateWithLifecycle()
    val isPro by (proManager?.isPro ?: kotlinx.coroutines.flow.MutableStateFlow(false))
        .collectAsStateWithLifecycle()
    val activity = LocalContext.current as android.app.Activity
    var showRecurringProLimit by remember { mutableStateOf(false) }

    val currency  = LocalCurrency.current
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }
    var showAddSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Income?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text("Income", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Brand400)
                        .clickable { showAddSheet = true }
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add income", tint = Color(0xFF0A0A0A), modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Month navigator ───────────────────────────────────────────
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

                // ── Total summary ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenDim)
                        .border(1.dp, Brand400.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Total Income", fontSize = 11.sp, color = TextMuted)
                        Text(
                            "$currency ${formatter.format(totalIncome)}",
                            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Brand400
                        )
                        Text("${incomeList.size} ${if (incomeList.size == 1) "source" else "sources"}", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                // ── List ───────────────────────────────────────────────────────
                if (incomeList.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💰", fontSize = 40.sp)
                            Text("No income added yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Tap + to add a one-time or recurring income source", fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(incomeList, key = { it.id }) { income ->
                            IncomeItemCard(
                                income    = income,
                                currency  = currency,
                                formatter = formatter,
                                onDelete  = { deleteTarget = income }
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = Surface,
            title  = { Text("Delete \"${target.description.ifBlank { "Income" }}\"?", color = TextPrimary) },
            text   = { Text("This income entry will be removed.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteIncome(target); deleteTarget = null }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    if (showAddSheet) {
        AddIncomeSheet(
            onSave = { amount, source, date, recurring ->
                // Free tier: 1 recurring income source; one-time incomes are unlimited
                if (recurring && !isPro && recurringSourceCount >= 1) {
                    showAddSheet = false
                    showRecurringProLimit = true
                    return@AddIncomeSheet
                }
                fun save() {
                    viewModel.addIncomeEntry(amount, source, date, recurring)
                    showAddSheet = false
                }
                if (isPro || interstitialAdManager == null) {
                    save()
                } else {
                    interstitialAdManager.showAdEveryOtherThenRun(activity, "income_entry") { save() }
                }
            },
            onDismiss = { showAddSheet = false }
        )
    }

    if (showRecurringProLimit) {
        AlertDialog(
            onDismissRequest = { showRecurringProLimit = false },
            containerColor   = Surface,
            title = { Text("More recurring incomes with Pro", color = TextPrimary) },
            text  = { Text("Free includes 1 recurring income source (one-time incomes stay unlimited). Upgrade to SmartSpend Pro to add more.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showRecurringProLimit = false; onUpgrade() }) {
                    Text("Upgrade", color = Brand400, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecurringProLimit = false }) { Text("Not now", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun IncomeItemCard(
    income: Income,
    currency: String,
    formatter: NumberFormat,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brand400.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💰", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(income.description.ifBlank { "Income" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (income.recurring) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("Recurring", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
                    }
                }
            }
            Text(income.date, fontSize = 11.sp, color = TextMuted)
        }
        Text("+$currency ${formatter.format(income.amount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand400)
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncomeSheet(
    onSave: (amount: Double, source: String, date: String, recurring: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context  = LocalContext.current
    val currency = LocalCurrency.current
    val sdf      = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today    = remember { sdf.format(Date()) }

    var source     by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var date       by remember { mutableStateOf(today) }
    var recurring  by remember { mutableStateOf(false) }

    var sourceError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance()
    val datePicker = DatePickerDialog(context,
        { _, y, m, d -> date = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

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
            Text("Add Income", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            OutlinedTextField(
                value = source,
                onValueChange = { source = it; sourceError = false },
                label = { Text("Source (e.g. Salary, Freelance)") },
                isError = sourceError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; amountError = false },
                label = { Text("Amount ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amountError,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = date, onValueChange = {}, readOnly = true,
                label = { Text("Date") },
                trailingIcon = { IconButton(onClick = { datePicker.show() }) { Text("📅") } },
                modifier = Modifier.fillMaxWidth().clickable { datePicker.show() }
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { recurring = !recurring }) {
                Switch(checked = recurring, onCheckedChange = { recurring = it })
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Recurring income", fontSize = 14.sp, color = TextPrimary)
                    Text("Repeats every month", fontSize = 11.sp, color = TextMuted)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand400)
                    .clickable {
                        val amt = amountText.toDoubleOrNull()
                        sourceError = source.isBlank()
                        amountError = amt == null || amt <= 0
                        if (sourceError || amountError) return@clickable
                        onSave(amt!!, source, date, recurring)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Add Income", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
            }
        }
    }
}
