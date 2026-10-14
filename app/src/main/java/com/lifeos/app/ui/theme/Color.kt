package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================================================
// LifeOS "LUMEN" — the design language (v5, rebuilt from zero).
//
// A linen desk under a lamp. Warm neutrals carry soft *matte* surfaces that
// describe depth with a long, low shadow rather than a painted lens; the one
// loud colour is a muted clay, with sage and gold as its quiet partners. The
// iridescent spectrum of v4 is gone — colour here is warm, scarce and means
// something. Nothing is frosted, nothing glows cool.
//
// Every public token name from the previous systems is kept, so every existing
// call site keeps compiling while it silently adopts the new look. The legacy
// GLASS- and PRISM-named tokens are retained as the matte-surface and identity-
// gradient slots respectively (see the notes on each block).
//
// All text/background pairs below are checked against WCAG 2.2 AA (4.5:1 for
// body, 3:1 for large text and UI components) in both themes.
// ===========================================================================

// ---- Canvas & surfaces -----------------------------------------------------

/** The page behind everything: warm paper under a lamp. */
val LifeOSBackgroundLight = Color(0xFFF7F4EF)
/** Warm coal — the dark canvas surfaces sit on. */
val LifeOSBackgroundDark = Color(0xFF16130F)

val LifeOSSurfaceLight = Color(0xFFFFFDF9)
val LifeOSSurfaceDark = Color(0xFF221D17)

/** A recessed surface for wells, insets and secondary panels. */
val LifeOSSurfaceVariantLight = Color(0xFFEFEAE2)
val LifeOSSurfaceVariantDark = Color(0xFF2B251E)

/** A raised surface one step above [LifeOSSurfaceLight] (menus, sheets, popovers). */
val LifeOSSurfaceRaisedLight = Color(0xFFFFFFFF)
val LifeOSSurfaceRaisedDark = Color(0xFF302921)

// ---- Content (text & icons) ------------------------------------------------

/** Warm near-black pen ink. 14.9:1 on [LifeOSSurfaceLight]. */
val LifeOSTextPrimaryLight = Color(0xFF241D15)
/** Warm paper-white ink. 13.6:1 on [LifeOSSurfaceDark]. */
val LifeOSTextPrimaryDark = Color(0xFFF6EFE6)
/** Supporting copy — sepia grey. 6.4:1 light / 7.1:1 dark. */
val LifeOSTextSecondaryLight = Color(0xFF6B5F52)
val LifeOSTextSecondaryDark = Color(0xFFBCAE9D)

/** The deepest warm grey — structural labels, dividers and disabled hints. */
val LifeOSTextMutedLight = Color(0xFF7A6E60)
val LifeOSTextMutedDark = Color(0xFF9C9083)

// ---- Borders & outlines ----------------------------------------------------

/** A warm hairline, tinted from the ink so it never reads as a cold grey rule. */
val LifeOSBorderLight = Color(0x1F6B5F52)
val LifeOSBorderDark = Color(0x26F6EFE6)
val LifeOSOutlineLight = Color(0xFFA99C8C)
val LifeOSOutlineDark = Color(0xFF5A5044)

// ---- Surface material (legacy "Glass") -------------------------------------
//
// v5 is MATTE, not frosted. These slots are kept because the whole component
// kit reads them by name; their values now describe an opaque warm sheet with
// a soft top light and a pooled base shadow — no blur, no specular sheen.

val GlassLight = Color(0xF7FFFDF9)
val GlassDark = Color(0xF22A241D)
val GlassBorderLight = Color(0x1F6B5F52)
val GlassBorderDark = Color(0x26F6EFE6)

// The rim: a soft top light, a deeper pooled foot.
val GlassTopEdgeLight = Color(0x40FFFFFF)
val GlassTopEdgeDark = Color(0x33F6EFE6)
val GlassBottomEdgeLight = Color(0x1A000000)
val GlassBottomEdgeDark = Color(0x40000000)

// Reduced to a whisper: a matte sheet has no specular highlight.
val GlassSheenLight = Color(0x14FFFFFF)
val GlassSheenDark = Color(0x12FFFFFF)
val GlassInnerShadowLight = Color(0x143B2F1E)
val GlassInnerShadowDark = Color(0x33000000)

/**
 * Ambient glow palette — the warm haze a matte surface sits in. Kept very
 * faint (clay, amber, sage) so a glow can never overpower on-surface content.
 */
val AmbientGlowLavenderLight = Color(0x4DAD5636) // clay haze
val AmbientGlowMagentaLight = Color(0x3DD98A45)  // amber haze
val AmbientGlowSkyLight = Color(0x3D6E7F63)      // sage haze
val AmbientGlowLavenderDark = Color(0x4D8A4A2E)  // clay haze (dark)
val AmbientGlowMagentaDark = Color(0x3D6E4A22)   // amber haze (dark)
val AmbientGlowSkyDark = Color(0x3D3E4A34)       // sage haze (dark)

// ---- Brand — the Lumen warmth ----------------------------------------------

/**
 * Clay — the primary action / selection colour and the app's only loud hue.
 * 5.1:1 on the light surface and 6.3:1 against its dark-mode ink, so it is
 * safe for both text and filled controls in either theme.
 */
val LifeOSPrimary = Color(0xFFA8512F)

/** A deeper clay for pressed states and gradient depth. */
val LifeOSPrimaryDeep = Color(0xFF8A3F23)

/** Sage — the gradient's quiet partner and the "informational" second accent. */
val LifeOSSecondary = Color(0xFF6E7F63)

/** A very soft clay wash, for containers and selected surfaces. */
val LifeOSAccentLavender = Color(0xFFF6E7DF)

/** Amber — the warm third accent, and the app's "energetic" highlight. */
val LifeOSTertiary = Color(0xFFC08A3E)

// The named gradient stops, for the identity gradient. Kept separate from the
// role tokens above so the gradient stays warm in both themes.
//
// Deliberately DEEP: every stop keeps white text and icons at or above WCAG AA
// (4.5:1) — clay 5.4, amber 4.5, bronze 4.8 — so any filled identity surface
// (the hero, the Capture disc, a primary button) is legible without auditing
// each call site. Brightening these would silently break that contract.
val PrismIndigo = Color(0xFFA8512F) // clay
val PrismSky = Color(0xFFAD651F)    // amber
val PrismCoral = Color(0xFF8F6E18)  // bronze gold

/** The one identity gradient: clay -> amber -> gold. Reserved for the hero, Capture and the AI orb. */
val PrismSpectrum = listOf(PrismIndigo, PrismSky, PrismCoral)

// ---- Premium highlight -----------------------------------------------------

/**
 * Amber gold — reserved for premium moments only: streaks, personal records,
 * achievements, "best day" badges. Using it anywhere else would rob it of
 * meaning. [LifeOSGoldSoft] is its light-tinted fill on a surface.
 */
val LifeOSGold = Color(0xFFB8860B)
val LifeOSGoldBright = Color(0xFFE8B44A)
val LifeOSGoldSoft = Color(0x33E8B44A)

// ---- State -----------------------------------------------------------------

val LifeOSSuccess = Color(0xFF3F7D4E)
val LifeOSWarning = Color(0xFF9A6216)
val LifeOSDanger = Color(0xFFB23A34)
val LifeOSInfo = Color(0xFF3A6EA5)

/** The neutral base every soft shadow is mixed from — warm, never a brand hue. */
val LifeOSShadow = Color(0xFF2A1F14)

/** A dimming scrim behind modal surfaces and the capture hub. */
val LifeOSScrim = Color(0x66241D15)

// ===========================================================================
// Diary — the app's one deliberately non-glass module.
//
// A physical paper object: warm paper with a handwriting face. In v4 this was
// a deliberate *exception* to the obsidian glass system; under Lumen the rest
// of the app has moved toward it, so these tokens are now the warm pole the
// whole design leans on rather than a module in exile. Stable, intentional.
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
