package com.lifeos.app.core.util

import com.lifeos.app.data.db.entities.RepeatRule
import java.time.LocalDate

/**
 * Pure date math for recurring tasks (no Android or storage dependencies,
 * which makes it directly unit-testable).
 *
 * On completion a repeating task spawns its next occurrence. The base date
 * is the current due date, or today when the task is already overdue, so
 * completing an old occurrence never back-fills the calendar with stale
 * entries.
 */
object RepeatRuleCalculator {

    /**
     * Returns the epoch day of the next occurrence, or null when [rule] is
     * [RepeatRule.NONE] or the custom-days list is empty/invalid.
     */
    fun nextOccurrence(
        rule: RepeatRule,
        customDaysCsv: String?,
        currentDueEpochDay: Long,
        todayEpochDay: Long
    ): Long? {
        if (rule == RepeatRule.NONE) return null
        val base = if (currentDueEpochDay >= todayEpochDay) currentDueEpochDay else todayEpochDay
        val baseDate = LocalDate.ofEpochDay(base)

        return when (rule) {
            RepeatRule.NONE -> null
            RepeatRule.DAILY -> base + 1
            RepeatRule.WEEKLY -> baseDate.plusWeeks(1).toEpochDay()
            RepeatRule.MONTHLY -> baseDate.plusMonths(1).toEpochDay()
            RepeatRule.CUSTOM_DAYS -> nextCustomDay(customDaysCsv, baseDate)
        }
    }

    private fun nextCustomDay(customDaysCsv: String?, baseDate: LocalDate): Long? {
        val days = customDaysCsv
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in 1..7 }
            ?.toSet()
            .orEmpty()
        if (days.isEmpty()) return null

        var candidate = baseDate.plusDays(1)
        while (true) {
            if (candidate.dayOfWeek.value in days) return candidate.toEpochDay()
            candidate = candidate.plusDays(1)
        }
    }
}