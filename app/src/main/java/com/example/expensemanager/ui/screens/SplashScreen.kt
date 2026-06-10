package com.example.expensemanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensemanager.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val iconScale = remember { Animatable(0.5f) }
    val iconAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        iconAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        iconScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(400))
        delay(900)
        onFinished()
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Icon ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brand400.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💚", fontSize = 52.sp)
            }

            Spacer(Modifier.height(28.dp))

            // ── App name ──────────────────────────────────────────────────────
            Text(
                text       = "SmartSpend",
                color      = TextPrimary,
                fontSize   = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier   = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text          = "TRACK  ·  SAVE  ·  GROW",
                color         = Brand300.copy(alpha = 0.8f),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 2.2.sp,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.alpha(textAlpha.value)
            )
        }

        // ── Version ───────────────────────────────────────────────────────────
        Text(
            text     = "v17",
            color    = Brand400.copy(alpha = 0.30f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(textAlpha.value)
        )
    }
}
