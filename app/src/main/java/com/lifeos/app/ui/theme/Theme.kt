package com.lifeos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ===========================================================================
// LifeOS theme — Material mapping of the rebuilt (Figma-derived) palette.
// Dark is the reference polarity; light is the derived paper-mint polarity.
// ===========================================================================

private val LightColors = lightColorScheme(
    primary = LifeOSPrimary,
    onPrimary = Color.White,
    primaryContainer = LifeOSAccentLavender,
    onPrimaryContainer = Color(0xFF06281F),
    secondary = LifeOSSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F0E6),
    onSecondaryContainer = Color(0xFF06281F),
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
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F8F6),
    surfaceContainer = Color(0xFFEDF3F1),
    surfaceContainerHigh = Color(0xFFE5EDEA),
    surfaceContainerHighest = LifeOSSurfaceVariantLight,
    surfaceTint = LifeOSPrimary,
    outline = LifeOSOutlineLight,
    outlineVariant = Color(0xFFD5E1DD),
    error = LifeOSDanger,
    onError = Color.White,
    scrim = LifeOSScrim,
)

private val DarkColors = darkColorScheme(
    // Mint is the signature light in the teal-navy night.
    primary = LifeOSMint,
    onPrimary = LifeOSMintInk,
    primaryContainer = Color(0xFF163D36),
    onPrimaryContainer = Color(0xFFCDEFE0),
    secondary = Color(0xFF8FBFB2),
    onSecondary = LifeOSMintInk,
    secondaryContainer = Color(0xFF1B3A34),
    onSecondaryContainer = Color(0xFFCDEFE3),
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
    surfaceContainerLowest = Color(0xFF0E1B23),
    surfaceContainerLow = Color(0xFF131F29),
    surfaceContainer = Color(0xFF182733),
    surfaceContainerHigh = Color(0xFF20333F),
    surfaceContainerHighest = Color(0xFF243946),
    surfaceTint = LifeOSMint,
    outline = LifeOSOutlineDark,
    outlineVariant = LifeOSCardStrokeDark,
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
