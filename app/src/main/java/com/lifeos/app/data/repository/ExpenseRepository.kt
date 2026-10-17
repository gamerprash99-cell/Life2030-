package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.ExpenseDao
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.PaymentMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeForDay(epochDay: Long): Flow<List<ExpenseEntity>> = dao.observeForDay(epochDay)
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>> = dao.observeInRange(startEpochDay, endEpochDay)
    fun observeTotalForDay(epochDay: Long): Flow<Double> = dao.observeTotalForDay(epochDay)
    fun observeTotalInRange(startEpochDay: Long, endEpochDay: Long): Flow<Double> = dao.observeTotalInRange(startEpochDay, endEpochDay)

    /** Efficient inclusive range read used by local reports and statistics. */
    suspend fun getInRange(startEpochDay: Long, endEpochDay: Long): List<ExpenseEntity> =
        dao.observeInRange(minOf(startEpochDay, endEpochDay), maxOf(startEpochDay, endEpochDay)).first()

    /** Local-only search used by LIFE commands; results stay in Room/app memory. */
    suspend fun search(query: String): List<ExpenseEntity> {
        if (query.isBlank()) return emptyList()
        val term = query.trim()
        return dao.getAllForBackup().filter { expense ->
            listOfNotNull(expense.merchant, expense.category, expense.note, expense.tagsCsv)
                .any { it.contains(term, ignoreCase = true) }
        }
    }

    suspend fun addExpense(
        amount: Double,
        category: String,
        dateEpochDay: Long,
        timeMinutes: Int,
        merchant: String? = null,
        paymentMethod: PaymentMethod = PaymentMethod.OTHER,
        note: String? = null,
        tags: List<String> = emptyList()
    ): String {
        val id = IdGenerator.newId()
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

}
