package com.lifeos.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.PaymentMethod
import com.lifeos.app.data.repository.ExpenseRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * On-device/emulator tests for the queries automatic capture added to
 * [com.lifeos.app.data.db.dao.ExpenseDao].
 *
 * These exist because the dedupe rule is the one place where the *SQL* decides
 * something: a range comparison instead of equality, an existing index instead
 * of a new one. The decision logic around them is covered by ordinary unit tests;
 * this file checks that the database actually answers the question.
 *
 * No schema change was needed for this feature, so there is deliberately no
 * migration here — see [AppDatabaseMigrationTest] for the migrations that do
 * exist.
 *
 * Run with: `./gradlew :app:assembleDebugAndroidTest` and an emulator/device
 * (`./gradlew :app:connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class ExpenseCaptureQueriesTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExpenseRepository

    private val today = 20_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        repository = ExpenseRepository(database.expenseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun existsByIdFindsOnlyTheStoredRow() = runBlockingTest {
        repository.addExpense(349.0, "Food", today, 1300, id = "auto-abc")

        assertTrue(repository.existsById("auto-abc"))
        assertFalse(repository.existsById("auto-def"))
    }

    @Test
    fun sameAmountOnDayFindsTheRowDespiteFloatRepresentation() = runBlockingTest {
        repository.addExpense(0.1 + 0.2, "Other", today, 1300, id = "a")
        repository.addExpense(500.0, "Other", today, 1300, id = "b")
        repository.addExpense(300.0, "Other", today + 1, 1300, id = "c")

        val found = repository.findSameAmountOnDay(today, 0.1 + 0.2)

        assertEquals(listOf("a"), found.map { it.id })
    }

    @Test
    fun sameAmountOnDayIgnoresAmountsOutsideTheHalfPaisaWindow() = runBlockingTest {
        repository.addExpense(349.00, "Other", today, 1300, id = "a")

        assertTrue(repository.findSameAmountOnDay(today, 349.10).isEmpty())
        assertTrue(repository.findSameAmountOnDay(today, 348.90).isEmpty())
        assertEquals(1, repository.findSameAmountOnDay(today, 349.01).size)
    }

    @Test
    fun automaticRowKeepsItsTagThroughASaveAndReload() = runBlockingTest {
        repository.addExpense(
            amount = 349.0, category = "Food", dateEpochDay = today, timeMinutes = 1300,
            merchant = "Swiggy", paymentMethod = PaymentMethod.UPI,
            tags = listOf("auto-captured"), id = "auto-xyz"
        )

        val stored = repository.observeInRange(today, today).first().single()

        assertTrue(stored.tagsCsv.split(',').any { it == "auto-captured" })
        assertEquals("Swiggy", stored.merchant)
        assertEquals(PaymentMethod.UPI, stored.paymentMethod)
    }

    /** Runs the block off the test thread, which is what Room requires. */
    private fun runBlockingTest(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
}