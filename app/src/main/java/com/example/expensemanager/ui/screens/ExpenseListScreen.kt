package com.example.expensemanager.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(viewModel: ExpenseViewModel) {
    val expenses   by viewModel.expenses.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val groupedExpenses = remember(expenses) {
        expenses.groupBy { e ->
            if (e.date.length >= 7) e.date.substring(0, 7) else "Unknown"
        }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Expenses", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (expenses.isEmpty()) {
            EmptyExpensesState(Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                groupedExpenses.forEach { (month, monthExpenses) ->
                    item(key = "header_$month") {
                        MonthHeader(
                            month = month,
                            total = monthExpenses.sumOf { it.amount },
                            count = monthExpenses.size
                        )
                    }
                    items(monthExpenses, key = { it.id }) { expense ->
                        SwipeToDeleteExpenseItem(
                            expense  = expense,
                            category = categoryMap[expense.categoryId],
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyExpensesState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Text("💸", style = MaterialTheme.typography.displaySmall) }
            Text(
                "No expenses yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tap Add to record your first expense",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Month header ──────────────────────────────────────────────────────────────

@Composable
fun MonthHeader(month: String, total: Double, count: Int) {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 2
    }
    val displayMonth = try {
        val parts = month.split("-")
        if (parts.size == 2) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
            }
            "${cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${parts[0]}"
        } else month
    } catch (_: Exception) { month }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = displayMonth,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$count transaction${if (count == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PKR", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                Text(
                    text = formatter.format(total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ── Delete wrapper ────────────────────────────────────────────────────────────

@Composable
fun SwipeToDeleteExpenseItem(expense: Expense, category: Category?, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete Expense") },
            text  = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }

    ExpenseItem(expense = expense, category = category, onDeleteClick = { showConfirm = true })
}

// ── Expense card ──────────────────────────────────────────────────────────────

@Composable
fun ExpenseItem(expense: Expense, category: Category?, onDeleteClick: () -> Unit = {}) {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 2
    }
    val categoryColor = remember(category) {
        category?.color?.let {
            runCatching { Color(AndroidColor.parseColor(it)) }.getOrDefault(Color(0xFF94A3B8))
        } ?: Color(0xFF94A3B8)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            // Category color accent strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(categoryColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon bubble
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(category?.icon ?: "💰", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category?.name ?: "Other",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = categoryColor
                        )
                        Text(
                            text = "PKR ${formatter.format(expense.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (expense.description.isNotBlank()) {
                        Text(
                            text = expense.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (expense.date.isNotBlank()) {
                            Text(
                                text = expense.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (expense.time.isNotBlank()) {
                            Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = expense.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (expense.source == "sms") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "SMS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (expense.source == "receipt") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "📷 Receipt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF6366F1),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (expense.source == "voice") {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "🎙 Voice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
