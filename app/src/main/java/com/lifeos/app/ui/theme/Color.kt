package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color

val LifeOSBackgroundLight = Color(0xFFFDF8FF)
val LifeOSBackgroundDark = Color(0xFF100D16)
val LifeOSSurfaceLight = Color(0xFFFFFFFF)
val LifeOSSurfaceDark = Color(0xFF1B1720)

val GlassLight = Color(0xE6FFFFFF)
val GlassDark = Color(0xCC2A2432)
val GlassBorderLight = Color(0x18000000)
val GlassBorderDark = Color(0x24FFFFFF)

val LifeOSPrimary = Color(0xFF7C4DFF)
val LifeOSSecondary = Color(0xFFD946EF)
val LifeOSAccentLavender = Color(0xFFEADDFF)

val LifeOSSuccess = Color(0xFF2E9D63)
val LifeOSWarning = Color(0xFFE39A28)
val LifeOSDanger = Color(0xFFD94A5B)

val LifeOSTextPrimaryLight = Color(0xFF21005D)
val LifeOSTextPrimaryDark = Color(0xFFF6EFFB)

// Diary redesign (Stitch "Tactile Editorial Journal" — mood palette + ink/paper tones)
val DiaryMoodHappy = Color(0xFFF59E0B)      // goldenrod
val DiaryMoodCalm = Color(0xFF10B981)       // sage green
val DiaryMoodSad = Color(0xFF6366F1)        // dusty hydrangea
val DiaryMoodStressed = Color(0xFFF43F5E)   // soft terracotta
val DiaryMoodExcited = Color(0xFF7D5260)    // dried rose plum

/**
 * The Diary's *heading* ink — dark, desaturated indigo reserved for text and
 * the timeline spine (Stitch "midnight indigo" #211A44). It is deliberately
 * never used for anything actionable: a tappable control painted in a
 * near-black heading colour reads as disabled. Actionable controls use
 * [DiaryActionViolet].
 */
val DiaryInkViolet = Color(0xFF211A44)

/**
 * The Diary's actionable hue (Stitch primary #6C47EB). Every control the user
 * can press — the composer FAB, the "+ Memory" action, the inline detail
 * actions, the selected-mood ring — is painted in this violet, so "pressable"
 * and "read this" are told apart by hue alone.
 */
val DiaryActionViolet = Color(0xFF6C47EB)

/** Stitch's tag-chip text, on the pale lavender chip fill. */
val DiaryTagInk = Color(0xFF493D7B)

val DiaryLavender = Color(0xFFEADDFF)       // pale thistle lavender
val DiaryPaperCard = Color(0xFFFFFCFF)      // pristine paper leaf
val DiaryHairline = Color(0xFFE8E1EA)       // hairline borders + timeline spine
val DiarySaveDisabled = Color(0xFFF4ECFF)   // disabled Save pill (Stitch)

// Pastel mood-pill fills (Stitch "Tactile Editorial Journal" mood palette).
// Each is the light tint behind its saturated accent; Happy/Sad/Stressed use
// the exact Stitch hexes, Calm/Excited are harmonized to LifeOS' own accents.
val DiaryMoodHappyPastel = Color(0xFFFEF3C7)
val DiaryMoodCalmPastel = Color(0xFFD1FAE5)
val DiaryMoodSadPastel = Color(0xFFE0E7FF)
val DiaryMoodStressedPastel = Color(0xFFFFE4E6)
val DiaryMoodExcitedPastel = Color(0xFFFCE7F3)

// Second mood wave (Daily Memory redesign). Purely additive: `mood` is a
// nullable TEXT column, so new keys need no Room migration and every existing
// entry keeps resolving against the five original accents above.
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
