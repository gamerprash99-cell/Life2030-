package com.lifeos.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetProgressTest {

    @Test
    fun `no budget is a null reading rather than a zero`() {
        // Zero is a real budget — spend nothing, deliberately. Conflating the two
        // would show a first-run user a progress bar for a budget they never set.
        assertNull(BudgetProgress.forMonth(spent = 0.0, budget = null))
    }

    @Test
    fun `spent, remaining and progress come from one budget`() {
        val progress = BudgetProgress.forMonth(spent = 4_000.0, budget = 15_000.0)!!

        assertEquals(15_000.0, progress.budget, TOLERANCE)
        assertEquals(4_000.0, progress.spent, TOLERANCE)
        assertEquals(11_000.0, progress.remaining, TOLERANCE)
        assertEquals((4_000.0 / 15_000.0).toFloat(), progress.fraction, 0.0001f)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun `reaching the budget exactly is not over budget`() {
        val progress = BudgetProgress.forMonth(spent = 15_000.0, budget = 15_000.0)!!

        assertFalse(progress.isOverBudget)
        assertEquals(0.0, progress.remaining, TOLERANCE)
        assertEquals(1.0f, progress.fraction, 0.0001f)
    }

    @Test
    fun `exceeding the budget reports an overage instead of negative money`() {
        val progress = BudgetProgress.forMonth(spent = 18_500.0, budget = 15_000.0)!!

        assertTrue(progress.isOverBudget)
        // Remaining never goes negative: the UI has one "left to spend" reading,
        // and an over-budget state shown as "-₹3,500 left" reads like a bug.
        assertEquals(0.0, progress.remaining, TOLERANCE)
        assertEquals(3_500.0, progress.overage, TOLERANCE)
    }

    @Test
    fun `progress is clamped so the bar cannot overflow`() {
        val progress = BudgetProgress.forMonth(spent = 30_000.0, budget = 15_000.0)!!

        assertEquals(1.0f, progress.fraction, 0.0001f)
    }

    @Test
    fun `a zero budget is unset rather than a division by zero`() {
        // Zero would render as a permanently full bar. Reporting it as "no budget
        // yet" is both safer and more honest.
        assertNull(BudgetProgress.forMonth(spent = 500.0, budget = 0.0))
    }

    @Test
    fun `a negative budget is treated as unset`() {
        assertNull(BudgetProgress.forMonth(spent = 100.0, budget = -1.0))
    }

    private companion object {
        const val TOLERANCE = 0.0001
    }
}