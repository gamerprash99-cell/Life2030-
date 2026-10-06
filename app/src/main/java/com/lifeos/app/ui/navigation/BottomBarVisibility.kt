package com.lifeos.app.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Routes that are **not** part of the primary tab experience and must not carry
 * the bottom navigation bar.
 *
 * App Lock setup is a security/settings flow, not a tab. Drawing the primary
 * Home/Tasks/Habits bar on it gave the flow a second, competing navigation
 * control and stole the height the PIN keypad needs — on a small phone with
 * three-button navigation the bottom row of keys was pushed under the system
 * navigation area.
 *
 * Stated as a set of routes rather than a special case at the call site so the
 * rule is one testable fact instead of a boolean buried in the Scaffold.
 */
private val ROUTES_WITHOUT_BOTTOM_BAR = setOf(
    Screen.AppLockSetup.route
)

/**
 * Whether the primary bottom navigation should be composed for [route].
 *
 * `route == null` means "no destination resolved yet", which is treated as
 * *not* showing the bar: on the first frame the bar must never appear over an
 * unknown destination and then vanish.
 *
 * [composerOwnsWindow] covers the Diary composer, which is the app's other
 * full-window surface (it consumes the bottom inset itself — see
 * [ComposerWindowOwner]).
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun shouldShowBottomBar(route: String?, composerOwnsWindow: Boolean): Boolean {
    if (composerOwnsWindow) return false
    if (route == null) return false
    return route !in ROUTES_WITHOUT_BOTTOM_BAR
}

/** Exposed for tests: the routes excluded from the bottom navigation. */
internal fun routesWithoutBottomBar(): Set<String> = ROUTES_WITHOUT_BOTTOM_BAR
