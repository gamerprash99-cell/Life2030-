package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================================================
// LifeOS "Aurora Noir" — the premium design language (v3).
//
// A cinematic, jewel-toned system. The old neon violet→magenta gradient is
// replaced by a calmer pair (Iris indigo → Orchid), with two supporting jewel
// hues: an aurora mint for "alive / success" and a champagne gold reserved for
// premium moments (streaks, records, achievements). Depth is neutral and soft
// rather than brand-coloured, so surfaces read as expensive, not loud.
//
// Every legacy public name below is kept, so every existing call site keeps
// compiling while adopting the new look. New names are additive.
// ===========================================================================

// ---- Canvas & surfaces -----------------------------------------------------

/** The page behind everything: a faintly violet porcelain in light mode. */
val LifeOSBackgroundLight = Color(0xFFF5F3FA)
/** Obsidian ink — deep, cool, calm. */
val LifeOSBackgroundDark = Color(0xFF08070D)

val LifeOSSurfaceLight = Color(0xFFFFFFFF)
val LifeOSSurfaceDark = Color(0xFF141220)

/** A recessed surface for wells, insets and secondary panels. */
val LifeOSSurfaceVariantLight = Color(0xFFECE8F5)
val LifeOSSurfaceVariantDark = Color(0xFF201D2E)

/** A raised surface one step above [LifeOSSurfaceLight] (menus, sheets, popovers). */
val LifeOSSurfaceRaisedLight = Color(0xFFFFFFFF)
val LifeOSSurfaceRaisedDark = Color(0xFF1C1929)

// ---- Content (text & icons) ------------------------------------------------

val LifeOSTextPrimaryLight = Color(0xFF141118)
val LifeOSTextPrimaryDark = Color(0xFFF2F0FA)
val LifeOSTextSecondaryLight = Color(0xFF5A5470)
val LifeOSTextSecondaryDark = Color(0xFFA9A3C2)

// ---- Borders & outlines ----------------------------------------------------

val LifeOSBorderLight = Color(0x14000000)
val LifeOSBorderDark = Color(0x1FFFFFFF)
val LifeOSOutlineLight = Color(0xFF9A94AE)
val LifeOSOutlineDark = Color(0xFF6C6682)

// ---- Glass -----------------------------------------------------------------
//
// Quieter than before: a touch less opaque, a finer rim, a softer sheen, so the
// content on a card leads and the material never competes with it.

val GlassLight = Color(0xE0FFFFFF)
val GlassDark = Color(0xC41B1826)
val GlassBorderLight = Color(0x14000000)
val GlassBorderDark = Color(0x22FFFFFF)

// The dual-tone rim: light catches the top edge, shadow pools on the bottom.
val GlassTopEdgeLight = Color(0xB0FFFFFF)
val GlassTopEdgeDark = Color(0x5CFFFFFF)
val GlassBottomEdgeLight = Color(0x24000000)
val GlassBottomEdgeDark = Color(0x4A000000)

// The "liquid" sheen across the upper part of a sheet, and the soft base shadow.
val GlassSheenLight = Color(0x30FFFFFF)
val GlassSheenDark = Color(0x14FFFFFF)
val GlassInnerShadowLight = Color(0x12000000)
val GlassInnerShadowDark = Color(0x2A000000)

/**
 * Ambient glow palette — the soft colour clouds a frosted surface refracts.
 * Re-cut for Aurora Noir: iris, orchid and a whisper of aurora mint, kept
 * deliberately faint so a glow can never overpower on-surface content.
 */
val AmbientGlowLavenderLight = Color(0x5CAD9EFF) // iris haze
val AmbientGlowMagentaLight = Color(0x4DDEA6FF)  // orchid haze
val AmbientGlowSkyLight = Color(0x408FD3FF)      // cool daylight haze
val AmbientGlowLavenderDark = Color(0x4D3A2E86)  // iris haze (dark)
val AmbientGlowMagentaDark = Color(0x402E1E63)   // orchid haze (dark)
val AmbientGlowSkyDark = Color(0x38204A8A)       // cool haze (dark)

// ---- Brand -----------------------------------------------------------------

/** Iris — the primary action / selection colour. */
val LifeOSPrimary = Color(0xFF5A4BE8)

/** A deeper iris for pressed states and gradient depth. */
val LifeOSPrimaryDeep = Color(0xFF3D2FBE)

/** Orchid — the gradient partner. Calm, not hot pink. */
val LifeOSSecondary = Color(0xFF8B3DDB)

/** A very soft iris wash, for containers and selected surfaces. */
val LifeOSAccentLavender = Color(0xFFE7E2FF)

/** Aurora mint — the "alive" second accent; success reads here too. */
val LifeOSTertiary = Color(0xFF0F9E8E)

// ---- Premium highlight -----------------------------------------------------

/**
 * Champagne gold — reserved for premium moments only: streaks, personal
 * records, achievements, "best day" badges. Using it anywhere else would rob
 * it of meaning. [LifeOSGoldSoft] is its light-tinted fill on a surface.
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
// Diary "Tactile Editorial Journal" — mood palette + ink/paper tones.
//
// The diary is deliberately a physical object: it stays warm paper even when
// the rest of LifeOS is dark, exactly as a paper notebook would. These tokens
// are used only by the diary module (and the shared timeline that renders diary
// entries), so they are intentionally outside the Aurora system.
// ===========================================================================

val DiaryMoodHappy = Color(0xFFF59E0B)      // goldenrod
val DiaryMoodCalm = Color(0xFF10B981)       // sage green
val DiaryMoodSad = Color(0xFF6366F1)        // dusty hydrangea
val DiaryMoodStressed = Color(0xFFF43F5E)   // soft terracotta
val DiaryMoodExcited = Color(0xFF7D5260)    // dried rose plum

/**
 * The Diary's *heading* ink: a warm near-black pen ink reserved for text and
 * the timeline spine. It is deliberately never used for anything actionable: a
 * tappable control painted in a near-black ink reads as disabled. Actionable
 * controls use [DiaryActionViolet].
 */
val DiaryInkViolet = Color(0xFF2E2A26)

/** Secondary ink for captions and supporting metadata — a soft sepia grey. */
val DiaryInkSoft = Color(0xFF6C6357)

/**
 * The Diary's actionable hue: the blue-black of a fountain-pen stroke. Every
 * control the user can press — the FAB, the "Write" action, the selected-mood
 * ring, the cursor — is painted in this ink, so "pressable" and "read this"
 * are told apart by hue alone.
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

// Pastel mood-pill fills (Stitch "Tactile Editorial Journal" mood palette).
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
