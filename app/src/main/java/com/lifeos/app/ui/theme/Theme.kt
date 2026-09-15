package com.lifeos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = LifeOSPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = LifeOSSecondary,
    secondaryContainer = LifeOSAccentLavender,
    onSecondaryContainer = Color(0xFF580066),
    tertiary = Color(0xFF6C37C7),
    background = LifeOSBackgroundLight,
    onBackground = LifeOSTextPrimaryLight,
    surface = LifeOSSurfaceLight,
    onSurface = LifeOSTextPrimaryLight,
    surfaceVariant = Color(0xFFE8DDFF),
    onSurfaceVariant = Color(0xFF494455),
    outline = Color(0xFF7A7487),
    outlineVariant = Color(0xFFCAC3D8),
    error = LifeOSDanger,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCDBDFF),
    onPrimary = Color(0xFF381080),
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color(0xFFF1E8FF),
    secondary = Color(0xFFEA57FF),
    secondaryContainer = Color(0xFF580066),
    onSecondaryContainer = Color(0xFFFFD6FD),
    tertiary = Color(0xFFB99AEF),
    background = LifeOSBackgroundDark,
    onBackground = LifeOSTextPrimaryDark,
    surface = LifeOSSurfaceDark,
    onSurface = LifeOSTextPrimaryDark,
    surfaceVariant = Color(0xFF302A38),
    onSurfaceVariant = Color(0xFFC5B9CF),
    outline = Color(0xFF958DA0),
    outlineVariant = Color(0xFF4B4453),
    error = LifeOSDanger,
)

data class GlassColors(val surface: Color, val border: Color)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(surface = GlassLight, border = GlassBorderLight)
}

@Composable
fun LifeOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic Material colors remain opt-in so the LifeOS brand palette is stable.
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val glassColors = if (darkTheme) GlassColors(GlassDark, GlassBorderDark) else GlassColors(GlassLight, GlassBorderLight)
    androidx.compose.runtime.CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(colorScheme = colorScheme, typography = LifeOSTypography, shapes = LifeOSShapes, content = content)
    }
}
