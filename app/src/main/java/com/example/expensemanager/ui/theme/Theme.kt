package com.example.expensemanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── SmartSpend brand greens ───────────────────────────────────────────────────
val Brand300  = Color(0xFF86EFAC)
val Brand400  = Color(0xFF4ADE80)   // primary green accent
val Brand500  = Color(0xFF22C55E)
val Brand700  = Color(0xFF15803D)
val Brand800  = Color(0xFF166534)

// ── Dark surfaces (exact design tokens) ──────────────────────────────────────
val Bg          = Color(0xFF0D0D0D)   // T.bg — app background
val Surface     = Color(0xFF161616)   // T.surface — cards, sheets
val SurfaceEl   = Color(0xFF1E1E1E)   // T.surfaceEl — elevated items, inputs
val SurfaceVar  = Color(0xFF222222)   // T.surfaceVar / T.border
val Divider     = Color(0xFF2A2A2A)   // T.divider

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF0F0F0)   // T.textPrimary
val TextSecondary = Color(0xFF888888)   // T.textSecondary
val TextMuted     = Color(0xFF555555)   // T.textMuted

// ── Semantic ──────────────────────────────────────────────────────────────────
val ErrorRed    = Color(0xFFFF6B6B)   // T.error
val WarnAmber   = Color(0xFFFBBF24)   // T.amber

// ── Accent palette ────────────────────────────────────────────────────────────
val AccentBlue   = Color(0xFF60A5FA)
val AccentPurple = Color(0xFFA78BFA)
val AccentOrange = Color(0xFFFB923C)
val AccentPink   = Color(0xFFF472B6)
val AccentCyan   = Color(0xFF22D3EE)
val AccentAmber  = Color(0xFFFBBF24)
val AccentGreen  = Brand400
val GreenDim     = Color(0xFF4ADE80).copy(alpha = 0.09f)

// ── Shapes ────────────────────────────────────────────────────────────────────
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ── SmartSpend Dark Color Scheme ──────────────────────────────────────────────
private val SmartSpendColorScheme = darkColorScheme(
    primary              = Brand400,
    onPrimary            = Color(0xFF0A0A0A),
    primaryContainer     = Brand800,
    onPrimaryContainer   = Brand300,

    secondary            = Brand500,
    onSecondary          = Color(0xFF0A0A0A),
    secondaryContainer   = Color(0xFF1A3A26),
    onSecondaryContainer = Brand300,

    tertiary             = AccentCyan,
    onTertiary           = Color(0xFF08142A),
    tertiaryContainer    = Color(0xFF1A2E4A),
    onTertiaryContainer  = Color(0xFFBAD3FF),

    background           = Bg,
    onBackground         = TextPrimary,

    surface              = Surface,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVar,
    onSurfaceVariant     = TextSecondary,

    outline              = Divider,
    outlineVariant       = SurfaceVar,

    error                = ErrorRed,
    onError              = Color(0xFF3D0000),
    errorContainer       = Color(0xFF3D0000),
    onErrorContainer     = Color(0xFFFFAAAA),

    inverseSurface       = TextPrimary,
    inverseOnSurface     = Surface,
    inversePrimary       = Brand700
)

@Composable
fun ExpenseManagerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SmartSpendColorScheme,
        shapes      = AppShapes,
        content     = content
    )
}
