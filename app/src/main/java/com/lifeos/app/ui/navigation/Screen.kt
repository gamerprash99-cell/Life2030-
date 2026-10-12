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

    /**
     * The Diary sub-screens.
     *
     * Declared before [DiaryDetail] on purpose. `diary/{entryId}` also matches
     * `diary/search`, and Nav Compose tries patterns in declaration order, so a
     * literal route registered after the argument route would be swallowed by it
     * — opening search would load an entry whose id happens to be "search".
     * Keeping the literals first makes that impossible.
     */
    object DiarySearch : Screen("diary/search")
    object DiaryCalendar : Screen("diary/calendar")
    object DiaryInsights : Screen("diary/insights")

    object DiaryDetail : Screen("diary/{entryId}") {
        fun createRoute(entryId: String) = "diary/$entryId"
    }

    object Timeline : Screen("timeline")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object AppLockSetup : Screen("settings/app_lock")

    // ---- Redesign additions -------------------------------------------------

    /** Smart Notes — the long-form thinking surface. A primary tab. */
    object Notes : Screen("notes")
    object NoteEditor : Screen("note/{noteId}") {
        fun createRoute(noteId: String) = "note/$noteId"
    }

    /** Life — the connected-history hub (timeline, calendar, search, modules). */
    object Life : Screen("life")

    /** The Capture hub — a full-window sheet opened from the centre action. */
    object Capture : Screen("capture")

    /** One search across every module. */
    object GlobalSearch : Screen("search")

    /** LifeOS AI — the on-device assistant over permitted data. */
    object AiAssistant : Screen("ai")

    /**
     * Snap — the in-app camera and post flow. Declared before [SnapView] so the
     * literal route is never swallowed by the argument route.
     */
    object Snap : Screen("snap")
    object SnapView : Screen("snap/view/{captureId}") {
        fun createRoute(captureId: String) = "snap/view/$captureId"
    }
}
