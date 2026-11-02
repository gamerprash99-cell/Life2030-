package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================================================
// LifeOS "CLEAN UTILITY + WARM MINIMAL JOURNAL" — the design language (v6).
//
// A lavender-paper desk with a deep-purple pen. The canvas is a near-white
// lavender (#FDF8FF), grouped content sits on soft lavender sheets
// (#F3EDF7), and the one loud colour is deep purple (#21005D) — used for
// selection, primary actions and the identity gradient. Warm terracotta, muted
// gold and soft sage stay exactly where they were: as the *semantic* accents
// (clay for money and focus, gold for streaks and records, sage for success),
// so the journal keeps its warmth while the structure reads clean and calm.
//
// Surfaces remain matte — depth comes from a long, low shadow, never a painted
// lens or a backdrop blur.
//
// Every public token name from the previous systems is kept, so every existing
// call site keeps compiling while it adopts the new palette. The legacy GLASS-
// and PRISM-named tokens are retained as the matte-surface and identity-
// gradient slots respectively (see the notes on each block).
//
// All text/background pairs below are checked against WCAG 2.2 AA (4.5:1 for
// body, 3:1 for large text and UI components) in both themes. The identity
// gradient's stops are all deep enough to keep white text at or above 4.5:1
// (deep purple 13.9, plum 6.2, clay 5.4).
// ===========================================================================

// ---- Canvas & surfaces -----------------------------------------------------

/** The page behind everything: near-white lavender paper. */
val LifeOSBackgroundLight = Color(0xFFFDF8FF)
/** Deep plum-black — the canvas the dark surfaces sit on. */
val LifeOSBackgroundDark = Color(0xFF141218)

/** Grouped content sits on white in light mode, raised off the lavender canvas. */
val LifeOSSurfaceLight = Color(0xFFFFFFFF)
val LifeOSSurfaceDark = Color(0xFF1E1B24)

/** The "soft surface": lavender sheets for wells, insets and secondary panels. */
val LifeOSSurfaceVariantLight = Color(0xFFF3EDF7)
val LifeOSSurfaceVariantDark = Color(0xFF2A2731)

/** A raised surface one step above [LifeOSSurfaceLight] (menus, sheets, popovers). */
val LifeOSSurfaceRaisedLight = Color(0xFFFFFFFF)
val LifeOSSurfaceRaisedDark = Color(0xFF322F3A)

// ---- Content (text & icons) ------------------------------------------------

/** Deep-neutral pen ink with a violet cast. 16.0:1 on [LifeOSSurfaceLight]. */
val LifeOSTextPrimaryLight = Color(0xFF1D1B20)
/** Lavender paper-white ink. 13.3:1 on [LifeOSSurfaceDark]. */
val LifeOSTextPrimaryDark = Color(0xFFE8E0EC)
/** Supporting copy — neutral grey-violet. 8.6:1 light / 8.3:1 dark. */
val LifeOSTextSecondaryLight = Color(0xFF49454F)
val LifeOSTextSecondaryDark = Color(0xFFC8C1CE)

/** The quietest grey — structural labels, dividers and disabled hints. */
val LifeOSTextMutedLight = Color(0xFF605A68)
val LifeOSTextMutedDark = Color(0xFFA49EAC)

// ---- Borders & outlines ----------------------------------------------------

/** A warm hairline, tinted from the ink so it never reads as a cold grey rule. */
val LifeOSBorderLight = Color(0x1F49454F)
val LifeOSBorderDark = Color(0x26E8E0EC)
val LifeOSOutlineLight = Color(0xFF7A757F)
val LifeOSOutlineDark = Color(0xFF6F6A78)

// ---- Surface material (legacy "Glass") -------------------------------------
//
// v5 is MATTE, not frosted. These slots are kept because the whole component
// kit reads them by name; their values now describe an opaque warm sheet with
// a soft top light and a pooled base shadow — no blur, no specular sheen.

val GlassLight = Color(0xF7FFFFFF)
val GlassDark = Color(0xF226222E)
val GlassBorderLight = Color(0x1F49454F)
val GlassBorderDark = Color(0x26E8E0EC)

// The rim: a soft top light, a deeper pooled foot.
val GlassTopEdgeLight = Color(0x40FFFFFF)
val GlassTopEdgeDark = Color(0x33E8E0EC)
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
val AmbientGlowLavenderLight = Color(0x4D7E57C2) // lavender haze
val AmbientGlowMagentaLight = Color(0x3DAD5636)  // clay haze
val AmbientGlowSkyLight = Color(0x3D6E7F63)      // sage haze
val AmbientGlowLavenderDark = Color(0x4D5B3FA0)  // lavender haze (dark)
val AmbientGlowMagentaDark = Color(0x3D8A4A2E)   // clay haze (dark)
val AmbientGlowSkyDark = Color(0x3D3E4A34)       // sage haze (dark)

// ---- Brand — deep purple ink, warm accents --------------------------------

/**
 * Deep purple — the primary action / selection colour and the app's one loud
 * hue. 13.9:1 on white, so filled controls, selected tabs and links are legible
 * without auditing each call site.
 */
val LifeOSPrimary = Color(0xFF21005D)

/** A deeper purple for pressed states and gradient depth. */
val LifeOSPrimaryDeep = Color(0xFF180042)

/** Sage — the gradient's quiet partner and the "informational" second accent. */
val LifeOSSecondary = Color(0xFF6E7F63)

/** The soft lavender container the brief calls for (#EADDFF), for chips and selected surfaces. */
val LifeOSAccentLavender = Color(0xFFEADDFF)

/** Amber — the warm third accent, and the app's "energetic" highlight. */
val LifeOSTertiary = Color(0xFFC08A3E)

// The named gradient stops, for the identity gradient. Kept separate from the
// role tokens above so every filled identity surface stays legible in both
// themes.
//
// Deliberately DEEP: every stop keeps white text and icons at or above WCAG AA
// (4.5:1) — deep purple 13.9, plum 6.2, clay 5.4 — so the hero, the Capture
// disc and a primary button never need a per-call-site contrast audit.
// Brightening a stop would silently break that contract.
val PrismIndigo = Color(0xFF2B0A6B) // deep purple
val PrismSky = Color(0xFF6A3FA0)    // plum
val PrismCoral = Color(0xFFA8512F)  // clay (the warm end of the brand)

/** The one identity gradient: deep purple -> plum -> clay. Reserved for the hero, Capture and the AI orb. */
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
