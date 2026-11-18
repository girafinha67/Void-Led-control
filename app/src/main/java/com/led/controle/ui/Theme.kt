// by: H3NRI_013/girafinha67
package com.led.controle.ui

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// Identidade preto e branco (referência "About me"): grafite, cinzas e branco.
private val Dark = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFEDEDED),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF0A0A0A),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFEDEDED),
    tertiary = Color(0xFFD0D0D0),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFB5B5B5),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF232323),
    surfaceContainerHighest = Color(0xFF2C2C2C),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFE59A94)
)

// Cores de status, bem suaves para não quebrar o visual monocromático.
val OkGreen = Color(0xFFA9D8B8)
val WarnYellow = Color(0xFFE8D29C)
val BadRed = Color(0xFFE59A94)

val Hairline = Color(0x33FFFFFF)
val InkBlack = Color(0xFF0A0A0A)

/** Fonte condensada e pesada dos títulos (como o "ABOUT" da referência). */
val Condensed = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

@Composable
fun LedTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, content = content)
}
