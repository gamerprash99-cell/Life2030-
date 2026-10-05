package com.lifeos.app.domain.model

import kotlin.math.roundToInt

/**
 * The monthly-budget numbers the Expenses screen shows.
 *
 * Pure arithmetic over the two values that already exist in the app — the
 * month's expense total (from [com.lifeos.app.data.db.dao.ExpenseDao]) and the
 * single budget value in
 * [com.lifeos.app.core.util.SettingsStore]. No screen-local copy of either, so
 * the bar cannot drift from the data: it is recomputed from the same flows the
 * month total is built from.
 *
 * [BudgetProgress.forMonth] returns **null** when no budget is set. That null is
 * the whole point: the screen shows a "create your first budget" state instead of
 * a bar drawn against a number the user never chose.
 */
data class BudgetProgress(
    val budget: Double,
    val spent: Double,
    /** Fill fraction in 0f..1f, clamped — over-budget spends still render a full bar. */
    val fraction: Float,
    /** Never negative; [isOverBudget] carries the overshoot instead. */
    val remaining: Double,
    val isOverBudget: Boolean,
    /** How far past the budget, or 0 when within it. */
    val overage: Double
) {
    companion object {
        /**
         * Money is compared to the cent, not to a floating-point epsilon chosen
         * for convenience: two numbers that are both "349" must never look like
         * "349.01" and tip the bar over. `HALF_CENT` is the tie point.
         */
        private const val HALF_CENT = 0.005

        fun forMonth(budget: Double?, spent: Double): BudgetProgress? {
            // A zero or negative budget is not a budget. Falling back to the
            // empty state is the honest rendering; dividing by it would be not.
            if (budget == null || budget <= 0.0) return null
            val spentClean = if (spent < 0.0) 0.0 else spent
            val remaining = budget - spentClean
            val overBudget = remaining < -HALF_CENT
            return BudgetProgress(
                budget = budget,
                spent = spentClean,
                fraction = ((spentClean / budget).coerceIn(0.0, 1.0)).toFloat(),
                remaining = if (overBudget) 0.0 else remaining.coerceAtLeast(0.0),
                isOverBudget = overBudget,
                overage = if (overBudget) -remaining else 0.0
            )
        }

        /**
         * Whole-percent used for the "x% of budget" line. Null rather than 0 when
         * there is no budget, so the screen never prints a 0% that means "you did
         * not set one".
         */
        fun percentOfBudget(progress: BudgetProgress?): Int? =
            progress?.let { ((it.spent / it.budget) * 100.0).roundToInt().coerceIn(0, 999) }
    }
}
