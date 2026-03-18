package com.example.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Income
import com.example.expensemanager.viewmodel.GoalViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingScreen(
    viewModel: GoalViewModel,
    onBack: () -> Unit
) {
    val selectedMonth      by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthLabel         by viewModel.selectedMonthLabel.collectAsStateWithLifecycle()
    val goal               by viewModel.goalForMonth.collectAsStateWithLifecycle()
    val incomeList         by viewModel.incomeForMonth.collectAsStateWithLifecycle()
    val totalExtraIncome   by viewModel.totalExtraIncomeForMonth.collectAsStateWithLifecycle()

    // Local fields for the goal form
    var incomeTargetText by remember(goal) { mutableStateOf(goal?.incomeTarget?.toLong()?.toString() ?: "") }
    var goalAmountText   by remember(goal) { mutableStateOf(goal?.goalAmount?.toLong()?.toString() ?: "") }

    var showAddIncomeDialog by remember { mutableStateOf(false) }

    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }

    val emerald = Color(0xFF10B981)
    val slate   = Color(0xFF475569)

    if (showAddIncomeDialog) {
        AddIncomeDialog(
            onDismiss = { showAddIncomeDialog = false },
            onConfirm = { amount, description ->
                viewModel.addManualIncome(amount, description)
                showAddIncomeDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget & Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddIncomeDialog = true },
                containerColor = emerald,
                contentColor   = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Income")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Month selector ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text       = monthLabel,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { viewModel.navigateMonth(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                    }
                }
            }

            // ── Goal Setting card ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Text("Monthly Budget", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value         = incomeTargetText,
                        onValueChange = { incomeTargetText = it.filter { c -> c.isDigit() || c == '.' } },
                        label         = { Text("Monthly Income (PKR)") },
                        placeholder   = { Text("e.g. 50000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value         = goalAmountText,
                        onValueChange = { goalAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label         = { Text("Savings Goal (PKR)") },
                        placeholder   = { Text("e.g. 10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )

                    val incomeTarget = incomeTargetText.toDoubleOrNull() ?: 0.0
                    val goalAmount   = goalAmountText.toDoubleOrNull() ?: 0.0
                    val spendingBudget = (incomeTarget - goalAmount).coerceAtLeast(0.0)

                    if (incomeTarget > 0 && goalAmount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(emerald.copy(alpha = 0.08f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Spending Budget", style = MaterialTheme.typography.labelSmall,
                                    color = emerald.copy(alpha = 0.7f))
                                Text("PKR ${formatter.format(spendingBudget)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold, color = emerald)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Save", style = MaterialTheme.typography.labelSmall,
                                    color = slate)
                                Text("PKR ${formatter.format(goalAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, color = slate)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val income = incomeTargetText.toDoubleOrNull() ?: return@Button
                            val goal   = goalAmountText.toDoubleOrNull() ?: return@Button
                            viewModel.saveGoal(goal, income)
                        },
                        enabled  = incomeTargetText.isNotBlank() && goalAmountText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = emerald)
                    ) {
                        Text(if (goal != null) "Update Goal" else "Save Goal",
                            modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            // ── Income list card ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Income This Month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        if (totalExtraIncome > 0) {
                            Text("PKR ${formatter.format(totalExtraIncome)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = emerald)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (incomeList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💰", style = MaterialTheme.typography.headlineMedium)
                                Text("No income added yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Tap + to add income or SMS credits auto-appear here",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        incomeList.forEachIndexed { i, income ->
                            IncomeRow(
                                income    = income,
                                formatter = formatter,
                                onDelete  = { viewModel.deleteIncome(income) }
                            )
                            if (i < incomeList.lastIndex)
                                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp)) // room for FAB
        }
    }
}

@Composable
private fun IncomeRow(income: Income, formatter: NumberFormat, onDelete: () -> Unit) {
    val emerald = Color(0xFF10B981)
    val smsBadgeColor = Color(0xFF6366F1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = income.description.ifBlank { "Income" },
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (income.source == "sms") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = smsBadgeColor.copy(alpha = 0.15f)
                    ) {
                        Text("SMS",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = smsBadgeColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
            }
            Text(income.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text       = "+PKR ${formatter.format(income.amount)}",
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color      = emerald
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete,
                contentDescription = "Delete income",
                tint  = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncomeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText      by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Income", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label         = { Text("Amount (PKR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = descriptionText,
                    onValueChange = { descriptionText = it },
                    label         = { Text("Description (optional)") },
                    placeholder   = { Text("Salary, freelance, etc.") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { onConfirm(amountText.toDouble(), descriptionText) },
                enabled  = amountText.toDoubleOrNull() != null && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
