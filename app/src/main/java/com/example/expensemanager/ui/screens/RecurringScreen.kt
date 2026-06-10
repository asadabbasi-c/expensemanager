package com.example.expensemanager.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Color as AndroidColor
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.RecurringExpense
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.RecurringViewModel
import java.text.NumberFormat
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

    var showAddSheet by remember { mutableStateOf(false) }
    var editTarget   by remember { mutableStateOf<RecurringExpense?>(null) }
    var deleteTarget by remember { mutableStateOf<RecurringExpense?>(null) }

    val currency = LocalCurrency.current
    val sdf      = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today    = remember { sdf.format(Date()) }

    val monthlyTotal = remember(recurringList) {
        recurringList.filter { it.isActive }.sumOf { r ->
            when (r.frequency) {
                "daily"  -> r.amount * 30
                "weekly" -> r.amount * 4.33
                "yearly" -> r.amount / 12.0
                else     -> r.amount
            }
        }
    }

    // Partition into Due Soon (≤7 days) and Upcoming
    fun daysUntil(dateStr: String): Int {
        return try {
            val due  = sdf.parse(dateStr)?.time ?: return 999
            val now  = sdf.parse(today)?.time ?: return 999
            ((due - now) / 86400000).toInt()
        } catch (_: Exception) { 999 }
    }

    val activeList = remember(recurringList) { recurringList.filter { it.isActive } }
    val dueSoon    = remember(activeList, today) { activeList.filter { daysUntil(it.nextDueDate) <= 7 } }
    val upcoming   = remember(activeList, today) { activeList.filter { daysUntil(it.nextDueDate) > 7 } }
    val paused     = remember(recurringList) { recurringList.filter { !it.isActive } }

    Scaffold(containerColor = Bg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Bg)
        ) {
            // ── Custom header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                }
                Text("Recurring", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Brand400)
                        .clickable { editTarget = null; showAddSheet = true }
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
                }
            }

            if (recurringList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text("🔄", fontSize = 48.sp)
                        Text("No recurring expenses", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Tap + to add rent, bills, subscriptions…", fontSize = 13.sp, color = TextMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Monthly commitment header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Surface)
                                .border(1.dp, SurfaceVar, RoundedCornerShape(16.dp))
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Column {
                                Text("Monthly Commitment", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    "$currency ${"%,.0f".format(monthlyTotal)}",
                                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ErrorRed
                                )
                                Text("${recurringList.count { it.isActive }} active payments", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    // Due Soon section
                    if (dueSoon.isNotEmpty()) {
                        item {
                            SectionLabel("Due Soon", WarnAmber)
                        }
                        items(dueSoon, key = { it.id }) { r ->
                            RecurringItemCard(
                                recurring = r,
                                category  = categoryMap[r.categoryId],
                                days      = daysUntil(r.nextDueDate),
                                currency  = currency,
                                onToggle  = { recurringViewModel.toggleActive(r) },
                                onEdit    = { editTarget = r; showAddSheet = true },
                                onDelete  = { deleteTarget = r }
                            )
                        }
                    }

                    // Upcoming section
                    if (upcoming.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            SectionLabel("Upcoming", TextMuted)
                        }
                        items(upcoming, key = { it.id }) { r ->
                            RecurringItemCard(
                                recurring = r,
                                category  = categoryMap[r.categoryId],
                                days      = daysUntil(r.nextDueDate),
                                currency  = currency,
                                onToggle  = { recurringViewModel.toggleActive(r) },
                                onEdit    = { editTarget = r; showAddSheet = true },
                                onDelete  = { deleteTarget = r }
                            )
                        }
                    }

                    // Paused section
                    if (paused.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)) }
                        item { SectionLabel("Paused", TextMuted) }
                        items(paused, key = { it.id }) { r ->
                            RecurringItemCard(
                                recurring = r,
                                category  = categoryMap[r.categoryId],
                                days      = 999,
                                currency  = currency,
                                onToggle  = { recurringViewModel.toggleActive(r) },
                                onEdit    = { editTarget = r; showAddSheet = true },
                                onDelete  = { deleteTarget = r }
                            )
                        }
                    }

                    // Add button
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, SurfaceVar, RoundedCornerShape(14.dp))
                                .clickable { editTarget = null; showAddSheet = true }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ Add Recurring Payment", fontSize = 14.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = Surface,
            title  = { Text("Delete \"${target.name}\"?", color = TextPrimary) },
            text   = { Text("This recurring expense will be removed. Past generated entries are not affected.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { recurringViewModel.deleteRecurring(target); deleteTarget = null }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    // Add / Edit sheet
    if (showAddSheet) {
        AddEditRecurringSheet(
            initial    = editTarget,
            categories = categories,
            onSave = { name, amount, catId, desc, freq, start, end ->
                if (editTarget == null) {
                    recurringViewModel.addRecurring(name, amount, catId, desc, freq, start, end)
                } else {
                    recurringViewModel.updateRecurring(editTarget!!.copy(
                        name = name, amount = amount, categoryId = catId,
                        description = desc, frequency = freq, startDate = start, endDate = end
                    ))
                }
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
private fun SectionLabel(label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun RecurringItemCard(
    recurring: RecurringExpense,
    category : Category?,
    days     : Int,
    currency : String,
    onToggle : () -> Unit,
    onEdit   : () -> Unit,
    onDelete : () -> Unit
) {
    val brand = remember(recurring.name) { findBrandByDescription(recurring.name) }
    val catColor = remember(category?.color) {
        category?.color?.let {
            runCatching { Color(AndroidColor.parseColor(it)) }.getOrDefault(Color(0xFF888888))
        } ?: Color(0xFF888888)
    }
    val dueColor = when {
        days <= 3  -> ErrorRed
        days <= 7  -> WarnAmber
        else       -> TextMuted
    }
    val dueLabel = when {
        days <= 0  -> "Today"
        days == 1  -> "Tomorrow"
        days > 100 -> "—"
        else       -> "in $days days"
    }
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (brand != null) BrandBadge(brand = brand, size = 40)
            else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text(category?.icon ?: "🔄", fontSize = 20.sp) }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(recurring.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text("${recurring.frequency.replaceFirstChar { it.uppercaseChar() }} · Next: ", fontSize = 11.sp, color = TextMuted)
                    Text(dueLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = dueColor)
                }
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 6.dp)) {
                Text(formatter.format(recurring.amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                Text("$currency / mo", fontSize = 9.sp, color = TextMuted)
            }

            Text(
                "⋯",
                fontSize = 18.sp,
                color    = TextMuted,
                modifier = Modifier.clickable { onEdit() }
            )
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
    val context  = LocalContext.current
    val currency = LocalCurrency.current
    val sdf      = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today    = remember { sdf.format(Date()) }

    var name        by remember { mutableStateOf(initial?.name ?: "") }
    var amountText  by remember { mutableStateOf(initial?.amount?.let { "%.2f".format(it) } ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var frequency   by remember { mutableStateOf(initial?.frequency ?: "monthly") }
    var startDate   by remember { mutableStateOf(initial?.startDate ?: today) }
    var endDate     by remember { mutableStateOf(initial?.endDate) }
    var hasEndDate  by remember { mutableStateOf(initial?.endDate != null) }
    var catId       by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id) }
    var catExpanded by remember { mutableStateOf(false) }
    var nameError   by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

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
            Text(
                if (initial == null) "Add Recurring Expense" else "Edit Recurring Expense",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary
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
                label = { Text("Amount ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amountError,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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

            Text("Frequency", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                frequencies.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick  = { frequency = freq },
                        label    = { Text(freq.replaceFirstChar { it.uppercaseChar() }) }
                    )
                }
            }

            OutlinedTextField(
                value = startDate, onValueChange = {}, readOnly = true,
                label = { Text("Start Date") },
                trailingIcon = { IconButton(onClick = { startPicker.show() }) { Text("📅") } },
                modifier = Modifier.fillMaxWidth().clickable { startPicker.show() }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = hasEndDate, onCheckedChange = { hasEndDate = it; if (!it) endDate = null })
                Spacer(Modifier.width(10.dp))
                Text("Set an end date (optional)", fontSize = 14.sp, color = TextSecondary)
            }
            AnimatedVisibility(visible = hasEndDate) {
                OutlinedTextField(
                    value = endDate ?: today, onValueChange = {}, readOnly = true,
                    label = { Text("End Date") },
                    trailingIcon = { IconButton(onClick = { endPicker.show() }) { Text("📅") } },
                    modifier = Modifier.fillMaxWidth().clickable { endPicker.show() }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand400)
                    .clickable {
                        val amt = amountText.toDoubleOrNull()
                        nameError   = name.isBlank()
                        amountError = amt == null || amt <= 0
                        if (nameError || amountError || catId == null) return@clickable
                        onSave(name, amt!!, catId!!, description, frequency, startDate, if (hasEndDate) endDate else null)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (initial == null) "Add Recurring Expense" else "Save Changes",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A)
                )
            }
        }
    }
}
