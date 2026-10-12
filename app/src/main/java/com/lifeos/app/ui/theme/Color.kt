package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================================================
// LifeOS "PRISM GLASS" — the design language (v4, rebuilt from zero).
//
// Light refracted through glass. A dark-first, cool-neutral obsidian canvas
// carries frosted, translucent surfaces; a single iridescent spectrum
// (indigo -> sky -> coral) is the ONLY loud colour and is reserved for identity
// moments: the hero, the Capture action and the AI orb. Everything else is
// calm neutral ink on glass, so content leads and colour means something.
//
// Every public token name from the previous system is kept, so every existing
// call site keeps compiling while it silently adopts the new look. New
// PRISM-named tokens are additive.
// ===========================================================================

// ---- Canvas & surfaces -----------------------------------------------------

/** The page behind everything: a faintly cool porcelain in light mode. */
val LifeOSBackgroundLight = Color(0xFFF5F5FA)
/** Obsidian void — the infinite dark canvas the glass floats over. */
val LifeOSBackgroundDark = Color(0xFF08080C)

val LifeOSSurfaceLight = Color(0xFFFFFFFF)
val LifeOSSurfaceDark = Color(0xFF121218)

/** A recessed surface for wells, insets and secondary panels. */
val LifeOSSurfaceVariantLight = Color(0xFFECECF3)
val LifeOSSurfaceVariantDark = Color(0xFF1B1B24)

/** A raised surface one step above [LifeOSSurfaceLight] (menus, sheets, popovers). */
val LifeOSSurfaceRaisedLight = Color(0xFFFFFFFF)
val LifeOSSurfaceRaisedDark = Color(0xFF1E1E28)

// ---- Content (text & icons) ------------------------------------------------

val LifeOSTextPrimaryLight = Color(0xFF14141A)
val LifeOSTextPrimaryDark = Color(0xFFF4F4F8)
val LifeOSTextSecondaryLight = Color(0xFF5C5C6E)
val LifeOSTextSecondaryDark = Color(0xFFA6A6B8)

/** The deepest tertiary slate — structural labels, dividers and disabled hints. */
val LifeOSTextMutedLight = Color(0xFF8A8A9C)
val LifeOSTextMutedDark = Color(0xFF6E6E80)

// ---- Borders & outlines ----------------------------------------------------

val LifeOSBorderLight = Color(0x14000000)
val LifeOSBorderDark = Color(0x1FFFFFFF)
val LifeOSOutlineLight = Color(0xFF9A9AAE)
val LifeOSOutlineDark = Color(0xFF4A4A5A)

// ---- Glass -----------------------------------------------------------------
//
// The frosted sheet itself. Deeply translucent so the aurora behind it shows
// through, with a bright top rim and a deep bottom rim so a surface reads as a
// real lens rather than a flat translucent rectangle.

val GlassLight = Color(0xE8FFFFFF)
val GlassDark = Color(0xCC14141C)
val GlassBorderLight = Color(0x14000000)
val GlassBorderDark = Color(0x1FFFFFFF)

// The dual-tone rim: light catches the top edge, shadow pools on the bottom.
val GlassTopEdgeLight = Color(0xB3FFFFFF)
val GlassTopEdgeDark = Color(0x66FFFFFF)
val GlassBottomEdgeLight = Color(0x1A000000)
val GlassBottomEdgeDark = Color(0x40000000)

// The "liquid" sheen across the upper part of a sheet, and the soft base shadow.
val GlassSheenLight = Color(0x33FFFFFF)
val GlassSheenDark = Color(0x1AFFFFFF)
val GlassInnerShadowLight = Color(0x14000000)
val GlassInnerShadowDark = Color(0x33000000)

/**
 * Ambient glow palette — the soft colour clouds a frosted surface refracts.
 * Prism Glass keeps them faint and cool (indigo, violet, sky) so a glow can
 * never overpower on-surface content.
 */
val AmbientGlowLavenderLight = Color(0x4D8B7CFF) // indigo haze
val AmbientGlowMagentaLight = Color(0x3DFF9AA5)  // coral haze
val AmbientGlowSkyLight = Color(0x3D4CC9F0)      // sky haze
val AmbientGlowLavenderDark = Color(0x4D4A3AC0)  // indigo haze (dark)
val AmbientGlowMagentaDark = Color(0x3D4A2E7A)   // violet haze (dark)
val AmbientGlowSkyDark = Color(0x38204A8A)       // sky haze (dark)

// ---- Brand — the Prism spectrum --------------------------------------------

/** Iris indigo — the primary action / selection colour. The spectrum's first stop. */
val LifeOSPrimary = Color(0xFF5B49E6)

/** A deeper iris for pressed states and gradient depth. */
val LifeOSPrimaryDeep = Color(0xFF3B2CC4)

/** Sky — the gradient partner and the "informational" second accent. */
val LifeOSSecondary = Color(0xFF2F9BE0)

/** A very soft iris wash, for containers and selected surfaces. */
val LifeOSAccentLavender = Color(0xFFE9E5FF)

/** Coral — the warm third accent, and the app's "energetic" highlight. */
val LifeOSTertiary = Color(0xFFE0576B)

// The named spectrum stops, for the identity gradient. Kept separate from the
// role tokens above so the gradient can stay vivid in both themes.
val PrismIndigo = Color(0xFF7C6BFF)
val PrismSky = Color(0xFF4CC9F0)
val PrismCoral = Color(0xFFFF7A85)

/** The one identity gradient. Reserved for the hero, Capture and the AI orb. */
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

val LifeOSSuccess = Color(0xFF15803D)
val LifeOSWarning = Color(0xFFB45309)
val LifeOSDanger = Color(0xFFC81E3A)
val LifeOSInfo = Color(0xFF2563EB)

/** The neutral base every soft shadow is mixed from — never a brand hue. */
val LifeOSShadow = Color(0xFF1A1626)

/** A dimming scrim behind modal surfaces and the capture hub. */
val LifeOSScrim = Color(0x66000000)

// ===========================================================================
// Diary — the app's one deliberately non-glass module.
//
// A physical paper object: it stays warm paper even when the rest of LifeOS is
// obsidian, exactly as a paper notebook would. These tokens are used only by
// the diary module and the shared timeline that renders diary entries.
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
