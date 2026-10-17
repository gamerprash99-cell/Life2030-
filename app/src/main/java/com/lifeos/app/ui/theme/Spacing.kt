package com.lifeos.app.ui.theme

import androidx.compose.ui.unit.dp

object LifeOSSpacing {
    val screenPadding = 24.dp
    val compactPadding = 16.dp

    /**
     * Vertical gap between blocks of content on a scroll surface. The redesign's
     * page rhythm is 18dp, so a stack of cards reads as a deliberate column
     * rather than an arbitrary pile.
     */
    val sectionSpacing = 18.dp

    /** Inner padding of a card / grouped panel — the redesign's 18dp card pad. */
    val cardPadding = 18.dp

    /** Gap between the rows inside one card. */
    val cardGap = 12.dp

    /**
     * The base 4dp rhythm scale. New layouts snap to these steps so vertical
     * rhythm is consistent across screens instead of every screen inventing its
     * own 10 / 14 / 18dp gaps. [screenPadding], [compactPadding] and
     * [sectionSpacing] above remain the named semantic steps used by existing
     * screens.
     */
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val huge = 64.dp

    // ---- Chrome geometry (redesign) ---------------------------------------
    //
    // The page header, the status strip and the gesture area are stated once
    // here and shared by every screen's chrome so the app's frame cannot drift
    // from screen to screen.

    /** The system status strip kept clear above the page (the design's 40dp). */
    val statusStripHeight = 40.dp
    /** The page-header band under the status strip (back / kicker+title / action). */
    val pageHeaderHeight = 86.dp
    /** The gesture area under the content (handle + breathing room). */
    val gestureAreaHeight = 22.dp
    /** The bottom navigation bar's own height. */
    val bottomNavHeight = 84.dp
    /** A 42x42 page-header action circle. */
    val headerActionSize = 42.dp

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

    /**
     * Where the Diary FAB's own bottom edge sits above the bottom of the list.
     *
     * Material's own margin for a FAB over content. The Diary list already sits
     * above the primary bottom bar (the Scaffold hands it that padding), so this
     * only has to clear the last line of the list — the 96dp this once was left
     * the button floating halfway up the screen.
     */
    val diaryFabBottomOffset = 16.dp

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
     */
    val diaryDateStripMinTouch = 48.dp
}
