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
    primaryContainer = LifeOSAccentLavender,
    onPrimaryContainer = Color(0xFF1E1050),
    secondary = LifeOSSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EFFB),
    onSecondaryContainer = Color(0xFF053246),
    tertiary = LifeOSTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0E3),
    onTertiaryContainer = Color(0xFF5B0016),
    background = LifeOSBackgroundLight,
    onBackground = LifeOSTextPrimaryLight,
    surface = LifeOSSurfaceLight,
    onSurface = LifeOSTextPrimaryLight,
    surfaceVariant = LifeOSSurfaceVariantLight,
    onSurfaceVariant = LifeOSTextSecondaryLight,
    surfaceContainerHighest = LifeOSSurfaceVariantLight,
    outline = LifeOSOutlineLight,
    outlineVariant = Color(0xFFDEDEE8),
    error = LifeOSDanger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9AEFF),
    onPrimary = Color(0xFF221070),
    primaryContainer = Color(0xFF4A3AC0),
    onPrimaryContainer = Color(0xFFE7E3FF),
    secondary = Color(0xFF7FD0F5),
    onSecondary = Color(0xFF003347),
    secondaryContainer = Color(0xFF0E5E86),
    onSecondaryContainer = Color(0xFFCDEBFF),
    tertiary = Color(0xFFFF9AA5),
    onTertiary = Color(0xFF5B0016),
    tertiaryContainer = Color(0xFF9A2E3C),
    onTertiaryContainer = Color(0xFFFFE0E3),
    background = LifeOSBackgroundDark,
    onBackground = LifeOSTextPrimaryDark,
    surface = LifeOSSurfaceDark,
    onSurface = LifeOSTextPrimaryDark,
    surfaceVariant = LifeOSSurfaceVariantDark,
    onSurfaceVariant = LifeOSTextSecondaryDark,
    surfaceContainerHighest = LifeOSSurfaceVariantDark,
    outline = LifeOSOutlineDark,
    outlineVariant = Color(0xFF2A2A38),
    error = Color(0xFFFF8AA0),
    onError = Color(0xFF3E0012),
)

/**
 * All the paints one glass surface needs. [surface] and [border] keep the
 * original two-field contract alive (existing call sites read them), while the
 * new fields carry the edge light, the specular sheen and the base shadow that
 * make a translucent surface read as a real curved lens.
 */
data class GlassColors(
    val surface: Color,
    val border: Color,
    val topEdge: Color,
    val bottomEdge: Color,
    val sheen: Color,
    val innerShadow: Color
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        surface = GlassLight,
        border = GlassBorderLight,
        topEdge = GlassTopEdgeLight,
        bottomEdge = GlassBottomEdgeLight,
        sheen = GlassSheenLight,
        innerShadow = GlassInnerShadowLight
    )
}

/** The three ambient colour clouds that float behind every screen. */
data class AmbientGlowColors(val primary: Color, val secondary: Color, val tertiary: Color)

val LocalAmbientGlowColors = staticCompositionLocalOf {
    AmbientGlowColors(
        primary = AmbientGlowLavenderLight,
        secondary = AmbientGlowMagentaLight,
        tertiary = AmbientGlowSkyLight
    )
}

/**
 * Semantic colours the Material scheme does not carry but the LifeOS UI needs
 * by name: success / warning / info states, the hairline border colour used on
 * opaque (non-glass) surfaces, the muted tertiary text, and the premium gold
 * used for streaks, records and achievements. Screens read these instead of
 * reaching for a raw hex, so a future palette change is a one-file edit.
 */
data class LifeOSSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val border: Color,
    val muted: Color,
    val gold: Color,
    val goldSoft: Color,
    /** The neutral base colour every soft shadow is mixed from. */
    val shadow: Color,
)

val LocalLifeOSSemanticColors = staticCompositionLocalOf {
    LifeOSSemanticColors(
        success = LifeOSSuccess,
        onSuccess = Color.White,
        warning = LifeOSWarning,
        onWarning = Color.White,
        info = LifeOSInfo,
        border = LifeOSBorderLight,
        muted = LifeOSTextMutedLight,
        gold = LifeOSGold,
        goldSoft = LifeOSGoldSoft,
        shadow = LifeOSShadow,
    )
}

@Composable
fun LifeOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic Material colors remain opt-in so the LifeOS brand palette is stable.
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val glassColors = if (darkTheme) {
        GlassColors(
            surface = GlassDark,
            border = GlassBorderDark,
            topEdge = GlassTopEdgeDark,
            bottomEdge = GlassBottomEdgeDark,
            sheen = GlassSheenDark,
            innerShadow = GlassInnerShadowDark
        )
    } else {
        GlassColors(
            surface = GlassLight,
            border = GlassBorderLight,
            topEdge = GlassTopEdgeLight,
            bottomEdge = GlassBottomEdgeLight,
            sheen = GlassSheenLight,
            innerShadow = GlassInnerShadowLight
        )
    }
    val ambientGlowColors = if (darkTheme) {
        AmbientGlowColors(
            primary = AmbientGlowLavenderDark,
            secondary = AmbientGlowMagentaDark,
            tertiary = AmbientGlowSkyDark
        )
    } else {
        AmbientGlowColors(
            primary = AmbientGlowLavenderLight,
            secondary = AmbientGlowMagentaLight,
            tertiary = AmbientGlowSkyLight
        )
    }
    val semanticColors = if (darkTheme) {
        LifeOSSemanticColors(
            success = Color(0xFF4ADE80),
            onSuccess = Color(0xFF00391C),
            warning = Color(0xFFFBBF24),
            onWarning = Color(0xFF2B1A00),
            info = Color(0xFF7FC4FF),
            border = LifeOSBorderDark,
            muted = LifeOSTextMutedDark,
            gold = Color(0xFFF0C868),
            goldSoft = LifeOSGoldSoft,
            shadow = Color.Black,
        )
    } else {
        LifeOSSemanticColors(
            success = LifeOSSuccess,
            onSuccess = Color.White,
            warning = LifeOSWarning,
            onWarning = Color.White,
            info = LifeOSInfo,
            border = LifeOSBorderLight,
            muted = LifeOSTextMutedLight,
            gold = LifeOSGold,
            goldSoft = LifeOSGoldSoft,
            shadow = LifeOSShadow,
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalGlassColors provides glassColors,
        LocalAmbientGlowColors provides ambientGlowColors,
        LocalLifeOSSemanticColors provides semanticColors
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = LifeOSTypography, shapes = LifeOSShapes, content = content)
    }
}
