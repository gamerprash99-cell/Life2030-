package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ===========================================================================
// LifeOS design language — rebuilt from the Figma source of truth
// (file ehSpxDfVot6SgPIdd2Tvf4). A deep teal-navy gradient canvas with
// translucent cards, one mint signature accent, and Inter throughout.
//
// Dark is the reference polarity (that is what the Figma frames show).
// Light is a deliberate derived polarity: the same structure on a soft
// paper-mint canvas with a deepened mint so text keeps AA contrast.
//
// All text/background pairs are checked against WCAG 2.2 AA (4.5:1 body,
// 3:1 large text / UI components) in both themes.
// ===========================================================================

// ---- Canvas ----------------------------------------------------------------
//
// The page is a three-stop vertical gradient, taken from the Figma frames:
// a lit teal horizon at the top, deep space through the middle, a slightly
// bluer deep at the bottom so the canvas never reads as flat black.

/** Dark canvas stops. */
val LifeOSCanvasTopDark = Color(0xFF203D48)
val LifeOSCanvasMidDark = Color(0xFF0B141C)
val LifeOSCanvasBottomDark = Color(0xFF101B27)

/** Light canvas stops — the derived polarity: paper with a mint breath. */
val LifeOSCanvasTopLight = Color(0xFFF3F8F6)
val LifeOSCanvasMidLight = Color(0xFFE9F1EF)
val LifeOSCanvasBottomLight = Color(0xFFDFEBE8)

// ---- Surfaces --------------------------------------------------------------

/** Base surface (search fields, wells): opaque, one step above the canvas. */
val LifeOSSurfaceLight = Color(0xFFFFFFFF)
val LifeOSSurfaceDark = Color(0xFF162630)

/** A recessed surface for wells, insets and secondary panels. */
val LifeOSSurfaceVariantLight = Color(0xFFE1EBE8)
val LifeOSSurfaceVariantDark = Color(0xFF1E313C)

/** A raised surface one step above the base (menus, sheets, popovers). */
val LifeOSSurfaceRaisedLight = Color(0xFFFFFFFF)
val LifeOSSurfaceRaisedDark = Color(0xFF1B2C37)

/**
 * The signature card fill: a translucent sheet the canvas gradient breathes
 * through (Figma: rgba(31,53,64,0.79)). Light polarity keeps the same
 * translucency so the mint canvas shows through white cards identically.
 */
val LifeOSCardFillLight = Color(0xCCFFFFFF)
val LifeOSCardFillDark = Color(0xC91F3540)

/** The 1px card hairline (Figma: rgba(182,217,229,0.12)). */
val LifeOSCardStrokeLight = Color(0x140B2A21)
val LifeOSCardStrokeDark = Color(0x1FB6D9E5)

/** The floating bottom navigation bar (Figma: rgba(16,32,42,0.94)). */
val LifeOSNavBarLight = Color(0xF2FFFFFF)
val LifeOSNavBarDark = Color(0xEF10202A)

/** The selected destination pill behind a bottom-bar icon (Figma: #163D36). */
val LifeOSNavPillSelectedLight = Color(0xFFCDEFE0)
val LifeOSNavPillSelectedDark = Color(0xFF163D36)

// ---- Content (text & icons) ------------------------------------------------

/** Near-black ink. ~16:1 on [LifeOSSurfaceLight]. */
val LifeOSTextPrimaryLight = Color(0xFF10242C)
/** Off-white ink. ~15:1 on the dark canvas. */
val LifeOSTextPrimaryDark = Color(0xFFF2F7F7)
/** Supporting copy. 7.0:1 light / 7.2:1 dark. */
val LifeOSTextSecondaryLight = Color(0xFF48626C)
val LifeOSTextSecondaryDark = Color(0xFFA3B6BF)

/** The quiet tertiary — timestamps, captions, disabled hints. */
val LifeOSTextMutedLight = Color(0xFF64808A)
val LifeOSTextMutedDark = Color(0xFF7D959F)

// ---- Borders & outlines ----------------------------------------------------

/** The card hairline on opaque (non-translucent) surfaces. */
val LifeOSBorderLight = Color(0x140B2A21)
val LifeOSBorderDark = Color(0x1FB6D9E5)
val LifeOSOutlineLight = Color(0xFFB4C7C1)
val LifeOSOutlineDark = Color(0xFF3A505C)

// ---- Surface material ------------------------------------------------------
//
// The component kit's card slots. Dark values are the Figma card; light
// values are the derived-polarity card. The top/bottom edge and sheen
// slots are kept so cards can catch a whisper of top light without
// reintroducing heavy glassmorphism.

val GlassLight = LifeOSCardFillLight
val GlassDark = LifeOSCardFillDark
val GlassBorderLight = LifeOSCardStrokeLight
val GlassBorderDark = LifeOSCardStrokeDark

val GlassTopEdgeLight = Color(0x50FFFFFF)
val GlassTopEdgeDark = Color(0x1EFFFFFF)
val GlassBottomEdgeLight = Color(0x0A0B2A21)
val GlassBottomEdgeDark = Color(0x26000000)

val GlassSheenLight = Color(0x0DFFFFFF)
val GlassSheenDark = Color(0x0FFFFFFF)
val GlassInnerShadowLight = Color(0x0A0B2A21)
val GlassInnerShadowDark = Color(0x26000000)

/**
 * Ambient glow palette — faint coloured light the canvas is washed with.
 * The Figma canvas is a clean gradient, so these stay barely-there teal/mint
 * hazes used only behind hero moments (welcome, AI), never behind dense text.
 */
val AmbientGlowLavenderLight = Color(0x1A14795C) // deep mint haze
val AmbientGlowMagentaLight = Color(0x142E6099)  // slate-blue haze
val AmbientGlowSkyLight = Color(0x1478A8A0)      // pale sea haze
val AmbientGlowLavenderDark = Color(0x331E6E5E)  // teal haze
val AmbientGlowMagentaDark = Color(0x262E6099)   // slate-blue haze
val AmbientGlowSkyDark = Color(0x2657A88F)       // sea-mint haze

// ---- Brand — the mint signature --------------------------------------------

/**
 * The app's one accent in its **light-polarity** form: a deepened mint that
 * carries white text at 5.3:1. Filled mint surfaces in light mode use the
 * brand mint with dark ink instead (see [LifeOSMint]).
 */
val LifeOSPrimary = Color(0xFF14795C)

/** A deeper mint for pressed states and gradient depth. */
val LifeOSPrimaryDeep = Color(0xFF0E5A45)

/** The calm partner: a desaturated sea-teal for secondary chips and tabs. */
val LifeOSSecondary = Color(0xFF45786B)

/** A very soft mint wash, for containers and selected surfaces (light). */
val LifeOSAccentLavender = Color(0xFFD5F0E6)

/** Amber-gold — the warm third accent (light-polarity value). */
val LifeOSTertiary = Color(0xFF8A6A12)

/**
 * The brand mint itself (#A8E8D3 from Figma), used as a *fill* in both
 * themes: the center Capture pill, progress fills, selected chips. On dark
 * it sits directly on the canvas; on light it is paired with [LifeOSMintInk].
 */
val LifeOSMint = Color(0xFFA8E8D3)

/** Dark ink that goes on mint fills (≈12:1 on [LifeOSMint]). */
val LifeOSMintInk = Color(0xFF06281F)

// The identity gradient: deep teal -> mint horizon. Hero, Capture, AI orb.
val PrismIndigo = Color(0xFF0E5A45)
val PrismSky = Color(0xFF1E8E6E)
val PrismCoral = Color(0xFFA8E8D3)
val PrismSpectrum = listOf(PrismIndigo, PrismSky, PrismCoral)

// ---- Premium highlight -----------------------------------------------------

/**
 * Gold — reserved for premium moments: streaks, personal records,
 * achievements, "best day" badges. [LifeOSGold] is the light-polarity ink,
 * [LifeOSGoldBright] the dark-canvas gold, [LifeOSGoldSoft] its translucent fill.
 */
val LifeOSGold = Color(0xFF8A6A12)
val LifeOSGoldBright = Color(0xFFE8C978)
val LifeOSGoldSoft = Color(0x33E8C978)

// ---- State -----------------------------------------------------------------

/** Light-polarity state inks; dark-polarity values live in Theme.kt. */
val LifeOSSuccess = Color(0xFF14795C)
val LifeOSWarning = Color(0xFF8A5B00)
val LifeOSDanger = Color(0xFFB3261E)
val LifeOSInfo = Color(0xFF2E6099)

/** Translucent state fills for chips and soft badges, both themes. */
val LifeOSSuccessSoft = Color(0x2E14795C)
val LifeOSWarningSoft = Color(0x2E8A5B00)
val LifeOSDangerSoft = Color(0x2EB3261E)
val LifeOSInfoSoft = Color(0x2E2E6099)

/** The neutral base every soft shadow is mixed from — the deep canvas ink. */
val LifeOSShadow = Color(0xFF06141A)

/** A dimming scrim behind modal surfaces and the capture hub. */
val LifeOSScrim = Color(0x9906141A)

// ---- Canvas brush ----------------------------------------------------------

/**
 * The page background: the three-stop vertical gradient every screen sits on.
 * This is the design's signature — never a flat color.
 */
fun lifeOSCanvasBrush(dark: Boolean): Brush =
    if (dark) {
        Brush.verticalGradient(
            colors = listOf(LifeOSCanvasTopDark, LifeOSCanvasMidDark, LifeOSCanvasBottomDark)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(LifeOSCanvasTopLight, LifeOSCanvasMidLight, LifeOSCanvasBottomLight)
        )
    }

// ===========================================================================
// Diary — mood palette.
//
// Moods are domain semantics (a mood's colour is part of the user's data),
// so they are kept stable across the redesign; the diary's paper surfaces
// are restyled with the app palette instead of their own hexes.
// ===========================================================================

val DiaryMoodHappy = Color(0xFFF59E0B)
val DiaryMoodCalm = Color(0xFF10B981)
val DiaryMoodSad = Color(0xFF6366F1)
val DiaryMoodStressed = Color(0xFFF43F5E)
val DiaryMoodExcited = Color(0xFF7D5260)

val DiaryMoodAngry = Color(0xFFEF6B5E)
val DiaryMoodAnxious = Color(0xFFF2A65A)
val DiaryMoodTired = Color(0xFF8B93B8)
val DiaryMoodNeutral = Color(0xFFB9AFC4)

/** Pastel mood-pill fills. */
val DiaryMoodHappyPastel = Color(0xFFFEF3C7)
val DiaryMoodCalmPastel = Color(0xFFD1FAE5)
val DiaryMoodSadPastel = Color(0xFFE0E7FF)
val DiaryMoodStressedPastel = Color(0xFFFFE4E6)
val DiaryMoodExcitedPastel = Color(0xFFFCE7F3)
val DiaryMoodAngryPastel = Color(0xFFFFE3E0)
val DiaryMoodAnxiousPastel = Color(0xFFFDEBD7)
val DiaryMoodTiredPastel = Color(0xFFE7E9F5)
