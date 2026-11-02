package com.lifeos.app.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType

/**
 * Switches to a primary tab, exactly as tapping it in the bottom bar does.
 *
 * One function so an in-screen link ("see all activity" on Home, the snap
 * confirm) and the bar itself cannot drift: both pop the tab graph back to its
 * start destination with the state saved, restore the target tab's saved state,
 * and never leave two copies of a tab on the back stack. Pushing a tab route
 * with a plain `navigate` instead is what makes Back walk through tabs.
 */
internal fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

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

    /** Full create/edit form; `wizard=true` opens the guided first-habit flow. */
    object HabitEditor : Screen("habits/edit?habitId={habitId}&wizard={wizard}") {
        fun createRoute(habitId: String? = null, wizard: Boolean = false) =
            "habits/edit?habitId=${habitId.orEmpty()}&wizard=$wizard"
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

    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object AppLockSetup : Screen("settings/app_lock")

    // ---- Redesign additions -------------------------------------------------

    /**
     * Life — a primary tab and the app's single chronological memory timeline.
     *
     * The retired "timeline" route used to be a separate day-view destination;
     * the timeline now lives here, so there is exactly one timeline screen and
     * one aggregation path (see ui/life/LifeTimelineScreen.kt).
     */
    object Life : Screen("life")

    /**
     * The full-window voice recorder, opened from the Capture hub's Audio tile.
     * It is a flow, not a destination of its own: it owns the window while
     * recording and writes a real AUDIO capture on save.
     */
    object AudioCapture : Screen("capture/audio")

    /** The full-window player for one saved voice capture. */
    object AudioPlayer : Screen("audio/{captureId}") {
        fun createRoute(captureId: String) = "audio/$captureId"
    }

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

/**
 * The destination a Home/Life timeline row must open.
 *
 * Home used to send every activity through `SnapView`, so tapping a diary,
 * task, habit or expense row opened the Snap viewer with an id that could not
 * exist and rendered an empty/missing capture. Mapping from the item's real
 * type keeps each row opening the module that produced it:
 *
 *  - photo/video moments open the media viewer (the viewer plays video),
 *  - a voice capture opens its own player,
 *  - text moments (thought/mood/place) open the viewer's card,
 *  - the four track modules open the module that owns the record.
 */
internal fun routeForTimelineItem(item: TimelineItem): String = when (item.type) {
    TimelineItemType.DIARY -> Screen.DiaryDetail.createRoute(item.sourceId)
    TimelineItemType.TASK_COMPLETED -> Screen.Tasks.route
    TimelineItemType.HABIT_COMPLETED -> Screen.Habits.route
    TimelineItemType.EXPENSE -> Screen.Expenses.route
    TimelineItemType.AUDIO -> Screen.AudioPlayer.createRoute(item.sourceId)
    TimelineItemType.PHOTO, TimelineItemType.VIDEO, TimelineItemType.SNAP ->
        Screen.SnapView.createRoute(item.sourceId)
}
