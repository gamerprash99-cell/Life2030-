package com.lifeos.app.ui.diary

import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.domain.intelligence.DiaryInsights
import com.lifeos.app.domain.intelligence.LocalQuestionEngine
import com.lifeos.app.domain.intelligence.WritingStreak
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.usecase.GetDiaryInsightsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryInsightsViewModelTest {

    private val today = LocalDate.of(2026, 10, 14).toEpochDay()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(dao: InMemoryDiaryDao) = DiaryInsightsViewModel(
        getInsights = GetDiaryInsightsUseCase(DiaryRepository(dao, io = dispatcher), compute = dispatcher) { today },
        today = { today }
    )

    /** Enough entries to clear [com.lifeos.app.domain.intelligence.DiaryAnalyzer.MIN_ENTRIES_FOR_INSIGHTS]. */
    private fun entries(count: Int, mood: String? = null) = (1..count).map {
        diaryEntry(
            id = "id$it",
            content = "Entry number $it with enough words to be counted by the statistics engine",
            mood = mood,
            dateEpochDay = today - (count - it).toLong()
        )
    }

    @Test
    fun `an empty journal shows the onboarding state, not a wall of zeros`() = runTest(dispatcher) {
        val vm = viewModel(InMemoryDiaryDao())
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.insights)
        assertTrue(state.showOnboarding)
        assertFalse("no figures may be shown for a sparse journal", state.showFigures)
    }

    @Test
    fun `an empty journal gets an onboarding question, not a present-tense one`() = runTest(dispatcher) {
        val vm = viewModel(InMemoryDiaryDao())
        advanceUntilIdle()

        // The bug this pins: DiaryInsights.empty used to build its question with
        // DiaryQuestion.forDay(), which greets a user who has never written
        // anything with "what do you remember about today?" — an invitation to
        // reflect on a day they have no material for.
        val question = vm.state.value.insights!!.question
        assertEquals(
            DiaryInsights.empty(today).question.text,
            question.text
        )
        assertEquals(
            LocalQuestionEngine.questionFor(
                com.lifeos.app.domain.intelligence.DiarySnapshot.EMPTY.copy(todayEpochDay = today)
            ).text,
            question.text
        )
    }

    @Test
    fun `a populated journal shows real figures`() = runTest(dispatcher) {
        val happy = DiaryMoods.OPTIONS.first { it.label == "Happy" }.key
        val vm = viewModel(InMemoryDiaryDao(entries(12, mood = happy)))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertFalse(state.showOnboarding)
        assertTrue(state.showFigures)
        assertEquals(12, state.insights!!.statistics.entryCount)
    }

    @Test
    fun `today comes from the injected clock, not the system`() = runTest(dispatcher) {
        val vm = viewModel(InMemoryDiaryDao())
        advanceUntilIdle()

        // The screen marks "today" on the trend chart with this value. If the
        // screen read LocalDate.now() itself it could straddle midnight against
        // the analyzers and mark the wrong day.
        assertEquals(today, vm.state.value.todayEpochDay)
    }

    @Test
    fun `the streak tolerates today being unwritten`() = runTest(dispatcher) {
        // Seven consecutive days ending yesterday, none today.
        val rows = (0..6).map { offset ->
            diaryEntry(
                id = "id$offset",
                content = "wrote something on this day with several words",
                dateEpochDay = today - (offset + 1)
            )
        }
        val vm = viewModel(InMemoryDiaryDao(rows))
        advanceUntilIdle()

        // Reporting zero until tonight would punish the user for a day that has
        // not finished — the worst bug a streak feature can have. `isAlive` is
        // the narrower "did you write *today*", so it is false here and the run
        // still counts.
        assertEquals(7, vm.state.value.streak.currentLength)
        assertFalse("nothing was written today", vm.state.value.streak.isAlive)
    }

    @Test
    fun `the streak dies once a whole day is missed`() = runTest(dispatcher) {
        val rows = (0..5).map { offset ->
            diaryEntry(
                id = "id$offset",
                content = "wrote something on this day with several words",
                dateEpochDay = today - (offset + 2)
            )
        }
        val vm = viewModel(InMemoryDiaryDao(rows))
        advanceUntilIdle()

        // A run that ended two days ago is not a *current* run of six days: it
        // is a finished run of six days. Claiming otherwise is how a streak
        // feature ends up congratulating someone for a streak they broke.
        assertFalse(vm.state.value.streak.isAlive)
        assertEquals(0, vm.state.value.streak.currentLength)
        assertEquals(6, vm.state.value.streak.longestLength)
    }

    @Test
    fun `an empty journal has a zero streak rather than an error`() = runTest(dispatcher) {
        val vm = viewModel(InMemoryDiaryDao())
        advanceUntilIdle()

        assertEquals(WritingStreak(currentLength = 0, longestLength = 0, isAlive = false), vm.state.value.streak)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `reloading recomputes rather than serving a cache`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao(entries(12))
        val vm = viewModel(dao)
        advanceUntilIdle()
        assertEquals(12, vm.state.value.insights!!.statistics.entryCount)

        // A save lands, then the user pulls to refresh.
        dao.upsert(diaryEntry("new", content = "one more entry with words", dateEpochDay = today))
        vm.reload()
        advanceUntilIdle()

        assertEquals(13, vm.state.value.insights!!.statistics.entryCount)
    }

    @Test
    fun `a failure surfaces as a message and not a crash`() = runTest(dispatcher) {
        val broken = object : InMemoryDiaryDao() {
            override suspend fun getAll(): List<com.lifeos.app.data.db.entities.DiaryEntity> =
                throw IllegalStateException("database is gone")
        }
        val vm = DiaryInsightsViewModel(
            getInsights = GetDiaryInsightsUseCase(DiaryRepository(broken, io = dispatcher), compute = dispatcher) { today },
            today = { today }
        )
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.errorMessage)
        assertNull("nothing may be rendered from a failed read", vm.state.value.insights)
        assertFalse(vm.state.value.showOnboarding)
        assertFalse(vm.state.value.showFigures)
    }

    @Test
    fun `attachments are decoded from their json before the engine counts them`() = runTest(dispatcher) {
        // attachmentCount reaches the engine as an Int, so the engine never has
        // to know that attachmentsJson is the data layer's encoding.
        val rows = (0..11).map { offset ->
            diaryEntry(
                id = "id$offset",
                content = "entry $offset with enough words to clear the minimum sample",
                dateEpochDay = today - offset.toLong(),
                attachmentsJson = DiaryAttachments.encode(listOf(DiaryAttachment.Photo("/data/p$offset.jpg")))
            )
        }
        val vm = viewModel(InMemoryDiaryDao(rows))
        advanceUntilIdle()

        // If decode silently produced 0, the attachment figure would be quietly
        // zero forever and nothing else on the screen would look wrong.
        assertEquals(12, vm.state.value.insights!!.statistics.attachmentCount)
    }
}