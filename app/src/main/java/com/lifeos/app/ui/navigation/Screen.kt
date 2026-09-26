package com.lifeos.app.ui.navigation

/**
 * Route of the nested graph that owns the primary (bottom-navigation)
 * destinations. Kept here (not private to the NavHost) so the bottom bar can
 * tell "inside a tab" apart from "on an overlay stacked above the tabs" and
 * decide whether a tap should switch tabs or push the tab on top.
 */
internal const val ROOT_TABS_GRAPH = "root_tabs"

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Habits : Screen("habits")
    object HabitDetail : Screen("habits/{habitId}") {
        fun createRoute(habitId: String) = "habits/$habitId"
    }
    object Expenses : Screen("expenses")
    object Diary : Screen("diary")
    object DiaryDetail : Screen("diary/{entryId}") {
        fun createRoute(entryId: String) = "diary/$entryId"
    }

    /**
     * The full-screen composer. `entryId` is optional so the same destination
     * serves both "new memory" and "edit this memory" — the argument is only
     * supplied when editing, and defaults to an empty string (a new entry).
     */
    object DiaryEditor : Screen("diary/editor?entryId={entryId}") {
        const val ARG_ENTRY_ID = "entryId"
        fun createRoute(entryId: String? = null): String =
            if (entryId.isNullOrBlank()) "diary/editor" else "diary/editor?$ARG_ENTRY_ID=$entryId"
    }
    object Timeline : Screen("timeline")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object AppLockSetup : Screen("settings/app_lock")
}
