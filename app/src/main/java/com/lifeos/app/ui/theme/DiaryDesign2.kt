package com.lifeos.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * LifeOS Diary Redesign — Design System v2
 *
 * Premium, minimal, emotionally engaging journal experience.
 * Soft pastel pink and lavender accents, clean white/tinted surfaces,
 * elegant typography, rounded cards, diary paper writing surfaces.
 *
 * Scoped to the Diary module; does not re-theme the rest of the app.
 */

/* ============================================================================
   Colors — pastel pink / lavender
   ============================================================================ */

val Diary2Canvas = Color(0xFFfdf0f5)      // very light pink paper base
val Diary2PaperCard = Color(0xFFfff5f8)   // near-white pink card
val Diary2SurfaceLavender = Color(0xFFF3edf7)  // soft lavender tint
val Diary2Hairline = Color(0xFFeacfd9)    // pale pink border
val Diary2InkViolet = Color(0xFF6a3fa0)   // violet-purple ink
val Diary2InkSoft = Color(0xFF8a7ba0)     // muted secondary ink
val Diary2Lavender = Color(0xFFF3edf7)    // soft lavender fill
val Diary2ActionViolet = Color(0xFFc084fc) // prominent pastel pink/purple action
val Diary2SaveDisabled = Color(0xFFe9d5f3) // disabled Save fill
val Diary2RuledLine = Color(0xFFfde8ee)   // very pale ruled line
val Diary2MarginLine = Color(0xFFf9ced7)  // soft pink margin line

val Diary2MoodHappyPastel = Color(0xFFFDECEA)
val Diary2MoodCalmPastel = Color(0xFFE4F2F4)
val Diary2MoodSadPastel = Color(0xFFEAE9FF)
val Diary2MoodStressedPastel = Color(0xFFFFEAEA)
val Diary2MoodExcitedPastel = Color(0xFFFDF0FF)

/* ============================================================================
   Typography
   ============================================================================ */

val Diary2BodyLineHeight = 30.sp

/* ============================================================================
   Spacing
   ============================================================================ */

val Diary2MinTouchTarget = 48.dp
val Diary2ScreenPadding = 20.dp
val Diary2CompactPadding = 12.dp
val Diary2SectionSpacing = 16.dp
val Diary2EditorWritingFill = 0.60f
val Diary2EditorTextMinHeight = 150.dp
val Diary2FabSize = 56.dp
val Diary2FabBottomOffset = 16.dp
val Diary2RuleSpacing = 30.dp
val Diary2HeaderVertical = 4.dp

/* ============================================================================
   Shapes
   ============================================================================ */

val Diary2RadiusSm = RoundedCornerShape(8.dp)
val Diary2RadiusMd = RoundedCornerShape(16.dp)
val Diary2RadiusLg = RoundedCornerShape(20.dp)
