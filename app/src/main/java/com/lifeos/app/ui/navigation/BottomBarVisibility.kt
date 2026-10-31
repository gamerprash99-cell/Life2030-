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
 * Snap is a full-screen camera. A live viewfinder with a navigation pill across
 * the bottom both looks wrong and covers the shutter controls; it must own the
 * whole window like the Diary composer does.
 *
 * Capture is the full-window Capture hub opened from the centre action — it is
 * a flow, not a tab, so the tab bar would be a second, competing navigation
 * control on it. The Note editor (a focused writing surface) and the AI
 * assistant (a full-height transcript with its own composer) likewise own the
 * window.
 *
 * Stated as a set of routes rather than a special case at the call site so the
 * rule is one testable fact instead of a boolean buried in the Scaffold.
 *
 * The Habit editor (guided wizard / full form) is a focused creation flow. It
 * carries its own Back/Continue bar, so the primary tab bar underneath would be
 * a second competing navigation control and would push the wizard's own bar up
 * by a full tab-bar height — the oversized blank blocker reported on the first
 * wizard step.
 */
private val ROUTES_WITHOUT_BOTTOM_BAR = setOf(
    Screen.AppLockSetup.route,
    Screen.Snap.route,
    Screen.Capture.route,
    Screen.NoteEditor.route,
    Screen.HabitEditor.route,
    Screen.GlobalSearch.route,
    Screen.AiAssistant.route
)

/**
 * Routes that consume the bottom system inset themselves.
 *
 * The root Scaffold normally reserves the navigation-bar inset (or the bottom
 * bar height) for every destination. These routes paint their own bottom bar
 * or controls with `navigationBarsPadding()`, so the root must hand them the
 * full window height and add no bottom padding of its own — otherwise the
 * inset is applied twice and an empty strip appears between the screen's own
 * bar and the system navigation area.
 */
private val ROUTES_OWNING_BOTTOM_INSET = setOf(
    Screen.HabitEditor.route,
    Screen.NoteEditor.route,
    Screen.AiAssistant.route,
    Screen.Capture.route
)

/**
 * Routes that consume the top (status bar) inset themselves.
 *
 * Same rule on the other edge: these screens apply `statusBarsPadding()` to
 * their own header/controls, so a root top inset on top of that would push
 * their first row down by the status-bar height twice.
 */
private val ROUTES_OWNING_TOP_INSET = setOf(
    Screen.NoteEditor.route,
    Screen.AiAssistant.route,
    Screen.Capture.route
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

/**
 * Whether [route] is responsible for the bottom window inset itself.
 *
 * [composerOwnsWindow] covers the Diary composer, which is the app's other
 * full-window surface (see [ComposerWindowOwner]).
 */
internal fun ownsBottomInset(route: String?, composerOwnsWindow: Boolean): Boolean {
    if (composerOwnsWindow) return true
    if (route == null) return false
    return route in ROUTES_OWNING_BOTTOM_INSET
}

/**
 * Whether [route] is responsible for the top status-bar inset itself.
 *
 * The Diary composer deliberately keeps the top edge with the root Scaffold
 * (see `DiaryEditor`), so it is not part of this set.
 */
internal fun ownsTopInset(route: String?): Boolean =
    route != null && route in ROUTES_OWNING_TOP_INSET

/** Exposed for tests: the routes excluded from the bottom navigation. */
internal fun routesWithoutBottomBar(): Set<String> = ROUTES_WITHOUT_BOTTOM_BAR
