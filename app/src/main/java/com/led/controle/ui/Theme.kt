package com.led.controle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF002D6E),
    primaryContainer = Color(0xFF2B4678),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFC0C6DC),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainer = Color(0xFF1E1F26),
    surfaceContainerHigh = Color(0xFF282A2F),
    outline = Color(0xFF8E9099),
    error = Color(0xFFFFB4AB)
)

val OkGreen = Color(0xFF7EDC9B)
val WarnYellow = Color(0xFFFFD27A)
val BadRed = Color(0xFFFF8A80)

@Composable
fun LedTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, content = content)
}
