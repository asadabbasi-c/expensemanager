package com.example.expensemanager.monetization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.ui.theme.Brand400
import com.example.expensemanager.ui.theme.Brand800

/**
 * Wraps any screen content behind a Pro paywall overlay.
 *
 * Usage:
 *   ProGate(proManager = proManager, featureName = "SMS Import", onUpgrade = { ... }) {
 *       SmsScreen(...)
 *   }
 */
@Composable
fun ProGate(
    proManager  : ProManager,
    featureName : String,
    featureIcon : String,
    description : String,
    onUpgrade   : () -> Unit,
    content     : @Composable () -> Unit
) {
    val isPro by proManager.isPro.collectAsStateWithLifecycle()

    if (isPro) {
        content()
    } else {
        ProLockedOverlay(
            featureName = featureName,
            featureIcon = featureIcon,
            description = description,
            onUpgrade   = onUpgrade
        )
    }
}

@Composable
private fun ProLockedOverlay(
    featureName : String,
    featureIcon : String,
    description : String,
    onUpgrade   : () -> Unit
) {
    val gradStart = Brand400
    val gradEnd   = Color(0xFF22D3EE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Lock icon with feature icon layered
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Brand800, Color(0xFF0A0A0A)))),
                contentAlignment = Alignment.Center
            ) {
                Text(featureIcon, style = MaterialTheme.typography.displaySmall)
                // Lock badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null,
                        tint = Brand400, modifier = Modifier.size(16.dp))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    featureName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    description,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Pro badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Brand400.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⭐", style = MaterialTheme.typography.labelMedium)
                    Text("SmartSpend Pro feature",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Brand400)
                }
            }

            Spacer(Modifier.height(4.dp))

            // Upgrade CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(gradStart, gradEnd)))
                    .clickable { onUpgrade() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Unlock with Pro →",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF0A0A0A)
                )
            }

            TextButton(onClick = onUpgrade) {
                Text("See all Pro features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
