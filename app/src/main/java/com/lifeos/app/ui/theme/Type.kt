package com.lifeos.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lifeos.app.R

/**
 * Lumen's display face: **Fraunces**, bundled in `res/font` so the app stays
 * offline-first with no runtime font download. A warm, slightly old-style serif
 * that carries real letterform character at heading sizes — it is what makes the
 * app read as a well-made object rather than a dashboard. It carries the display
 * and headline steps only; everything the user reads at length or must act on
 * stays in the UI sans below.
 *
 * Instanced from the upstream variable font at `opsz=48`, `SOFT=0`, `WONK=0`
 * (see `design.md` §4) — a display optical size with the clean, non-wonky
 * letterforms — so the files are static TTFs and need no runtime variable-axis
 * support.
 */
val FrauncesFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold)
)

/**
 * Lumen's UI face: **Manrope**, bundled in `res/font` so the app stays
 * offline-first with no runtime font download. A quiet, slightly geometric sans
 * with open counters: legible at 12sp, neutral enough to sit under a serif
 * without competing, and warm enough not to read as a system default. It carries
 * every title, body and label.
 */
val ManropeFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold)
)

/**
 * Legacy alias kept so any call site written against the v4 name still compiles
 * and silently adopts the new UI face. Prefer [ManropeFamily] in new code; the
 * Poppins files are no longer bundled and this will be removed in the Phase 6
 * dead-code sweep.
 */
@Deprecated("Lumen uses ManropeFamily", ReplaceWith("ManropeFamily"))
val PoppinsFamily = ManropeFamily

/**
 * The diary's own hand: a real handwriting typeface (Kalam) bundled in
 * `res/font`, so the app stays offline-first with no runtime font download.
 *
 * Used only by the diary module for the things a person actually writes — a
 * memory's title and body, the day's date, the empty-page invitation — while
 * controls and small labels keep the app's legible default sans.
 */
val DiaryHandwriting = FontFamily(
    Font(R.font.kalam_light, FontWeight.Light),
    Font(R.font.kalam_regular, FontWeight.Normal),
    Font(R.font.kalam_bold, FontWeight.Bold)
)

/**
 * Lumen hierarchy: Fraunces for display and headlines, Manrope for titles, body
 * and labels.
 *
 * Serif headings want a little more size and line-height than a geometric sans
 * did, so display steps carry negative tracking to bind into units while body
 * copy relaxes to a ~1.6x line-height for comfortable long-form reading in
 * Notes and the editor. Labels keep a touch of positive tracking so small caps
 * read as deliberate eyebrows rather than as cramped text, and every style
 * survives 200% font scaling without collision.
 */
val LifeOSTypography = Typography(
    displayLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-1.0).sp),
    displayMedium = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 33.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 29.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)
