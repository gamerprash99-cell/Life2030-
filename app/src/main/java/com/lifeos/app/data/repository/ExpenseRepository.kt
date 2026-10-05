package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.ExpenseDao
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.PaymentMethod
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeForDay(epochDay: Long): Flow<List<ExpenseEntity>> = dao.observeForDay(epochDay)
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>> = dao.observeInRange(startEpochDay, endEpochDay)
    fun observeTotalForDay(epochDay: Long): Flow<Double> = dao.observeTotalForDay(epochDay)
    fun observeTotalInRange(startEpochDay: Long, endEpochDay: Long): Flow<Double> = dao.observeTotalInRange(startEpochDay, endEpochDay)

    suspend fun addExpense(
        amount: Double,
        category: String,
        dateEpochDay: Long,
        timeMinutes: Int,
        merchant: String? = null,
        paymentMethod: PaymentMethod = PaymentMethod.OTHER,
        note: String? = null,
        tags: List<String> = emptyList(),
        /**
         * Row id to write. Automatic capture supplies a deterministic id derived
         * from the transaction itself, so the same payment arriving twice as two
         * notifications resolves to one row through the primary key instead of
         * needing a separate lookup table. Defaults to a fresh UUID, which is
         * what every manual entry gets.
         */
        id: String = IdGenerator.newId()
    ): String {
        dao.upsert(
            ExpenseEntity(
                id = id, amount = amount, category = category, dateEpochDay = dateEpochDay,
                timeMinutes = timeMinutes, merchant = merchant, paymentMethod = paymentMethod,
                note = note, tagsCsv = tags.joinToString(","), createdAt = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun getAllForBackup(): List<ExpenseEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(expenses: List<ExpenseEntity>) = expenses.forEach { dao.upsert(it) }

    suspend fun existsById(id: String): Boolean = dao.existsById(id)

    /**
     * Rows recorded on [epochDay] whose amount is within half a cent of
     * [amount] — the candidate set a duplicate check reasons about. See
     * [ExpenseDao.findSameAmountOnDay] for why the comparison is a range rather
     * than equality.
     */
    suspend fun findSameAmountOnDay(epochDay: Long, amount: Double): List<ExpenseEntity> =
        dao.findSameAmountOnDay(epochDay, amount - AMOUNT_MATCH_TOLERANCE, amount + AMOUNT_MATCH_TOLERANCE)

    companion object {
        /** Half a cent: enough to cover float representation, too small to merge real amounts. */
        const val AMOUNT_MATCH_TOLERANCE = 0.005
    }
}
