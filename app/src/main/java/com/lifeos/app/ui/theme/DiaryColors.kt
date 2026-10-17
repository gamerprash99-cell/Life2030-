package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/*
 * The Diary module's own paper palette.
 *
 * The Diary (and the Timeline that reads it) is the one module that deliberately
 * does not use the app's glass/canvas palette: it renders as paper — warm pages,
 * ruled lines, a red margin rule and a fabric ribbon — so a memory reads like a
 * page rather than a card. These tokens are that palette and are kept here, out
 * of `Color.kt`, so the app's palette and the paper palette cannot be confused.
 *
 * The app itself is single-voice Inter; `DiaryHandwriting` stays as an alias so
 * the diary's existing call sites keep compiling now that the bundled Kalam face
 * is gone.
 */

val DiaryInkViolet = Color(0xFF2E2A26)
val DiaryInkSoft = Color(0xFF6C6357)
val DiaryActionViolet = Color(0xFF2E4A6E)
val DiaryTagInk = Color(0xFF4A4335)
val DiaryPaperBase = Color(0xFFEFE6D2)      // the desk / screen behind the pages
val DiaryPaperCard = Color(0xFFFCF7EA)      // one fresh page leaf
val DiaryLavender = Color(0xFFE6DAC0)       // aged-paper wash (chips, surfaces, selection)
val DiaryHairline = Color(0xFFD8CBB0)       // page border + timeline spine
val DiaryRuledLine = Color(0xFFD3C4A3)      // the faint horizontal rules on a page
val DiaryMarginLine = Color(0xFFC4796A)     // the red rule down the page's left margin
val DiaryRibbon = Color(0xFF9C3A3A)         // fabric bookmark
val DiarySaveDisabled = Color(0xFFEDE3CE)   // disabled Save pill
val DiaryPageShadow = Color(0x243B2F1E)     // soft drop shadow under a page

val DiaryHandwriting: FontFamily get() = InterFamily
