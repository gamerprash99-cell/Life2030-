package com.lifeos.app.ui.diary

import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.usecase.SearchDiaryEntriesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
class DiarySearchViewModelTest {

    private val today = LocalDate.of(2026, 10, 14).toEpochDay()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(dao: InMemoryDiaryDao) =
        DiarySearchViewModel(SearchDiaryEntriesUseCase(DiaryRepository(dao, io = dispatcher)))

    // ---- the use case ----------------------------------------------------

    @Test
    fun `blank query returns nothing and never touches the database`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao(listOf(diaryEntry("a", content = "anything at all", dateEpochDay = today)))
        val useCase = SearchDiaryEntriesUseCase(DiaryRepository(dao, io = dispatcher))

        assertEquals(emptyList<Any>(), useCase("   "))
        assertEquals(emptyList<Any>(), useCase(""))
        // The regression this guards: a blank pattern reaching the DAO as '%',
        // which turns an accidental focus in an empty field into "show me my
        // whole journal" — on the one screen where that is most alarming.
        assertEquals(0, dao.searchCallCount)
    }

    @Test
    fun `limit caps the rows returned to the screen`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao((1..30).map { diaryEntry("id$it", content = "shared word", dateEpochDay = today) })

        assertEquals(5, SearchDiaryEntriesUseCase(DiaryRepository(dao, io = dispatcher))("shared", limit = 5).size)
    }

    @Test
    fun `a nonsensical limit degrades to one row rather than none or everything`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao((1..5).map { diaryEntry("id$it", content = "shared", dateEpochDay = today) })
        val useCase = SearchDiaryEntriesUseCase(DiaryRepository(dao, io = dispatcher))

        // An off-by-one in a caller's arithmetic should show a single result, not
        // an empty screen that looks like "nothing matched".
        assertEquals(1, useCase("shared", limit = 0).size)
        assertEquals(1, useCase("shared", limit = -10).size)
    }

    @Test
    fun `a blank query beats the limit even when the limit is huge`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao(listOf(diaryEntry("a", content = "x", dateEpochDay = today)))
        assertEquals(0, SearchDiaryEntriesUseCase(DiaryRepository(dao, io = dispatcher))("", limit = Int.MAX_VALUE).size)
    }

    // ---- the ViewModel ---------------------------------------------------

    @Test
    fun `typing debounces into a single query`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao(listOf(diaryEntry("a", content = "moonlight", dateEpochDay = today)))
        val vm = viewModel(dao)

        vm.onQueryChange("m")
        advanceTimeBy(40)
        vm.onQueryChange("mo")
        advanceTimeBy(40)
        vm.onQueryChange("moon")
        advanceTimeBy(DiarySearchViewModel.SEARCH_DEBOUNCE_MS + 50)
        advanceUntilIdle()

        // One scan for three keystrokes. Every character firing its own full
        // table query is precisely what the debounce exists to prevent.
        assertEquals(1, dao.searchCallCount)
        assertEquals(listOf("moon"), dao.searchArgs.map { it.first })
    }

    @Test
    fun `a stale response cannot overwrite a newer one`() = runTest(dispatcher) {
        // The first query resolves slowly and the second immediately: the real
        // ordering hazard, since the two run on different threads with no
        // ordering guarantee between them.
        val dao = InMemoryDiaryDao(
            initial = listOf(
                diaryEntry("slow", content = "alpha", dateEpochDay = today),
                diaryEntry("fast", content = "beta", dateEpochDay = today)
            ),
            delayMillis = { if (it == "alpha") 800L else 0L }
        )
        val vm = viewModel(dao)

        vm.onQueryChange("alpha")
        advanceTimeBy(DiarySearchViewModel.SEARCH_DEBOUNCE_MS + 10)
        vm.onQueryChange("beta")
        advanceTimeBy(DiarySearchViewModel.SEARCH_DEBOUNCE_MS + 10)
        advanceUntilIdle()

        assertEquals("both queries should have run", 2, dao.searchCallCount)
        // The visible results must belong to the *latest* query, not to whichever
        // happened to finish last.
        assertEquals(listOf("fast"), vm.state.value.results.map { it.id })
        assertFalse("a stale response must not leave the spinner up", vm.state.value.isSearching)
    }

    @Test
    fun `clearing the query returns to the prompt instead of leaving stale hits`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao(listOf(diaryEntry("a", content = "keep", dateEpochDay = today)))
        val vm = viewModel(dao)

        vm.onQueryChange("keep")
        advanceTimeBy(DiarySearchViewModel.SEARCH_DEBOUNCE_MS + 50)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.results.size)

        vm.onClearQuery()
        advanceUntilIdle()

        // Yesterday's hits left behind a blank field read as "your journal is
        // empty", which is the worst possible thing to imply on this screen.
        assertEquals("", vm.state.value.query)
        assertEquals(emptyList<Any>(), vm.state.value.results)
        assertTrue(vm.state.value.showPrompt)
        assertFalse(vm.state.value.showEmptyResult)
    }

    @Test
    fun `tapping the active mood filter clears it`() = runTest(dispatcher) {
        val happy = DiaryMoods.OPTIONS.first { it.label == "Happy" }
        val dao = InMemoryDiaryDao(
            listOf(diaryEntry("a", content = "keep", mood = happy.key, dateEpochDay = today))
        )
        val vm = viewModel(dao)

        vm.onQueryChange("keep")
        advanceTimeBy(DiarySearchViewModel.SEARCH_DEBOUNCE_MS + 50)
        vm.onMoodFilterChange(happy.key)
        advanceUntilIdle()
        assertEquals(happy.key, vm.state.value.moodKey)
        assertEquals(1, vm.state.value.results.size)

        vm.onMoodFilterChange(happy.key)
        advanceUntilIdle()

        // Same gesture as the editor's selector: tapping the thing that is
        // already on turns it off.
        assertNull(vm.state.value.moodKey)
    }

    @Test
    fun `a mood filter is passed through to the database`() = runTest(dispatcher) {
        val happy = DiaryMoods.OPTIONS.first { it.label == "Happy" }
        val sad = DiaryMoods.OPTIONS.first { it.label == "Sad" }
        val dao = InMemoryDiaryDao(
            listOf(
                diaryEntry("h", content = "shared", mood = happy.key, dateEpochDay = today),
                diaryEntry("s", content = "shared", mood = sad.key, dateEpochDay = today)
            )
        )
        val vm = viewModel(dao)

        vm.onQueryChange("shared")
        advanceTimeBy(DiarySearchViewModel.SEARCH_DEBOUNCE_MS + 50)
        vm.onMoodFilterChange(sad.key)
        advanceUntilIdle()

        assertEquals(listOf("s"), vm.state.value.results.map { it.id })
        assertTrue(dao.searchArgs.any { it.second == sad.key })
    }

    @Test
    fun `an empty result and a blank field are different states`() = runTest(dispatcher) {
        val vm = viewModel(InMemoryDiaryDao())

        assertTrue("nothing typed is an invitation, not a failure", vm.state.value.showPrompt)
        assertFalse(vm.state.value.showEmptyResult)

        vm.onQueryChange("absent")
        advanceTimeBy(DiarySearchViewModel.SEARCH_DEBOUNCE_MS + 50)
        advanceUntilIdle()

        assertFalse(vm.state.value.showPrompt)
        assertTrue("a completed search with no hits is a result", vm.state.value.showEmptyResult)
    }

    @Test
    fun `submit searches immediately without waiting for the debounce`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao(listOf(diaryEntry("a", content = "instant", dateEpochDay = today)))
        val vm = viewModel(dao)

        vm.onQueryChange("instant")
        vm.onSubmit()
        advanceUntilIdle()

        // The keyboard's search key must not wait out the debounce window; the
        // user has already said they are done typing.
        assertEquals(1, vm.state.value.results.size)
    }
}