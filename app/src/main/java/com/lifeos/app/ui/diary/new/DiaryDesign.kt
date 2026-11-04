package com.lifeos.app.ui.diary.new

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * LifeOS Diary Redesign — design system.
 *
 * A completely new Diary visual experience, soft pink/pastel canvas, editorial
 * date typography, minimal writing surface, pale borders, restrained shadows,
 * rounded panels, prominent pink Save action, compact floating/anchored toolbar.
 *
 * These tokens are scoped to Diary only — they do not re-theme the rest of the
 * app. The rest of LifeOS keeps its Lumen/warm palette.
 */

// ---- Canvas & surfaces -----------------------------------------------------

/** Soft pastel pink canvas — the Diary's own background, scoped to Diary. */
val DiaryNewCanvas = Color(0xFFFDF0F5)

/** Warm near-white paper card for the writing surface. */
val DiaryNewPaper = Color(0xFFFFFDF8)

/** Raised panel surface for palettes and sheets. */
val DiaryNewPanel = Color(0xFFFFFAF0)

/** Muted surface for secondary containers. */
val DiaryNewSurface = Color(0xFFF5E8EA)

/** Aged-paper lavender wash for chips and selection surfaces. */
val DiaryNewLavender = Color(0xFFE6DAC0)

/** Muted ink for secondary text. */
val DiaryNewInkMuted = Color(0xFFC9A9B3)

// ---- Content (text & icons) ------------------------------------------------

/** Deep ink for primary text — readable on pastel canvas. */
val DiaryNewInk = Color(0xFF3A2E35)

/** Secondary ink for captions and metadata. */
val DiaryNewInkSoft = Color(0xFF8A757E)

// ---- Brand / action --------------------------------------------------------

/** Prominent pink Save action — the Diary's primary action colour. */
val DiaryNewSave = Color(0xFFE91E63)

/** Save pressed state. */
val DiaryNewSavePressed = Color(0xFFC2185B)

/** Save disabled state. */
val DiaryNewSaveDisabled = Color(0xFFE57373)

// ---- Borders & dividers ----------------------------------------------------

/** Pale border — never a cold grey rule. */
val DiaryNewBorder = Color(0x1A3A2E35)

/** Hairline divider. */
val DiaryNewHairline = Color(0xFFEAD3D8)

// ---- Mood pastels ----------------------------------------------------------

/** Pastel mood ring colours (never colour-only meaning — always paired with emoji). */
val DiaryNewMoodHappy = Color(0xFFFCE4EC)
val DiaryNewMoodCalm = Color(0xFFF1F8E9)
val DiaryNewMoodSad = Color(0xFFF3E5F5)
val DiaryNewMoodStressed = Color(0xFFFFEBEE)
val DiaryNewMoodExcited = Color(0xFFFCE4EC)
val DiaryNewMoodNeutral = Color(0xFFEFEBEE)

// ---- Typography ------------------------------------------------------------

/** Editorial/handwritten display size for dates. */
val DiaryNewDateTextSize = 20.sp

/** Body text size for the writing surface. */
val DiaryNewBodyTextSize = 16.sp

/** Label size for metadata and captions. */
val DiaryNewLabelTextSize = 12.sp

/** Title size for entry titles. */
val DiaryNewTitleTextSize = 18.sp

// ---- Spacing ---------------------------------------------------------------

/** Screen padding. */
val DiaryNewScreenPadding = 16.dp

/** Compact padding for dense sections. */
val DiaryNewCompactPadding = 8.dp

/** Section spacing. */
val DiaryNewSectionSpacing = 12.dp

/** Minimum touch target. */
val DiaryNewMinTouchTarget = 48.dp

/** Toolbar button size. */
val DiaryNewToolbarButtonSize = 56.dp

/** Palette item size. */
val DiaryNewPaletteItemSize = 80.dp

// ---- Animation -------------------------------------------------------------

/** Standard fade duration. */
val DiaryNewFadeDuration = 220L
