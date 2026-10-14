package com.lifeos.app.data.repository

import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The Diary repository's two jobs: keep a row and its files in agreement, and
 * never do blocking storage work on the thread that called in.
 *
 * These use *real* files in a temp directory rather than a mocked `MediaStorage`
 * because the behaviour under test is precisely "does the user's photo actually
 * get unlinked" — a mock would only prove the mock was called.
 */
class DiaryRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val dao = RecordingDiaryDao()
    private val repository = DiaryRepository(dao)

    /** One folder per test, created lazily; `newFolder` refuses to reuse a name. */
    private val mediaDir: File by lazy { temp.newFolder("media") }

    private fun mediaFile(name: String): File =
        File(mediaDir, name).apply { writeText("bytes of $name") }

    private fun entry(
        id: String = "e1",
        attachments: List<DiaryAttachment> = emptyList()
    ) = DiaryEntity(
        id = id,
        content = "content of $id",
        dateEpochDay = 20_000L,
        timeMinutes = 480,
        attachmentsJson = DiaryAttachments.encode(attachments),
        createdAt = 0L,
        updatedAt = 0L
    )

    // ---- the row and its files must not drift apart ------------------------

    @Test
    fun `deleting an entry also deletes the media it owns`() = runTest {
        val photo = mediaFile("photo.jpg")
        val audio = mediaFile("take.m4a")
        dao.seed(
            entry(
                attachments = listOf(
                    DiaryAttachment.Photo(photo.absolutePath),
                    DiaryAttachment.VoiceNote(audio.absolutePath, durationMillis = 1_234L),
                    DiaryAttachment.Place(latitude = 1.0, longitude = 2.0, placeName = "somewhere")
                )
            )
        )

        repository.delete("e1")

        assertTrue("the row must be gone", dao.saved.none { it.id == "e1" })
        assertFalse("the photo the user deleted must be unlinked", photo.exists())
        assertFalse("the voice note must be unlinked", audio.exists())
    }

    @Test
    fun `deleting an entry never touches another entry's media`() = runTest {
        val mine = mediaFile("mine.jpg")
        val theirs = mediaFile("theirs.jpg")
        dao.seed(
            entry("e1", listOf(DiaryAttachment.Photo(mine.absolutePath))),
            entry("e2", listOf(DiaryAttachment.Photo(theirs.absolutePath)))
        )

        repository.delete("e1")

        assertTrue("a sibling entry's photo must survive", theirs.exists())
    }

    @Test
    fun `replacing attachments reclaims what the user removed and keeps the rest`() = runTest {
        val kept = mediaFile("kept.jpg")
        val dropped = mediaFile("dropped.jpg")
        dao.seed(
            entry(
                attachments = listOf(
                    DiaryAttachment.Photo(kept.absolutePath),
                    DiaryAttachment.Photo(dropped.absolutePath)
                )
            )
        )

        repository.updateEntry(
            id = "e1",
            title = null,
            content = "edited",
            mood = null,
            tags = emptyList(),
            attachments = listOf(DiaryAttachment.Photo(kept.absolutePath))
        )

        assertTrue("a kept photo must not be unlinked", kept.exists())
        assertFalse("a removed photo must be unlinked, not orphaned", dropped.exists())
        val stored = DiaryAttachments.decode(dao.saved.single().attachmentsJson)
        assertEquals(1, DiaryAttachments.photos(stored).size)
    }

    @Test
    fun `removing a photo in place unlinks it and leaves the rest of the entry alone`() = runTest {
        val kept = mediaFile("kept.jpg")
        val dropped = mediaFile("dropped.jpg")
        dao.seed(
            entry(
                attachments = listOf(
                    DiaryAttachment.Photo(kept.absolutePath),
                    DiaryAttachment.Photo(dropped.absolutePath)
                )
            )
        )

        repository.removeAttachment("e1", dropped.absolutePath)

        assertTrue(kept.exists())
        assertFalse(dropped.exists())
        val stored = DiaryAttachments.decode(dao.saved.single().attachmentsJson)
        assertEquals(listOf(kept.absolutePath), DiaryAttachments.photos(stored).map { it.filePath })
    }

    @Test
    fun `a stale tap on a path the entry does not hold changes nothing`() = runTest {
        val mine = mediaFile("mine.jpg")
        val stranger = mediaFile("stranger.jpg")
        dao.seed(entry(attachments = listOf(DiaryAttachment.Photo(mine.absolutePath))))
        val before = dao.saved.single().attachmentsJson

        repository.removeAttachment("e1", stranger.absolutePath)

        assertEquals("the row must be untouched", before, dao.saved.single().attachmentsJson)
        assertTrue("a file this entry does not own must not be unlinked", stranger.exists())
    }

    @Test
    fun `a media file that is already gone does not fail the delete`() = runTest {
        val photo = mediaFile("photo.jpg")
        dao.seed(entry(attachments = listOf(DiaryAttachment.Photo(photo.absolutePath))))
        assertTrue(photo.delete())

        repository.delete("e1")

        assertTrue("the row must still be deleted", dao.saved.isEmpty())
    }

    // ---- threading ---------------------------------------------------------
    //
    // Every caller is a `viewModelScope` (Main). Room moves its *own* suspend
    // calls onto its query executor, but the JSON decode and the blocking
    // `File.delete()` around them do not — so without an explicit switch these
    // unlink a user's photo on the UI thread.

    @Test
    fun `deleting an entry does no storage work on the calling thread`() = runTest {
        val photo = mediaFile("photo.jpg")
        dao.seed(entry(attachments = listOf(DiaryAttachment.Photo(photo.absolutePath))))
        val callerThread = Thread.currentThread().name

        repository.delete("e1")

        assertNotEquals(
            "the delete must not run on the calling (UI) thread",
            callerThread,
            dao.threadNames.firstOrNull()
        )
    }

    @Test
    fun `writing an entry does no storage work on the calling thread`() = runTest {
        val callerThread = Thread.currentThread().name

        repository.createEntry(
            title = null,
            content = "a memory",
            mood = "happy",
            tags = listOf("one"),
            dateEpochDay = 20_000L,
            timeMinutes = 480,
            attachments = listOf(DiaryAttachment.Place(1.0, 2.0, "somewhere"))
        )

        assertNotEquals(callerThread, dao.threadNames.firstOrNull())
        assertEquals(1, dao.saved.size)
    }

    @Test
    fun `the write is complete and durable by the time the call returns`() = runTest {
        val id = repository.createEntry(
            title = null,
            content = "a memory",
            mood = "happy",
            tags = emptyList(),
            dateEpochDay = 20_000L,
            timeMinutes = 480
        )

        // Read straight back with no scheduler pump: if the repository returned
        // before its own write landed, this would still be empty.
        assertEquals("a memory", dao.getByIdBlocking(id)?.content)
    }

    /**
     * In-memory DAO that records which thread each call arrived on, so the
     * threading contract can be asserted rather than assumed.
     */
    private class RecordingDiaryDao : DiaryDao {

        private val state = MutableStateFlow<List<DiaryEntity>>(emptyList())
        val saved: List<DiaryEntity> get() = state.value
        val threadNames = mutableListOf<String>()

        fun seed(vararg entries: DiaryEntity) {
            state.value = entries.toList()
        }

        /** Non-suspending peek, for asserting after a call has already returned. */
        fun getByIdBlocking(id: String): DiaryEntity? = state.value.firstOrNull { it.id == id }

        private fun record() {
            synchronized(threadNames) { threadNames += Thread.currentThread().name }
        }

        override suspend fun upsert(entry: DiaryEntity) {
            record()
            state.value = state.value.filterNot { it.id == entry.id } + entry
        }

        override suspend fun delete(id: String) {
            record()
            state.value = state.value.filterNot { it.id == id }
        }

        override fun observeAll(): Flow<List<DiaryEntity>> = state

        override fun observeForDay(epochDay: Long): Flow<List<DiaryEntity>> =
            state.map { all -> all.filter { it.dateEpochDay == epochDay } }

        override fun observeById(id: String): Flow<DiaryEntity?> =
            state.map { all -> all.firstOrNull { it.id == id } }

        override suspend fun getById(id: String): DiaryEntity? {
            record()
            return state.value.firstOrNull { it.id == id }
        }

        override suspend fun getAllForBackup(): List<DiaryEntity> = state.value

        override suspend fun getAll(): List<DiaryEntity> = state.value

        override suspend fun getAllInRange(fromEpochDay: Long, toEpochDay: Long): List<DiaryEntity> =
            state.value.filter { it.dateEpochDay in fromEpochDay..toEpochDay }

        /** Records the exact pattern handed to SQL, so escaping can be asserted end to end. */
        var lastSearchPattern: String? = null
            private set

        override suspend fun search(pattern: String, moodKey: String?): List<DiaryEntity> {
            lastSearchPattern = pattern
            return state.value.filter { entry ->
                val hit = entry.title?.contains(pattern, ignoreCase = true) == true ||
                    entry.content.contains(pattern, ignoreCase = true) ||
                    entry.tagsCsv.contains(pattern, ignoreCase = true)
                hit && (moodKey == null || entry.mood == moodKey)
            }
        }
    }

    // ---- search ---------------------------------------------------------

    @Test
    fun `a blank query returns nothing instead of everything`() = runTest {
        dao.seed(entry("a", content = "anything"))
        // Escaped, "" would become '%' and match the entire journal back with
        // no indication the query had been ignored.
        assertEquals(emptyList<DiaryEntity>(), repository.search(""))
        assertEquals(emptyList<DiaryEntity>(), repository.search("   \n "))
        assertNull(dao.lastSearchPattern)
    }

    /**
     * The escaping matters because the query declares `ESCAPE '\'`: without it,
     * searching for `50%` or `a_b` matches almost everything instead of the
     * literal text. Asserted on the pattern that reaches the DAO, not on a
     * helper, so the test fails if the escaping is ever dropped from the path
     * the search actually takes.
     */
    @Test
    fun `LIKE metacharacters are escaped on the way to SQL`() = runTest {
        repository.search("50%")
        assertEquals("50\\%", dao.lastSearchPattern)

        repository.search("a_b")
        assertEquals("a\\_b", dao.lastSearchPattern)

        repository.search("c:\\dir")
        assertEquals("c:\\\\dir", dao.lastSearchPattern)
    }

    @Test
    fun `the query is trimmed and ordinary text is left alone`() = runTest {
        repository.search("  coffee  ")
        assertEquals("coffee", dao.lastSearchPattern)
    }

    @Test
    fun `a mood filter narrows the result set`() = runTest {
        dao.seed(
            entry("a", content = "a good day", mood = "😊 Happy"),
            entry("b", content = "a good day", mood = "😔 Sad")
        )
        assertEquals(2, repository.search("good").size)
        assertEquals(1, repository.search("good", moodKey = "😊 Happy").size)
    }

    // ---- range ----------------------------------------------------------

    @Test
    fun `a day range is inclusive at both ends`() = runTest {
        dao.seed(entry("a", day = 10), entry("b", day = 11), entry("c", day = 12))
        assertEquals(3, repository.getAllInRange(10, 12).size)
        assertEquals(1, repository.getAllInRange(11, 11).size)
    }

    /**
     * A reversed range yields an empty list. Left alone, `BETWEEN 12 AND 10`
     * is false for every row, but swapping the bounds means a caller bug shows
     * up as "empty month" rather than as the whole table.
     */
    @Test
    fun `a reversed day range is normalised rather than returning everything`() = runTest {
        dao.seed(entry("a", day = 10), entry("b", day = 11), entry("c", day = 12))
        assertEquals(3, repository.getAllInRange(12, 10).size)
        assertEquals(emptyList<DiaryEntity>(), repository.getAllInRange(50, 40))
    }

    private fun entry(
        id: String,
        content: String = "content",
        mood: String? = null,
        day: Long = 20_000L
    ) = DiaryEntity(
        id = id, title = null, content = content, mood = mood, tagsCsv = "",
        dateEpochDay = day, timeMinutes = 600, aiGenerated = false, isReviewed = true,
        attachmentsJson = "", isFavorite = false, createdAt = 0L, updatedAt = 0L
    )
}
