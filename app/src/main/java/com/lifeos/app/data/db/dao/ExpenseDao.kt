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

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE dateEpochDay = :epochDay")
    fun observeTotalForDay(epochDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    fun observeTotalInRange(startEpochDay: Long, endEpochDay: Long): Flow<Double>

    @Query("SELECT * FROM expenses")
    suspend fun getAllForBackup(): List<ExpenseEntity>

    // ---- automatic-capture duplicate detection ----------------------------
    //
    // Two one-row lookups, both served by the existing primary key or the
    // existing `index_expenses_dateEpochDay` index, so no new index and no new
    // column is needed for deduplication.

    /** Primary-key existence check — the exact-repetition backstop. */
    @Query("SELECT EXISTS(SELECT 1 FROM expenses WHERE id = :id)")
    suspend fun existsById(id: String): Boolean

    /**
     * Same-amount candidates for one local day, which is where a repeated
     * notification for a single payment lands. The `amount BETWEEN` half-cent
     * window is what lets a re-notified amount match regardless of whether the
     * value went in as 349.0 or 349.00, without comparing doubles for equality.
     * Deliberately returns whole rows: the merchant/time comparison that decides
     * a real duplicate needs those columns, and the result set is one day.
     */
    @Query(
        "SELECT * FROM expenses " +
            "WHERE dateEpochDay = :epochDay AND amount BETWEEN :lowestAmount AND :highestAmount"
    )
    suspend fun findSameAmountOnDay(epochDay: Long, lowestAmount: Double, highestAmount: Double): List<ExpenseEntity>
}
