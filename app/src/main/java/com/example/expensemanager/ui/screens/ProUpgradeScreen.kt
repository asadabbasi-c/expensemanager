package com.example.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.monetization.ProManager
import com.example.expensemanager.ui.theme.Brand400
import com.example.expensemanager.ui.theme.Brand800
import com.example.expensemanager.ui.theme.Dark200
import com.example.expensemanager.ui.theme.Dark300

private data class ProFeature(val icon: String, val title: String, val subtitle: String)

private val proFeatures = listOf(
    ProFeature("📩", "SMS Auto-Import",
        "Scan bank messages and auto-log debit transactions instantly"),
    ProFeature("🎙️", "Voice Entry",
        "Just say \"Spent 500 at KFC\" and it's logged — hands-free"),
    ProFeature("🎯", "Budget & Goal Tracking",
        "Set monthly saving goals, track daily budget, get early warnings"),
    ProFeature("🚫", "No Ads",
        "Clean, distraction-free experience across all screens"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeScreen(
    proManager : ProManager,
    onBack     : () -> Unit
) {
    val isPro by proManager.isPro.collectAsStateWithLifecycle()

    val gradStart = Brand400
    val gradEnd   = Color(0xFF22D3EE)   // cyan accent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(8.dp))

            // ── Hero ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Brand800, Brand400))),
                contentAlignment = Alignment.Center
            ) {
                Text("⭐", style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "SmartSpend Pro",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = Brand400
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Unlock your full financial intelligence",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── Feature cards ─────────────────────────────────────────────────
            proFeatures.forEach { feature ->
                ProFeatureRow(feature)
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(20.dp))

            // ── Pricing card ─────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Dark200)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "PKR 299",
                            style      = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Brand400
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "/month",
                            style  = MaterialTheme.typography.bodyMedium,
                            color  = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        "Cancel anytime · No hidden charges",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isPro) {
                // Already pro — show confirmation
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = Brand400.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null,
                            tint = Brand400, modifier = Modifier.size(20.dp))
                        Text("You're on SmartSpend Pro!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Brand400)
                    }
                }
            } else {
                // ── Upgrade button ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
                        .clickable {
                            /*
                             * TODO: Launch Play Billing flow here.
                             *
                             * val billingClient = BillingClient.newBuilder(context)...
                             * val productDetails = // query from Play
                             * val billingFlowParams = BillingFlowParams.newBuilder()
                             *     .setProductDetailsParamsList(...)
                             *     .build()
                             * billingClient.launchBillingFlow(activity, billingFlowParams)
                             *
                             * On successful purchase acknowledgement call:
                             *   proManager.grantPro()
                             *
                             * For now, during development, use proManager.debugToggle()
                             * to test the pro/free gate without a real purchase.
                             */
                            proManager.debugToggle()   // ← remove before publishing
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Upgrade to Pro →",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF0A0A0A)
                    )
                }

                Spacer(Modifier.height(12.dp))

                TextButton(onClick = { /* TODO: Play Billing restorePurchases() */ }) {
                    Text("Restore Purchase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Pro feature row card ──────────────────────────────────────────────────────

@Composable
private fun ProFeatureRow(feature: ProFeature) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Dark200)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brand400.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(feature.icon, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp)
            }
            Icon(Icons.Filled.Check, contentDescription = null,
                tint = Brand400, modifier = Modifier.size(18.dp))
        }
    }
}
