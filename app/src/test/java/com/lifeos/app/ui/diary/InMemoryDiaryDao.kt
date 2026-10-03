package com.lifeos.app.ui.diary

import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.entities.DiaryEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * An in-memory [DiaryDao] for the Diary sub-screen tests.
 *
 * Shared rather than private per file, because the search, calendar and insights
 * tests all need one and three near-identical copies drift apart — which is how
 * a fake stops matching the interface it is standing in for.
 *
 * [delayMillis] exists for one reason: to reproduce the stale-response race in
 * search, where an early query resolves *after* a later one. Without a way to
 * make the first call slow, that ordering cannot be tested at all on a single
 * test dispatcher.
 */
internal open class InMemoryDiaryDao(
    initial: List<DiaryEntity> = emptyList(),
    private val delayMillis: (String) -> Long = { 0L }
) : DiaryDao {

    val rows = MutableStateFlow(initial)
    private var inFlight = initial

    var searchCallCount = 0
        private set

    /** Every `search` call's `(pattern, moodKey)`, so tests can assert what was asked for. */
    var searchArgs: List<Pair<String, String?>> = emptyList()
        private set

    var rangeQueries: List<Pair<Long, Long>> = emptyList()
        private set

    override suspend fun search(pattern: String, moodKey: String?): List<DiaryEntity> {
        searchCallCount++
        searchArgs = searchArgs + (pattern to moodKey)
        delay(delayMillis(pattern))
        return inFlight.filter { row ->
            val textMatches = listOfNotNull(row.title, row.content, row.tagsCsv)
                .any { it.contains(pattern, ignoreCase = true) }
            textMatches && (moodKey == null || row.mood.equals(moodKey, ignoreCase = true))
        }
    }

    override suspend fun getAllInRange(fromEpochDay: Long, toEpochDay: Long): List<DiaryEntity> {
        rangeQueries = rangeQueries + (fromEpochDay to toEpochDay)
        return inFlight.filter { it.dateEpochDay in fromEpochDay..toEpochDay }
    }

    override suspend fun getAll(): List<DiaryEntity> = inFlight

    override suspend fun upsert(entry: DiaryEntity) {
        inFlight = inFlight.filterNot { it.id == entry.id } + entry
        rows.value = inFlight
    }

    override suspend fun delete(id: String) {
        inFlight = inFlight.filterNot { it.id == id }
        rows.value = inFlight
    }

    override fun observeAll(): Flow<List<DiaryEntity>> = rows

    override fun observeForDay(epochDay: Long): Flow<List<DiaryEntity>> =
        rows.map { all -> all.filter { it.dateEpochDay == epochDay } }

    override suspend fun getById(id: String): DiaryEntity? = inFlight.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<DiaryEntity?> = rows.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun getAllForBackup(): List<DiaryEntity> = inFlight
}

/** A [DiaryEntity] with sensible defaults, so tests only state what they care about. */
internal fun diaryEntry(
    id: String,
    title: String? = null,
    content: String = "",
    mood: String? = null,
    tagsCsv: String = "",
    dateEpochDay: Long,
    timeMinutes: Int = 600,
    isFavorite: Boolean = false,
    attachmentsJson: String = ""
) = DiaryEntity(
    id = id,
    title = title,
    content = content,
    mood = mood,
    tagsCsv = tagsCsv,
    dateEpochDay = dateEpochDay,
    timeMinutes = timeMinutes,
    isFavorite = isFavorite,
    attachmentsJson = attachmentsJson,
    // Fixed timestamps: nothing under test reads them, and pinning them keeps a
    // fixture's equality comparable.
    createdAt = 0L,
    updatedAt = 0L
)

/** An unused flow, for fakes that must return a Flow but have nothing to emit. */
internal fun <T> neverEmits(): Flow<T> = flowOf()