package com.lifeos.app.domain.intelligence

import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.model.MoodValence

/**
 * Counts and compares moods.
 *
 * Reads only [DiarySnapshot], and is a pure function of it: the same snapshot
 * always yields the same [MoodAnalysis]. That is what makes the Insights screen
 * safe to recompute on every database emission without the figures flickering.
 *
 * What this class deliberately does **not** do is interpret. It reports how
 * many entries carry each mood, and whether the balance moved. It never
 * suggests a cause, a diagnosis, or a state of mind — "4 of your last 6
 * entries are negative" is a count, and the user can decide what it means.
 */
object MoodAnalyzer {

    /** Days shown on the trend chart, and each half of the trend window. */
    const val TREND_WINDOW_DAYS = 7

    /**
     * Minimum mood-tagged entries in *each* half of the trend window before a
     * direction is reported.
     *
     * With one entry in each half, "the trend" is a coin flip dressed up as a
     * finding. Below this the trend is [MoodTrend.UNKNOWN] and the UI shows no
     * arrow at all.
     */
    const val MIN_ENTRIES_FOR_TREND = 3

    /** POSITIVE as +1, NEGATIVE as -1, NEUTRAL as 0. */
    private fun scoreOf(valence: MoodValence): Float = when (valence) {
        MoodValence.POSITIVE -> 1f
        MoodValence.NEUTRAL -> 0f
        MoodValence.NEGATIVE -> -1f
    }

    fun analyze(snapshot: DiarySnapshot): MoodAnalysis {
        val tagged = snapshot.entries.filter { !it.moodKey.isNullOrBlank() }
        if (tagged.isEmpty()) return MoodAnalysis.EMPTY

        val byKey = LinkedHashMap<String, MutableList<DiarySnapshotEntry>>()
        for (entry in tagged) {
            val key = entry.moodKey!!
            byKey.getOrPut(key) { mutableListOf() }.add(entry)
        }

        val distribution = byKey.entries
            .map { (key, group) ->
                val mood = DiaryMoods.fromStored(key)
                MoodShare(
                    moodKey = key,
                    label = mood?.label ?: key,
                    emoji = mood?.emoji ?: "",
                    count = group.size,
                    share = group.size.toFloat() / tagged.size
                )
            }
            .sortedWith(compareByDescending<MoodShare> { it.count }.thenBy { it.label })

        val resolved = tagged.count { DiaryMoods.valenceOf(it.moodKey) != null }

        return MoodAnalysis(
            distribution = distribution,
            positiveShare = shareOf(tagged, MoodValence.POSITIVE),
            negativeShare = shareOf(tagged, MoodValence.NEGATIVE),
            neutralShare = shareOf(tagged, MoodValence.NEUTRAL),
            dominantMoodKey = dominantKey(distribution),
            dominantMoodByDay = dominantMoodByDay(tagged),
            trend = trend(snapshot),
            recentPoints = recentPoints(snapshot),
            taggedEntryCount = tagged.size,
            resolvedEntryCount = resolved
        )
    }

    /**
     * Share of mood-tagged entries at [valence].
     *
     * Unresolvable moods are excluded from both the numerator and the
     * denominator, so a single unrecognised row cannot dilute the figure. The
     * three shares therefore need not sum to 1, and that is correct.
     */
    private fun shareOf(tagged: List<DiarySnapshotEntry>, valence: MoodValence): Float {
        val matching = tagged.count { DiaryMoods.valenceOf(it.moodKey) == valence }
        val resolvable = tagged.count { DiaryMoods.valenceOf(it.moodKey) != null }
        return if (resolvable == 0) 0f else matching.toFloat() / resolvable
    }

    /**
     * The most common mood, or `null` when there is no outright winner.
     *
     * Two moods tied for first has no single dominant mood, and picking one
     * arbitrarily would make the headline flip between runs.
     */
    private fun dominantKey(distribution: List<MoodShare>): String? {
        if (distribution.isEmpty()) return null
        val first = distribution.first().count
        val second = distribution.getOrNull(1)?.count ?: 0
        return if (first > second) distribution.first().moodKey else null
    }

    /**
     * The mood that best represents each day, for the calendar's dots.
     *
     * When a day holds several moods the most frequent one wins, and ties are
     * broken toward the *positive* reading and then alphabetically — so the
     * same set of entries always paints the same dot, rather than whichever
     * mood happened to be iterated first.
     */
    private fun dominantMoodByDay(tagged: List<DiarySnapshotEntry>): Map<Long, String> =
        tagged.groupBy { it.dateEpochDay }.mapValues { (_, dayEntries) ->
            dayEntries.groupingBy { it.moodKey!! }.eachCount()
                .entries
                .sortedWith(
                    compareByDescending<Map.Entry<String, Int>> { it.value }
                        .thenByDescending { DiaryMoods.valenceOf(it.key) == MoodValence.POSITIVE }
                        .thenBy { it.key }
                )
                .first()
                .key
        }

    /**
     * Compares the most recent [TREND_WINDOW_DAYS] against the [TREND_WINDOW_DAYS]
     * before it.
     *
     * Both halves must clear [MIN_ENTRIES_FOR_TREND]; otherwise the result is
     * [MoodTrend.UNKNOWN] rather than a direction computed from almost nothing.
     */
    private fun trend(snapshot: DiarySnapshot): MoodTrend {
        val recentFrom = snapshot.todayEpochDay - (TREND_WINDOW_DAYS - 1)
        val earlierFrom = snapshot.todayEpochDay - (2 * TREND_WINDOW_DAYS - 1)

        val recent = scored(snapshot, recentFrom, snapshot.todayEpochDay)
        val earlier = scored(snapshot, earlierFrom, recentFrom - 1)

        if (recent.size < MIN_ENTRIES_FOR_TREND || earlier.size < MIN_ENTRIES_FOR_TREND) {
            return MoodTrend.UNKNOWN
        }
        val recentMean = recent.average().toFloat()
        val earlierMean = earlier.average().toFloat()
        return MoodTrend(
            recentValence = recentMean,
            earlierValence = earlierMean,
            change = recentMean - earlierMean
        )
    }

    /** The `-1..+1` scores of entries whose mood resolved, within an inclusive day range. */
    private fun scored(snapshot: DiarySnapshot, fromEpochDay: Long, toEpochDay: Long): List<Float> =
        snapshot.entries
            .filter { it.dateEpochDay in fromEpochDay..toEpochDay }
            .mapNotNull { DiaryMoods.valenceOf(it.moodKey) }
            .map(::scoreOf)

    /**
     * One point per day for the last [TREND_WINDOW_DAYS] days, ascending.
     *
     * Days with no mood are *present* with a zero score rather than omitted:
     * dropping them would make a gap look like a continuous line of mood and
     * quietly inflate the mean. A zero reads as "nothing recorded", which is
     * what it is.
     */
    private fun recentPoints(snapshot: DiarySnapshot): List<MoodTrendPoint> {
        val byDay = snapshot.entries
            .filter { it.dateEpochDay > snapshot.todayEpochDay - TREND_WINDOW_DAYS }
            .groupBy { it.dateEpochDay }

        return (TREND_WINDOW_DAYS - 1 downTo 0).map { back ->
            val epochDay = snapshot.todayEpochDay - back
            val dayEntries = byDay[epochDay].orEmpty()
            val scores = dayEntries.mapNotNull { DiaryMoods.valenceOf(it.moodKey) }.map(::scoreOf)
            MoodTrendPoint(
                epochDay = epochDay,
                valence = if (scores.isEmpty()) 0f else scores.average().toFloat(),
                entryCount = dayEntries.size
            )
        }
    }

    /**
     * Current and longest runs of consecutive days holding at least one entry.
     *
     * [WritingStreak.isAlive] is `false` when today is empty but yesterday was
     * not. Someone on a 30-day streak who has simply not written yet today still
     * has a 30-day streak, and showing them zero — because it is currently
     * Tuesday and Tuesday is not over — is the single most demotivating bug a
     * streak feature can have.
     *
     * Streaks count *entries*, not moods, because mood is optional by design.
     */
    fun streaks(snapshot: DiarySnapshot): WritingStreak {
        val days = snapshot.daysWithEntries
        if (days.isEmpty()) return WritingStreak(currentLength = 0, longestLength = 0, isAlive = false)

        var longest = 1
        var run = 1
        var previous: Long? = null
        for (day in days) {
            if (previous != null) {
                run = if (day == previous + 1) run + 1 else 1
            }
            if (run > longest) longest = run
            previous = day
        }

        val today = snapshot.todayEpochDay
        val alive = days.contains(today)
        val anchor = when {
            alive -> today
            days.contains(today - 1) -> today - 1
            else -> return WritingStreak(currentLength = 0, longestLength = longest, isAlive = false)
        }

        var current = 0
        var cursor = anchor
        while (days.contains(cursor)) {
            current++
            cursor--
        }
        return WritingStreak(currentLength = current, longestLength = longest, isAlive = alive)
    }
}