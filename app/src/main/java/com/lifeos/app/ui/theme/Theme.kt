package com.lifeos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ===========================================================================
// LifeOS theme — Material mapping of the Lavender Dream palette.
// Light: #F1EDF7 canvas / #FCFAFF surface / #A995D1 primary.
// Dark: #17141F canvas / #24202F surface / #302A3E elevated / #BBA7E6 primary.
// ===========================================================================

private val LightColors = lightColorScheme(
    primary = LifeOSPrimary,
    onPrimary = LifeOSInkOnBrand,
    primaryContainer = LifeOSAccentLavender,
    onPrimaryContainer = LifeOSTextPrimaryLight,
    secondary = LifeOSSecondary,
    onSecondary = LifeOSInkOnBrand,
    secondaryContainer = Color(0xFFE7DFF3),
    onSecondaryContainer = LifeOSTextPrimaryLight,
    tertiary = LifeOSTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E3C4),
    onTertiaryContainer = Color(0xFF3E2A08),
    background = LifeOSCanvasMidLight,
    onBackground = LifeOSTextPrimaryLight,
    surface = LifeOSSurfaceLight,
    onSurface = LifeOSTextPrimaryLight,
    surfaceVariant = LifeOSSurfaceVariantLight,
    onSurfaceVariant = LifeOSTextSecondaryLight,
    surfaceContainerLowest = Color(0xFFFCFAFF),
    surfaceContainerLow = Color(0xFFF7F3FB),
    surfaceContainer = Color(0xFFF1EDF7),
    surfaceContainerHigh = Color(0xFFEAE4F3),
    surfaceContainerHighest = Color(0xFFE4DDF0),
    surfaceTint = LifeOSPrimary,
    outline = LifeOSOutlineLight,
    outlineVariant = LifeOSBorderLight,
    error = LifeOSDanger,
    onError = Color.White,
    scrim = LifeOSScrim,
)

private val DarkColors = darkColorScheme(
    // Lavender is the signature light in the plum night.
    primary = LifeOSMint,
    onPrimary = LifeOSInkOnBrand,
    primaryContainer = Color(0xFF463956),
    onPrimaryContainer = Color(0xFFD8C9EF),
    secondary = Color(0xFFC8B7EB),
    onSecondary = LifeOSInkOnBrand,
    secondaryContainer = Color(0xFF463956),
    onSecondaryContainer = Color(0xFFE8E0F6),
    tertiary = LifeOSGoldBright,
    onTertiary = Color(0xFF3A2606),
    tertiaryContainer = Color(0xFF4A3B14),
    onTertiaryContainer = Color(0xFFF8E4C4),
    background = LifeOSCanvasMidDark,
    onBackground = LifeOSTextPrimaryDark,
    surface = LifeOSSurfaceDark,
    onSurface = LifeOSTextPrimaryDark,
    surfaceVariant = LifeOSSurfaceVariantDark,
    onSurfaceVariant = LifeOSTextSecondaryDark,
    surfaceContainerLowest = Color(0xFF120F1A),
    surfaceContainerLow = Color(0xFF1B1725),
    surfaceContainer = Color(0xFF201C2A),
    surfaceContainerHigh = Color(0xFF2A2436),
    surfaceContainerHighest = Color(0xFF302A3E),
    surfaceTint = LifeOSMint,
    outline = LifeOSOutlineDark,
    outlineVariant = LifeOSBorderDark,
    error = Color(0xFFFF8F8A),
    onError = Color(0xFF3A0D0B),
    scrim = LifeOSScrim,
)

/**
 * All the paints one translucent card needs. [surface] and [border] keep the
 * original two-field contract alive (existing call sites read them), while
 * the remaining fields carry the soft top light, the faint upper wash and the
 * pooled base shadow that let a sheet sit on the gradient without looking
 * like a flat rectangle.
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

/** The ambient colour clouds that can float behind hero moments. */
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
 * by name: success / warning / info / danger states (with soft translucent
 * fills for chips and badges), the hairline border colour, the muted tertiary
 * text, and the premium gold used for streaks, records and achievements.
 * Screens read these instead of reaching for a raw hex, so a future palette
 * change is a one-file edit.
 */
data class LifeOSSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successSoft: Color,
    val warning: Color,
    val onWarning: Color,
    val warningSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerSoft: Color,
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
        successSoft = LifeOSSuccessSoft,
        warning = LifeOSWarning,
        onWarning = Color.White,
        warningSoft = LifeOSWarningSoft,
        info = LifeOSInfo,
        infoSoft = LifeOSInfoSoft,
        danger = LifeOSDanger,
        onDanger = Color.White,
        dangerSoft = LifeOSDangerSoft,
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
            success = Color(0xFF86E0BE),
            onSuccess = Color(0xFF06281F),
            successSoft = Color(0x2E86E0BE),
            warning = Color(0xFFE9C46A),
            onWarning = Color(0xFF3A2606),
            warningSoft = Color(0x2EE9C46A),
            info = Color(0xFF8FB8E0),
            infoSoft = Color(0x2E8FB8E0),
            danger = Color(0xFFFF8F8A),
            onDanger = Color(0xFF3A0D0B),
            dangerSoft = Color(0x2EFF8F8A),
            border = LifeOSBorderDark,
            muted = LifeOSTextMutedDark,
            gold = LifeOSGoldBright,
            goldSoft = LifeOSGoldSoft,
            shadow = Color.Black,
        )
    } else {
        LifeOSSemanticColors(
            success = LifeOSSuccess,
            onSuccess = Color.White,
            successSoft = LifeOSSuccessSoft,
            warning = LifeOSWarning,
            onWarning = Color.White,
            warningSoft = LifeOSWarningSoft,
            info = LifeOSInfo,
            infoSoft = LifeOSInfoSoft,
            danger = LifeOSDanger,
            onDanger = Color.White,
            dangerSoft = LifeOSDangerSoft,
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
