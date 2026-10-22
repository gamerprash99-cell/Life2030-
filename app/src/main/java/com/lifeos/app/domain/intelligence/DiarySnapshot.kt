package com.lifeos.app.domain.intelligence

/**
 * One entry, as the intelligence layer sees it.
 *
 * Deliberately *not* [com.lifeos.app.data.db.entities.DiaryEntity]. The engine
 * takes a snapshot of plain data so that:
 *  - it has no dependency on Room, Android or Compose, and its tests run on a
 *    bare JVM with no Robolectric and no in-memory database;
 *  - the reduction logic is a pure function of its input, so a test can assert
 *    on an exact expected output rather than on query side effects;
 *  - `attachmentsJson` — an opaque encoding owned by the data layer — never
 *    has to be understood here. [attachmentCount] is passed in already
 *    decoded, because "how many photos are in this entry" is a fact worth
 *    reporting but not a string worth parsing.
 */
data class DiarySnapshotEntry(
    val id: String,
    val title: String?,
    val content: String,
    val moodKey: String?,
    val tags: List<String>,
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val isFavorite: Boolean = false,
    val attachmentCount: Int = 0
) {
    /** Title if the entry has one, otherwise the first line of the body. */
    val displayHeading: String
        get() = title?.takeIf { it.isNotBlank() }
            ?: content.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
}

/**
 * Everything the engine is allowed to know about the journal.
 *
 * [todayEpochDay] is passed in rather than read from the clock so that "is
 * there a live streak?" and "what is the last 7 days?" are answerable in a
 * test without freezing time. Every analyzer derives relative dates from this
 * one value, which is also why they cannot disagree with each other about
 * where "today" is.
 */
data class DiarySnapshot(
    val entries: List<DiarySnapshotEntry>,
    val todayEpochDay: Long
) {
    /** Days that have at least one entry, ascending. */
    val daysWithEntries: Set<Long> = entries.mapTo(sortedSetOf()) { it.dateEpochDay }

    val isEmpty: Boolean get() = entries.isEmpty()

    companion object {
        val EMPTY = DiarySnapshot(emptyList(), todayEpochDay = 0L)
    }
}