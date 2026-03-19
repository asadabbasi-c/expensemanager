package com.example.expensemanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── SmartSpend brand greens (from icon gradient) ──────────────────────────────
val Brand50   = Color(0xFFF0FDF4)
val Brand100  = Color(0xFFDCFCE7)
val Brand200  = Color(0xFFBBF7D0)
val Brand300  = Color(0xFF86EFAC)   // light accent
val Brand400  = Color(0xFF4ADE80)   // primary — bright lime-green from icon
val Brand500  = Color(0xFF22C55E)   // secondary
val Brand600  = Color(0xFF16A34A)
val Brand700  = Color(0xFF15803D)
val Brand800  = Color(0xFF166534)   // deep container
val Brand900  = Color(0xFF14532D)
val Brand950  = Color(0xFF052E16)

// ── Dark surfaces (matches icon background) ───────────────────────────────────
val Dark50    = Color(0xFF0A0A0A)   // deepest background
val Dark100   = Color(0xFF111111)   // background
val Dark200   = Color(0xFF1A1A1A)   // surface / card
val Dark300   = Color(0xFF242424)   // surface variant
val Dark400   = Color(0xFF2E2E2E)   // elevated surface / outline
val Dark500   = Color(0xFF3A3A3A)   // outline
val Dark600   = Color(0xFF525252)   // disabled / muted

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFFAAAAAA)
val TextMuted     = Color(0xFF6B6B6B)

// ── Semantic ──────────────────────────────────────────────────────────────────
val ErrorRed      = Color(0xFFFF6B6B)
val ErrorRedDark  = Color(0xFF3D0000)
val WarnAmber     = Color(0xFFFBBF24)

// ── Vibrant accent palette (used in categories, charts, badges) ───────────────
val AccentBlue    = Color(0xFF60A5FA)
val AccentPurple  = Color(0xFFA78BFA)
val AccentOrange  = Color(0xFFFB923C)
val AccentPink    = Color(0xFFF472B6)
val AccentCyan    = Color(0xFF22D3EE)
val AccentAmber   = Color(0xFFFBBF24)
val AccentRose    = Color(0xFFFF6B6B)
val AccentGreen   = Brand400

// ── Shapes ────────────────────────────────────────────────────────────────────
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ── SmartSpend Dark Color Scheme ──────────────────────────────────────────────
// Always dark — matches the brand icon (dark charcoal + lime-green)
private val SmartSpendColorScheme = darkColorScheme(
    primary              = Brand400,          // #4ADE80 bright lime-green
    onPrimary            = Dark50,            // near-black on green
    primaryContainer     = Brand800,          // deep forest green
    onPrimaryContainer   = Brand300,          // light green on dark green

    secondary            = Brand500,          // #22C55E medium green
    onSecondary          = Dark50,
    secondaryContainer   = Color(0xFF1A3A26),
    onSecondaryContainer = Brand300,

    tertiary             = AccentBlue,        // vibrant blue for contrast
    onTertiary           = Color(0xFF08142A),
    tertiaryContainer    = Color(0xFF1A2E4A),
    onTertiaryContainer  = Color(0xFFBAD3FF),

    background           = Dark100,           // #111111
    onBackground         = TextPrimary,

    surface              = Dark200,           // #1A1A1A  — cards
    onSurface            = TextPrimary,
    surfaceVariant       = Dark300,           // #242424
    onSurfaceVariant     = TextSecondary,

    outline              = Dark500,           // #3A3A3A
    outlineVariant       = Dark400,

    error                = ErrorRed,
    onError              = ErrorRedDark,
    errorContainer       = Color(0xFF3D0000),
    onErrorContainer     = Color(0xFFFFAAAA),

    inverseSurface       = TextPrimary,
    inverseOnSurface     = Dark200,
    inversePrimary       = Brand700
)

@Composable
fun ExpenseManagerTheme(
    content: @Composable () -> Unit
) {
    // SmartSpend is always dark — consistent with the brand icon
    MaterialTheme(
        colorScheme = SmartSpendColorScheme,
        shapes      = AppShapes,
        content     = content
    )
}
