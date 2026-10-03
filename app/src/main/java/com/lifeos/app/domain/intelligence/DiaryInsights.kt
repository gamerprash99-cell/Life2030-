package com.lifeos.app.domain.intelligence

import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.model.MoodValence
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A mood and how many entries carry it.
 *
 * [share] is a fraction of mood-tagged entries, not of all entries: an entry
 * with no mood is absent from the question entirely rather than counted as a
 * ninth "no mood" bucket.
 */
data class MoodShare(
    val moodKey: String,
    val label: String,
    val emoji: String,
    val count: Int,
    val share: Float
)

/** One day on the mood trend chart. [valence] is -1 (negative) to +1 (positive). */
data class MoodTrendPoint(
    val epochDay: Long,
    val valence: Float,
    val entryCount: Int
)

/**
 * Positive-vs-negative balance, and how it moved.
 *
 * [change] is `recent - earlier`. `null` when there is not enough data on both
 * sides to say anything — see [MoodAnalyzer.MIN_ENTRIES_FOR_TREND]. A trend
 * arrow drawn from two entries is worse than no arrow, because it looks like
 * information.
 */
data class MoodTrend(
    val recentValence: Float?,
    val earlierValence: Float?,
    val change: Float?
) {
    val isRising: Boolean get() = (change ?: 0f) > 0.05f
    val isFalling: Boolean get() = (change ?: 0f) < -0.05f

    companion object {
        val UNKNOWN = MoodTrend(null, null, null)
    }
}

/**
 * A run of consecutive days that each hold at least one entry.
 *
 * [currentLength] counts back from today and is what the UI celebrates. It
 * deliberately tolerates today being empty: if someone has written every day
 * for a week but has not yet written *today*, their streak is still a week and
 * should not read as zero. [isAlive] distinguishes the two cases.
 */
data class WritingStreak(
    val currentLength: Int,
    val longestLength: Int,
    val isAlive: Boolean
)

/** Mood facts about a journal. */
data class MoodAnalysis(
    /** Per-mood counts, descending. Empty when no entry carries a mood. */
    val distribution: List<MoodShare>,
    /** Share of mood-tagged entries that are POSITIVE / NEGATIVE / NEUTRAL. */
    val positiveShare: Float,
    val negativeShare: Float,
    val neutralShare: Float,
    /** The single most common mood, or `null` when there are none or no clear winner. */
    val dominantMoodKey: String?,
    /** Dominant mood per day, ascending, for the calendar's day dots. */
    val dominantMoodByDay: Map<Long, String>,
    val trend: MoodTrend,
    /** The last [MoodAnalyzer.TREND_WINDOW_DAYS] days, ascending, including empty days. */
    val recentPoints: List<MoodTrendPoint>,
    /** Entries that carry a mood, and among those the ones whose mood resolved. */
    val taggedEntryCount: Int,
    val resolvedEntryCount: Int
) {
    val hasData: Boolean get() = taggedEntryCount > 0

    companion object {
        val EMPTY = MoodAnalysis(
            distribution = emptyList(), positiveShare = 0f, negativeShare = 0f, neutralShare = 0f,
            dominantMoodKey = null, dominantMoodByDay = emptyMap(), trend = MoodTrend.UNKNOWN,
            recentPoints = emptyList(), taggedEntryCount = 0, resolvedEntryCount = 0
        )
    }
}

/**
 * Volume facts about a journal.
 *
 * Every average is `null` below its own minimum sample. The rule is the same
 * everywhere in this layer: an honest blank beats a confident number derived
 * from two rows.
 */
data class WritingStatistics(
    val entryCount: Int,
    val wordCount: Int,
    val activeDayCount: Int,
    val averageWordsPerEntry: Int?,
    val averageEntriesPerActiveDay: Float?,
    val favoriteCount: Int,
    val attachmentCount: Int,
    /** Entries per day-of-week, Monday-first, all seven days always present. */
    val entriesByWeekday: List<DayOfWeekCount>,
    /** Entries per four-hour band across the day, 0..5. */
    val entriesByTimeBand: List<TimeBandCount>,
    val firstEntryEpochDay: Long?,
    val latestEntryEpochDay: Long?
) {
    companion object {
        val EMPTY = WritingStatistics(
            entryCount = 0, wordCount = 0, activeDayCount = 0, averageWordsPerEntry = null,
            averageEntriesPerActiveDay = null, favoriteCount = 0, attachmentCount = 0,
            entriesByWeekday = emptyList(), entriesByTimeBand = emptyList(),
            firstEntryEpochDay = null, latestEntryEpochDay = null
        )
    }
}

data class DayOfWeekCount(val dayOfWeek: DayOfWeek, val count: Int)

data class TimeBandCount(val band: Int, val count: Int)

/** A word that showed up on more than one day. */
data class RecurringKeyword(
    val word: String,
    val occurrences: Int,
    val distinctDays: Int
)

/** A period of the week or day that the user writes in noticeably more. */
data class WritingPattern(
    val kind: Kind,
    val label: String,
    val count: Int,
    val share: Float
) {
    enum class Kind { WEEKDAY, TIME_OF_DAY }
}

/** Patterns found across entries. All fields are empty rather than wrong when unsupported. */
data class PatternAnalysis(
    val recurringKeywords: List<RecurringKeyword>,
    val topTags: List<RecurringKeyword>,
    val patterns: List<WritingPattern>
) {
    val isEmpty: Boolean get() = recurringKeywords.isEmpty() && topTags.isEmpty() && patterns.isEmpty()

    companion object {
        val EMPTY = PatternAnalysis(emptyList(), emptyList(), emptyList())
    }
}

/**
 * Everything [DiaryAnalyzer] produced, in one bundle.
 *
 * The screen consumes this as a unit so it renders one consistent set of
 * figures. It is a value object with no behaviour: it exists so that the
 * analyzers stay independent of each other and of the UI, and so a screen can
 * never be handed half-computed results.
 */
data class DiaryInsights(
    val statistics: WritingStatistics,
    val mood: MoodAnalysis,
    val patterns: PatternAnalysis,
    val question: DiaryQuestion
) {
    /**
     * True when there is too little to say anything honest about. The Insights
     * screen uses this to show its onboarding state instead of a wall of zeros.
     */
    val isSparse: Boolean get() = statistics.entryCount < DiaryAnalyzer.MIN_ENTRIES_FOR_INSIGHTS

    companion object {
        /**
         * The bundle for a journal with no entries.
         *
         * Routes through [LocalQuestionEngine] rather than straight to the
         * fallback, so an empty journal gets the same onboarding question a
         * directly-analysed empty journal would. Calling [DiaryQuestion.forDay]
         * here instead silently produced a *present-tense* prompt — "what is one
         * thing you remember about today?" — for someone who has never written
         * anything, which is the wrong first thing to say to a new user.
         */
        fun empty(todayEpochDay: Long): DiaryInsights = DiaryInsights(
            statistics = WritingStatistics.EMPTY,
            mood = MoodAnalysis.EMPTY,
            patterns = PatternAnalysis.EMPTY,
            question = LocalQuestionEngine.questionFor(DiarySnapshot.EMPTY.copy(todayEpochDay = todayEpochDay))
        )
    }
}

/**
 * One reflective prompt.
 *
 * Generated locally from a fixed table by day-of-year, so the same date always
 * yields the same question and nothing leaves the device. [isForToday] is false
 * for the backlog questions that ask about the past.
 */
data class DiaryQuestion(
    val text: String,
    val kind: Kind
) {
    enum class Kind {
        /** About the present moment. */
        PRESENT,

        /** About a specific past entry the user already wrote. */
        PAST_ENTRY,

        /** About a recurring theme across entries. */
        THEME,

        /** Shown before there is enough to reflect on. */
        ONBOARDING
    }

    val isForToday: Boolean get() = kind == Kind.PRESENT

    companion object {
        /** Stable fallback, so a screen always has something to render. */
        fun forDay(todayEpochDay: Long): DiaryQuestion =
            DiaryQuestion("What is one thing you would like to remember about today?", Kind.PRESENT)
    }
}