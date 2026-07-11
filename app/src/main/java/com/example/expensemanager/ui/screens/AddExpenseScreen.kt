package com.example.expensemanager.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.SideProject
import com.example.expensemanager.ui.theme.LocalCurrency
import com.example.expensemanager.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

private val INLINE_CATEGORY_COLORS = listOf(
    "#FF6B6B", "#60A5FA", "#A78BFA", "#FBBF24",
    "#F472B6", "#22D3EE", "#4ADE80", "#FB923C"
)

/**
 * Sheet-friendly add/edit expense form. No Scaffold — intended to be hosted inside
 * a ModalBottomSheet in ExpenseListScreen.
 * Pass [existingExpense] to pre-fill fields for editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheetContent(
    viewModel       : ExpenseViewModel,
    onSaved         : () -> Unit,
    onScanReceipt   : () -> Unit = {},
    onVoice         : () -> Unit = {},
    existingExpense : Expense?   = null,
    onBeforeSave    : ((doSave: () -> Unit) -> Unit)? = null,
    onSaveRecurring : ((name: String, amount: Double, categoryId: Long, description: String, frequency: String, startDate: String) -> Unit)? = null,
    projects        : List<SideProject> = emptyList()
) {
    val isEditing = existingExpense != null

    val categories   by viewModel.categories.collectAsStateWithLifecycle()
    val currency      = LocalCurrency.current
    val context       = LocalContext.current
    val focusManager  = LocalFocusManager.current

    var amountText               by remember { mutableStateOf(if (isEditing) existingExpense!!.amount.toString() else "") }
    var selectedCategoryId       by remember { mutableStateOf<Long?>(existingExpense?.categoryId) }
    var description              by remember { mutableStateOf(existingExpense?.description ?: "") }
    var location                 by remember { mutableStateOf(existingExpense?.location ?: "") }
    var date                     by remember { mutableStateOf(existingExpense?.date ?: "") }
    var time                     by remember { mutableStateOf(existingExpense?.time ?: "") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var amountError              by remember { mutableStateOf(false) }
    var categoryError            by remember { mutableStateOf(false) }
    var selectedProjectId        by remember { mutableStateOf(existingExpense?.projectId) }
    var projectDropdownExpanded  by remember { mutableStateOf(false) }
    // When a project is selected: false = project-only (default), true = also in main
    var alsoCountInMain          by remember {
        mutableStateOf(existingExpense?.let { it.projectId != null && it.includeInMain } ?: false)
    }

    // Inline category creation state
    var creatingCategory  by remember { mutableStateOf(false) }
    var newCategoryName   by remember { mutableStateOf("") }
    val newCategoryFocus  = remember { FocusRequester() }
    var justAddedCategory by remember { mutableStateOf(false) }

    // Recurring state
    var isRecurring       by remember { mutableStateOf(false) }
    var frequency         by remember { mutableStateOf("monthly") }

    LaunchedEffect(Unit) {
        if (!isEditing) {
            val now = Date()
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        }
    }

    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().id
        }
        // Auto-select after inline category creation
        if (justAddedCategory && categories.isNotEmpty()) {
            selectedCategoryId = categories.last().id
            justAddedCategory = false
        }
    }

    val selectedCategory = categories.find { it.id == selectedCategoryId }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day -> date = String.format("%04d-%02d-%02d", year, month + 1, day) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute -> time = String.format("%02d:%02d", hour, minute) },
        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    val gradStart = MaterialTheme.colorScheme.primary
    val gradEnd   = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Sheet title + quick-entry shortcuts ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isEditing) "Edit Expense" else "New Expense",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (!isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = onScanReceipt,
                        colors  = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) { Icon(Icons.Filled.CameraAlt, contentDescription = "Scan Receipt", modifier = Modifier.size(20.dp)) }
                    FilledTonalIconButton(
                        onClick = onVoice,
                        colors  = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) { Icon(Icons.Filled.Mic, contentDescription = "Voice Entry", modifier = Modifier.size(20.dp)) }
                }
            }
        }

        // ── Amount ────────────────────────────────────────────────────────────
        OutlinedTextField(
            value         = amountText,
            onValueChange = { amountText = it; amountError = false },
            label         = { Text("Amount") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction    = ImeAction.Next
            ),
            isError       = amountError,
            supportingText = if (amountError) ({ Text("Enter a valid amount") }) else null,
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            leadingIcon   = {
                Text(
                    currency,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            },
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        // ── Category (with inline creation) ───────────────────────────────────
        AnimatedVisibility(visible = creatingCategory) {
            OutlinedTextField(
                value         = newCategoryName,
                onValueChange = { newCategoryName = it },
                label         = { Text("New category name") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .focusRequester(newCategoryFocus),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (newCategoryName.isNotBlank()) {
                        val color = INLINE_CATEGORY_COLORS[categories.size % INLINE_CATEGORY_COLORS.size]
                        viewModel.addCategory(Category(name = newCategoryName.trim(), icon = "💰", color = color))
                        newCategoryName = ""
                        creatingCategory = false
                        justAddedCategory = true
                    }
                }),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            if (newCategoryName.isNotBlank()) {
                                val color = INLINE_CATEGORY_COLORS[categories.size % INLINE_CATEGORY_COLORS.size]
                                viewModel.addCategory(Category(name = newCategoryName.trim(), icon = "💰", color = color))
                                newCategoryName = ""
                                creatingCategory = false
                                justAddedCategory = true
                            }
                        }) { Icon(Icons.Filled.Check, contentDescription = "Confirm", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            )
        }

        LaunchedEffect(creatingCategory) {
            if (creatingCategory) newCategoryFocus.requestFocus()
        }

        AnimatedVisibility(visible = !creatingCategory) {
            ExposedDropdownMenuBox(
                expanded        = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value    = if (selectedCategory != null)
                        "${selectedCategory.icon} ${selectedCategory.name}"
                    else "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label    = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryDropdownExpanded) },
                    isError  = categoryError,
                    supportingText = if (categoryError) ({ Text("Please select a category") }) else null,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded        = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text    = { Text("${cat.icon} ${cat.name}") },
                            onClick = {
                                selectedCategoryId = cat.id
                                categoryDropdownExpanded = false
                                categoryError = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Text("Create new category",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium)
                            }
                        },
                        onClick = {
                            categoryDropdownExpanded = false
                            creatingCategory = true
                        }
                    )
                }
            }
        }

        // ── Description ───────────────────────────────────────────────────────
        OutlinedTextField(
            value         = description,
            onValueChange = { description = it },
            label         = { Text("Description (optional)") },
            modifier      = Modifier.fillMaxWidth(),
            maxLines      = 2
        )

        // ── Location ──────────────────────────────────────────────────────────
        OutlinedTextField(
            value         = location,
            onValueChange = { location = it },
            label         = { Text("Location / Address (optional)") },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true
        )

        // ── Side Project (optional) ──────────────────────────────────────────
        if (projects.isNotEmpty()) {
            val selectedProject = projects.find { it.id == selectedProjectId }
            ExposedDropdownMenuBox(
                expanded = projectDropdownExpanded,
                onExpandedChange = { projectDropdownExpanded = !projectDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProject?.let { "${it.icon} ${it.name}" } ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Side Project (optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(projectDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = projectDropdownExpanded, onDismissRequest = { projectDropdownExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = { selectedProjectId = null; projectDropdownExpanded = false }
                    )
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = { Text("${project.icon} ${project.name}") },
                            onClick = { selectedProjectId = project.id; projectDropdownExpanded = false }
                        )
                    }
                }
            }
        }

        // Scope: project-only (default) vs also counted in main spending
        if (selectedProjectId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { alsoCountInMain = !alsoCountInMain },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked         = alsoCountInMain,
                    onCheckedChange = { alsoCountInMain = it }
                )
                Column {
                    Text(
                        "Also count in main spending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (alsoCountInMain) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (alsoCountInMain) "Counted in project AND monthly budget"
                        else "Project-only — kept out of monthly budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Date & Time ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value    = date,
                onValueChange = {},
                readOnly = true,
                label    = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick date")
                    }
                },
                modifier  = Modifier
                    .weight(1f)
                    .clickable { datePickerDialog.show() },
                singleLine = true
            )
            OutlinedTextField(
                value    = time,
                onValueChange = {},
                readOnly = true,
                label    = { Text("Time") },
                trailingIcon = {
                    IconButton(onClick = { timePickerDialog.show() }) {
                        Icon(Icons.Outlined.Schedule, contentDescription = "Pick time")
                    }
                },
                modifier  = Modifier
                    .weight(1f)
                    .clickable { timePickerDialog.show() },
                singleLine = true
            )
        }

        // ── Recurring toggle (add mode only) ─────────────────────────────────
        if (!isEditing && onSaveRecurring != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isRecurring = !isRecurring },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked         = isRecurring,
                    onCheckedChange = { isRecurring = it }
                )
                Text(
                    "Make this a recurring payment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecurring) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isRecurring) {
                val frequencies = listOf("daily", "weekly", "monthly", "yearly")
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    frequencies.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick  = { frequency = freq },
                            label    = { Text(freq.replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Save / Update button ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
                .clickable {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) { amountError = true; return@clickable }
                    if (selectedCategoryId == null) { categoryError = true; return@clickable }
                    if (isEditing) {
                        val updatedExpense = existingExpense!!.copy(
                            amount      = amount,
                            categoryId  = selectedCategoryId!!,
                            description = description.trim(),
                            location    = location.trim(),
                            address     = location.trim(),
                            date        = date,
                            time        = time,
                            projectId   = selectedProjectId,
                            includeInMain = selectedProjectId == null || alsoCountInMain
                        )
                        val doSave = {
                            viewModel.updateExpense(updatedExpense)
                            onSaved()
                        }
                        if (onBeforeSave != null) onBeforeSave(doSave) else doSave()
                    } else {
                        viewModel.addExpense(
                            Expense(
                                amount      = amount,
                                categoryId  = selectedCategoryId!!,
                                description = description.trim(),
                                location    = location.trim(),
                                address     = location.trim(),
                                date        = date,
                                time        = time,
                                source      = if (isRecurring) "recurring" else "manual",
                                projectId   = selectedProjectId,
                                includeInMain = selectedProjectId == null || alsoCountInMain
                            )
                        )
                        if (isRecurring && onSaveRecurring != null) {
                            val name = description.trim().ifBlank {
                                selectedCategory?.name ?: "Recurring"
                            }
                            onSaveRecurring(name, amount, selectedCategoryId!!, description.trim(), frequency, date)
                        }
                        onSaved()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isEditing) "Update Expense" else "Save Expense",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }
}
