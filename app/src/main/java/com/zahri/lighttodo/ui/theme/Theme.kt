package com.zahri.lighttodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Brand Colors ────────────────────────────────────────────────
private val BrandOrange = Color(0xFFFF9F0A)   // iOS-style warm amber
private val OverdueRed = Color(0xFFFF453A)    // iOS system red

// ─── Dark Scheme ─────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color(0xFF1A1100),
    secondary = BrandOrange,
    background = Color(0xFF000000),            // True black like iOS
    surface = Color(0xFF1C1C1E),              // iOS secondary system background
    surfaceVariant = Color(0xFF2C2C2E),       // iOS tertiary system background
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF8E8E93),     // iOS secondary label
    outline = Color(0xFF38383A),              // iOS separator
    error = OverdueRed
)

// ─── Light Scheme ────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    secondary = BrandOrange,
    background = Color(0xFFF2F2F7),           // iOS system grouped background
    surface = Color(0xFFFFFFFF),              // iOS card/cell background
    surfaceVariant = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF8E8E93),     // iOS secondary label
    outline = Color(0xFFC6C6C8),             // iOS separator
    error = OverdueRed
)

@Composable
fun LightTodoTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

// ─── App-wide color tokens ───────────────────────────────────────
object AppColors {
    val Brand: Color = BrandOrange
    val BrandForegroundLight: Color = Color(0xFF8A4B00)
    val BrandForegroundDark: Color = Color(0xFFFFB340)
    val Overdue: Color = OverdueRed
    val DoneGreen: Color = Color(0xFF30D158)  // iOS system green for completion
}

// ─── Typography scale (iOS-inspired) ─────────────────────────────
object AppType {
    val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.37.sp)
    val title1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.36.sp)
    val title2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.35.sp)
    val title3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.38.sp)
    val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.41).sp)
    val body = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.41).sp)
    val callout = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.32).sp)
    val subheadline = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.24).sp)
    val footnote = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.08).sp)
    val caption1 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val caption2 = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.07.sp)
}
