package com.example.expensemanager.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.sms.ParsedSms
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.SmsViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(viewModel: SmsViewModel, expenseViewModel: ExpenseViewModel) {
    val context            = LocalContext.current
    val parsedTransactions by viewModel.parsedTransactions.collectAsStateWithLifecycle()
    val isLoading          by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasPermission      by viewModel.hasPermission.collectAsStateWithLifecycle()
    val errorMessage       by viewModel.errorMessage.collectAsStateWithLifecycle()
    val categories         by expenseViewModel.categories.collectAsStateWithLifecycle()

    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.find { it.name == "Other" }?.id
                ?: categories.first().id
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setPermissionGranted(granted)
        if (granted) viewModel.loadSmsTransactions(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setPermissionGranted(granted)
        if (granted) viewModel.loadSmsTransactions(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Transactions", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (hasPermission) {
                        IconButton(onClick = { viewModel.loadSmsTransactions(context) }) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            errorMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                    }
                }
            }

            when {
                !hasPermission -> SmsPermissionContent {
                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                }

                isLoading -> SmsLoadingContent()

                parsedTransactions.isEmpty() -> SmsEmptyContent {
                    viewModel.loadSmsTransactions(context)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ── Count chip ────────────────────────────────────────
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${parsedTransactions.size} transaction${if (parsedTransactions.size == 1) "" else "s"} found",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // ── Category chips (horizontal scroll, no dropdown) ───
                        if (categories.isNotEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(1.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            "Category for all additions:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            categories.forEach { cat ->
                                                val selected = cat.id == selectedCategoryId
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = if (selected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .clickable { selectedCategoryId = cat.id }
                                                ) {
                                                    Text(
                                                        text = "${cat.icon} ${cat.name}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Transaction cards ─────────────────────────────────
                        itemsIndexed(parsedTransactions) { _, tx ->
                            SmsTransactionItem(
                                transaction = tx,
                                onAdd = {
                                    val catId = selectedCategoryId
                                        ?: categories.firstOrNull()?.id
                                        ?: 7L
                                    viewModel.addParsedAsExpense(tx, catId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Permission ────────────────────────────────────────────────────────────────

@Composable
private fun SmsPermissionContent(onRequest: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Message,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text("SMS Permission Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "To read bank transaction SMS messages, this app needs permission to access your SMS inbox. Your messages are processed locally and never shared.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Grant SMS Permission", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun SmsLoadingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "sms_load")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Message, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp))
            }
            Text("Scanning SMS messages…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            @Suppress("DEPRECATION")
            LinearProgressIndicator(
                modifier = Modifier.width(200.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

// ── Empty ─────────────────────────────────────────────────────────────────────

@Composable
private fun SmsEmptyContent(onRefresh: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Text("📨", style = MaterialTheme.typography.displaySmall) }
            Text("No bank SMS found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "No bank transaction SMS messages were detected in your inbox.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Scan Again")
            }
        }
    }
}

// ── Transaction card ──────────────────────────────────────────────────────────

@Composable
fun SmsTransactionItem(transaction: ParsedSms, onAdd: () -> Unit) {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 2
    }
    var added by remember { mutableStateOf(false) }

    // Confidence colour
    val (confidenceLabel, confidenceColor) = when {
        transaction.confidence >= 0.85f -> "Auto-categorized" to Color(0xFF10B981)
        transaction.confidence >= 0.6f  -> "Likely match"     to Color(0xFFF59E0B)
        else                            -> "Unclassified"      to Color(0xFF94A3B8)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Row 1: bank chip + amount ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = transaction.bankName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "PKR ${formatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // ── Row 2: merchant name ──────────────────────────────────────
            Text(
                text = transaction.merchant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // ── Row 3: category badge + confidence label ──────────────────
            if (transaction.confidence > 0f) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = confidenceColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = transaction.suggestedCategory,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = confidenceColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = confidenceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = confidenceColor
                    )
                }
            }

            // ── Row 4: raw SMS preview ────────────────────────────────────
            Text(
                text = transaction.rawMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = transaction.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            if (added) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✓ Added to Expenses",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            ))
                        )
                        .clickable { onAdd(); added = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add to Expenses", fontWeight = FontWeight.Bold,
                        color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
