package com.example.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensemanager.data.model.ProjectTypes
import com.example.expensemanager.ui.theme.*

/**
 * The one project-creation pop-up, shared by the Side Projects screen and the
 * add-expense sheet. Name is required, type picks the icon/color, budget is
 * optional (no budget = the project just groups expenses).
 *
 * Rendered as a dialog so it stacks on top of whatever opened it — a
 * half-filled expense form underneath keeps its state.
 */
@Composable
fun CreateProjectDialog(
    onCreate : (name: String, type: String, budget: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val currency = LocalCurrency.current

    var name         by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ProjectTypes.ALL.first().key) }
    var budgetText   by remember { mutableStateOf("") }
    var nameError    by remember { mutableStateOf(false) }
    var budgetError  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Surface,
        title = { Text("New Project", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Project name") },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text("Name is required") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Project type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    ProjectTypes.ALL.chunked(2).forEach { rowTypes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowTypes.forEach { type ->
                                val isSelected = selectedType == type.key
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Brand400.copy(alpha = 0.09f) else SurfaceEl)
                                        .border(1.5.dp, if (isSelected) Brand400 else SurfaceVar, RoundedCornerShape(10.dp))
                                        .clickable { selectedType = type.key }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(type.icon, fontSize = 15.sp)
                                    Text(
                                        type.label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Brand400 else TextSecondary
                                    )
                                }
                            }
                            if (rowTypes.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' }; budgetError = false },
                    label = { Text("Budget ($currency) — optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = budgetError,
                    supportingText = if (budgetError) ({ Text("Enter a valid amount or leave empty") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                nameError = name.isBlank()
                val budget = if (budgetText.isBlank()) 0.0 else budgetText.toDoubleOrNull()
                budgetError = budget == null || budget < 0
                if (nameError || budgetError) return@TextButton
                onCreate(name.trim(), selectedType, budget!!)
            }) { Text("Create", color = Brand400, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
