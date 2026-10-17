package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/*
 * The Diary module's own lavender-paper palette.
 *
 * The Diary (and the Timeline that reads it) is the one module that keeps its
 * own paper character — pages, ruled lines, a margin rule and a fabric ribbon —
 * so a memory reads like a page rather than a card. The paper itself now runs
 * in the app's Lavender Dream family: cool lavender-tinted pages and washes
 * instead of the old warm cream, so the Diary belongs to the same night as
 * every other screen.
 *
 * The app itself is single-voice Inter; `DiaryHandwriting` stays as an alias so
 * the diary's existing call sites keep compiling now that the bundled Kalam face
 * is gone.
 */

val DiaryInkViolet = Color(0xFF2A2440)
val DiaryInkSoft = Color(0xFF6B6280)
val DiaryActionViolet = Color(0xFF5B4A97)
val DiaryTagInk = Color(0xFF4A4260)
val DiaryPaperBase = Color(0xFFE8E1F2)      // the desk / screen behind the pages
val DiaryPaperCard = Color(0xFFF7F3FB)      // one fresh page leaf
val DiaryLavender = Color(0xFFE4DDF0)       // page wash (chips, surfaces, selection)
val DiaryHairline = Color(0xFFD4CBE4)       // page border + timeline spine
val DiaryRuledLine = Color(0xFFDCD3E8)      // the faint horizontal rules on a page
val DiaryMarginLine = Color(0xFFC08494)     // the muted rose rule down the left margin
val DiaryRibbon = Color(0xFF7A5C86)         // fabric bookmark
val DiarySaveDisabled = Color(0xFFE7E0F0)   // disabled Save pill
val DiaryPageShadow = Color(0x24241B36)     // soft drop shadow under a page

val DiaryHandwriting: FontFamily get() = InterFamily
