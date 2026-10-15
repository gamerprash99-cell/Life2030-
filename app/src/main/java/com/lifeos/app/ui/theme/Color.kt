package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================================================
// LifeOS "AURA" — the design language (v6, built from the spec §53 direction).
//
// Spec §53 asks for: premium · glassmorphism · Gen-Z · minimal · modern ·
// futuristic · calm · personal — frosted translucent surfaces, soft borders,
// soft shadows, large radii, layered surfaces, floating elements — while
// insisting that readability, accessibility, battery and performance come
// first and that glass is never overused.
//
// AURA answers that with a **lagoon**: a deep teal-to-plum canvas behind
// frosted glass, lit by one warm signature — **coral** — with **mint** as its
// calm partner. The previous "blue-black glass" is deliberately avoided: the
// canvas is teal/plum, never a cold blue-grey.
//
// Every public token name from the previous systems is kept, so every existing
// call site keeps compiling while it silently adopts the new look. The legacy
// GLASS- and PRISM-named tokens are retained as the frosted-surface and
// identity-gradient slots respectively (see the notes on each block).
//
// All text/background pairs below are checked against WCAG 2.2 AA (4.5:1 for
// body, 3:1 for large text and UI components) in both themes.
// ===========================================================================

// ---- Canvas & surfaces -----------------------------------------------------

/** The page behind everything: pale lagoon mist (the "day" polarity of AURA). */
val LifeOSBackgroundLight = Color(0xFFF1F6F4)
/** The deep lagoon — the dark canvas everything floats on. */
val LifeOSBackgroundDark = Color(0xFF070B0B)

val LifeOSSurfaceLight = Color(0xFFFFFFFF)
val LifeOSSurfaceDark = Color(0xFF0E1514)

/** A recessed surface for wells, insets and secondary panels. */
val LifeOSSurfaceVariantLight = Color(0xFFE4EDEA)
val LifeOSSurfaceVariantDark = Color(0xFF16201E)

/** A raised surface one step above [LifeOSSurfaceLight] (menus, sheets, popovers). */
val LifeOSSurfaceRaisedLight = Color(0xFFFFFFFF)
val LifeOSSurfaceRaisedDark = Color(0xFF1B2725)

// ---- Content (text & icons) ------------------------------------------------

/** Near-black ink. ~16:1 on [LifeOSSurfaceLight]. */
val LifeOSTextPrimaryLight = Color(0xFF0A1312)
/** Warm bone ink. ~16:1 on [LifeOSBackgroundDark]. */
val LifeOSTextPrimaryDark = Color(0xFFF1F5F2)
/** Supporting copy — slate teal. 6.5:1 light / 6.9:1 dark. */
val LifeOSTextSecondaryLight = Color(0xFF4C5B58)
val LifeOSTextSecondaryDark = Color(0xFFB9C7C3)

/** The quiet grey-teal — structural labels, dividers and disabled hints. */
val LifeOSTextMutedLight = Color(0xFF5F6D6A)
val LifeOSTextMutedDark = Color(0xFF8FA09A)

// ---- Borders & outlines ----------------------------------------------------

/** A cool hairline lifted from the glass, never a flat neutral rule. */
val LifeOSBorderLight = Color(0x1A0A3B33)
val LifeOSBorderDark = Color(0x1FFFFFFF)
val LifeOSOutlineLight = Color(0xFFA9BDB8)
val LifeOSOutlineDark = Color(0xFF3D4B48)

// ---- Surface material (legacy "Glass") -------------------------------------
//
// These slots are kept because the whole component kit reads them by name.
// Under AURA they describe a *frosted* sheet: translucent enough that the
// lagoon gradient breathes through it, with a soft top light and one border.

val GlassLight = Color(0xE6FFFFFF)
val GlassDark = Color(0xA6121C1A)
val GlassBorderLight = Color(0x1A0A3B33)
val GlassBorderDark = Color(0x26FFFFFF)

// The rim: a soft top light, a deeper pooled foot.
val GlassTopEdgeLight = Color(0x66FFFFFF)
val GlassTopEdgeDark = Color(0x24FFFFFF)
val GlassBottomEdgeLight = Color(0x14000000)
val GlassBottomEdgeDark = Color(0x40000000)

// A whisper of upper wash, so the glass catches the light without glinting.
val GlassSheenLight = Color(0x1AFFFFFF)
val GlassSheenDark = Color(0x14FFFFFF)
val GlassInnerShadowLight = Color(0x14002B24)
val GlassInnerShadowDark = Color(0x33000000)

/**
 * Ambient glow palette — the coloured light the frosted glass sits in. Kept
 * faint so a wash can never overpower on-surface content: a cool lagoon mint,
 * a warm coral, and a deep plum.
 */
val AmbientGlowLavenderLight = Color(0x335EE0B0) // mint haze
val AmbientGlowMagentaLight = Color(0x33FF9E80)  // coral haze
val AmbientGlowSkyLight = Color(0x2E7C5CBF)      // plum haze
val AmbientGlowLavenderDark = Color(0x3D146B5E)  // deep teal haze
val AmbientGlowMagentaDark = Color(0x3DB7452A)   // deep coral haze
val AmbientGlowSkyDark = Color(0x3D3A1E4D)       // deep plum haze

// ---- Brand — the AURA lagoon -----------------------------------------------

/**
 * Coral — the primary action / selection colour and the app's signature light,
 * in its **light-theme** polarity (deep enough for white text on it).
 */
val LifeOSPrimary = Color(0xFFC2410C)

/** A deeper coral for pressed states and gradient depth. */
val LifeOSPrimaryDeep = Color(0xFF9A3208)

/** Mint — the calm partner and the "kept up / success" second accent. */
val LifeOSSecondary = Color(0xFF1E8E6E)

/** A very soft coral wash, for containers and selected surfaces. */
val LifeOSAccentLavender = Color(0xFFFFE7DC)

/** Amber — the warm third accent, and the app's "energetic" highlight. */
val LifeOSTertiary = Color(0xFFB8862B)

// The named gradient stops, for the identity gradient. A lagoon sunset:
// deep teal -> rust -> coral. Deliberately DEEP so white text and icons on any
// filled identity surface (the hero, the Capture disc, a primary button) stay
// at or above WCAG AA (4.5:1) — brightening them would silently break that.
val PrismIndigo = Color(0xFF0F6E5F) // deep teal
val PrismSky = Color(0xFFA84B2E)    // deep rust
val PrismCoral = Color(0xFFC43D26)  // deep coral

/** The one identity gradient: teal -> rust -> coral. Hero, Capture and AI orb. */
val PrismSpectrum = listOf(PrismIndigo, PrismSky, PrismCoral)

// ---- Premium highlight -----------------------------------------------------

/**
 * Gold — reserved for premium moments only: streaks, personal records,
 * achievements, "best day" badges. Using it anywhere else would rob it of
 * meaning. [LifeOSGoldSoft] is its light-tinted fill on a surface.
 */
val LifeOSGold = Color(0xFFB8860B)
val LifeOSGoldBright = Color(0xFFE8B44A)
val LifeOSGoldSoft = Color(0x33E8B44A)

// ---- State -----------------------------------------------------------------

val LifeOSSuccess = Color(0xFF1E8E6E)
val LifeOSWarning = Color(0xFF9A6A16)
val LifeOSDanger = Color(0xFFB4322E)
val LifeOSInfo = Color(0xFF2F6E8E)

/** The neutral base every soft shadow is mixed from — cool, never a brand hue. */
val LifeOSShadow = Color(0xFF04100E)

/** A dimming scrim behind modal surfaces and the capture hub. */
val LifeOSScrim = Color(0x66101514)

// ===========================================================================
// Diary — the app's one deliberately non-glass module.
//
// A physical paper object: warm paper with a handwriting face. It renders on
// a fixed paper surface and never reads the frosted tokens, so it stays a
// warm counterpoint inside the cool lagoon rather than flipping with the
// theme. Stable, intentional.
// ===========================================================================

val DiaryMoodHappy = Color(0xFFF59E0B)      // goldenrod
val DiaryMoodCalm = Color(0xFF10B981)       // sage green
val DiaryMoodSad = Color(0xFF6366F1)        // dusty hydrangea
val DiaryMoodStressed = Color(0xFFF43F5E)   // soft terracotta
val DiaryMoodExcited = Color(0xFF7D5260)    // dried rose plum

/**
 * The Diary's *heading* ink: a warm near-black pen ink reserved for text and
 * the timeline spine. Deliberately never used for anything actionable.
 */
val DiaryInkViolet = Color(0xFF2E2A26)

/** Secondary ink for captions and supporting metadata — a soft sepia grey. */
val DiaryInkSoft = Color(0xFF6C6357)

/**
 * The Diary's actionable hue: the blue-black of a fountain-pen stroke. Every
 * control the user can press is painted in this ink, so "pressable" and
 * "read this" are told apart by hue alone.
 */
val DiaryActionViolet = Color(0xFF2E4A6E)

/** Tag-chip text, on the aged-paper chip fill. */
val DiaryTagInk = Color(0xFF4A4335)

// The paper itself.
val DiaryPaperBase = Color(0xFFEFE6D2)      // the desk / screen behind the pages
val DiaryPaperCard = Color(0xFFFCF7EA)      // one fresh page leaf
val DiaryLavender = Color(0xFFE6DAC0)       // aged-paper wash (chips, surfaces, selection)
val DiaryHairline = Color(0xFFD8CBB0)       // page border + timeline spine
val DiaryRuledLine = Color(0xFFD3C4A3)      // the faint horizontal rules on a page
val DiaryMarginLine = Color(0xFFC4796A)     // the red rule down the page's left margin
val DiaryRibbon = Color(0xFF9C3A3A)         // fabric bookmark
val DiarySaveDisabled = Color(0xFFEDE3CE)   // disabled Save pill
val DiaryPageShadow = Color(0x243B2F1E)     // soft drop shadow under a page

// Pastel mood-pill fills.
val DiaryMoodHappyPastel = Color(0xFFFEF3C7)
val DiaryMoodCalmPastel = Color(0xFFD1FAE5)
val DiaryMoodSadPastel = Color(0xFFE0E7FF)
val DiaryMoodStressedPastel = Color(0xFFFFE4E6)
val DiaryMoodExcitedPastel = Color(0xFFFCE7F3)

// Second mood wave (Daily Memory redesign). Purely additive.
val DiaryMoodAngry = Color(0xFFEF6B5E)       // soft coral
val DiaryMoodAnxious = Color(0xFFF2A65A)     // soft orange
val DiaryMoodTired = Color(0xFF8B93B8)       // muted lavender-blue
val DiaryMoodAngryPastel = Color(0xFFFFE3E0)
val DiaryMoodAnxiousPastel = Color(0xFFFDEBD7)
val DiaryMoodTiredPastel = Color(0xFFE7E9F5)

/**
 * Marker colour for an entry stored before moods existed (or with an
 * unrecognised custom value). Deliberately desaturated so an absent mood reads
 * as "no signal" rather than as a fifth feeling.
 */
val DiaryMoodNeutral = Color(0xFFB9AFC4)
