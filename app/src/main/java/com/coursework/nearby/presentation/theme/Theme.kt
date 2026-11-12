package com.coursework.nearby.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF2869DC)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDAE5FB),
    onPrimaryContainer = Color(0xFF0A2A66),
    secondary = Color(0xFF55607A),
    tertiary = Color(0xFF1E8E6E),
    error = Color(0xFFB3261E),
    background = Color(0xFFFAFBFD),
    surface = Color(0xFFFAFBFD),
    surfaceVariant = Color(0xFFE3E7EF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F5F9),
    surfaceContainer = Color(0xFFEEF1F6),
    surfaceContainerHigh = Color(0xFFE8ECF2),
    surfaceContainerHighest = Color(0xFFE3E7EF),
    secondaryContainer = Color(0xFFDAE5FB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC7FA),
    onPrimary = Color(0xFF0A2A66),
    primaryContainer = Color(0xFF1A4BA8),
    onPrimaryContainer = Color(0xFFDAE5FB),
    secondary = Color(0xFFBCC6E0),
    tertiary = Color(0xFF7FD8B8),
    error = Color(0xFFF2B8B5),
    background = Color(0xFF111318),
    surface = Color(0xFF111318),
    surfaceVariant = Color(0xFF43474F),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C21),
    surfaceContainer = Color(0xFF1D2026),
    surfaceContainerHigh = Color(0xFF282A31),
    surfaceContainerHighest = Color(0xFF33353C),
    secondaryContainer = Color(0xFF2A3F6B),
)

/** Material 3, светлая или тёмная схема по системной настройке. */
@Composable
fun NearbyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
