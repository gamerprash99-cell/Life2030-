package com.lifeos.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyUpdatePercentTest {

    @Test
    fun `no goals today is zero`() {
        assertEquals(0, computeDailyUpdatePercent(tasksToday = 0, tasksDoneToday = 0, habitsToday = 0, habitsDoneToday = 0))
    }

    @Test
    fun `everything completed is one hundred`() {
        assertEquals(100, computeDailyUpdatePercent(tasksToday = 2, tasksDoneToday = 2, habitsToday = 1, habitsDoneToday = 1))
    }

    @Test
    fun `tasks and habits count together`() {
        assertEquals(50, computeDailyUpdatePercent(tasksToday = 3, tasksDoneToday = 1, habitsToday = 1, habitsDoneToday = 1))
    }

    @Test
    fun `habits only drive the percentage`() {
        assertEquals(33, computeDailyUpdatePercent(tasksToday = 0, tasksDoneToday = 0, habitsToday = 3, habitsDoneToday = 1))
    }

    @Test
    fun `tasks only drive the percentage`() {
        assertEquals(25, computeDailyUpdatePercent(tasksToday = 4, tasksDoneToday = 1, habitsToday = 0, habitsDoneToday = 0))
    }

    @Test
    fun `no completed goals is zero regardless of count`() {
        assertEquals(0, computeDailyUpdatePercent(tasksToday = 5, tasksDoneToday = 0, habitsToday = 2, habitsDoneToday = 0))
    }

    @Test
    fun `never exceeds one hundred`() {
        assertEquals(100, computeDailyUpdatePercent(tasksToday = 1, tasksDoneToday = 1, habitsToday = 1, habitsDoneToday = 1).coerceIn(0, 100))
    }
}