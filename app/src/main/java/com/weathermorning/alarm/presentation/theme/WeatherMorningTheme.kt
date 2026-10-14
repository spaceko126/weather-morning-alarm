package com.weathermorning.alarm.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.weathermorning.alarm.R

/**
 * True when the app is painting its dark palette. Read this instead of
 * [isSystemInDarkTheme] inside the theme, because the ring screen deliberately
 * stays dark whatever the system is set to.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F3EF),
    onPrimaryContainer = Color(0xFF083B35),
    secondary = Color(0xFF1D4ED8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF102A66),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8C2),
    onTertiaryContainer = Color(0xFF4A2600),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEFF6F5),
    onSurfaceVariant = Color(0xFF374151),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFD6DEE6),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFFE3DE),
    onErrorContainer = Color(0xFF7A170F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5CC9BA),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF0B4F48),
    onPrimaryContainer = Color(0xFFB9EFE7),
    secondary = Color(0xFFA8C7FA),
    onSecondary = Color(0xFF0A2B62),
    secondaryContainer = Color(0xFF1B3A6E),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFFF5B96B),
    onTertiary = Color(0xFF452400),
    tertiaryContainer = Color(0xFF6A3A00),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF0E1216),
    onBackground = Color(0xFFE4E9EF),
    surface = Color(0xFF161B21),
    onSurface = Color(0xFFE4E9EF),
    surfaceVariant = Color(0xFF1E252C),
    onSurfaceVariant = Color(0xFFB6C1CB),
    outline = Color(0xFF6B7885),
    outlineVariant = Color(0xFF2B343D),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF6D1F17),
    onErrorContainer = Color(0xFFFFDAD5),
)

@Composable
fun WeatherMorningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = MaterialTheme.typography
    val appFontFamily = FontFamily(Font(R.font.app_sans_kr_regular))
    val appTypography = base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = appFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = appFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = appFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = appFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = appFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = appFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = appFontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = appFontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = appFontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = appFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = appFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = appFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = appFontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = appFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = appFontFamily),
    )
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = appTypography,
            content = content,
        )
    }
}

/** Sunrise wash behind the main screen, warm at dawn and muted at night. */
@Composable
fun appBackgroundBrush(): Brush {
    val colors = if (LocalIsDarkTheme.current) {
        listOf(
            Color(0xFF0B1A22),
            MaterialTheme.colorScheme.background,
            Color(0xFF1B1510),
        )
    } else {
        listOf(
            Color(0xFFEAF7FF),
            MaterialTheme.colorScheme.background,
            Color(0xFFFFF7ED),
        )
    }
    return Brush.verticalGradient(colors)
}
