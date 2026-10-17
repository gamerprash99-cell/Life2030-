package com.lifeos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The redesign's one accent is **mint** (`#A8E8D3`). On the light adaptation
 * that accent is too pale to carry white ink, so the light theme keeps the same
 * *system* — mint signature, slate support tones, teal canvas — but pulls the
 * accent down to a legible depth and lifts the canvas to a cool mist. The
 * light theme is a faithful adaptation of the same tokens, never a different
 * design; the 15 Figma frames are dark, so dark is the reference rendition.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F7D68),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEFE4),
    onPrimaryContainer = Color(0xFF06372C),
    secondary = Color(0xFF3C6E62),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E7E3),
    onSecondaryContainer = Color(0xFF14251F),
    tertiary = Color(0xFF8A6A2A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E4C6),
    onTertiaryContainer = Color(0xFF3A2A08),
    background = Color(0xFFEEF4F3),
    onBackground = Color(0xFF0C1A18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0C1A18),
    surfaceVariant = Color(0xFFE0EAE8),
    onSurfaceVariant = Color(0xFF41514E),
    surfaceContainerHighest = Color(0xFFE0EAE8),
    outline = Color(0xFF7D959F),
    outlineVariant = Color(0xFFC9D8D5),
    error = Color(0xFFB4322E),
    onError = Color.White,
)

/**
 * The dark theme is the Figma reference rendition, token-for-token: the mint
 * signature (`#A8E8D3`), ink `#F2F7F7`, the deep teal/navy canvas
 * (`#0B141C` core), slate surfaces (`#162630` / `#1C303A`) and the hairline
 * stroke `rgba(182,217,229,0.12)`. Values are lifted verbatim from the Figma
 * global variables (see `Color.kt`), so Compose and the design share one source
 * of truth.
 */
private val DarkColors = darkColorScheme(
    primary = FigmaAccent,
    onPrimary = FigmaOnAccent,
    primaryContainer = FigmaAccentDark,
    onPrimaryContainer = FigmaAccent,
    secondary = FigmaHeatmap,
    onSecondary = FigmaOnAccent,
    secondaryContainer = FigmaElevated,
    onSecondaryContainer = FigmaText,
    tertiary = FigmaWarning,
    onTertiary = Color(0xFF2E1C05),
    tertiaryContainer = Color(0xFF3A2E18),
    onTertiaryContainer = FigmaWarning,
    background = FigmaCanvasMid,
    onBackground = FigmaText,
    surface = FigmaSurface,
    onSurface = FigmaText,
    surfaceVariant = FigmaElevated,
    onSurfaceVariant = FigmaMuted,
    surfaceContainerHighest = FigmaElevated,
    outline = FigmaTertiary,
    outlineVariant = FigmaStroke,
    error = Color(0xFFFFB4AB),
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
        // The Figma card material: a translucent slate sheet `rgba(31,53,64,0.79)`
        // with the hairline stroke `rgba(182,217,229,0.12)`, a whisper of top
        // light and a pooled foot shadow — flat and engineered, not frosted.
        GlassColors(
            surface = FigmaCard,
            border = FigmaStroke,
            topEdge = Color(0x14FFFFFF),
            bottomEdge = Color(0x33000000),
            sheen = Color(0x0AFFFFFF),
            innerShadow = Color(0x1F000000)
        )
    } else {
        GlassColors(
            surface = Color(0xFCFFFFFF),
            border = Color(0x140A3B33),
            topEdge = GlassTopEdgeLight,
            bottomEdge = Color(0x0F000000),
            sheen = Color(0x0FFFFFFF),
            innerShadow = Color(0x0F002B24)
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
            success = FigmaHeatmap,
            onSuccess = FigmaOnAccent,
            warning = FigmaWarning,
            onWarning = Color(0xFF2E1C05),
            info = Color(0xFF9FC6DC),
            border = FigmaStroke,
            muted = FigmaTertiary,
            gold = FigmaWarning,
            goldSoft = Color(0x33E9C997),
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
