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

// The named gradient stops for the identity gradient, retuned to the redesign's
// teal/mint family (the old lagoon sunset is gone — mint is now the one
// signature). Deliberately kept DEEP so white text and icons on any filled
// identity surface stay at or above WCAG AA (4.5:1); the bright `FigmaAccent`
// mint is only ever used as a flat fill with dark `FigmaOnAccent` ink.
val PrismIndigo = Color(0xFF0F6E5F) // deep teal
val PrismSky = Color(0xFF2E8C77)    // mid teal-mint
val PrismCoral = Color(0xFF15564A)  // deep mint-teal

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

// ===========================================================================
// LifeOS 2030 — the Figma redesign palette.
//
// The 15 redesigned frames are a single dark, calm "night glass" system: a
// deep teal-to-navy canvas behind translucent slate cards, lit by one mint
// signature (#A8E8D3) with slate/steel support tones. Every value here is
// lifted verbatim from the Figma file's global variables (see the frame dump),
// so the Compose theme and the design share one source of truth.
//
// These tokens are additive: the historical AURA names above are untouched, so
// every existing call site keeps compiling while the theme below adopts these
// values for the redesign.
// ===========================================================================

// ---- Canvas gradients ------------------------------------------------------

/** The app canvas: `linear-gradient(140deg, #203D48 0%, #0B141C 38%, #101B27 100%)`. */
val FigmaCanvasTop = Color(0xFF203D48)
val FigmaCanvasMid = Color(0xFF0B141C)
val FigmaCanvasBottom = Color(0xFF101B27)

/** The welcome frame's `linear-gradient(135deg, #24454D 0%, #101D29 50%, #0B141C 100%)`. */
val FigmaWelcomeTop = Color(0xFF24454D)
val FigmaWelcomeMid = Color(0xFF101D29)
val FigmaWelcomeBottom = Color(0xFF0B141C)

// ---- Content (text & icons) ------------------------------------------------

/** Primary ink — `#F2F7F7` (fill_08897ac8). */
val FigmaText = Color(0xFFF2F7F7)
/** Secondary/supporting ink — `#A3B6BF` (fill_978a75fa). */
val FigmaMuted = Color(0xFFA3B6BF)
/** Tertiary / timestamps — `#7D959F` (fill_0c5c5f09). */
val FigmaTertiary = Color(0xFF7D959F)

// ---- Brand — the mint signature -------------------------------------------

/** The one accent — `#A8E8D3` (fill_a48f85b1). Primary actions, selection, kickers. */
val FigmaAccent = Color(0xFFA8E8D3)
/** Ink on a mint surface — `#0A2622` (fill_fd78fd65). */
val FigmaOnAccent = Color(0xFF0A2622)
/** A deep mint-tinted container — `#163D36` (fill_a3e883f4). */
val FigmaAccentDark = Color(0xFF163D36)
/** The active-tag hairline — `rgba(168, 232, 211, 0.3)`. */
val FigmaAccentStroke = Color(0x4DA8E8D3)

// ---- Surfaces --------------------------------------------------------------

/** The solid raised surface — `#162630` (fill_fd05e050). Cards, search, sheets. */
val FigmaSurface = Color(0xFF162630)
/** The deeper inset surface — `#1C303A` (fill_23b8e791). Secondary buttons, wells. */
val FigmaElevated = Color(0xFF1C303A)
/** The translucent card sheet — `rgba(31, 53, 64, 0.79)` (fill_8b5bd4d4). */
val FigmaCard = Color(0xC91F3540)
/** The card / hairline stroke — `rgba(182, 217, 229, 0.12)` (fill_c3f102cb). */
val FigmaStroke = Color(0x1FB6D9E5)
/** A faint white fill for chips and header-action discs — `rgba(255,255,255,0.03)`. */
val FigmaWash = Color(0x08FFFFFF)
/** A progress / meter track — `rgba(255,255,255,0.07)` (fill_4f6956c5). */
val FigmaTrack = Color(0x12FFFFFF)

// ---- Semantic accents ------------------------------------------------------

/** The warm "careful" tone — `#E9C997`. */
val FigmaWarning = Color(0xFFE9C997)
/** The habit heatmap fill — `#578E80`. */
val FigmaHeatmap = Color(0xFF578E80)
/** A softer heat tone — `#558D82`. */
val FigmaHeatSoft = Color(0xFF558D82)
/** The expense chart palette. */
val FigmaExpensePlum = Color(0xFFC6C0F1)
val FigmaExpenseSteel = Color(0xFF668396)
val FigmaExpenseBlue = Color(0xFF7898AF)
/** The diary paper gradients (the redesign's one light surface). */
val FigmaPaperTop = Color(0xFFF4E8DF)
val FigmaPaperBottom = Color(0xFFE6DFD9)
/** The Android gesture handle — `#DAE7EC`. */
val FigmaGestureHandle = Color(0xFFDAE7EC)
/** The bottom navigation bar fill — `rgba(16, 32, 42, 0.94)`. */
val FigmaBottomNav = Color(0xF010202A)
