package com.example.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.monetization.ProManager
import com.example.expensemanager.viewmodel.SettingsViewModel
import com.example.expensemanager.ui.theme.*

private val COMMON_CURRENCIES = listOf(
    "PKR" to "Pakistani Rupee",
    "USD" to "US Dollar",
    "EUR" to "Euro",
    "GBP" to "British Pound",
    "INR" to "Indian Rupee",
    "AED" to "UAE Dirham",
    "SAR" to "Saudi Riyal",
    "CAD" to "Canadian Dollar",
    "AUD" to "Australian Dollar",
    "JPY" to "Japanese Yen",
    "CNY" to "Chinese Yuan",
    "SGD" to "Singapore Dollar",
    "MYR" to "Malaysian Ringgit",
    "BDT" to "Bangladeshi Taka",
    "NPR" to "Nepalese Rupee"
)

@Composable
fun SettingsScreen(
    settingsViewModel       : SettingsViewModel,
    proManager              : ProManager,
    onNavigateToGoals       : () -> Unit,
    onNavigateToCategories  : () -> Unit,
    onNavigateToFiles       : () -> Unit,
    onNavigateToUpgrade     : () -> Unit,
    onNavigateToSideBudget  : () -> Unit = {},
    onNavigateToIncome      : () -> Unit = {},
    onNavigateToSideProjects: () -> Unit = {},
    onNavigateToBadges      : () -> Unit = {}
) {
    val currency by settingsViewModel.currencySymbol.collectAsStateWithLifecycle()
    val isPro    by proManager.isPro.collectAsStateWithLifecycle()

    var showCurrencyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Settings",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
        }

        LazyColumn(
            modifier        = Modifier.fillMaxSize(),
            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Pro banner ────────────────────────────────────────────────────
            item {
                if (isPro) ProActiveCard() else ProUpgradeCard(onClick = onNavigateToUpgrade)
            }

            // ── Preferences ──────────────────────────────────────────────────
            item {
                SettingsSection(title = "PREFERENCES", titleColor = Brand400) {
                    SettingsItem(
                        icon    = Icons.Filled.AttachMoney,
                        iconTint = AccentBlue,
                        label   = "Currency",
                        value   = currency,
                        onClick = { showCurrencyDialog = true }
                    )
                }
            }

            // ── Budget & Goals ────────────────────────────────────────────────
            item {
                SettingsSection(title = "BUDGET & GOALS", titleColor = WarnAmber) {
                    SettingsItem(
                        icon     = Icons.Filled.TrackChanges,
                        iconTint = Brand400,
                        label    = "Budget Settings",
                        value    = "Period, limits & savings",
                        isPro    = !isPro,
                        onClick  = onNavigateToGoals
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 64.dp),
                        thickness = 1.dp,
                        color     = Divider
                    )
                    SettingsItem(
                        icon     = Icons.Filled.AttachMoney,
                        iconTint = Brand400,
                        label    = "Income",
                        value    = "Manage income sources",
                        onClick  = onNavigateToIncome
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 64.dp),
                        thickness = 1.dp,
                        color     = Divider
                    )
                    SettingsItem(
                        icon     = Icons.Filled.Schedule,
                        iconTint = AccentOrange,
                        label    = "Side Budget",
                        value    = "Time-boxed budget tracker",
                        onClick  = onNavigateToSideBudget
                    )
                }
            }

            // ── More ─────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "MORE", titleColor = AccentPurple) {
                    SettingsItem(
                        icon     = Icons.Filled.FolderOpen,
                        iconTint = AccentPurple,
                        label    = "Side Projects",
                        value    = "Separate budget projects",
                        isPro    = !isPro,
                        onClick  = onNavigateToSideProjects
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 64.dp),
                        thickness = 1.dp,
                        color     = Divider
                    )
                    SettingsItem(
                        icon     = Icons.Filled.EmojiEvents,
                        iconTint = AccentAmber,
                        label    = "Badges",
                        value    = "Your monthly score",
                        isPro    = !isPro,
                        onClick  = onNavigateToBadges
                    )
                }
            }

            // ── Data ─────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "DATA", titleColor = AccentBlue) {
                    SettingsItem(
                        icon     = Icons.Filled.Label,
                        iconTint = AccentPurple,
                        label    = "Categories",
                        value    = "Manage",
                        onClick  = onNavigateToCategories
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 64.dp),
                        thickness = 1.dp,
                        color     = Divider
                    )
                    SettingsItem(
                        icon     = Icons.Filled.FolderOpen,
                        iconTint = WarnAmber,
                        label    = "Export / Import",
                        value    = "CSV & backup",
                        onClick  = onNavigateToFiles
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "ABOUT", titleColor = TextSecondary) {
                    SettingsItem(
                        icon        = Icons.Filled.Star,
                        iconTint    = WarnAmber,
                        label       = "Version",
                        value       = "17.0",
                        showChevron = false,
                        onClick     = {}
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showCurrencyDialog) {
        CurrencyPickerDialog(
            current   = currency,
            onSelect  = { settingsViewModel.setCurrency(it); showCurrencyDialog = false },
            onDismiss = { showCurrencyDialog = false }
        )
    }
}

// ── Pro banner ────────────────────────────────────────────────────────────────

@Composable
private fun ProUpgradeCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0B1F13), Color(0xFF161616)))
            )
            .border(1.dp, Brand400.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brand400.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Text("⭐", fontSize = 22.sp) }

            Column(Modifier.weight(1f)) {
                Text(
                    "SmartSpend Pro",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Text(
                    "Voice entry, receipt scan, goals & no ads",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brand400)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    "Upgrade →",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF0A0A0A)
                )
            }
        }
    }
}

@Composable
private fun ProActiveCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0B1F13), Color(0xFF161616)))
            )
            .border(1.dp, Brand400.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brand400.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Text("⭐", fontSize = 22.sp) }

            Column(Modifier.weight(1f)) {
                Text(
                    "SmartSpend Pro",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Text(
                    "All features unlocked ✓",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand400
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brand400.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    "Active",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Brand400
                )
            }
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title      : String,
    titleColor : Color,
    content    : @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            fontWeight = FontWeight.Bold,
            color    = titleColor,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .border(1.dp, SurfaceVar, RoundedCornerShape(16.dp))
        ) {
            Column { content() }
        }
    }
}

// ── Settings item row ─────────────────────────────────────────────────────────

@Composable
private fun SettingsItem(
    icon        : ImageVector,
    iconTint    : Color,
    label       : String,
    value       : String  = "",
    isPro       : Boolean = false,
    showChevron : Boolean = true,
    onClick     : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon bubble
        Box(
            modifier         = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceEl),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        // Label
        Text(
            label,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color      = TextPrimary,
            modifier   = Modifier.weight(1f)
        )

        // Trailing: value + PRO badge + chevron
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (value.isNotEmpty()) {
                Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (isPro) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(WarnAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "PRO",
                        style      = MaterialTheme.typography.labelSmall.copy(
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color    = WarnAmber,
                        fontSize = 9.sp
                    )
                }
            }
            if (showChevron) {
                Text("›", style = MaterialTheme.typography.titleMedium, color = TextMuted, fontSize = 18.sp)
            }
        }
    }
}

// ── Currency picker ───────────────────────────────────────────────────────────

@Composable
private fun CurrencyPickerDialog(
    current  : String,
    onSelect : (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Surface,
        title = {
            Text("Select Currency", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                COMMON_CURRENCIES.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (code == current) Brand400.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(code) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            code,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = if (code == current) Brand400 else TextPrimary,
                            modifier   = Modifier.width(48.dp)
                        )
                        Text(name, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
