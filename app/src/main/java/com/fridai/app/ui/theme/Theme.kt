package com.fridai.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00D9FF),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004D63),
    onPrimaryContainer = Color(0xFF9EEFFF),
    secondary = Color(0xFF6C63FF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF3D35B0),
    onSecondaryContainer = Color(0xFFE4DFFF),
    background = Color(0xFF0A0A1A),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1A1A2E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2A2A3E),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF000000)
)

@Composable
fun FridaiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
