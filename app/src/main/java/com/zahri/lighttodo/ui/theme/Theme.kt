package com.zahri.lighttodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandOrange = Color(0xFFFFB75A)
private val OverdueRed = Color(0xFFF26A6A)

private val DarkColors = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color(0xFF1A1100),
    secondary = BrandOrange,
    background = Color(0xFF111315),
    surface = Color(0xFF1E2226),
    surfaceVariant = Color(0xFF2A2F34),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFB6B6B6),
    error = OverdueRed
)

private val LightColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    secondary = BrandOrange,
    background = Color(0xFFF8F4F0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEEAE5),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF666666),
    error = OverdueRed
)

@Composable
fun LightTodoTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

object AppColors {
    val Overdue: Color = OverdueRed
    val Brand: Color = BrandOrange
}
