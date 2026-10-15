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
    onPrimaryContainer = Color(0xFF4A1A08),
    secondary = LifeOSSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEFE3),
    onSecondaryContainer = Color(0xFF0B2A21),
    tertiary = LifeOSTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E3C4),
    onTertiaryContainer = Color(0xFF3E2A08),
    background = LifeOSBackgroundLight,
    onBackground = LifeOSTextPrimaryLight,
    surface = LifeOSSurfaceLight,
    onSurface = LifeOSTextPrimaryLight,
    surfaceVariant = LifeOSSurfaceVariantLight,
    onSurfaceVariant = LifeOSTextSecondaryLight,
    surfaceContainerHighest = LifeOSSurfaceVariantLight,
    outline = LifeOSOutlineLight,
    outlineVariant = Color(0xFFCBD9D5),
    error = LifeOSDanger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    // Coral is the signature light in the lagoon.
    primary = Color(0xFFFF9E80),
    onPrimary = Color(0xFF2A100A),
    primaryContainer = Color(0xFF7A3418),
    onPrimaryContainer = Color(0xFFFFDBCE),
    // Mint is the calm partner.
    secondary = Color(0xFF5EE0B0),
    onSecondary = Color(0xFF04231A),
    secondaryContainer = Color(0xFF154A3B),
    onSecondaryContainer = Color(0xFFCDEFE3),
    tertiary = Color(0xFFE9C46A),
    onTertiary = Color(0xFF3A2606),
    tertiaryContainer = Color(0xFF6E5320),
    onTertiaryContainer = Color(0xFFF8E4C4),
    background = LifeOSBackgroundDark,
    onBackground = LifeOSTextPrimaryDark,
    surface = LifeOSSurfaceDark,
    onSurface = LifeOSTextPrimaryDark,
    surfaceVariant = LifeOSSurfaceVariantDark,
    onSurfaceVariant = LifeOSTextSecondaryDark,
    surfaceContainerHighest = LifeOSSurfaceVariantDark,
    outline = LifeOSOutlineDark,
    outlineVariant = Color(0xFF2C3835),
    error = Color(0xFFFF8F8A),
    onError = Color(0xFF3A0D0B),
)

/**
 * All the paints one frosted surface needs. [surface] and [border] keep the
 * original two-field contract alive (existing call sites read them), while the
 * remaining fields carry the soft top light, the faint upper wash and the
 * pooled base shadow that make a translucent sheet read as glass. Under AURA
 * [surface] is deliberately translucent so the lagoon gradient breathes
 * through every card.
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
            success = Color(0xFF5EE0B0),
            onSuccess = Color(0xFF03211A),
            warning = Color(0xFFE9C46A),
            onWarning = Color(0xFF2E1C05),
            info = Color(0xFF8FB8E0),
            border = LifeOSBorderDark,
            muted = LifeOSTextMutedDark,
            gold = Color(0xFFE8C978),
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
