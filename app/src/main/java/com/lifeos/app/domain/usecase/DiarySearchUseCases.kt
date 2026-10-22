package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository

/**
 * Search across a user's own journal.
 *
 * Wraps the repository rather than letting a screen call it directly, so the
 * two rules search has — blank is not "everything", and the result is capped —
 * hold for every caller, including the next one.
 *
 * There is no relevance ranking here. SQLite's `LIKE` reports whether a row
 * matched, not how well, so any ordering would be a number invented after the
 * fact. Newest-first is what someone reading their own journal actually wants.
 */
class SearchDiaryEntriesUseCase(private val diaryRepository: DiaryRepository) {

    suspend operator fun invoke(
        query: String,
        moodKey: String? = null,
        limit: Int = DEFAULT_LIMIT
    ): List<DiaryEntity> {
        if (query.isBlank()) return emptyList()
        return diaryRepository.search(query, moodKey).take(limit.coerceAtLeast(1))
    }

    /**
     * Hard ceiling on returned rows.
     *
     * Not a performance measure — the scan already happened — but a promise to
     * the user. A `LazyColumn` that quietly renders 4,000 hits is indistinguishable
     * from a frozen app, and the count is not what they were looking for anyway.
     */
    companion object {
        const val DEFAULT_LIMIT = 200
    }
}

/**
 * The days of one month that hold entries, for the calendar grid.
 *
 * Built from the month's own day bounds rather than by paging a year, so the
 * grid's cost does not grow with how long the journal is.
 */
class GetDiaryCalendarUseCase(private val diaryRepository: DiaryRepository) {

    suspend operator fun invoke(year: Int, month: Int): DiaryMonth {
        val first = java.time.YearMonth.of(year, month)
        val from = first.atDay(1).toEpochDay()
        val to = first.atEndOfMonth().toEpochDay()

        val entries = diaryRepository.getAllInRange(from, to)
        return DiaryMonth(
            year = year,
            month = month,
            entries = entries,
            // A day with three entries contributes one dot. The count of entries
            // is not encoded into the dot: a 4px vs 6px difference is not
            // readable at calendar density, and pretending it is would be a
            // chart that lies.
            daysWithEntries = entries.map { it.dateEpochDay }.toSet()
        )
    }
}

/** One month's worth of entries, plus the day bounds they came from. */
data class DiaryMonth(
    val year: Int,
    val month: Int,
    val entries: List<DiaryEntity>,
    val daysWithEntries: Set<Long>
) {
    val isEmpty: Boolean get() = entries.isEmpty()

    fun entryCountFor(epochDay: Long): Int = entries.count { it.dateEpochDay == epochDay }

    companion object {
        /**
         * The month [DateTimeUtils] says it is, so "this month" is never two
         * different definitions in two different files.
         */
        fun current(): DiaryMonth {
            val today = DateTimeUtils.today()
            return DiaryMonth(today.year, today.monthValue, emptyList(), emptySet())
        }
    }
}