package com.lifeos.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = LifeOSPrimary,
    onPrimary = Color.WhiteCompat,
    secondary = LifeOSSecondary,
    background = LifeOSBackgroundLight,
    surface = LifeOSSurfaceLight,
    onBackground = LifeOSTextPrimaryLight,
    onSurface = LifeOSTextPrimaryLight,
    error = LifeOSDanger,
)

private val DarkColors = darkColorScheme(
    primary = LifeOSPrimary,
    onPrimary = Color.WhiteCompat,
    secondary = LifeOSSecondary,
    background = LifeOSBackgroundDark,
    surface = LifeOSSurfaceDark,
    onBackground = LifeOSTextPrimaryDark,
    onSurface = LifeOSTextPrimaryDark,
    error = LifeOSDanger,
)

/**
 * Local "is dark theme" + glass tokens accessor, so screens don't need to
 * re-derive glass colors themselves. Kept simple (no CompositionLocal needed)
 * since [LocalGlassColors] below already does that job.
 */
data class GlassColors(
    val surface: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
)

val LocalGlassColors = androidx.compose.runtime.staticCompositionLocalOf {
    GlassColors(surface = GlassLight, border = GlassBorderLight)
}

@Composable
fun LifeOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Off by default: LifeOS has a deliberate brand palette (Section 53)
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val glassColors = if (darkTheme) {
        GlassColors(surface = GlassDark, border = GlassBorderDark)
    } else {
        GlassColors(surface = GlassLight, border = GlassBorderLight)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LifeOSTypography,
            shapes = LifeOSShapes,
            content = content
        )
    }
}

// Small shim to avoid importing Color twice with clashing names above.
private object Color {
    val WhiteCompat = androidx.compose.ui.graphics.Color.White
}
