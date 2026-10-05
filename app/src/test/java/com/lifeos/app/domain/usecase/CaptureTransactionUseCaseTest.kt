package com.lifeos.app.domain.usecase

import com.lifeos.app.data.db.dao.ExpenseDao
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.domain.model.AUTO_CAPTURED_TAG
import com.lifeos.app.domain.model.ExpenseCategories
import com.lifeos.app.domain.model.isAutomaticallyCaptured
import com.lifeos.app.domain.model.TransactionCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The rules that decide whether a notification becomes an expense, and how often.
 *
 * Exercised against a fake DAO rather than a database so the *decisions* are what
 * is under test; the SQL itself is Room's concern and is covered by the
 * instrumentation tests.
 */
class CaptureTransactionUseCaseTest {

    private val dao = FakeExpenseDao()
    private val repository = ExpenseRepository(dao)
    private val useCase = CaptureTransactionUseCase(repository)

    // ---- the write happens only for high-confidence spending ----------------

    @Test
    fun `a high confidence upi payment becomes an expense`() = runTest {
        val outcome = capture(
            body = "₹349 paid to Swiggy via UPI",
            postedAt = at(21, 40)
        )

        assertTrue(outcome is CaptureOutcome.Created)
        val saved = dao.single()
        assertEquals(349.0, saved.amount, TOLERANCE)
        assertEquals("Swiggy", saved.merchant)
        assertEquals("Food", saved.category)
        assertEquals(at(21, 40).let { epochDayOf(it) }, saved.dateEpochDay)
        assertEquals(21 * 60 + 40, saved.timeMinutes)
        assertTrue(saved.isAutomaticallyCaptured())
        assertNull(saved.note)
        assertEquals(AUTO_CAPTURED_TAG, saved.tagsCsv)
    }

    @Test
    fun `the row id is derived from the transaction and prefixed`() = runTest {
        val outcome = capture(body = "₹349 paid to Swiggy via UPI", postedAt = at(21, 40))
        val id = (outcome as CaptureOutcome.Created).expenseId

        assertTrue(id.startsWith(CaptureTransactionUseCase.AUTO_CAPTURED_ID_PREFIX))
        assertEquals(id, dao.single().id)
    }

    @Test
    fun `a medium confidence reading is left to the user`() = runTest {
        // Payment plus balance: one of the two numbers is not the payment.
        val outcome = capture(body = "₹349 paid to Swiggy. Avl Bal ₹12,430", postedAt = at(21, 40))

        assertEquals(CaptureOutcome.Skipped(SkipReason.BELOW_AUTO_CONFIDENCE), outcome)
        assertEquals(0, dao.rows.size)
    }

    @Test
    fun `a low confidence bare amount is left to the user`() = runTest {
        val outcome = capture(body = "Debited 349 from your account", postedAt = at(21, 40))

        assertEquals(CaptureOutcome.Skipped(SkipReason.BELOW_AUTO_CONFIDENCE), outcome)
        assertEquals(0, dao.rows.size)
    }

    @Test
    fun `income is never filed as spending`() = runTest {
        val outcome = capture(body = "₹5,000 credited to your HDFC card ending 4321", postedAt = at(9, 0))

        assertEquals(CaptureOutcome.Skipped(SkipReason.NOT_AN_EXPENSE), outcome)
        assertEquals(0, dao.rows.size)
    }

    @Test
    fun `a refund is never filed as spending`() = runTest {
        val outcome = capture(body = "₹500 refunded to your account", postedAt = at(9, 0))

        assertEquals(CaptureOutcome.Skipped(SkipReason.NOT_AN_EXPENSE), outcome)
    }

    @Test
    fun `an atm withdrawal is never filed as spending`() = runTest {
        val outcome = capture(body = "₹2,000 withdrawn from ATM", postedAt = at(14, 0))

        assertEquals(CaptureOutcome.Skipped(SkipReason.NOT_A_TRANSACTION), outcome)
        assertEquals(0, dao.rows.size)
    }

    @Test
    fun `a promotional notification is never filed`() = runTest {
        val outcome = capture(body = "Flat ₹500 off your next order this weekend", postedAt = at(14, 0))

        assertEquals(CaptureOutcome.Skipped(SkipReason.NOT_A_TRANSACTION), outcome)
    }

    @Test
    fun `an implausibly old timestamp is refused`() = runTest {
        val replayed = at(21, 40) - CaptureTransactionUseCase.MAX_PLAUSIBLE_AGE_MILLIS - 60_000
        val outcome = capture(body = "₹349 paid to Swiggy via UPI", postedAt = replayed, nowMillis = at(21, 40))

        assertEquals(CaptureOutcome.Skipped(SkipReason.IMPLAUSIBLE_TIMESTAMP), outcome)
        assertEquals(0, dao.rows.size)
    }

    // ---- deduplication ------------------------------------------------------

    @Test
    fun `the same notification twice records one expense`() = runTest {
        val first = capture(body = "₹349 paid to Swiggy via UPI", postedAt = at(21, 40))
        val second = capture(body = "₹349 paid to Swiggy via UPI", postedAt = at(21, 40))

        assertTrue(first is CaptureOutcome.Created)
        assertEquals(CaptureOutcome.Duplicate, second)
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `a second notification for the same payment in different words is one expense`() = runTest {
        // The shape real banks produce: a confirmation, then a debit notice.
        capture(body = "₹349 paid to Swiggy via UPI", postedAt = at(21, 40))
        val second = capture(body = "₹349 debited for Swiggy", postedAt = at(21, 41))

        assertEquals(CaptureOutcome.Duplicate, second)
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `two equal purchases minutes apart are two expenses`() = runTest {
        capture(body = "₹349 paid to Swiggy via UPI", postedAt = at(21, 40))
        val second = capture(body = "₹349 paid to Swiggy via UPI", postedAt = at(21, 45))

        assertTrue(second is CaptureOutcome.Created)
        assertEquals(2, dao.rows.size)
    }

    @Test
    fun `equal amounts at different merchants are separate expenses`() = runTest {
        capture(body = "₹120 paid to Starbucks via UPI", postedAt = at(21, 40))
        val second = capture(body = "₹120 paid to Croma via UPI", postedAt = at(21, 41))

        assertTrue(second is CaptureOutcome.Created)
        assertEquals(2, dao.rows.size)
    }

    @Test
    fun `merchant-less amounts two minutes apart are not merged`() = runTest {
        capture(body = "₹349 debited via UPI", postedAt = at(21, 40))
        val second = capture(body = "₹349 debited via UPI", postedAt = at(21, 42))

        assertTrue(second is CaptureOutcome.Created)
        assertEquals(2, dao.rows.size)
    }

    @Test
    fun `same amount, same minute and no merchant counts as one payment`() = runTest {
        capture(body = "₹349 debited via UPI. UTR: 111111111", postedAt = at(21, 40))
        val second = capture(body = "₹349 debited via UPI. UTR: 222222222", postedAt = at(21, 40))

        assertEquals(CaptureOutcome.Duplicate, second)
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `the raw reference never reaches the database`() = runTest {
        capture(body = "₹349 paid to Swiggy via UPI. UTR: 5123456789", postedAt = at(21, 40))

        val saved = dao.single()
        assertFalse(saved.id.contains("5123456789"))
        assertFalse(saved.tagsCsv.contains("5123456789"))
        assertNull(saved.note)
    }

    // ---- categorisation ----------------------------------------------------

    @Test
    fun `an unknown merchant lands in Other rather than inventing a category`() = runTest {
        capture(body = "₹349 paid to Kettle & Kettle Enterprises via UPI", postedAt = at(21, 40))

        assertEquals(ExpenseCategories.DEFAULT, dao.single().category)
        assertTrue(ExpenseCategories.isKnown(dao.single().category))
    }

    @Test
    fun `a missing merchant still produces an expense`() = runTest {
        val outcome = capture(body = "₹349 debited via UPI", postedAt = at(21, 40))

        assertTrue(outcome is CaptureOutcome.Created)
        val saved = dao.single()
        assertNull(saved.merchant)
        assertEquals(ExpenseCategories.DEFAULT, saved.category)
        assertTrue(saved.isAutomaticallyCaptured())
    }

    // ---- helpers ------------------------------------------------------------

    /**
     * [nowMillis] is separate from the notification's post time so a stale
     * notification can be replayed without the test having to move the clock.
     */
    private suspend fun capture(body: String, postedAt: Long, nowMillis: Long = postedAt): CaptureOutcome =
        useCase(
            TransactionCandidate(title = "Bank", body = body, postedAtEpochMillis = postedAt),
            nowMillis
        )

    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(LocalDate.of(2026, 3, 14), LocalTime.of(hour, minute))
            .atZone(com.lifeos.app.core.util.DateTimeUtils.zoneId())
            .toInstant()
            .toEpochMilli()

    private fun epochDayOf(epochMillis: Long): Long =
        java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(com.lifeos.app.core.util.DateTimeUtils.zoneId())
            .toLocalDate()
            .toEpochDay()

    private companion object {
        const val TOLERANCE = 0.0001
    }
}

/** In-memory stand-in for [ExpenseDao]; only the queries the use case makes. */
private class FakeExpenseDao : ExpenseDao {
    val rows = mutableListOf<ExpenseEntity>()

    fun single(): ExpenseEntity {
        assertEquals(1, rows.size)
        return rows.single()
    }

    override suspend fun upsert(expense: ExpenseEntity) {
        rows.removeAll { it.id == expense.id }
        rows += expense
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }

    override fun observeForDay(epochDay: Long) = throw UnsupportedOperationException()

    override fun observeInRange(startEpochDay: Long, endEpochDay: Long) = throw UnsupportedOperationException()

    override fun observeTotalForDay(epochDay: Long) = throw UnsupportedOperationException()

    override fun observeTotalInRange(startEpochDay: Long, endEpochDay: Long) = throw UnsupportedOperationException()

    override suspend fun getAllForBackup(): List<ExpenseEntity> = rows

    override suspend fun existsById(id: String): Boolean = rows.any { it.id == id }

    override suspend fun findSameAmountOnDay(
        epochDay: Long,
        lowestAmount: Double,
        highestAmount: Double
    ): List<ExpenseEntity> = rows.filter { it.dateEpochDay == epochDay && it.amount in lowestAmount..highestAmount }
}