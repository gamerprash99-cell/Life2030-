package com.lifeos.app.ui.navigation

import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Home's recent-activity rows used to all open the Snap viewer. These tests pin
 * the real per-type destination so a diary/task/habit/expense row can never be
 * routed into the capture viewer again.
 */
class TimelineNavigationTest {

    private fun item(type: TimelineItemType, sourceId: String) = TimelineItem(
        id = "timeline-$sourceId",
        type = type,
        title = "Title",
        dateEpochDay = 0L,
        timeMinutes = 0,
        icon = "•",
        sourceId = sourceId
    )

    @Test
    fun diaryOpensItsDetailScreen() {
        assertEquals(
            Screen.DiaryDetail.createRoute("entry-1"),
            routeForTimelineItem(item(TimelineItemType.DIARY, "entry-1"))
        )
    }

    @Test
    fun taskOpensTheTasksTab() {
        assertEquals(Screen.Tasks.route, routeForTimelineItem(item(TimelineItemType.TASK_COMPLETED, "task-1")))
    }

    @Test
    fun habitOpensTheHabitsTab() {
        assertEquals(Screen.Habits.route, routeForTimelineItem(item(TimelineItemType.HABIT_COMPLETED, "habit-1")))
    }

    @Test
    fun expenseOpensTheExpensesScreen() {
        assertEquals(Screen.Expenses.route, routeForTimelineItem(item(TimelineItemType.EXPENSE, "expense-1")))
    }

    @Test
    fun snapOpensItsViewer() {
        assertEquals(
            Screen.SnapView.createRoute("capture-1"),
            routeForTimelineItem(item(TimelineItemType.SNAP, "capture-1"))
        )
    }
}
