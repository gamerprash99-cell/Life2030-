package com.lifeos.app.ui.diary

import com.lifeos.app.core.location.LocationFailure
import com.lifeos.app.core.location.LocationOutcome
import com.lifeos.app.core.location.LocationProvider
import com.lifeos.app.core.media.AudioPlayback
import com.lifeos.app.core.media.AudioRecorder
import com.lifeos.app.core.media.PhotoImporter
import com.lifeos.app.core.media.PlaybackState
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
 * The behaviour this pins down: a save used to read the clock *at save time*,
 * so opening the composer at 8:04, writing for twenty minutes and saving filed
 * the memory at 8:24 — and the time shown while writing was a value that
 * changed under the user's hands. The minute is now captured when the composer
 * opens; these tests hold it there.
 *
 * The timestamp and save rules now live in [DiaryEditorViewModel] (which owns the
 * draft and the write) while day selection and the day list live in
 * [DiaryViewModel], so both are covered here.
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
        todayEpochDay = { today }
    )

    /**
     * The composer. The platform collaborators are fakes, which is only possible
     * because the view model depends on the `AudioRecorder` / `AudioPlayback` /
     * `LocationProvider` / `PhotoImporter` interfaces rather than on the Android
     * implementations.
     */
    /** An editor whose load has deliberately not been allowed to land yet. */
    private fun rawEditor(entryId: String? = null, defaultDay: Long = today) = DiaryEditorViewModel(
        diaryRepository = DiaryRepository(dao),
        weatherRepository = FakeWeatherRepository(),
        locationProvider = FakeLocationProvider(),
        recorder = FakeRecorder(),
        player = FakePlayback(),
        photoImporter = FakePhotoImporter(),
        nowMinutes = { clockMinutes },
        todayEpochDay = { today }
    ).apply { start(entryId, defaultDay) }

    private suspend fun TestScope.editor(
        entryId: String? = null,
        defaultDay: Long = today
    ): DiaryEditorViewModel = DiaryEditorViewModel(
        diaryRepository = DiaryRepository(dao),
        weatherRepository = FakeWeatherRepository(),
        locationProvider = FakeLocationProvider(),
        recorder = FakeRecorder(),
        player = FakePlayback(),
        photoImporter = FakePhotoImporter(),
        nowMinutes = { clockMinutes },
        todayEpochDay = { today }
    ).apply {
        start(entryId, defaultDay)
        // Let the row load land, so what the test types into is the real draft.
        advanceUntilIdle()
    }

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
    fun `a new entry keeps the minute the composer was opened at, not the minute it was saved at`() =
        runTest(dispatcher) {
            val vm = editor()

            // The user writes. Twenty minutes pass; the clock moves on.
            clockMinutes = 8 * 60 + 24
            vm.onContentChange("a long thought")
            vm.save()
            advanceUntilIdle()

            assertEquals(8 * 60 + 4, dao.saved.single().timeMinutes)
        }

    @Test
    fun `the minute shown while writing does not drift as the clock moves`() =
        runTest(dispatcher) {
            val vm = editor()
            val captured = vm.state.value.timeMinutes

            clockMinutes = 23 * 60 + 59

            assertEquals(captured, vm.state.value.timeMinutes)
        }

    @Test
    fun `each new entry is stamped with its own open time`() = runTest(dispatcher) {
        val vm = editor()

        vm.onContentChange("morning")
        vm.save()
        advanceUntilIdle()

        clockMinutes = 21 * 60 + 15
        vm.start(null, today)
        vm.onContentChange("evening")
        vm.save()
        advanceUntilIdle()

        assertEquals(
            listOf(8 * 60 + 4, 21 * 60 + 15),
            dao.saved.map { it.timeMinutes }.sorted()
        )
    }

    @Test
    fun `reopening the composer starts a fresh draft rather than keeping the last one`() =
        runTest(dispatcher) {
            val vm = editor()

            vm.onContentChange("a half-written thought")
            vm.onMoodChange("calm")
            vm.start(null, today)

            assertEquals("", vm.state.value.content)
            assertNull(vm.state.value.mood)
        }

    @Test
    fun `editing an existing memory never moves its timestamp`() = runTest(dispatcher) {
        val original = entry("a", timeMinutes = 7 * 60 + 30)
        dao.seed(original)
        val vm = editor("a")

        clockMinutes = 19 * 60
        vm.onContentChange("revised wording")
        vm.onMoodChange("calm")
        vm.save()
        advanceUntilIdle()

        val saved = dao.saved.single()
        assertEquals(7 * 60 + 30, saved.timeMinutes)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals("revised wording", saved.content)
    }

    @Test
    fun `saving before the edited row finishes loading cannot create a duplicate`() =
        runTest(dispatcher) {
            dao.seed(entry("a", timeMinutes = 7 * 60 + 30))

            // The composer opens and the user starts typing while the read is
            // still in flight. The draft has no id yet, so an unguarded save
            // would insert a second row rather than update the first.
            val vm = rawEditor("a")
            vm.onContentChange("typed straight away")
            assertFalse(vm.state.value.canSave)

            vm.save()
            advanceUntilIdle()

            assertEquals(1, dao.saved.size)
            assertEquals("a", dao.saved.single().id)
        }

    @Test
    fun `a new memory is filed under the day being read, not today`() = runTest(dispatcher) {
        val vm = editor(defaultDay = today - 5)

        vm.onContentChange("a memory from last week")
        vm.save()
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

    // ---- save guards and confirmation --------------------------------------

    @Test
    fun `a blank memory is never written`() = runTest(dispatcher) {
        val vm = editor()

        vm.onContentChange("   ")
        vm.save()
        advanceUntilIdle()

        assertEquals(0, dao.saved.size)
    }

    @Test
    fun `a rejected blank save emits no confirmation event`() = runTest(dispatcher) {
        val vm = editor()

        vm.onContentChange("first")
        vm.save()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.saveCount)

        vm.start(null, today)
        vm.onContentChange("   ")
        vm.save()
        advanceUntilIdle()

        // A rejected blank save must not produce another success event.
        assertEquals(1, vm.state.value.saveCount)
    }

    @Test
    fun `saving the same memory twice fires the confirmation twice`() = runTest(dispatcher) {
        dao.seed(entry("a"))
        val vm = editor("a")

        vm.onContentChange("first revision")
        vm.save()
        advanceUntilIdle()
        vm.onContentChange("second revision")
        vm.save()
        advanceUntilIdle()

        // A monotonic counter, not the id: the second write of the same row is a
        // distinct event the UI still has to acknowledge.
        assertEquals(2, vm.state.value.saveCount)
    }

    @Test
    fun `the save action is released after a write`() = runTest(dispatcher) {
        val vm = editor()

        vm.onContentChange("first")
        vm.save()
        advanceUntilIdle()

        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun `the day view closes the composer once the editor reports a committed write`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.startNewEntry()
            assertEquals(true, vm.showEditor.value)

            vm.onEditorSaved()

            assertFalse(vm.showEditor.value)
            assertNull(vm.editingEntry.value)
        }
}

/**
 * Minimal in-memory stand-in for the Room DAO — a real database cannot be opened
 * in a JVM unit test, and the ViewModels only ever need the six methods below.
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

// ---- platform collaborators -------------------------------------------------

private class FakeRecorder : AudioRecorder {
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state

    override fun start(): RecordingState = _state.value
    override fun tick(): RecordingState = _state.value
    override fun stop(): RecordingState = _state.value
    override fun cancel() { _state.value = RecordingState.Idle }
    override fun isRecording(): Boolean = false
}

private class FakePlayback : AudioPlayback {
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state
    override fun toggle(filePath: String): PlaybackState = _state.value
    override fun play(filePath: String): PlaybackState = _state.value
    override fun stop() { _state.value = PlaybackState.Idle }
    override fun release() = stop()
}

private class FakeLocationProvider(
    private val outcome: LocationOutcome = LocationOutcome.Failure(LocationFailure.NO_FIX)
) : LocationProvider {
    override fun hasLocationPermission(): Boolean = true
    override suspend fun currentPlace(): LocationOutcome = outcome
}

private class FakePhotoImporter : PhotoImporter {
    override fun import(sourceUri: android.net.Uri): DiaryAttachment.Photo? =
        DiaryAttachment.Photo(filePath = "/private/fake-${sourceUri.lastPathSegment}.jpg")
}

private class FakeWeatherRepository : WeatherRepository {
    override suspend fun weatherFor(place: DiaryAttachment.Place?): DiaryWeather =
        DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_SOURCE_OFFLINE_ONLY)
}
