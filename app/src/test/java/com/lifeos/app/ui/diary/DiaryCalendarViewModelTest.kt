package com.lifeos.app.ui.diary

import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.model.MoodValence
import com.lifeos.app.domain.usecase.GetDiaryCalendarUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryCalendarViewModelTest {

    private val today = LocalDate.of(2026, 10, 14)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun state(month: YearMonth, todayEpochDay: Long = today.toEpochDay()) = DiaryCalendarState(
        year = month.year,
        month = month.monthValue,
        todayEpochDay = todayEpochDay
    )

    // ---- grid alignment --------------------------------------------------
    //
    // A one-day grid offset is invisible in a screenshot: every entry looks like
    // it is one day early and nothing appears broken. It is also wrong for every
    // user, every month, so it gets pinned here.

    @Test
    fun `the first of the month lands under its own weekday`() {
        val firstCell = state(YearMonth.of(2026, 10)).gridCells.first { it != null }

        assertEquals(
            "the 1st must sit under its real weekday",
            LocalDate.of(2026, 10, 1).toEpochDay(),
            firstCell
        )
    }

    @Test
    fun `the grid is Monday-first`() {
        val cells = state(YearMonth.of(2026, 10)).gridCells

        // 2026-10-01 is a Thursday: three Monday-first leading blanks. Getting the
        // leading-offset formula's convention wrong is the actual bug here — the
        // header row and the cells would disagree while every number stayed valid.
        assertEquals(3, cells.takeWhile { it == null }.size)
        assertEquals(DayOfWeek.THURSDAY, LocalDate.ofEpochDay(cells.first { it != null }!!).dayOfWeek)
    }

    @Test
    fun `a month starting on Monday has no leading blank`() {
        // 2026-06-01 is a Monday.
        assertEquals(0, state(YearMonth.of(2026, 6)).gridCells.takeWhile { it == null }.size)
    }

    @Test
    fun `a month starting on Sunday has six leading blanks`() {
        // 2026-02-01 is a Sunday — the worst case for a Monday-first offset.
        assertEquals(6, state(YearMonth.of(2026, 2)).gridCells.takeWhile { it == null }.size)
    }

    @Test
    fun `the grid is six whole rows and only ever this month's days`() {
        val cells = state(YearMonth.of(2026, 10)).gridCells

        assertEquals(42, cells.size)
        // Every row is seven cells, or the columns drift between weeks.
        assertEquals(List(6) { 7 }, cells.chunked(7).map { it.size })

        val days = cells.filterNotNull()
        assertEquals(31, days.size)
        assertTrue(days.all { LocalDate.ofEpochDay(it).monthValue == 10 && LocalDate.ofEpochDay(it).year == 2026 })
    }

    @Test
    fun `padded cells are null rather than neighbouring-month dates`() {
        val cells = state(YearMonth.of(2026, 2)).gridCells
        val days = cells.filterNotNull()

        // 2026 is not a leap year, so February ends on the 28th. The trailing
        // blanks must stay null: rendering them as greyed 1st/2nd of March would
        // put a tappable-looking date from another month under this one.
        assertEquals(28, days.size)
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), days.last())
    }

    @Test
    fun `a leap february keeps its extra day`() {
        assertEquals(29, state(YearMonth.of(2028, 2)).gridCells.filterNotNull().size)
    }

    @Test
    fun `the month label names the visible month`() {
        assertEquals("October", state(YearMonth.of(2026, 10)).monthLabel)
        assertEquals("February", state(YearMonth.of(2028, 2)).monthLabel)
    }

    // ---- month reads -----------------------------------------------------

    @Test
    fun `only the requested month is read`() = runTest(dispatcher) {
        val inOctober = LocalDate.of(2026, 10, 3).toEpochDay()
        val inSeptember = LocalDate.of(2026, 9, 30).toEpochDay()
        val inNovember = LocalDate.of(2026, 11, 1).toEpochDay()
        val dao = InMemoryDiaryDao(
            listOf(
                diaryEntry("oct", dateEpochDay = inOctober),
                diaryEntry("sep", dateEpochDay = inSeptember),
                diaryEntry("nov", dateEpochDay = inNovember)
            )
        )

        val month = GetDiaryCalendarUseCase(DiaryRepository(dao, io = dispatcher))(2026, 10)

        assertEquals(listOf("oct"), month.entries.map { it.id })
        assertEquals(setOf(inOctober), month.daysWithEntries)
        assertEquals(1, month.entryCountFor(inOctober))
        assertEquals(0, month.entryCountFor(inSeptember))
    }

    @Test
    fun `a month read asks the database for that month's own bounds`() = runTest(dispatcher) {
        val dao = InMemoryDiaryDao()
        GetDiaryCalendarUseCase(DiaryRepository(dao, io = dispatcher))(2026, 2)

        val expected = YearMonth.of(2026, 2)
        assertEquals(
            listOf(expected.atDay(1).toEpochDay() to expected.atEndOfMonth().toEpochDay()),
            dao.rangeQueries
        )
        // Built from the month's bounds, so the grid's cost does not grow with
        // how many years of journal the user has.
        assertEquals(1, dao.rangeQueries.size)
    }

    @Test
    fun `selection starts on today, not the first of the month`() = runTest(dispatcher) {
        val vm = DiaryCalendarViewModel(GetDiaryCalendarUseCase(DiaryRepository(InMemoryDiaryDao(), io = dispatcher))) { today.toEpochDay() }
        advanceUntilIdle()

        // Someone opening the calendar on the 14th is asking about now. Starting
        // them on the 1st shows an empty day panel and reads as an empty journal.
        assertEquals(today.toEpochDay(), vm.state.value.selectedDay)
        assertEquals(10, vm.state.value.month)
    }

    @Test
    fun `selecting a day shows that day's entries`() = runTest(dispatcher) {
        val day3 = LocalDate.of(2026, 10, 3).toEpochDay()
        val day9 = LocalDate.of(2026, 10, 9).toEpochDay()
        val dao = InMemoryDiaryDao(
            listOf(
                diaryEntry("a", content = "third", dateEpochDay = day3, timeMinutes = 540),
                diaryEntry("b", content = "also third", dateEpochDay = day3, timeMinutes = 900),
                diaryEntry("c", content = "ninth", dateEpochDay = day9)
            )
        )
        val vm = DiaryCalendarViewModel(GetDiaryCalendarUseCase(DiaryRepository(dao, io = dispatcher))) { today.toEpochDay() }
        advanceUntilIdle()

        vm.selectDay(day3)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.entriesForSelectedDay.size)
        assertEquals(2, vm.state.value.selectedDayCount)
        // Newest first, matching the order the diary list itself uses.
        assertEquals(listOf(900, 540), vm.state.value.entriesForSelectedDay.map { it.timeMinutes })
    }

    @Test
    fun `days with entries are deduplicated`() = runTest(dispatcher) {
        val day3 = LocalDate.of(2026, 10, 3).toEpochDay()
        val dao = InMemoryDiaryDao(
            listOf(
                diaryEntry("a", dateEpochDay = day3),
                diaryEntry("b", dateEpochDay = day3),
                diaryEntry("c", dateEpochDay = day3)
            )
        )
        val vm = DiaryCalendarViewModel(GetDiaryCalendarUseCase(DiaryRepository(dao, io = dispatcher))) { today.toEpochDay() }
        advanceUntilIdle()

        // Three entries on one day is one dot. The day grid has room for exactly
        // one marker, so a repeated key here would collide on the same cell.
        assertEquals(setOf(day3), vm.state.value.daysWithEntries)
    }

    // ---- month paging ----------------------------------------------------

    @Test
    fun `next month is refused at the current month`() = runTest(dispatcher) {
        val vm = DiaryCalendarViewModel(
            GetDiaryCalendarUseCase(DiaryRepository(InMemoryDiaryDao(), io = dispatcher)),
            today = { today.toEpochDay() }
        )
        advanceUntilIdle()

        vm.nextMonth()
        advanceUntilIdle()

        // The composer clamps a memory's date to the past, so a future month can
        // only ever be empty. Offering it would be a screen that shows nothing.
        assertEquals(10, vm.state.value.month)
        assertTrue(vm.state.value.isCurrentMonth)
    }

    @Test
    fun `next month is allowed once past the current month`() = runTest(dispatcher) {
        val vm = DiaryCalendarViewModel(
            GetDiaryCalendarUseCase(DiaryRepository(InMemoryDiaryDao(), io = dispatcher)),
            today = { today.toEpochDay() }
        )
        advanceUntilIdle()

        vm.previousMonth()
        advanceUntilIdle()
        vm.nextMonth()
        advanceUntilIdle()

        assertEquals(10, vm.state.value.month)
    }

    @Test
    fun `paging crosses a year boundary`() = runTest(dispatcher) {
        // January, so stepping back lands in the previous year.
        val january = LocalDate.of(2027, 1, 10)
        val vm = DiaryCalendarViewModel(
            GetDiaryCalendarUseCase(DiaryRepository(InMemoryDiaryDao(), io = dispatcher)),
            today = { january.toEpochDay() }
        )
        advanceUntilIdle()

        vm.previousMonth()
        advanceUntilIdle()

        assertEquals(12, vm.state.value.month)
        assertEquals(2026, vm.state.value.year)
    }

    @Test
    fun `paging forward stops at the current month`() = runTest(dispatcher) {
        val january = LocalDate.of(2027, 1, 10)
        val vm = DiaryCalendarViewModel(
            GetDiaryCalendarUseCase(DiaryRepository(InMemoryDiaryDao(), io = dispatcher)),
            today = { january.toEpochDay() }
        )
        advanceUntilIdle()

        // January 2027 *is* the current month, so forward is already at its
        // limit and pressing it changes nothing. That is the guard working, not
        // a stuck button — the user has to go back before there is anywhere to
        // go.
        repeat(3) { vm.nextMonth() }
        advanceUntilIdle()
        assertEquals(1, vm.state.value.month)

        vm.previousMonth()
        advanceUntilIdle()
        assertEquals(12, vm.state.value.month)

        // From December, one step forward lands on the current month and further
        // presses are ignored rather than running off into empty future months.
        repeat(4) { vm.nextMonth() }
        advanceUntilIdle()

        assertEquals("stopped at the current month", 1, vm.state.value.month)
        assertEquals(2027, vm.state.value.year)
    }

    @Test
    fun `jumping to today returns to the current month and clears the selection`() = runTest(dispatcher) {
        val vm = DiaryCalendarViewModel(
            GetDiaryCalendarUseCase(DiaryRepository(InMemoryDiaryDao(), io = dispatcher)),
            today = { today.toEpochDay() }
        )
        advanceUntilIdle()
        vm.previousMonth()
        vm.selectDay(LocalDate.of(2026, 9, 3).toEpochDay())
        advanceUntilIdle()
        assertEquals(9, vm.state.value.month)

        vm.jumpToToday()
        advanceUntilIdle()

        assertEquals(10, vm.state.value.month)
        assertEquals(today.toEpochDay(), vm.state.value.selectedDay)
    }

    // ---- day dots --------------------------------------------------------

    @Test
    fun `a day's dot takes its dominant mood`() = runTest(dispatcher) {
        val day = LocalDate.of(2026, 10, 3).toEpochDay()
        val happy = DiaryMoods.OPTIONS.first { it.label == "Happy" }.key
        val sad = DiaryMoods.OPTIONS.first { it.label == "Sad" }.key
        val dao = InMemoryDiaryDao(
            listOf(
                diaryEntry("1", mood = happy, dateEpochDay = day),
                diaryEntry("2", mood = happy, dateEpochDay = day),
                diaryEntry("3", mood = sad, dateEpochDay = day)
            )
        )
        val vm = DiaryCalendarViewModel(GetDiaryCalendarUseCase(DiaryRepository(dao, io = dispatcher))) { today.toEpochDay() }
        advanceUntilIdle()

        assertEquals(happy, vm.state.value.moodByDay[day])
    }

    @Test
    fun `a tie between moods breaks toward the positive reading`() = runTest(dispatcher) {
        // Matches MoodAnalyzer.dominantMoodByDay, so the calendar's dot and the
        // insights chart cannot disagree about the same day.
        val day = LocalDate.of(2026, 10, 3).toEpochDay()
        val happy = DiaryMoods.OPTIONS.first { it.label == "Happy" }.key
        val sad = DiaryMoods.OPTIONS.first { it.label == "Sad" }.key
        val dao = InMemoryDiaryDao(
            listOf(
                diaryEntry("1", mood = happy, dateEpochDay = day),
                diaryEntry("2", mood = sad, dateEpochDay = day)
            )
        )
        val vm = DiaryCalendarViewModel(GetDiaryCalendarUseCase(DiaryRepository(dao, io = dispatcher))) { today.toEpochDay() }
        advanceUntilIdle()

        assertEquals(happy, vm.state.value.moodByDay[day])
        assertEquals(MoodValence.POSITIVE, DiaryMoods.valenceOf(vm.state.value.moodByDay[day]))
    }

    @Test
    fun `a day with no mood gets no dot colour`() = runTest(dispatcher) {
        val day = LocalDate.of(2026, 10, 3).toEpochDay()
        val dao = InMemoryDiaryDao(
            listOf(
                diaryEntry("1", dateEpochDay = day),
                diaryEntry("2", mood = "", dateEpochDay = day)
            )
        )
        val vm = DiaryCalendarViewModel(GetDiaryCalendarUseCase(DiaryRepository(dao, io = dispatcher))) { today.toEpochDay() }
        advanceUntilIdle()

        // The day still counts as "has entries" — it is only the colour that is
        // absent. Blank is not neutral, and it must not be counted as neutral.
        assertTrue(vm.state.value.daysWithEntries.contains(day))
        assertNull(vm.state.value.moodByDay[day])
    }
}