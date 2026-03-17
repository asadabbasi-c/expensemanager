package com.example.expensemanager.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceExpenseScreen(
    expenseViewModel: ExpenseViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val categories by expenseViewModel.categories.collectAsStateWithLifecycle()

    var recognizedText by remember { mutableStateOf("") }
    var parsedAmount   by remember { mutableStateOf("") }
    var parsedCategory by remember { mutableStateOf("") }
    var parsedMerchant by remember { mutableStateOf("") }
    var isListening    by remember { mutableStateOf(false) }
    var showPreview    by remember { mutableStateOf(false) }
    var savedSuccess   by remember { mutableStateOf(false) }
    var noSpeechApp    by remember { mutableStateOf(false) }

    // Pulse animation for mic button when listening
    val micScale = remember { Animatable(1f) }
    LaunchedEffect(isListening) {
        if (isListening) {
            while (true) {
                micScale.animateTo(1.18f, tween(500, easing = FastOutSlowInEasing))
                micScale.animateTo(1f,    tween(500, easing = FastOutSlowInEasing))
            }
        } else {
            micScale.animateTo(1f, tween(200))
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: return@rememberLauncherForActivityResult
            recognizedText = text
            val (amt, cat, mer) = parseVoiceInput(text, categories.map { it.name })
            parsedAmount   = amt?.toString() ?: ""
            parsedCategory = cat ?: categories.firstOrNull()?.name ?: ""
            parsedMerchant = mer ?: ""
            showPreview    = true
            savedSuccess   = false
        }
    }

    fun launchSpeech() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT,
                    "Say: amount, category, and merchant — e.g. \"500 food at KFC\"")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            isListening = true
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            isListening = false
            noSpeechApp = true
        }
    }

    val gradStart = MaterialTheme.colorScheme.primary
    val gradEnd   = MaterialTheme.colorScheme.tertiary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Mic circle ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(micScale.value)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(gradStart, gradEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(52.dp))
            }

            Text(
                text = when {
                    isListening -> "Listening…"
                    showPreview -> "Tap mic to try again"
                    else        -> "Tap to speak"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isListening) gradStart else MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Say something like:\n\"500 food at KFC\" or\n\"Spent 1200 on transport Uber\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // ── Speak button ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (!isListening) Brush.horizontalGradient(listOf(gradStart, gradEnd))
                        else Brush.horizontalGradient(listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        ))
                    )
                    .clickable(enabled = !isListening) { launchSpeech() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Mic, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        if (isListening) "Listening…" else "Start Listening",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // ── Error: no speech app ──────────────────────────────────────
            if (noSpeechApp) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        "Speech recognition is not available. Please install Google app or enable voice input.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Recognized text display ───────────────────────────────────
            if (recognizedText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Heard",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("\"$recognizedText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ── Preview / edit before saving ──────────────────────────────
            if (showPreview && !savedSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Review & Edit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = parsedAmount,
                            onValueChange = { parsedAmount = it },
                            label = { Text("Amount (PKR)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = parsedMerchant,
                            onValueChange = { parsedMerchant = it },
                            label = { Text("Merchant / Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Category selector (horizontal chips, no dropdown)
                        Text("Category",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val selected = cat.name == parsedCategory
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (selected) gradStart
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { parsedCategory = cat.name }
                                ) {
                                    Text(
                                        text = "${cat.icon} ${cat.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Save button
                        val canSave = parsedAmount.toDoubleOrNull() != null
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (canSave) Brush.horizontalGradient(listOf(gradStart, gradEnd))
                                    else Brush.horizontalGradient(listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    ))
                                )
                                .clickable(
                                    enabled = canSave,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    val amount = parsedAmount.toDoubleOrNull() ?: return@clickable
                                    val catId  = categories.find { it.name == parsedCategory }?.id
                                        ?: categories.firstOrNull()?.id ?: 1L
                                    val now   = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    expenseViewModel.addExpense(
                                        Expense(
                                            amount      = amount,
                                            categoryId  = catId,
                                            description = parsedMerchant.ifBlank { "Voice expense" },
                                            merchant    = parsedMerchant.ifBlank { null },
                                            date        = today,
                                            time        = now,
                                            source      = "voice"
                                        )
                                    )
                                    savedSuccess = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Save Expense",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (canSave) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Success card ──────────────────────────────────────────────
            if (savedSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("✓", style = MaterialTheme.typography.displaySmall,
                            color = Color(0xFF10B981))
                        Text("Expense Saved!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981))
                        OutlinedButton(
                            onClick = {
                                recognizedText = ""; parsedAmount = ""
                                parsedMerchant = ""; parsedCategory = ""
                                showPreview = false; savedSuccess = false
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Add Another") }
                        TextButton(onClick = onSaved) {
                            Text("View Expenses")
                        }
                    }
                }
            }
        }
    }
}

// ── Voice text parser ─────────────────────────────────────────────────────────

private fun parseVoiceInput(text: String, categoryNames: List<String>): Triple<Double?, String?, String?> {
    val lower = text.lowercase()

    // Amount: first digit sequence
    val amount = Regex("""(\d+(?:\.\d{1,2})?)""").find(lower)
        ?.groupValues?.get(1)?.toDoubleOrNull()

    // Category: check if any category name exists verbatim in text first
    val category = categoryNames.firstOrNull { lower.contains(it.lowercase()) }
        ?: inferCategoryFromKeywords(lower, categoryNames)

    // Merchant: words after "at", "from", "in"
    val merchant = Regex("""(?:at|from|in)\s+([a-z][a-z\s]{1,25})(?:\s+for|\s+on|\s+worth|\s+using|$)""")
        .find(lower)?.groupValues?.get(1)?.trim()
        ?.split(" ")?.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    return Triple(amount, category, merchant)
}

private fun inferCategoryFromKeywords(lower: String, categoryNames: List<String>): String? {
    val keywordMap = mapOf(
        "Food"          to listOf("food", "eat", "restaurant", "cafe", "lunch", "dinner", "breakfast", "snack", "meal"),
        "Transport"     to listOf("transport", "uber", "taxi", "fuel", "petrol", "bus", "careem", "ride"),
        "Shopping"      to listOf("shop", "store", "mart", "mall", "buy", "bought", "purchase"),
        "Groceries"     to listOf("grocery", "groceries", "vegetable", "fruit", "ration"),
        "Bills"         to listOf("bill", "electricity", "internet", "phone", "utility", "gas"),
        "Health"        to listOf("health", "medicine", "doctor", "pharmacy", "clinic"),
        "Entertainment" to listOf("movie", "cinema", "game", "entertainment", "show")
    )
    for ((cat, keywords) in keywordMap) {
        if (keywords.any { lower.contains(it) }) {
            return categoryNames.find { it.equals(cat, ignoreCase = true) } ?: cat
        }
    }
    return null
}
