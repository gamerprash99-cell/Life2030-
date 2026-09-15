package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.CategoryTotal
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

    suspend fun getCategoryTotals(startEpochDay: Long, endEpochDay: Long): List<CategoryTotal> =
        dao.getCategoryTotals(startEpochDay, endEpochDay)

    suspend fun search(query: String): List<ExpenseEntity> {
        if (query.isBlank()) return emptyList()
        return dao.search(query)
    }

    suspend fun getInRange(startEpochDay: Long, endEpochDay: Long): List<ExpenseEntity> = dao.getInRange(startEpochDay, endEpochDay)

    suspend fun getAllForBackup(): List<ExpenseEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(expenses: List<ExpenseEntity>) = expenses.forEach { dao.upsert(it) }
}
