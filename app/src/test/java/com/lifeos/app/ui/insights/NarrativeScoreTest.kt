package com.lifeos.app.ui.insights

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The narrative-score row belonged to the previous Insights design and no
 * longer exists in the current offline, repository-backed Insights screen.
 * These tests cover the current state model instead of testing the removed
 * narrativeScore parser.
 */
class InsightsUiStateTest {

    @Test
    fun `default state starts loading with month selected`() {
        val state = InsightsUiState()

        assertEquals(true, state.loading)
        assertEquals(InsightsPeriod.MONTH, state.period)
    }

    @Test
    fun `default metrics are zero until local repository data is loaded`() {
        val state = InsightsUiState()

        assertEquals(0, state.tasksCompleted)
        assertEquals(0, state.habitCompletions)
        assertEquals(0, state.habitsTracked)
        assertEquals(0.0, state.spendTotal, 0.0)
        assertEquals(0.0, state.dailyAverage, 0.0)
        assertEquals(0, state.diaryEntries)
        assertEquals(0, state.captures)
    }

    @Test
    fun `period options expose clear labels`() {
        assertEquals("Week", InsightsPeriod.WEEK.label)
        assertEquals("Month", InsightsPeriod.MONTH.label)
        assertEquals("Year", InsightsPeriod.YEAR.label)
    }

    @Test
    fun `top category remains absent when no spend category exists`() {
        val state = InsightsUiState()

        assertEquals(null, state.topCategory)
        assertEquals(0.0, state.topCategoryAmount, 0.0)
    }
}
