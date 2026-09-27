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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    /** ViewModels own Main-scope jobs; clear every test instance so one test cannot leak into the next. */
    private val createdScopes = mutableListOf<kotlinx.coroutines.CoroutineScope>()

    /** The wall clock, moved explicitly by the test rather than by wall time. */
    private var clockMinutes = 8 * 60 + 4

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeDiaryDao()
    }

    @After
    fun tearDown() {
        createdScopes.forEach { it.coroutineContext.cancelChildren() }
        createdScopes.clear()
        Dispatchers.resetMain()
    }

    /**
     * The repository does its row reads, JSON work and file unlinking on an IO
     * dispatcher rather than on the caller's thread, because every production
     * caller is a `viewModelScope`. Handed the shared test scheduler here so
     * `advanceUntilIdle()` still governs the storage work — pointing it at the
     * real `Dispatchers.IO` would make these tests race a live thread pool.
     */
    private fun diaryRepository() = DiaryRepository(dao, io = dispatcher)

    private fun viewModel() = DiaryViewModel(
        diaryRepository = diaryRepository(),
        todayEpochDay = { today }
    ).also { createdScopes += it.viewModelScope }

    /**
     * The composer. The platform collaborators are fakes, which is only possible
     * because the view model depends on the `AudioRecorder` / `AudioPlayback` /
     * `LocationProvider` / `PhotoImporter` interfaces rather than on the Android
     * implementations.
     */
    /** An editor whose load has deliberately not been allowed to land yet. */
    private fun rawEditor(entryId: String? = null, defaultDay: Long = today) =
        DiaryEditorViewModel(
            diaryRepository = diaryRepository(),
            weatherRepository = FakeWeatherRepository(),
            locationProvider = FakeLocationProvider(),
            recorder = FakeRecorder(),
            player = FakePlayback(),
            photoImporter = FakePhotoImporter(),
            nowMinutes = { clockMinutes },
            todayEpochDay = { today }
        ).also { createdScopes += it.viewModelScope }.apply { start(entryId, defaultDay) }

    private suspend fun TestScope.editor(
        entryId: String? = null,
        defaultDay: Long = today
    ): DiaryEditorViewModel = DiaryEditorViewModel(
        diaryRepository = diaryRepository(),
        weatherRepository = FakeWeatherRepository(),
        locationProvider = FakeLocationProvider(),
        recorder = FakeRecorder(),
        player = FakePlayback(),
        photoImporter = FakePhotoImporter(),
        nowMinutes = { clockMinutes },
        todayEpochDay = { today }
    ).also { createdScopes += it.viewModelScope }.apply {
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

            vm.onEditorSaved("a")

            assertFalse(vm.showEditor.value)
            assertNull(vm.editingEntry.value)
        }

    // ---- the post-save confirmation ----------------------------------------

    @Test
    fun `a committed write is confirmed with the entry it produced`() = runTest(dispatcher) {
        dao.seed(entry("a", timeMinutes = 8 * 60 + 2))
        val vm = viewModel()

        vm.startNewEntry()
        vm.onEditorSaved("a")

        assertEquals("a", vm.savedEntryId.value)
        // A WhileSubscribed flow, so read it through `first` rather than `.value`.
        val confirmed = vm.savedEntry.first { it != null }!!
        assertEquals("a", confirmed.id)
    }

    @Test
    fun `the confirmation names the real stored minute, not the minute of the save`() =
        runTest(dispatcher) {
            // The row carries the minute it was originally filed under. An edit
            // re-saves without moving it, and the sheet must report that minute
            // rather than whatever the clock says now.
            dao.seed(entry("a", timeMinutes = 7 * 60 + 30))
            val vm = viewModel()

            clockMinutes = 21 * 60
            vm.onEditorSaved("a")

            assertEquals(7 * 60 + 30, vm.savedEntry.first { it != null }!!.timeMinutes)
        }

    @Test
    fun `a commit that produced no id does not show an empty confirmation`() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.startNewEntry()
            vm.onEditorSaved(null)

            assertNull(vm.savedEntryId.value)
            // Nothing to name, so the sheet must stay hidden: the screen gates it
            // on a resolvable entry, and an id-less commit leaves nothing to
            // resolve.
            assertNull(vm.savedEntry.first())
        }

    @Test
    fun `dismissing the confirmation retires it`() = runTest(dispatcher) {
        dao.seed(entry("a"))
        val vm = viewModel()

        vm.onEditorSaved("a")
        assertEquals("a", vm.savedEntryId.value)

        vm.dismissSavedConfirmation()

        assertNull(vm.savedEntryId.value)
    }

    @Test
    fun `moving to another day retires the confirmation`() = runTest(dispatcher) {
        dao.seed(entry("a", day = today), entry("b", day = today - 1))
        val vm = viewModel()

        vm.onEditorSaved("a")
        vm.selectDay(today - 1)

        assertNull(vm.savedEntryId.value)
    }

    @Test
    fun `add another memory retires the confirmation and reopens the composer empty`() =
        runTest(dispatcher) {
            dao.seed(entry("a"))
            val vm = viewModel()
            vm.onEditorSaved("a")

            // What the sheet's second action calls.
            vm.startNewEntry()

            assertNull(vm.savedEntryId.value)
            assertEquals(true, vm.showEditor.value)
            // A second memory must be a new draft, never the one just written.
            assertNull(vm.editingEntry.value)
        }

    @Test
    fun `a second save of the same memory confirms again`() = runTest(dispatcher) {
        dao.seed(entry("a"))
        val vm = viewModel()

        vm.onEditorSaved("a")
        vm.dismissSavedConfirmation()
        vm.onEditorSaved("a")

        assertEquals("a", vm.savedEntryId.value)
    }

    // ---- voice note --------------------------------------------------------

    @Test
    fun `a running recording's live duration and level reach the state the row renders`() =
        runTest(dispatcher) {
            // The composer's timer and level dot are rendered from
            // `state.recording`, which the UI advances only by calling
            // tickRecording(). That call was never made, so a take used to sit on
            // the 0:00 / zero-amplitude reading `start()` wrote for its whole
            // length. This pins the contract the poll depends on.
            val recorder = FakeRecorder()
            val vm = DiaryEditorViewModel(
                diaryRepository = diaryRepository(),
                weatherRepository = FakeWeatherRepository(),
                locationProvider = FakeLocationProvider(),
                recorder = recorder,
                player = FakePlayback(),
                photoImporter = FakePhotoImporter(),
                nowMinutes = { clockMinutes },
                todayEpochDay = { today }
            )
            vm.start(null, today)
            advanceUntilIdle()

            vm.startRecording()
            val atStart = vm.state.value.recording
            assertTrue("expected a running take, was $atStart", atStart is RecordingState.Recording)
            assertEquals(0L, (atStart as RecordingState.Recording).elapsedMillis)

            recorder.elapsedMillis = 4_500L
            recorder.amplitude = 9_000
            vm.tickRecording()

            val live = vm.state.value.recording as RecordingState.Recording
            assertEquals(4_500L, live.elapsedMillis)
            assertEquals(9_000, live.amplitude)
        }

    @Test
    fun `ticking with no take running reports idle rather than inventing a recording`() =
        runTest(dispatcher) {
            val vm = editor()

            vm.tickRecording()

            assertEquals(RecordingState.Idle, vm.state.value.recording)
        }

    @Test
    fun `a memory cannot be saved while a take is still running`() = runTest(dispatcher) {
        val vm = editor()
        vm.onContentChange("words with a take still open")

        vm.startRecording()
        vm.tickRecording()

        assertFalse("a running take must block the save", vm.state.value.canSave)
    }

    // ---- the Save button's own contract ------------------------------------
    //
    // The composable no longer re-derives "is Save live?" for itself; it reads
    // `DiaryEditorState.canSave`. So every guard in that property is now also a
    // promise about what the user sees on screen, and each one is pinned here.

    @Test
    fun `an entry that is still loading reports itself as not saveable`() =
        runTest(dispatcher) {
            dao.seed(entry("slow"))
            // `rawEditor` deliberately does not let the load land, so the draft
            // is still in its `isLoading` window.
            val vm = rawEditor("slow")
            advanceUntilIdle()

            // Whatever the row turned out to hold, the guard held for the whole
            // time the load was outstanding.
            assertFalse(
                "an un-loaded row must not offer Save",
                loadWasNeverSaveable(vm)
            )
            assertFalse("and the load must have finished", vm.state.value.isLoading)
        }

    @Test
    fun `a blank memory is not saveable, so the button is never live on empty words`() =
        runTest(dispatcher) {
            val vm = editor()

            assertFalse(vm.state.value.canSave)

            vm.onContentChange("   ")
            assertFalse("whitespace is not a memory", vm.state.value.canSave)

            vm.onContentChange("real words")
            assertTrue(vm.state.value.canSave)
        }

    @Test
    fun `a memory is not saveable while its own write is in flight`() = runTest(dispatcher) {
        dao.seed(entry("busy"))
        val vm = editor("busy")
        vm.onContentChange("first revision")

        vm.save()
        advanceUntilIdle()

        // And it comes back once the write settles, so the button is reusable.
        assertTrue(vm.state.value.canSave)
    }

    /** Walks the load window and reports whether Save was ever offered inside it. */
    private suspend fun loadWasNeverSaveable(vm: DiaryEditorViewModel): Boolean {
        vm.onContentChange("typed before the row arrived")
        return !vm.state.value.canSave
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

    /**
     * The wall clock a fake recording runs on. Advanced explicitly by the test
     * so the elapsed reading is a value the test chose, not one that depended on
     * how long the test took to run.
     */
    var elapsedMillis = 0L
    var amplitude = 0

    override fun start(): RecordingState {
        elapsedMillis = 0L
        _state.value = RecordingState.Recording(elapsedMillis = 0L, amplitude = 0)
        return _state.value
    }

    override fun tick(): RecordingState {
        val active = _state.value
        if (active !is RecordingState.Recording) return active
        _state.value = RecordingState.Recording(elapsedMillis = elapsedMillis, amplitude = amplitude)
        return _state.value
    }

    override fun stop(): RecordingState {
        val filePath = "/private/fake-take.m4a"
        _state.value = RecordingState.Finished(filePath, elapsedMillis)
        return _state.value
    }

    override fun cancel() { _state.value = RecordingState.Idle }

    override fun isRecording(): Boolean = _state.value is RecordingState.Recording
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
