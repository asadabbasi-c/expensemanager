package com.example.expensemanager.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Color as AndroidColor
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.RecurringExpense
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.RecurringViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    recurringViewModel: RecurringViewModel,
    expenseViewModel  : ExpenseViewModel,
    onBack            : () -> Unit
) {
    val recurringList by recurringViewModel.recurringExpenses.collectAsStateWithLifecycle()
    val categories    by expenseViewModel.categories.collectAsStateWithLifecycle()
    val categoryMap   = remember(categories) { categories.associateBy { it.id } }

    var showAddSheet  by remember { mutableStateOf(false) }
    var editTarget    by remember { mutableStateOf<RecurringExpense?>(null) }
    var deleteTarget  by remember { mutableStateOf<RecurringExpense?>(null) }

    val emerald   = Color(0xFF10B981)
    val gradStart = MaterialTheme.colorScheme.primary
    val gradEnd   = MaterialTheme.colorScheme.tertiary

    // Monthly commitment sum (active entries only)
    val monthlyTotal = remember(recurringList) {
        recurringList.filter { it.isActive }.sumOf { r ->
            when (r.frequency) {
                "daily"  -> r.amount * 30
                "weekly" -> r.amount * 4.33
                "yearly" -> r.amount / 12.0
                else     -> r.amount   // monthly
            }
        }
    }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Expenses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editTarget = null; showAddSheet = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add recurring")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor     = MaterialTheme.colorScheme.primary,
                    titleContentColor  = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor     = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->

        if (recurringList.isEmpty()) {
            // ── Empty state ───────────────────────────────────────────────
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Text("🔄", style = MaterialTheme.typography.displaySmall) }
                    Text("No recurring expenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("Tap + to add rent, bills, loans…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
                            .clickable { editTarget = null; showAddSheet = true }
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Text("Add Recurring Expense",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Monthly commitment summary ─────────────────────────────
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Monthly commitment",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("PKR ${"%,.0f".format(monthlyTotal)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Text("${recurringList.count { it.isActive }} active",
                                style = MaterialTheme.typography.labelLarge,
                                color = emerald,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // ── Recurring entries ─────────────────────────────────────
                items(recurringList, key = { it.id }) { r ->
                    val category = categoryMap[r.categoryId]
                    val isOverdue = r.isActive && r.nextDueDate <= today
                    RecurringCard(
                        recurring  = r,
                        category   = category,
                        isOverdue  = isOverdue,
                        onToggle   = { recurringViewModel.toggleActive(r) },
                        onEdit     = { editTarget = r; showAddSheet = true },
                        onDelete   = { deleteTarget = r }
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title  = { Text("Delete \"${target.name}\"?") },
            text   = { Text("This recurring expense will be removed. Past generated entries are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    recurringViewModel.deleteRecurring(target)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // ── Add / Edit bottom sheet ───────────────────────────────────────────────
    if (showAddSheet) {
        AddEditRecurringSheet(
            initial   = editTarget,
            categories = categories,
            onSave = { name, amount, catId, desc, freq, start, end ->
                if (editTarget == null) {
                    recurringViewModel.addRecurring(name, amount, catId, desc, freq, start, end)
                } else {
                    recurringViewModel.updateRecurring(
                        editTarget!!.copy(
                            name        = name,
                            amount      = amount,
                            categoryId  = catId,
                            description = desc,
                            frequency   = freq,
                            startDate   = start,
                            endDate     = end,
                            nextDueDate = start  // reset next due to new start
                        )
                    )
                }
                showAddSheet = false
                editTarget   = null
            },
            onDismiss = { showAddSheet = false; editTarget = null }
        )
    }
}

// ── Recurring expense card ────────────────────────────────────────────────────

@Composable
private fun RecurringCard(
    recurring : RecurringExpense,
    category  : Category?,
    isOverdue : Boolean,
    onToggle  : () -> Unit,
    onEdit    : () -> Unit,
    onDelete  : () -> Unit
) {
    val categoryColor = remember(category) {
        category?.color?.let {
            runCatching { Color(AndroidColor.parseColor(it)) }
                .getOrDefault(Color(0xFF94A3B8))
        } ?: Color(0xFF94A3B8)
    }
    val alpha = if (recurring.isActive) 1f else 0.45f

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            // Category colour strip
            Box(
                Modifier.width(5.dp).fillMaxHeight()
                    .background(categoryColor.copy(alpha = alpha))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon bubble
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.13f * alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(category?.icon ?: "🔄",
                        style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            recurring.name,
                            style    = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "PKR ${"%,.0f".format(recurring.amount)}",
                            style  = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color  = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Frequency badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = categoryColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                recurring.frequency.replaceFirstChar { it.uppercaseChar() },
                                style    = MaterialTheme.typography.labelSmall,
                                color    = categoryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (!recurring.isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF94A3B8).copy(alpha = 0.15f)
                            ) {
                                Text("Paused",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        } else if (isOverdue) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f)
                            ) {
                                Text("⚠ Due today!",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        } else {
                            Text("Next: ${recurring.nextDueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Action buttons
                IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (recurring.isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (recurring.isActive) "Pause" else "Resume",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Add / Edit sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditRecurringSheet(
    initial   : RecurringExpense?,
    categories: List<Category>,
    onSave    : (name: String, amount: Double, catId: Long,
                 desc: String, freq: String, start: String, end: String?) -> Unit,
    onDismiss : () -> Unit
) {
    val context = LocalContext.current
    val sdf     = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today   = remember { sdf.format(Date()) }

    var name         by remember { mutableStateOf(initial?.name ?: "") }
    var amountText   by remember { mutableStateOf(initial?.amount?.let { "%.2f".format(it) } ?: "") }
    var description  by remember { mutableStateOf(initial?.description ?: "") }
    var frequency    by remember { mutableStateOf(initial?.frequency ?: "monthly") }
    var startDate    by remember { mutableStateOf(initial?.startDate ?: today) }
    var endDate      by remember { mutableStateOf(initial?.endDate) }
    var hasEndDate   by remember { mutableStateOf(initial?.endDate != null) }
    var catId        by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id) }
    var catExpanded  by remember { mutableStateOf(false) }
    var nameError    by remember { mutableStateOf(false) }
    var amountError  by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (catId == null && categories.isNotEmpty()) catId = categories.first().id
    }

    val selectedCat = categories.find { it.id == catId }
    val frequencies = listOf("daily", "weekly", "monthly", "yearly")

    val cal = Calendar.getInstance()
    val startPicker = DatePickerDialog(context,
        { _, y, m, d -> startDate = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    val endPicker = DatePickerDialog(context,
        { _, y, m, d -> endDate = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

    val gradStart = MaterialTheme.colorScheme.primary
    val gradEnd   = MaterialTheme.colorScheme.tertiary

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (initial == null) "Add Recurring Expense" else "Edit Recurring Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Name (e.g. Rent, Internet)") },
                isError = nameError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; amountError = false },
                label = { Text("Amount (PKR)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amountError,
                leadingIcon = {
                    Text("PKR", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                OutlinedTextField(
                    value    = selectedCat?.let { "${it.icon} ${it.name}" } ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label    = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.icon} ${cat.name}") },
                            onClick = { catId = cat.id; catExpanded = false }
                        )
                    }
                }
            }

            // Frequency chips
            Text("Frequency",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                frequencies.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick  = { frequency = freq },
                        label    = { Text(freq.replaceFirstChar { it.uppercaseChar() }) }
                    )
                }
            }

            // Start date
            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Date") },
                trailingIcon = {
                    IconButton(onClick = { startPicker.show() }) {
                        Text("📅", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { startPicker.show() }
            )

            // End date toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = hasEndDate, onCheckedChange = {
                    hasEndDate = it
                    if (!it) endDate = null
                })
                Spacer(Modifier.width(10.dp))
                Text("Set an end date (optional)",
                    style = MaterialTheme.typography.bodyMedium)
            }
            AnimatedVisibility(visible = hasEndDate) {
                OutlinedTextField(
                    value = endDate ?: today,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("End Date") },
                    trailingIcon = {
                        IconButton(onClick = { endPicker.show() }) {
                            Text("📅", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { endPicker.show() }
                )
            }

            Spacer(Modifier.height(4.dp))

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
                    .clickable {
                        val amt = amountText.toDoubleOrNull()
                        nameError   = name.isBlank()
                        amountError = amt == null || amt <= 0
                        if (nameError || amountError || catId == null) return@clickable
                        onSave(name, amt!!, catId!!, description, frequency,
                            startDate, if (hasEndDate) endDate else null)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (initial == null) "Add Recurring Expense" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White)
            }
        }
    }
}
