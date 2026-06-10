package com.example.expensemanager.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.ui.theme.LocalCurrency
import com.example.expensemanager.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VoiceExpenseScreen(
    expenseViewModel: ExpenseViewModel,
    onSaved         : () -> Unit,
    onBack          : () -> Unit
) {
    val categories by expenseViewModel.categories.collectAsStateWithLifecycle()
    val currency    = LocalCurrency.current

    var recognizedText by remember { mutableStateOf("") }
    var parsedAmount   by remember { mutableStateOf("") }
    var parsedCategory by remember { mutableStateOf("") }
    var parsedMerchant by remember { mutableStateOf("") }
    var isListening    by remember { mutableStateOf(false) }
    var showPreview    by remember { mutableStateOf(false) }
    var savedSuccess   by remember { mutableStateOf(false) }
    var noSpeechApp    by remember { mutableStateOf(false) }

    val context = LocalContext.current

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

    fun openSpeechRecognizer() {
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

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) openSpeechRecognizer() }

    fun launchSpeech() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) openSpeechRecognizer()
        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        // ── Back header ───────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Voice Expense",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Pulse mic ─────────────────────────────────────────────────────
            PulseMicButton(isListening = isListening, onClick = { launchSpeech() })

            // ── Status label ──────────────────────────────────────────────────
            Text(
                text = when {
                    isListening -> "Listening…"
                    showPreview -> "Tap mic to try again"
                    else        -> "Tap to speak"
                },
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = if (isListening) Brand400 else TextPrimary
            )

            Text(
                text      = "Say something like:\n\"500 food at KFC\" or\n\"Spent 1200 on transport Uber\"",
                style     = MaterialTheme.typography.bodySmall,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )

            // ── Error: no speech app ──────────────────────────────────────────
            if (noSpeechApp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ErrorRed.copy(alpha = 0.10f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "Speech recognition is not available. Please install Google app or enable voice input.",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Transcript card ───────────────────────────────────────────────
            if (recognizedText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .border(1.dp, SurfaceVar, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "HEARD",
                            style  = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color  = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "\"$recognizedText\"",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color      = TextPrimary
                        )
                    }
                }
            }

            // ── Parsed expense preview ────────────────────────────────────────
            if (showPreview && !savedSuccess) {
                ParsedExpenseCard(
                    currency       = currency,
                    parsedAmount   = parsedAmount,
                    parsedMerchant = parsedMerchant,
                    parsedCategory = parsedCategory,
                    categories     = categories,
                    onAmountChange   = { parsedAmount   = it },
                    onMerchantChange = { parsedMerchant = it },
                    onCategoryChange = { parsedCategory = it },
                    onSave = {
                        val amount = parsedAmount.toDoubleOrNull() ?: return@ParsedExpenseCard
                        val catId  = categories.find { it.name == parsedCategory }?.id
                            ?: categories.firstOrNull()?.id ?: 1L
                        val now   = SimpleDateFormat("HH:mm",       Locale.getDefault()).format(Date())
                        val today = SimpleDateFormat("yyyy-MM-dd",  Locale.getDefault()).format(Date())
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
                    onTryAgain = {
                        recognizedText = ""; parsedAmount = ""
                        parsedMerchant = ""; parsedCategory = ""
                        showPreview = false
                        launchSpeech()
                    }
                )
            }

            // ── Success state ─────────────────────────────────────────────────
            if (savedSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brand400.copy(alpha = 0.09f))
                        .border(1.dp, Brand400.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("✓", style = MaterialTheme.typography.displaySmall, color = Brand400)
                        Text(
                            "Expense Saved!",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = Brand400
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceEl)
                                    .border(1.dp, SurfaceVar, RoundedCornerShape(10.dp))
                                    .clickable {
                                        recognizedText = ""; parsedAmount = ""
                                        parsedMerchant = ""; parsedCategory = ""
                                        showPreview = false; savedSuccess = false
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text("Add Another", style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brand400)
                                    .clickable(onClick = onSaved)
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text("View Expenses", style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Pulse mic button (3 expanding rings) ──────────────────────────────────────

@Composable
private fun PulseMicButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.6f,
        animationSpec  = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label          = "ring1"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.6f,
        animationSpec  = infiniteRepeatable(tween(1200, 400, easing = LinearEasing), RepeatMode.Restart),
        label          = "ring2"
    )
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.6f,
        animationSpec  = infiniteRepeatable(tween(1200, 800, easing = LinearEasing), RepeatMode.Restart),
        label          = "ring3"
    )
    val ringAlpha1 = ((1.6f - ring1Scale) / 0.6f).coerceIn(0f, 1f)
    val ringAlpha2 = ((1.6f - ring2Scale) / 0.6f).coerceIn(0f, 1f)
    val ringAlpha3 = ((1.6f - ring3Scale) / 0.6f).coerceIn(0f, 1f)

    Box(
        modifier         = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            // Ring 1
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(ring1Scale)
                    .clip(CircleShape)
                    .background(Brand400.copy(alpha = ringAlpha1 * 0.18f))
            )
            // Ring 2
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(ring2Scale)
                    .clip(CircleShape)
                    .background(Brand400.copy(alpha = ringAlpha2 * 0.12f))
            )
            // Ring 3
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(ring3Scale)
                    .clip(CircleShape)
                    .background(Brand400.copy(alpha = ringAlpha3 * 0.07f))
            )
        }
        // Mic button
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(if (isListening) Brand400 else SurfaceEl)
                .border(
                    width = if (isListening) 0.dp else 1.5.dp,
                    color = if (isListening) Color.Transparent else SurfaceVar,
                    shape = CircleShape
                )
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Mic",
                tint     = if (isListening) Color(0xFF0A0A0A) else Brand400,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

// ── Parsed expense preview card ───────────────────────────────────────────────

@Composable
private fun ParsedExpenseCard(
    currency        : String,
    parsedAmount    : String,
    parsedMerchant  : String,
    parsedCategory  : String,
    categories      : List<com.example.expensemanager.data.model.Category>,
    onAmountChange  : (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSave          : () -> Unit,
    onTryAgain      : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Review & Edit",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )

            // Amount field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Amount ($currency)", style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceEl)
                        .border(1.dp, SurfaceVar, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (parsedAmount.isEmpty()) {
                        Text("0.00", color = TextMuted,
                            style = MaterialTheme.typography.bodyLarge)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value       = parsedAmount,
                        onValueChange = onAmountChange,
                        singleLine  = true,
                        textStyle   = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                    )
                }
            }

            // Merchant field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Merchant / Description", style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceEl)
                        .border(1.dp, SurfaceVar, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (parsedMerchant.isEmpty()) {
                        Text("e.g. KFC, Carrefour", color = TextMuted,
                            style = MaterialTheme.typography.bodyLarge)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value       = parsedMerchant,
                        onValueChange = onMerchantChange,
                        singleLine  = true,
                        textStyle   = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                    )
                }
            }

            // Category chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = cat.name == parsedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) Brand400 else SurfaceEl)
                                .border(
                                    1.dp,
                                    if (selected) Brand400 else SurfaceVar,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { onCategoryChange(cat.name) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "${cat.icon} ${cat.name}",
                                style  = MaterialTheme.typography.labelMedium,
                                color  = if (selected) Color(0xFF0A0A0A) else TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Action row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Try Again
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceEl)
                        .border(1.dp, SurfaceVar, RoundedCornerShape(12.dp))
                        .clickable(onClick = onTryAgain),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Try Again", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                // Save
                val canSave = parsedAmount.toDoubleOrNull() != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSave) Brand400 else Brand400.copy(alpha = 0.35f))
                        .clickable(enabled = canSave, onClick = onSave),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save Expense", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
                }
            }
        }
    }
}

// ── Voice text parser (unchanged logic) ──────────────────────────────────────

private fun parseVoiceInput(text: String, categoryNames: List<String>): Triple<Double?, String?, String?> {
    val lower = text.lowercase()
    val amount = Regex("""(\d+(?:\.\d{1,2})?)""").find(lower)
        ?.groupValues?.get(1)?.toDoubleOrNull()
    val category = categoryNames.firstOrNull { lower.contains(it.lowercase()) }
        ?: inferCategoryFromKeywords(lower, categoryNames)
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
