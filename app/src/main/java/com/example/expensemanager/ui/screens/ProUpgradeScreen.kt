package com.example.expensemanager.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.expensemanager.BuildConfig
import com.example.expensemanager.monetization.BillingManager
import com.example.expensemanager.monetization.ProManager
import com.example.expensemanager.ui.theme.*

private data class ProFeature(
    val icon      : String,
    val title     : String,
    val subtitle  : String,
    val iconBg    : Color
)

private val proFeatures = listOf(
    ProFeature("🎙️", "Voice Entry",     "Say it, log it — hands free",      AccentBlue),
    ProFeature("🎯", "Budget Goals",    "Monthly targets & early warnings",  Brand400),
    ProFeature("📷", "Receipt Scan",    "Snap a receipt, auto-fill fields",  AccentOrange),
    ProFeature("🚫", "No Ads",          "Clean, distraction-free UI",        AccentPurple)
)

@Composable
fun ProUpgradeScreen(
    proManager         : ProManager,
    billingManager     : BillingManager,
    onBack             : () -> Unit,
    onPurchase         : (planId: String) -> Unit,
    onRestore          : () -> Unit,
    onDebugActivatePro : () -> Unit = {}
) {
    val isPro          by proManager.isPro.collectAsStateWithLifecycle()
    val isLoading      by billingManager.isLoading.collectAsStateWithLifecycle()
    val errorMessage   by billingManager.errorMessage.collectAsStateWithLifecycle()
    val productDetails by billingManager.productDetails.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        val msg = errorMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            billingManager.clearError()
        }
    }

    var selectedPlan by remember { mutableStateOf(BillingManager.PLAN_YEARLY) }

    val monthlyPrice = productDetails?.let { billingManager.getPriceForPlan(BillingManager.PLAN_MONTHLY) } ?: ""
    val yearlyPrice  = productDetails?.let { billingManager.getPriceForPlan(BillingManager.PLAN_YEARLY)  } ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Back header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            }

            Column(
                modifier            = Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Hero card ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0B1F13), Color(0xFF111111)))
                        )
                        .border(1.dp, Brand400.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brand400.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⭐", fontSize = 34.sp)
                        }
                        Text(
                            "SmartSpend Pro",
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Brand400
                        )
                        Text(
                            "Unlock your full financial intelligence",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Feature grid (2×2) ────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    proFeatures.chunked(2).forEach { row ->
                        Row(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { feature ->
                                FeatureCard(feature, Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (isPro) {
                    // ── Already Pro card ──────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brand400.copy(alpha = 0.10f))
                            .border(1.dp, Brand400.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null,
                                tint = Brand400, modifier = Modifier.size(20.dp))
                            Text(
                                "You're on SmartSpend Pro!",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Brand400
                            )
                        }
                    }
                } else {
                    // ── Plan selector ─────────────────────────────────────────
                    PlanSelector(
                        selectedPlan   = selectedPlan,
                        monthlyPrice   = monthlyPrice,
                        yearlyPrice    = yearlyPrice,
                        onPlanSelected = { selectedPlan = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── CTA button ────────────────────────────────────────────
                    val canPurchase = !isLoading && productDetails != null
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (canPurchase) Brand400
                                else Brand400.copy(alpha = 0.35f)
                            )
                            .clickable(enabled = canPurchase) { onPurchase(selectedPlan) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(18.dp),
                                    color       = Color(0xFF0A0A0A),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "Connecting…",
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color(0xFF0A0A0A)
                                )
                            }
                        } else {
                            Text(
                                if (canPurchase) "Start Pro Subscription →"
                                else "Unavailable — Tap to Retry",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF0A0A0A)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Subscription renews automatically. Cancel anytime in Google Play. " +
                        "Basic features remain free.",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    TextButton(onClick = onRestore) {
                        Text(
                            "Restore Purchase",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    if (BuildConfig.DEBUG) {
                        TextButton(onClick = onDebugActivatePro) {
                            Text(
                                "⚙ Debug: Activate Pro",
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── Feature card (grid item) ──────────────────────────────────────────────────

@Composable
private fun FeatureCard(feature: ProFeature, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(feature.iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(feature.icon, fontSize = 20.sp)
            }
            Text(
                feature.title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary
            )
            Text(
                feature.subtitle,
                style      = MaterialTheme.typography.bodySmall,
                color      = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Plan selector ─────────────────────────────────────────────────────────────

@Composable
private fun PlanSelector(
    selectedPlan   : String,
    monthlyPrice   : String,
    yearlyPrice    : String,
    onPlanSelected : (String) -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlanOption(
            planId     = BillingManager.PLAN_MONTHLY,
            label      = "Monthly",
            price      = if (monthlyPrice.isNotEmpty()) "$monthlyPrice / month" else "Loading…",
            badge      = null,
            isSelected = selectedPlan == BillingManager.PLAN_MONTHLY,
            onClick    = { onPlanSelected(BillingManager.PLAN_MONTHLY) }
        )
        PlanOption(
            planId     = BillingManager.PLAN_YEARLY,
            label      = "Yearly",
            price      = if (yearlyPrice.isNotEmpty()) "$yearlyPrice / year" else "Loading…",
            badge      = "BEST VALUE",
            isSelected = selectedPlan == BillingManager.PLAN_YEARLY,
            onClick    = { onPlanSelected(BillingManager.PLAN_YEARLY) }
        )
    }
}

@Composable
private fun PlanOption(
    planId     : String,
    label      : String,
    price      : String,
    badge      : String?,
    isSelected : Boolean,
    onClick    : () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) Brand400 else SurfaceVar,
        animationSpec = tween(200),
        label         = "planBorder"
    )
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) Brand400.copy(alpha = 0.08f) else SurfaceEl,
        animationSpec = tween(200),
        label         = "planBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Radio dot
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 6.dp else 1.5.dp,
                        color = if (isSelected) Brand400 else SurfaceVar,
                        shape = CircleShape
                    )
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
                Text(price, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(WarnAmber.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        badge,
                        style      = MaterialTheme.typography.labelSmall.copy(
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color    = WarnAmber,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
