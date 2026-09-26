package com.lifeos.app.ui.diary

import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * The Diary's timestamp and day-selection rules.
 *
 * The behaviour this pins down: `saveEntry` used to read the clock *at save
 * time*, so opening the editor at 8:04, writing for twenty minutes and saving
 * filed the memory at 8:24 — and the time shown while writing was a value that
 * changed under the user's hands. The minute is now captured when the editor
 * opens; these tests hold it there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {

    private val today = LocalDate.of(2026, 9, 26).toEpochDay()

    /**
     * One dispatcher shared by `Dispatchers.Main` and every `runTest`, so
     * `advanceUntilIdle()` actually drives the ViewModel's coroutines instead of
     * a second, unrelated scheduler.
     */
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeDiaryDao

    /** The wall clock, moved explicitly by the test rather than by wall time. */
    private var clockMinutes = 8 * 60 + 4

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeDiaryDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DiaryViewModel(
        diaryRepository = DiaryRepository(dao),
        nowMinutes = { clockMinutes },
        todayEpochDay = { today }
    )

    private fun entry(id: String, day: Long = today, timeMinutes: Int = 480) = DiaryEntity(
        id = id,
        content = "content of $id",
        dateEpochDay = day,
        timeMinutes = timeMinutes,
        createdAt = 0L,
        updatedAt = 0L
    )

    // ---- timestamp capture -------------------------------------------------

    @Test
    fun `a new entry keeps the minute the editor was opened at, not the minute it was saved at`() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.startNewEntry()
            assertEquals(8 * 60 + 4, vm.editorTimeMinutes.value)

            // The user writes. Twenty minutes pass; the clock moves on.
            clockMinutes = 8 * 60 + 24
            vm.saveEntry("a long thought", null)
            advanceUntilIdle()

            assertEquals(8 * 60 + 4, dao.saved.single().timeMinutes)
        }

    @Test
    fun `the minute shown while writing does not drift as the clock moves`() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.startNewEntry()
            clockMinutes = 23 * 60 + 59

            assertEquals(8 * 60 + 4, vm.editorTimeMinutes.value)
        }

    @Test
    fun `each new entry is stamped with its own open time`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.startNewEntry()
        vm.saveEntry("morning", null)
        advanceUntilIdle()

        clockMinutes = 21 * 60 + 15
        vm.startNewEntry()
        vm.saveEntry("evening", null)
        advanceUntilIdle()

        assertEquals(
            listOf(8 * 60 + 4, 21 * 60 + 15),
            dao.saved.map { it.timeMinutes }.sorted()
        )
    }

    @Test
    fun `editing an existing memory never moves its timestamp`() = runTest(dispatcher) {
        val original = entry("a", timeMinutes = 7 * 60 + 30)
        dao.seed(original)
        val vm = viewModel()

        vm.startEdit(original)
        clockMinutes = 19 * 60
        vm.saveEntry("revised wording", "calm")
        advanceUntilIdle()

        val saved = dao.saved.single()
        assertEquals(7 * 60 + 30, saved.timeMinutes)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals("revised wording", saved.content)
    }

    @Test
    fun `the editor's minute is cleared once it closes`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.startNewEntry()
        assertEquals(8 * 60 + 4, vm.editorTimeMinutes.value)

        vm.dismissEditor()
        assertNull(vm.editorTimeMinutes.value)
    }

    @Test
    fun `a new memory is filed under the day being read, not today`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.selectDay(today - 5)

        vm.startNewEntry()
        vm.saveEntry("a memory from last week", null)
        advanceUntilIdle()

        assertEquals(today - 5, dao.saved.single().dateEpochDay)
    }

    // ---- day selection bounds ---------------------------------------------

    @Test
    fun `selecting today keeps today`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.selectDay(today)
        assertEquals(today, vm.selectedDay.value)
    }

    @Test
    fun `selecting a future day is refused because there is no tomorrow to journal`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.selectDay(today + 1)
            assertEquals(today, vm.selectedDay.value)
        }

    @Test
    fun `selecting beyond the strip's history window is clamped to its oldest day`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.selectDay(today - HISTORY_DAYS - 40)
            assertEquals(today - HISTORY_DAYS, vm.selectedDay.value)
        }

    // ---- day content -------------------------------------------------------

    @Test
    fun `only the selected day's memories are shown, newest first`() = runTest(dispatcher) {
        dao.seed(
            entry("older", timeMinutes = 9 * 60),
            entry("newer", timeMinutes = 21 * 60),
            entry("other-day", day = today - 1, timeMinutes = 12 * 60)
        )
        val vm = viewModel()

        // This is a WhileSubscribed state flow, so it only produces a value once
        // something is actually collecting — read through `first`.
        val shown = vm.memoriesForSelectedDay.first { it.isNotEmpty() }
        assertEquals(listOf("newer", "older"), shown.map { it.id })
    }

    @Test
    fun `days holding memories are reported for the strip's markers`() = runTest(dispatcher) {
        dao.seed(
            entry("a", day = today),
            entry("b", day = today - 2),
            entry("c", day = today - 2)
        )
        val vm = viewModel()

        val marked = vm.daysWithMemories.first { it.isNotEmpty() }
        assertEquals(setOf(today, today - 2), marked)
    }

    @Test
    fun `a blank memory is never written`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.startNewEntry()
        vm.saveEntry("   ", null)
        advanceUntilIdle()

        assertEquals(0, dao.saved.size)
    }

    @Test
    fun `successful save emits one confirmation event and blank save emits none`() = runTest(dispatcher) {
        val vm = viewModel()

        assertEquals(0, vm.saveConfirmation.value)

        vm.startNewEntry()
        vm.saveEntry("first", null)
        advanceUntilIdle()

        assertEquals(1, vm.saveConfirmation.value)

        vm.startNewEntry()
        vm.saveEntry("   ", null)
        advanceUntilIdle()

        // A rejected blank save must not produce another success event.
        assertEquals(1, vm.saveConfirmation.value)
    }

    @Test
    fun `the editor closes and the save action is released after a write`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.startNewEntry()
        vm.saveEntry("first", null)
        advanceUntilIdle()

        assertFalse(vm.saving.value)
        assertFalse(vm.showEditor.value)
    }
}

/**
 * Minimal in-memory stand-in for the Room DAO — a real database cannot be opened
 * in a JVM unit test, and the ViewModel only ever needs the six methods below.
 */
private class FakeDiaryDao : DiaryDao {

    private val state = MutableStateFlow<List<DiaryEntity>>(emptyList())

    /** Current contents, as the ViewModel would observe them. */
    val saved: List<DiaryEntity> get() = state.value

    fun seed(vararg entries: DiaryEntity) {
        state.value = entries.toList()
    }

    override suspend fun upsert(entry: DiaryEntity) {
        state.value = state.value.filterNot { it.id == entry.id } + entry
    }

    override suspend fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
    }

    override fun observeAll(): Flow<List<DiaryEntity>> = state

    override fun observeForDay(epochDay: Long): Flow<List<DiaryEntity>> =
        state.map { all -> all.filter { it.dateEpochDay == epochDay } }

    override suspend fun getById(id: String): DiaryEntity? = state.value.firstOrNull { it.id == id }

    override suspend fun getAllForBackup(): List<DiaryEntity> = state.value
}
