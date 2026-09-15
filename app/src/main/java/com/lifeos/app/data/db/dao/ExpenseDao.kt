package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM expenses WHERE dateEpochDay = :epochDay ORDER BY timeMinutes DESC")
    fun observeForDay(epochDay: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay DESC, timeMinutes DESC")
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun getInRange(startEpochDay: Long, endEpochDay: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE dateEpochDay = :epochDay")
    fun observeTotalForDay(epochDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    fun observeTotalInRange(startEpochDay: Long, endEpochDay: Long): Flow<Double>

    @Query("""
        SELECT category, COALESCE(SUM(amount), 0) as total FROM expenses
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY category ORDER BY total DESC
    """)
    suspend fun getCategoryTotals(startEpochDay: Long, endEpochDay: Long): List<CategoryTotal>

    @Query("SELECT * FROM expenses WHERE merchant LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAllForBackup(): List<ExpenseEntity>
}

data class CategoryTotal(val category: String, val total: Double)
