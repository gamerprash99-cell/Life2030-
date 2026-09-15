package com.lifeos.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spacing/sizing tokens used across screens to keep padding,
 * touch targets, and FAB clearance consistent app-wide.
 *
 * Added during the UI/UX polish pass — purely additive (no existing
 * component was changed to create this file). Screens opt into using
 * these constants; nothing is forced.
 */
object LifeOSSpacing {
    /** Standard horizontal/top screen edge padding used by list screens. */
    val screenPadding = 16.dp

    /** Space between cards/rows in a vertical list. */
    val cardSpacing = 12.dp

    /** Space between distinct sections on a screen (e.g. "Today" vs "Habits"). */
    val sectionSpacing = 20.dp

    /**
     * Bottom content padding for scrollable lists that sit behind a
     * standard (56dp) FloatingActionButton, so the last item never sits
     * underneath it. FAB height + its default Scaffold margin + a little
     * breathing room.
     */
    val fabContentClearance = 96.dp

    /**
     * Bottom content padding for scrollable lists that sit behind an
     * ExtendedFloatingActionButton (taller, has a text label) — used on
     * Home, where the Capture button is an ExtendedFloatingActionButton.
     */
    val extendedFabContentClearance = 112.dp

    /** Minimum recommended touch target size (Android accessibility guidance). */
    val minTouchTarget = 48.dp
}
