package com.coursework.nearby.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Золотисто-янтарный акцент на тёмном и его приглушённый вариант для светлого фона. */
val Gold = Color(0xFFE8B15A)
val GoldDeep = Color(0xFFA9741C)
val Navy = Color(0xFF0B1220)

// Основное оформление: глубокий тёмно-синий фон, тёплый акцент.
private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1F1503),
    primaryContainer = Color(0xFF3A2C12),
    onPrimaryContainer = Color(0xFFF6DDB0),
    secondary = Color(0xFF9FB3D1),
    onSecondary = Color(0xFF0F1B2E),
    secondaryContainer = Color(0xFF3A2C12),
    onSecondaryContainer = Color(0xFFF6DDB0),
    tertiary = Color(0xFF6FD3B4),
    onTertiary = Color(0xFF04261C),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0907),
    background = Navy,
    onBackground = Color(0xFFEEF2F8),
    surface = Navy,
    onSurface = Color(0xFFEEF2F8),
    surfaceVariant = Color(0xFF1B2842),
    onSurfaceVariant = Color(0xFF9AA8BF),
    surfaceContainerLowest = Color(0xFF070C16),
    surfaceContainerLow = Color(0xFF111A2B),
    surfaceContainer = Color(0xFF152036),
    surfaceContainerHigh = Color(0xFF1B2842),
    surfaceContainerHighest = Color(0xFF22314F),
    outline = Color(0xFF3D4D6C),
    outlineVariant = Color(0xFF223049),
    scrim = Color(0xFF000000),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF14213D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7ECF7),
    onPrimaryContainer = Color(0xFF14213D),
    secondary = Color(0xFF5A6478),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E3C3),
    onSecondaryContainer = Color(0xFF3A2C12),
    tertiary = GoldDeep,
    onTertiary = Color.White,
    error = Color(0xFFB3261E),
    background = Color(0xFFF6F4EF),
    onBackground = Color(0xFF14171F),
    surface = Color(0xFFF6F4EF),
    onSurface = Color(0xFF14171F),
    surfaceVariant = Color(0xFFE9E5DC),
    onSurfaceVariant = Color(0xFF5D6370),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFAF7),
    surfaceContainer = Color(0xFFF0EDE6),
    surfaceContainerHigh = Color(0xFFE9E5DC),
    surfaceContainerHighest = Color(0xFFE2DDD2),
    outline = Color(0xFFA9A396),
    outlineVariant = Color(0xFFDDD8CC),
)

private val Base = Typography()

/** Крупные плотные заголовки и спокойный основной текст. */
private val AppTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(lineHeight = 24.sp),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.2.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),   // поля ввода, меню
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),       // карточки
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),   // диалоги, нижние панели
)

/** Тёмное ли сейчас оформление приложения (может не совпадать с системным). */
val LocalDarkTheme = staticCompositionLocalOf { true }

@Composable
fun NearbyTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
