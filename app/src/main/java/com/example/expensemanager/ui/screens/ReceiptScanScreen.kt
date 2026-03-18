package com.example.expensemanager.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.viewmodel.ExpenseViewModel
import com.example.expensemanager.viewmodel.ReceiptViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    receiptViewModel : ReceiptViewModel,
    expenseViewModel : ExpenseViewModel,
    onSaved          : () -> Unit,
    onBack           : () -> Unit
) {
    val context       = LocalContext.current
    val imagePath     by receiptViewModel.imagePath.collectAsStateWithLifecycle()
    val isProcessing  by receiptViewModel.isProcessing.collectAsStateWithLifecycle()
    val parsedReceipt by receiptViewModel.parsedReceipt.collectAsStateWithLifecycle()
    val errorMessage  by receiptViewModel.errorMessage.collectAsStateWithLifecycle()
    val isSaved       by receiptViewModel.isSaved.collectAsStateWithLifecycle()
    val categories    by expenseViewModel.categories.collectAsStateWithLifecycle()

    // Local editable state seeded from OCR result
    var amountText         by remember(parsedReceipt) {
        mutableStateOf(parsedReceipt?.amount?.let { "%.2f".format(it) } ?: "")
    }
    var merchantText       by remember(parsedReceipt) {
        mutableStateOf(parsedReceipt?.merchant ?: "")
    }
    var dateText           by remember(parsedReceipt) {
        mutableStateOf(parsedReceipt?.date
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var descriptionText    by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var catDropdownOpen    by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCategoryId == null && categories.isNotEmpty())
            selectedCategoryId = categories.first().id
    }

    // Temp file reference (remembered so the same file is reused during this screen session)
    val photoFile = remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoFile.value?.absolutePath?.let { path ->
                receiptViewModel.setImageAndProcess(path)
            }
        }
    }

    fun launchCamera() {
        val file = File(context.filesDir, "receipts/${System.currentTimeMillis()}.jpg")
            .also { it.parentFile?.mkdirs() }
        photoFile.value = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        takePictureLauncher.launch(uri)
    }

    // Navigate away after save
    LaunchedEffect(isSaved) {
        if (isSaved) { receiptViewModel.reset(); onSaved() }
    }

    val emerald   = Color(0xFF10B981)
    val gradStart = MaterialTheme.colorScheme.primary
    val gradEnd   = MaterialTheme.colorScheme.tertiary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { receiptViewModel.reset(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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

            // ── Receipt image / camera CTA ────────────────────────────────
            if (imagePath == null) {
                // No photo yet — show camera prompt card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clickable { launchCamera() },
                    shape  = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Tap to Take a Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Point your camera at the receipt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // Show captured image
                val bitmap = remember(imagePath) {
                    imagePath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(24.dp)
                ) {
                    Box {
                        if (bitmap != null) {
                            Image(
                                bitmap           = bitmap.asImageBitmap(),
                                contentDescription = "Receipt photo",
                                contentScale     = ContentScale.Crop,
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        } else {
                            Box(
                                Modifier.fillMaxWidth().height(220.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) { Text("📷", style = MaterialTheme.typography.displayMedium) }
                        }

                        // Retake button overlay
                        IconButton(
                            onClick  = { launchCamera() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Filled.Refresh,
                                contentDescription = "Retake",
                                tint = Color.White)
                        }
                    }
                }
            }

            // ── OCR loading state ─────────────────────────────────────────
            if (isProcessing) {
                OcrLoadingCard()
            }

            // ── Error banner ──────────────────────────────────────────────
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text     = errorMessage!!,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // ── Parsed / editable expense form ────────────────────────────
            if (imagePath != null && !isProcessing) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Expense Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            if (parsedReceipt != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = emerald.copy(alpha = 0.12f)
                                ) {
                                    Text("✓ Auto-filled",
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = emerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        OutlinedTextField(
                            value         = amountText,
                            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                            label         = { Text("Amount (PKR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            leadingIcon   = {
                                Text("PKR",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        )

                        OutlinedTextField(
                            value         = merchantText,
                            onValueChange = { merchantText = it },
                            label         = { Text("Merchant / Store") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value         = descriptionText,
                            onValueChange = { descriptionText = it },
                            label         = { Text("Description (optional)") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value         = dateText,
                            onValueChange = { dateText = it },
                            label         = { Text("Date (yyyy-MM-dd)") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth()
                        )

                        // Category dropdown
                        val selectedCat = categories.find { it.id == selectedCategoryId }
                        ExposedDropdownMenuBox(
                            expanded          = catDropdownOpen,
                            onExpandedChange  = { catDropdownOpen = !catDropdownOpen }
                        ) {
                            OutlinedTextField(
                                value      = selectedCat?.let { "${it.icon} ${it.name}" } ?: "Select Category",
                                onValueChange = {},
                                readOnly   = true,
                                label      = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catDropdownOpen) },
                                modifier   = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded          = catDropdownOpen,
                                onDismissRequest  = { catDropdownOpen = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text    = { Text("${cat.icon} ${cat.name}") },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            catDropdownOpen    = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Save button ───────────────────────────────────────────
                val canSave = amountText.toDoubleOrNull() != null
                    && (amountText.toDoubleOrNull() ?: 0.0) > 0
                    && selectedCategoryId != null

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (canSave) Brush.horizontalGradient(listOf(gradStart, gradEnd))
                            else Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                        )
                        .clickable(enabled = canSave) {
                            receiptViewModel.saveExpense(
                                amount      = amountText.toDouble(),
                                categoryId  = selectedCategoryId!!,
                                description = descriptionText,
                                merchant    = merchantText,
                                date        = dateText
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp))
                        Text("Save Expense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── OCR loading card ──────────────────────────────────────────────────────────

@Composable
private fun OcrLoadingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "ocr_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue   = 0.92f,
        targetValue    = 1.08f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ocr_scale"
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text     = "📄",
                style    = MaterialTheme.typography.displaySmall,
                modifier = Modifier.scale(scale)
            )
            Text("Reading receipt…",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Text("ML Kit is extracting text from your photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            @Suppress("DEPRECATION")
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
