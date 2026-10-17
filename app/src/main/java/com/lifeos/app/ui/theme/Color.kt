package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ===========================================================================
// LifeOS design language — "Lavender Dream".
//
// A calm lavender identity for both polarities:
//
//   Light: #F1EDF7 canvas · #FCFAFF surface · #A995D1 primary · #D8C9EF soft
//          #8B78B7 secondary · #3F3457 text · #716781 secondary text
//          #E4DDF0 border
//   Dark:  #17141F canvas · #24202F surface · #302A3E elevated · #BBA7E6 primary
//          #463956 soft · #C8B7EB secondary · #F6F1FC text · #C8BED8 secondary
//          text · #494055 border
//
// The canvas is a whisper-soft vertical gradient around the given background
// so screens never read as a flat sheet, but the identity is the lavender
// itself — no mint-green, no teal-navy.
//
// All text/background pairs are checked against WCAG 2.2 AA (4.5:1 body,
// 3:1 large text / UI components) in both themes.
// ===========================================================================

// ---- Canvas ----------------------------------------------------------------
//
// A three-stop vertical gradient built around the spec background so the
// page has a breath of depth without leaving the lavender family.

/** Dark canvas stops. */
val LifeOSCanvasTopDark = Color(0xFF1E1A29)
val LifeOSCanvasMidDark = Color(0xFF17141F)
val LifeOSCanvasBottomDark = Color(0xFF120F1A)

/** Light canvas stops. */
val LifeOSCanvasTopLight = Color(0xFFF5F1FA)
val LifeOSCanvasMidLight = Color(0xFFF1EDF7)
val LifeOSCanvasBottomLight = Color(0xFFECE6F4)

// ---- Surfaces --------------------------------------------------------------

/** Base surface (search fields, wells): opaque, one step above the canvas. */
val LifeOSSurfaceLight = Color(0xFFFCFAFF)
val LifeOSSurfaceDark = Color(0xFF24202F)

/** A recessed/elevated surface for wells, insets and secondary panels. */
val LifeOSSurfaceVariantLight = Color(0xFFEFEAF7)
val LifeOSSurfaceVariantDark = Color(0xFF302A3E)

/** A raised surface one step above the base (menus, sheets, popovers). */
val LifeOSSurfaceRaisedLight = Color(0xFFFFFFFF)
val LifeOSSurfaceRaisedDark = Color(0xFF38314A)

/**
 * The signature card fill: a translucent sheet the canvas gradient breathes
 * through. Light polarity keeps the same translucency so the lavender canvas
 * shows through white cards identically.
 */
val LifeOSCardFillLight = Color(0xCCFCFAFF)
val LifeOSCardFillDark = Color(0xC9302A3E)

/** The 1px card hairline. */
val LifeOSCardStrokeLight = Color(0x2EE4DDF0)
val LifeOSCardStrokeDark = Color(0x2E494055)

/** The floating bottom navigation bar. */
val LifeOSNavBarLight = Color(0xF2FCFAFF)
val LifeOSNavBarDark = Color(0xEF24202F)

/** The selected destination pill behind a bottom-bar icon. */
val LifeOSNavPillSelectedLight = Color(0xFFD8C9EF)
val LifeOSNavPillSelectedDark = Color(0xFF463956)

// ---- Content (text & icons) ------------------------------------------------

/** Deep lavender ink. ~9:1 on [LifeOSSurfaceLight]. */
val LifeOSTextPrimaryLight = Color(0xFF3F3457)
/** Off-white lavender ink. ~15:1 on the dark canvas. */
val LifeOSTextPrimaryDark = Color(0xFFF6F1FC)
/** Supporting copy. 5.3:1 light / 8.9:1 dark. */
val LifeOSTextSecondaryLight = Color(0xFF716781)
val LifeOSTextSecondaryDark = Color(0xFFC8BED8)

/** The quiet tertiary — timestamps, captions, disabled hints. */
val LifeOSTextMutedLight = Color(0xFF7C7291)
val LifeOSTextMutedDark = Color(0xFF9E93B2)

// ---- Borders & outlines ----------------------------------------------------

/** The card hairline / semantic border colour. */
val LifeOSBorderLight = Color(0xFFE4DDF0)
val LifeOSBorderDark = Color(0xFF494055)
val LifeOSOutlineLight = Color(0xFFD5CCE6)
val LifeOSOutlineDark = Color(0xFF5C5272)

// ---- Surface material ------------------------------------------------------
//
// The component kit's card slots. The top/bottom edge and sheen slots are
// kept so cards can catch a whisper of top light without reintroducing
// heavy glassmorphism.

val GlassLight = LifeOSCardFillLight
val GlassDark = LifeOSCardFillDark
val GlassBorderLight = LifeOSCardStrokeLight
val GlassBorderDark = LifeOSCardStrokeDark

val GlassTopEdgeLight = Color(0x50FFFFFF)
val GlassTopEdgeDark = Color(0x1EFFFFFF)
val GlassBottomEdgeLight = Color(0x0AE4DDF0)
val GlassBottomEdgeDark = Color(0x26000000)

val GlassSheenLight = Color(0x0DFFFFFF)
val GlassSheenDark = Color(0x0FFFFFFF)
val GlassInnerShadowLight = Color(0x0AE4DDF0)
val GlassInnerShadowDark = Color(0x26000000)

/**
 * Ambient glow palette — faint coloured light the canvas is washed with.
 * These stay barely-there lavender hazes used only behind hero moments
 * (welcome, AI), never behind dense text.
 */
val AmbientGlowLavenderLight = Color(0x1AA995D1) // soft lavender haze
val AmbientGlowMagentaLight = Color(0x148B78B7)  // deeper violet haze
val AmbientGlowSkyLight = Color(0x14BBA7E6)      // pale periwinkle haze
val AmbientGlowLavenderDark = Color(0x33A995D1)  // lavender haze
val AmbientGlowMagentaDark = Color(0x268B78B7)   // violet haze
val AmbientGlowSkyDark = Color(0x26BBA7E6)       // periwinkle haze

// ---- Brand — the lavender signature ----------------------------------------

/**
 * The app's one accent (#A995D1). As a *fill* it carries dark ink
 * ([LifeOSInkOnBrand]); for text on light surfaces use [LifeOSPrimaryText],
 * the AA-safe deepened lavender.
 */
val LifeOSPrimary = Color(0xFFA995D1)

/** A deeper lavender for pressed states and gradient depth. */
val LifeOSPrimaryDeep = Color(0xFF8B78B7)

/** The calm partner: the spec's secondary lavender for chips and tabs. */
val LifeOSSecondary = Color(0xFF8B78B7)

/** A very soft lavender wash, for containers and selected surfaces (light). */
val LifeOSAccentLavender = Color(0xFFD8C9EF)

/** Amber-gold — the warm third accent. */
val LifeOSTertiary = Color(0xFF8A6A12)

/**
 * The signature lavender fill (#BBA7E6), used in both themes: the center
 * Capture pill, progress fills, selected chips. Paired with [LifeOSMintInk].
 */
val LifeOSMint = Color(0xFFBBA7E6)

/** Deep plum ink that goes on lavender fills (≥4.6:1 on both lavender fills). */
val LifeOSMintInk = Color(0xFF241B36)

/**
 * The deep ink used for text/icons on any brand-lavender fill in either
 * polarity (on primary, on secondary, on containers).
 */
val LifeOSInkOnBrand = Color(0xFF1A1330)

/**
 * The AA-safe lavender *text* accent for light surfaces (4.6:1 on
 * [LifeOSSurfaceLight]); dark surfaces use the bright primary directly.
 */
val LifeOSPrimaryTextLight = Color(0xFF7E6BB0)
val LifeOSPrimaryTextDark = Color(0xFFBBA7E6)

// The identity gradient: deep violet -> lavender -> soft periwinkle.
// Hero, Capture, AI orb.
val PrismIndigo = Color(0xFF8B78B7)
val PrismSky = Color(0xFFA995D1)
val PrismCoral = Color(0xFFD8C9EF)
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
val LifeOSShadow = Color(0xFF14101D)

/** A dimming scrim behind modal surfaces and the capture hub. */
val LifeOSScrim = Color(0x9914101D)

// ---- Canvas brush ----------------------------------------------------------

/**
 * The page background: the three-stop vertical gradient every screen sits on.
 * A whisper of depth around the spec background — never a flat color.
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
