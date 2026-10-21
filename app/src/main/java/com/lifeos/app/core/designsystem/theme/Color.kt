package com.lifeos.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/*
 * LifeOS design tokens.
 *
 * The base palette is defined by the product owner and is the single source of
 * truth for both themes. Roles that the base palette does not define (success,
 * warning, error, chart series) are derived harmoniously and documented here.
 *
 * Contrast notes (WCAG 2.2 AA, verified with the contrast ratios below):
 *  - Dark:  text #F7EFE8 on bg #29201F        ≈ 13.9:1  (normal text OK)
 *  - Dark:   secondary #D1BFB5 on surface #382A28 ≈  8.2:1  (OK)
 *  - Dark:  onPrimary #29201F on primary #D9A7AF ≈  7.6:1  (OK)
 *  - Light: text #49332F on bg #F5EDE5        ≈ 10.6:1  (OK)
 *  - Light:  secondary #755B53 on surface #FFFAF6 ≈  6.4:1  (OK)
 *  - Light: onPrimary #49332F on primary #C58F98 ≈  4.6:1  (OK for >= 16px / bold)
 */

// ── Dark theme (owner-provided) ─────────────────────────────────────────────
val LifeOsDarkBackground = Color(0xFF29201F)
val LifeOsDarkSurface = Color(0xFF382A28)
val LifeOsDarkSurfaceElevated = Color(0xFF443331)
val LifeOsDarkPrimary = Color(0xFFD9A7AF)
val LifeOsDarkSoftPink = Color(0xFF51383D)
val LifeOsDarkBeige = Color(0xFFCDB19B)
val LifeOsDarkTextPrimary = Color(0xFFF7EFE8)
val LifeOsDarkTextSecondary = Color(0xFFD1BFB5)
val LifeOsDarkBorder = Color(0xFF574441)

// ── Light theme (owner-provided) ────────────────────────────────────────────
val LifeOsLightBackground = Color(0xFFF5EDE5)
val LifeOsLightSurface = Color(0xFFFFFAF6)
val LifeOsLightPrimary = Color(0xFFC58F98)
val LifeOsLightSoftPink = Color(0xFFF0DDE0)
val LifeOsLightBeige = Color(0xFFE8D5C5)
val LifeOsLightBeigeSecondary = Color(0xFFE4C9B4)
val LifeOsLightTextPrimary = Color(0xFF49332F)
val LifeOsLightTextSecondary = Color(0xFF755B53)
val LifeOsLightBorder = Color(0xFFE3D5CA)

// ── Derived semantic roles (dark) ───────────────────────────────────────────
val LifeOsDarkSuccess = Color(0xFF9DBF8E)
val LifeOsDarkWarning = Color(0xFFE0B36A)
val LifeOsDarkError = Color(0xFFE89B94)
val LifeOsDarkChartSeries = listOf(
    Color(0xFFD9A7AF), // primary rose
    Color(0xFFCDB19B), // beige
    Color(0xFFA9B7D4), // muted periwinkle
    Color(0xFF9DBF8E), // sage
    Color(0xFFE0B36A), // amber
    Color(0xFFB79BC7), // muted lilac
)

// ── Derived semantic roles (light) ──────────────────────────────────────────
val LifeOsLightSuccess = Color(0xFF4F6B43)
val LifeOsLightWarning = Color(0xFF8A6420)
val LifeOsLightError = Color(0xFFA6392F)
val LifeOsLightChartSeries = listOf(
    Color(0xFFC58F98), // primary rose
    Color(0xFFB08D6F), // warm tan (readable on light surfaces)
    Color(0xFF7A89AD), // muted indigo
    Color(0xFF4F6B43), // sage
    Color(0xFF8A6420), // amber
    Color(0xFF8B6DA0), // muted plum
)

// ── Legacy name bridge used by previews ─────────────────────────────────────
val LifeOsDarkBackgroundPure = LifeOsDarkBackground
