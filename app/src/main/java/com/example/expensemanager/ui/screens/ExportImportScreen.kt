package com.example.expensemanager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.service.DataTransferService
import com.example.expensemanager.viewmodel.ExportImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(viewModel: ExportImportViewModel) {
    val context        = LocalContext.current
    val isWorking      by viewModel.isWorking.collectAsStateWithLifecycle()
    val exportResult   by viewModel.exportResult.collectAsStateWithLifecycle()
    val importResult   by viewModel.importResult.collectAsStateWithLifecycle()
    val expenseCount   by viewModel.expenseCount.collectAsStateWithLifecycle()

    val gradStart = MaterialTheme.colorScheme.primary
    val gradEnd   = MaterialTheme.colorScheme.tertiary

    // ── File-picker launchers ─────────────────────────────────────────────────

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> uri?.let { viewModel.exportToXlsx(it, context) } }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { viewModel.exportToPdf(it, context) } }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportToCsv(it, context) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importFile(it, context) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import / Export", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Loading overlay ───────────────────────────────────────────
            if (isWorking) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                            @Suppress("DEPRECATION")
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(80.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Text("Processing…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // ── Export result ─────────────────────────────────────────────
            exportResult?.let { result ->
                ResultBanner(
                    isSuccess = result is ExportImportViewModel.ExportResult.Success,
                    message   = when (result) {
                        is ExportImportViewModel.ExportResult.Success -> result.message
                        is ExportImportViewModel.ExportResult.Error   -> result.message
                    },
                    onDismiss = { viewModel.clearExportResult() }
                )
            }

            // ── Import result ─────────────────────────────────────────────
            importResult?.let { result ->
                ImportResultCard(result = result, onDismiss = { viewModel.clearImportResult() })
            }

            // ── Export section ────────────────────────────────────────────
            SectionCard(
                icon       = Icons.Outlined.FileDownload,
                title      = "Export",
                subtitle   = "$expenseCount expense${if (expenseCount == 1) "" else "s"} ready to export",
                gradStart  = gradStart,
                gradEnd    = gradEnd
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExportButton(
                        label    = "Export to Excel (.xlsx)",
                        icon     = Icons.Outlined.TableChart,
                        description = "Opens in Microsoft Excel, Google Sheets, LibreOffice",
                        enabled  = !isWorking && expenseCount > 0,
                        onClick  = { xlsxExportLauncher.launch("expenses_${today()}.xlsx") }
                    )
                    ExportButton(
                        label    = "Export to PDF",
                        icon     = Icons.Outlined.PictureAsPdf,
                        description = "Formatted printable report with all expenses",
                        enabled  = !isWorking && expenseCount > 0,
                        onClick  = { pdfExportLauncher.launch("expenses_${today()}.pdf") }
                    )
                    ExportButton(
                        label    = "Export to CSV",
                        icon     = Icons.Outlined.GridOn,
                        description = "Plain text format, compatible with any spreadsheet app",
                        enabled  = !isWorking && expenseCount > 0,
                        onClick  = { csvExportLauncher.launch("expenses_${today()}.csv") }
                    )
                    if (expenseCount == 0) {
                        Text(
                            "Add some expenses before exporting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Import section ────────────────────────────────────────────
            SectionCard(
                icon      = Icons.Outlined.FileUpload,
                title     = "Import",
                subtitle  = "Load expenses from an existing spreadsheet",
                gradStart = Color(0xFF7C3AED),
                gradEnd   = Color(0xFFEC4899)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Format spec card
                    FormatSpecCard()

                    // Import button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
                                )
                            )
                            .then(
                                if (!isWorking)
                                    Modifier.then(Modifier) // clickable added below
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { importLauncher.launch("*/*") },
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor   = Color.White,
                                disabledContainerColor = Color.Transparent
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
                        ) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Choose File to Import (.xlsx or .csv)",
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradStart: Color,
    gradEnd: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(gradStart, gradEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            content()
        }
    }
}

@Composable
private fun ExportButton(
    label: String,
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FormatSpecCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("File Format", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold)

            FormatRow("Required columns", DataTransferService.FORMAT_REQUIRED.joinToString(", "))
            FormatRow("Optional columns", DataTransferService.FORMAT_OPTIONAL.joinToString(", "))
            FormatRow("Date format", "YYYY-MM-DD  (e.g. 2024-01-15)")
            FormatRow("Time format", "HH:MM  (e.g. 14:30)")
            FormatRow("Amount", "Decimal number  (e.g. 500.00)")
            FormatRow("Category", "Must match a category name in the app")
            FormatRow("Source", "manual, sms, or voice")

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Text("Example row:", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                DataTransferService.FORMAT_EXAMPLE_ROW,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f))
        Text(value, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.55f))
    }
}

@Composable
private fun ResultBanner(isSuccess: Boolean, message: String, onDismiss: () -> Unit) {
    val bgColor = if (isSuccess) Color(0xFF10B981).copy(alpha = 0.12f)
                  else MaterialTheme.colorScheme.errorContainer
    val textColor = if (isSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer
    val icon = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Text(message, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = textColor)
            TextButton(onClick = onDismiss) {
                Text("OK", style = MaterialTheme.typography.labelMedium, color = textColor)
            }
        }
    }
}

@Composable
private fun ImportResultCard(
    result: DataTransferService.ImportResult,
    onDismiss: () -> Unit
) {
    val success = result.rows.isNotEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (success) Color(0xFF10B981).copy(alpha = 0.10f)
                             else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (success) "Import Complete" else "Import Failed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (success) Color(0xFF065F46) else MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }

            if (success) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, null,
                        tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    Text("${result.rows.size} of ${result.totalRowsRead} rows imported successfully",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF065F46))
                }
            }

            if (result.errors.isNotEmpty()) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Text(
                    "${result.errors.size} issue${if (result.errors.size == 1) "" else "s"} found:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                result.errors.take(8).forEach { error ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                        Text(error, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (result.errors.size > 8) {
                    Text("… and ${result.errors.size - 8} more issues",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun today(): String {
    val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}
