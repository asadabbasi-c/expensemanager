package com.example.expensemanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Emerald palette ──────────────────────────────────────────────────────────
val Emerald950 = Color(0xFF022C22)
val Emerald900 = Color(0xFF064E3B)
val Emerald800 = Color(0xFF065F46)
val Emerald700 = Color(0xFF047857)
val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald300 = Color(0xFF6EE7B7)
val Emerald200 = Color(0xFFA7F3D0)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald50  = Color(0xFFECFDF5)

// ── Teal accent ──────────────────────────────────────────────────────────────
val Teal700 = Color(0xFF0F766E)
val Teal500 = Color(0xFF14B8A6)
val Teal400 = Color(0xFF2DD4BF)
val Teal100 = Color(0xFFCCFBF1)

// ── Slate neutrals ───────────────────────────────────────────────────────────
val Slate950 = Color(0xFF020617)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50  = Color(0xFFF8FAFC)

// ── Semantic ─────────────────────────────────────────────────────────────────
val Rose500 = Color(0xFFF43F5E)
val Rose400 = Color(0xFFFB7185)
val Rose200 = Color(0xFFFECACA)

// ── Shapes ───────────────────────────────────────────────────────────────────
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ── Color schemes ────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Emerald800,
    onPrimary            = Color.White,
    primaryContainer     = Emerald100,
    onPrimaryContainer   = Emerald900,
    secondary            = Emerald600,
    onSecondary          = Color.White,
    secondaryContainer   = Emerald50,
    onSecondaryContainer = Emerald800,
    tertiary             = Teal700,
    onTertiary           = Color.White,
    tertiaryContainer    = Teal100,
    onTertiaryContainer  = Teal700,
    background           = Slate50,
    onBackground         = Slate900,
    surface              = Color.White,
    onSurface            = Slate900,
    surfaceVariant       = Slate100,
    onSurfaceVariant     = Slate500,
    outline              = Slate200,
    outlineVariant       = Slate100,
    error                = Rose500,
    onError              = Color.White,
    errorContainer       = Rose200,
    onErrorContainer     = Color(0xFF9F1239)
)

private val DarkColorScheme = darkColorScheme(
    primary              = Emerald400,
    onPrimary            = Emerald950,
    primaryContainer     = Emerald800,
    onPrimaryContainer   = Emerald200,
    secondary            = Emerald500,
    onSecondary          = Emerald950,
    secondaryContainer   = Emerald800,
    onSecondaryContainer = Emerald200,
    tertiary             = Teal400,
    onTertiary           = Slate950,
    tertiaryContainer    = Color(0xFF004F47),
    onTertiaryContainer  = Color(0xFF99F6E4),
    background           = Slate950,
    onBackground         = Slate100,
    surface              = Slate900,
    onSurface            = Slate100,
    surfaceVariant       = Slate800,
    onSurfaceVariant     = Slate400,
    outline              = Slate700,
    outlineVariant       = Slate800,
    error                = Rose400,
    onError              = Color(0xFF881337),
    errorContainer       = Color(0xFF4C0519),
    onErrorContainer     = Rose400
)

@Composable
fun ExpenseManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content
    )
}
