package com.lifeos.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Extended LifeOS tokens that Material 3's ColorScheme does not model
 * (borders, soft pink surface, beige accent, chart series). Features read these
 * through [LocalLifeOsTokens] so no screen hardcodes a color.
 */
@Immutable
data class LifeOsTokens(
    val softPinkSurface: Color,
    val beigeAccent: Color,
    val border: Color,
    val surfaceElevated: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val chartSeries: List<Color>,
    val isDark: Boolean,
)

val LocalLifeOsTokens = staticCompositionLocalOf { LifeOsDarkTokens }

val LifeOsDarkTokens = LifeOsTokens(
    softPinkSurface = LifeOsDarkSoftPink,
    beigeAccent = LifeOsDarkBeige,
    border = LifeOsDarkBorder,
    surfaceElevated = LifeOsDarkSurfaceElevated,
    success = LifeOsDarkSuccess,
    warning = LifeOsDarkWarning,
    error = LifeOsDarkError,
    chartSeries = LifeOsDarkChartSeries,
    isDark = true,
)

val LifeOsLightTokens = LifeOsTokens(
    softPinkSurface = LifeOsLightSoftPink,
    beigeAccent = LifeOsLightBeige,
    border = LifeOsLightBorder,
    surfaceElevated = LifeOsLightSurface,
    success = LifeOsLightSuccess,
    warning = LifeOsLightWarning,
    error = LifeOsLightError,
    chartSeries = LifeOsLightChartSeries,
    isDark = false,
)

private val DarkColors = darkColorScheme(
    primary = LifeOsDarkPrimary,
    onPrimary = Color(0xFF29201F),
    primaryContainer = LifeOsDarkSoftPink,
    onPrimaryContainer = LifeOsDarkTextPrimary,
    secondary = LifeOsDarkBeige,
    onSecondary = Color(0xFF29201F),
    secondaryContainer = LifeOsDarkSurfaceElevated,
    onSecondaryContainer = LifeOsDarkTextPrimary,
    tertiary = LifeOsDarkBeige,
    onTertiary = Color(0xFF29201F),
    background = LifeOsDarkBackground,
    onBackground = LifeOsDarkTextPrimary,
    surface = LifeOsDarkSurface,
    onSurface = LifeOsDarkTextPrimary,
    surfaceVariant = LifeOsDarkSurfaceElevated,
    onSurfaceVariant = LifeOsDarkTextSecondary,
    outline = LifeOsDarkBorder,
    outlineVariant = LifeOsDarkBorder,
    error = LifeOsDarkError,
    onError = Color(0xFF29201F),
    errorContainer = LifeOsDarkSoftPink,
    onErrorContainer = LifeOsDarkError,
)

private val LightColors = lightColorScheme(
    primary = LifeOsLightPrimary,
    onPrimary = Color(0xFF49332F),
    primaryContainer = LifeOsLightSoftPink,
    onPrimaryContainer = LifeOsLightTextPrimary,
    secondary = LifeOsLightBeige,
    onSecondary = Color(0xFF49332F),
    secondaryContainer = LifeOsLightBeigeSecondary,
    onSecondaryContainer = LifeOsLightTextPrimary,
    tertiary = LifeOsLightBeigeSecondary,
    onTertiary = Color(0xFF49332F),
    background = LifeOsLightBackground,
    onBackground = LifeOsLightTextPrimary,
    surface = LifeOsLightSurface,
    onSurface = LifeOsLightTextPrimary,
    surfaceVariant = LifeOsLightBeige,
    onSurfaceVariant = LifeOsLightTextSecondary,
    outline = LifeOsLightBorder,
    outlineVariant = LifeOsLightBorder,
    error = LifeOsLightError,
    onError = Color(0xFFFFFAF6),
    errorContainer = LifeOsLightSoftPink,
    onErrorContainer = LifeOsLightError,
)

/**
 * LifeOS theme. [darkTheme] defaults to the system setting; the app-level
 * setting (in settings) overrides it via [LifeOSTheme] calls from MainActivity.
 */
@Composable
fun LifeOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val tokens = if (darkTheme) LifeOsDarkTokens else LifeOsLightTokens
    CompositionLocalProvider(LocalLifeOsTokens provides tokens) {
        MaterialTheme(
            colorScheme = colors,
            typography = LifeOsTypography,
            shapes = LifeOsShapes,
            content = content,
        )
    }
}

/** Convenience accessor for the extended tokens. */
object LifeOs {
    val tokens: LifeOsTokens
        @Composable @ReadOnlyComposable get() = LocalLifeOsTokens.current
}
