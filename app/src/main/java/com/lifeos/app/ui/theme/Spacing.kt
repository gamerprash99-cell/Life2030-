package com.lifeos.app.ui.theme

import androidx.compose.ui.unit.dp

object LifeOSSpacing {
    val screenPadding = 24.dp
    val compactPadding = 16.dp
    val sectionSpacing = 20.dp
    val fabContentClearance = 96.dp
    val minTouchTarget = 48.dp

    // Diary-specific rhythm tokens keep the reference spacing tunable in one place.
    val diaryHeaderVertical = 4.dp
    val diaryEditorSection = 20.dp
    val diaryEditorTextMinHeight = 132.dp

    /**
     * The composer's writing surface fills at least this share of the height the
     * card actually has, so an empty editor reads as a page to write on instead of
     * a small field floating in a large empty card. It is a *share of the
     * available height*, never a fixed height, so it shrinks with the keyboard and
     * grows on a tall screen; a long memory still grows past it and scrolls.
     */
    val diaryEditorWritingFill = 0.55f

    /**
     * Minimum touch height of each half of the composer's date/time strip. It is
     * what sets the strip's whole height, so raising it raises the strip.
     *
     * This was 38dp — chosen in Phase 1 only to keep the strip as short as the
     * 26dp-content row it replaced. Phase 5 raised it to the app's own
     * [minTouchTarget], because these two halves are the *only* way to change a
     * memory's date or time, and by this file's own standard a 38dp target is
     * the same defect Phase 3 fixed on the add-tag control: a control the user
     * can see and want to press, sized below the minimum.
     *
     * Cost: the strip is 10dp taller, so the writing surface gives up 10dp of
     * its resting height. That is affordable because the surface is a
     * `weight(1f)` share of the card and floors itself at 132dp, so it absorbs
     * the loss without moving the card, the footer hairline or the pinned
     * actions — and the date half already wraps to two lines at large font
     * scales, where this floor was being exceeded anyway.
     */
    val diaryDateStripMinTouch = 48.dp
}
