package com.lifeos.app.ui.theme

import androidx.compose.ui.unit.dp

object LifeOSSpacing {
    val screenPadding = 24.dp
    val compactPadding = 16.dp
    val sectionSpacing = 20.dp

    /**
     * Trailing clearance a scrollable reserves for a `Scaffold`-hosted FAB.
     *
     * Correct where the Scaffold puts the button itself: ~16dp above the content
     * edge, so 96dp of clearance clears the button's top by ~24dp. Diary does
     * *not* use this — its button is drawn higher up by [diaryFabBottomOffset],
     * so it reserves [diaryFabOccupiedBottom] instead.
     */
    val fabContentClearance = 96.dp
    val minTouchTarget = 48.dp

    // ---- the Diary FAB ----------------------------------------------------
    //
    // The button, its offset and the band it occupies are stated together and
    // kept separate, because conflating the offset with the clearance is exactly
    // what let the Diary button cover the last line of a long memory: content
    // was given the button's *offset* as its trailing clearance, so the final
    // item could be scrolled up to the button's bottom edge but never above its
    // top edge.

    /**
     * Material 3's small FAB container size. Pinned onto the button by the
     * shared Diary FAB composable so the button and the clearance derived from
     * it cannot be sized independently. Material 3 1.3 does not expose this
     * constant publicly, so it has to live somewhere; here it is one number in
     * one file rather than one number per call site.
     */
    val diaryFabSize = 56.dp

    /** Where the Diary FAB's own bottom edge sits above the bottom of the list. */
    val diaryFabBottomOffset = 96.dp

    /** Breathing room kept between the top of the Diary FAB and the last line of content. */
    val diaryFabContentGap = 16.dp

    /**
     * The band at the bottom of a Diary list that the FAB occupies: its offset,
     * its own height, and a gap above it.
     *
     * Diary's scrollable content reserves this as trailing clearance, which is
     * what lets the final item of a long memory be scrolled fully clear of the
     * button. Derived rather than written out so it cannot drift from the
     * button's real geometry.
     */
    val diaryFabOccupiedBottom = diaryFabBottomOffset + diaryFabSize + diaryFabContentGap

    /**
     * The empty Diary day's illustration at its widest. It is a square, so it is
     * sized from the available width and capped here: a narrow phone or a split
     * window gets a proportionally smaller circle instead of a clipped one, while
     * a normal phone still gets exactly the artwork size the design calls for.
     */
    val diaryEmptyArtworkMaxSize = 300.dp

    /**
     * What share of the empty state's own height the artwork may claim.
     *
     * The artwork and the three lines of type under it have to fit the space the
     * FAB does *not* cover, and that space shrinks on a small phone and on a
     * short window. Reserving the artwork a share of the height left over after
     * the FAB's band — rather than leaving it at a fixed 300dp — is what keeps
     * the invitation centred and complete instead of pushed under the button.
     */
    val diaryEmptyArtworkFill = 0.62f

    /**
     * The floor for that share, so a very short window shrinks the artwork
     * rather than reducing it to a dot. Below this the surrounding text
     * outgrows the box and the empty state scrolls, which is reachable; a
     * two-dot artwork would just look broken.
     */
    val diaryEmptyArtworkMinSize = 120.dp

    /**
     * Trailing breathing room for a Diary scroll view that has no button drawn
     * over it — the memory detail screen. Kept as its own token so "no FAB here"
     * is expressed by its own name rather than by borrowing the Diary FAB's
     * geometry, which is what made the two roles drift apart in the first place.
     */
    val scrollBottomBreathingRoom = 96.dp

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
