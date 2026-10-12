package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.DiaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DiaryEntity)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM diary_entries ORDER BY dateEpochDay DESC, timeMinutes DESC")
    fun observeAll(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary_entries WHERE dateEpochDay = :epochDay ORDER BY timeMinutes DESC")
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DiaryEntity?

    @Query("SELECT * FROM diary_entries WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<DiaryEntity?>

    @Query("SELECT * FROM diary_entries")
    suspend fun getAllForBackup(): List<DiaryEntity>

    /**
     * Whole journal, for the intelligence layer.
     *
     * Separate from [getAllForBackup] even though both are `SELECT *`, because
     * they answer different questions and will diverge: backup needs fidelity
     * and must never reorder or filter, while analysis needs a plain reading
     * surface it can page through and reduce. Sharing one query would couple
     * the two contracts for no saving — this is a personal journal, and the row
     * count is small enough that a full read is the right tool either way.
     */
    @Query("SELECT * FROM diary_entries")
    suspend fun getAll(): List<DiaryEntity>

    /**
     * Entries between two epoch days inclusive, for the calendar month grid.
     *
     * Inclusive on both ends so a single-day range returns that one day. The
     * range is always supplied by the caller and is clamped to the journal's
     * own bounds — it is never concatenated into the SQL, so it cannot be used
     * to read outside the table.
     */
    @Query(
        "SELECT * FROM diary_entries " +
            "WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay " +
            "ORDER BY dateEpochDay DESC, timeMinutes DESC"
    )
    suspend fun getAllInRange(fromEpochDay: Long, toEpochDay: Long): List<DiaryEntity>

    /**
     * Full-text-ish search across title, content and tags.
     *
     * Deliberately `LIKE`, not FTS. FTS needs a schema migration, a
     * shadow table kept in sync by triggers, and its own query syntax; for a
     * personal journal's row count a scan is imperceptible, and `LIKE` keeps
     * the database at version 5 with no data migration at all.
     *
     * The three `LIKE` metacharacters are escaped by [DiaryRepository] and the
     * `ESCAPE '\'` clause makes that escaping real — without it, searching for
     * `50%` or `a_b` would match almost everything instead of the literal text.
     *
     * [moodKey] null means "any mood", expressed as `:moodKey IS NULL` so the
     * filter is part of the single prepared statement rather than a second,
     * differently-shaped query.
     *
     * Ordering is newest-first, which is also the order the user expects to
     * read their own journal back in. There is no relevance ranking: SQLite's
     * `LIKE` gives no positional information, and inventing a score would imply
     * a precision the query does not have.
     */
    @Query(
        "SELECT * FROM diary_entries " +
            "WHERE (title LIKE :pattern ESCAPE '\\' " +
            "    OR content LIKE :pattern ESCAPE '\\' " +
            "    OR tagsCsv LIKE :pattern ESCAPE '\\') " +
            "AND (:moodKey IS NULL OR mood = :moodKey) " +
            "ORDER BY dateEpochDay DESC, timeMinutes DESC"
    )
    suspend fun search(pattern: String, moodKey: String?): List<DiaryEntity>
}
