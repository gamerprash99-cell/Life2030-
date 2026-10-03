package com.lifeos.app.domain.intelligence

/**
 * The one entry point to the intelligence layer.
 *
 * Every analyzer is independently usable and independently testable, but the
 * screen should not have to know that — it asks for [DiaryInsights] and gets a
 * consistent set of figures computed against one snapshot. That matters more
 * than it looks: if the screen pulled statistics from one read of the journal
 * and moods from another, a save landing between the two reads would produce
 * figures that disagree with each other, and the user would have no way to tell
 * which was stale.
 *
 * The class holds no state and opens nothing. Given the same snapshot it
 * returns the same bundle, every time, with no clock and no network involved.
 * That is what makes it safe to run on every database emission.
 */
object DiaryAnalyzer {

    /**
     * Below this many entries the Insights screen shows an onboarding state
     * instead of figures.
     *
     * Not every average guards itself — counts are always meaningful — but a
     * screen showing "1 entry, 1 active day, 0 patterns" reads as a broken
     * feature rather than an honest start.
     */
    const val MIN_ENTRIES_FOR_INSIGHTS = 3

    /** Analyses a journal. Never throws for an empty snapshot. */
    fun analyze(snapshot: DiarySnapshot): DiaryInsights {
        if (snapshot.isEmpty) return DiaryInsights.empty(snapshot.todayEpochDay)
        return DiaryInsights(
            statistics = StatisticsEngine.analyze(snapshot),
            mood = MoodAnalyzer.analyze(snapshot),
            patterns = PatternDetector.analyze(snapshot),
            question = LocalQuestionEngine.questionFor(snapshot)
        )
    }

    /** The streak, which is a property of the journal rather than of any one analyzer's output. */
    fun streak(snapshot: DiarySnapshot): WritingStreak = MoodAnalyzer.streaks(snapshot)

    /**
     * Builds a snapshot from persisted rows.
     *
     * Lives here rather than in the repository so that the mapping from storage
     * row to analysis input is visible next to the thing that consumes it, and
     * so the repository never has to import the intelligence layer.
     *
     * [attachmentCount] is passed separately because counting them means
     * decoding `attachmentsJson`, which is the data layer's encoding.
     */
    fun snapshotOf(
        entries: List<SnapshotRow>,
        todayEpochDay: Long
    ): DiarySnapshot = DiarySnapshot(
        entries = entries.map { row ->
            DiarySnapshotEntry(
                id = row.id,
                title = row.title,
                content = row.content,
                moodKey = row.mood,
                tags = row.tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() },
                dateEpochDay = row.dateEpochDay,
                timeMinutes = row.timeMinutes,
                isFavorite = row.isFavorite,
                attachmentCount = row.attachmentCount
            )
        },
        todayEpochDay = todayEpochDay
    )

    /** The subset of a diary row the engine needs. Keeps [snapshotOf] testable without Room. */
    data class SnapshotRow(
        val id: String,
        val title: String?,
        val content: String,
        val mood: String?,
        val tagsCsv: String,
        val dateEpochDay: Long,
        val timeMinutes: Int,
        val isFavorite: Boolean,
        val attachmentCount: Int
    )
}