package com.lifeos.app.ui.diary

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryWeather
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The detail page's own state holder, where the one behaviour that cannot be
 * exercised through the UI is the *timing* of a delete.
 *
 * The screen pops its destination the moment the user confirms a delete, and
 * popping clears the back-stack entry — which clears the ViewModelStore entry,
 * which cancels `viewModelScope`. A delete launched into that scope is therefore
 * racing its own cancellation, and it loses whenever the (SQLCipher-encrypted)
 * commit is slower than the navigation transition. The user is told the memory
 * is deleted; it reappears on the timeline.
 *
 * These tests drive that race directly rather than hoping to hit it on a device.
 */
class DiaryDetailViewModelTest {

    private val dao = GatedDiaryDao()

    @Before
    fun setUp() {
        // `viewModelScope` needs a Main dispatcher. `Unconfined` models
        // `Dispatchers.Main.immediate` on a device: the launched block starts
        // eagerly on the calling thread and runs until its first real
        // suspension, which is exactly the interleaving being tested.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun entry(id: String) = DiaryEntity(
        id = id,
        content = "content of $id",
        dateEpochDay = 20_000L,
        timeMinutes = 480,
        createdAt = 0L,
        updatedAt = 0L
    )

    /**
     * A detail view model held in a real [ViewModelStore], so a test can do the
     * one thing the platform does on a pop: `clear()` it.
     */
    private fun storeWith(id: String): Pair<ViewModelStore, DiaryDetailViewModel> {
        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            LambdaViewModelFactory {
                DiaryDetailViewModel(id, DiaryRepository(dao), NoWeather)
            }
        )[id, DiaryDetailViewModel::class.java]
        return store to vm
    }

    /**
     * The regression: ask for the delete, close the screen while it is still in
     * flight, and the memory must still be gone.
     */
    @Test
    fun `a delete is honoured even though the screen that asked for it was closed mid-flight`() =
        runBlocking {
            dao.seed(entry("doomed"))
            // Arm the gate so the delete is provably still in flight when the
            // screen goes away, rather than having raced to completion first.
            dao.holdReads()

            val (store, vm) = storeWith("doomed")
            vm.delete("doomed")

            // Exactly what popping the detail destination does.
            store.clear()

            // Now let the read return. Written as a cancellable suspension, so a
            // plain `viewModelScope.launch` resumes into a cancelled scope here
            // and the row survives — which is the bug this pins shut.
            dao.releaseReads()

            assertTrue(
                "the delete must complete even though the screen was closed",
                dao.awaitDelete(5, TimeUnit.SECONDS)
            )
            assertTrue("the memory must actually be gone", dao.saved.none { it.id == "doomed" })
        }

    @Test
    fun `a delete of an entry that is already gone leaves the others alone`() = runBlocking {
        dao.seed(entry("present"))
        val (store, vm) = storeWith("absent")

        vm.delete("absent")
        store.clear()

        assertTrue(dao.awaitDelete(5, TimeUnit.SECONDS))
        assertTrue("a sibling entry must be untouched", dao.saved.any { it.id == "present" })
    }

    @Test
    fun `toggling favourite persists the flipped value`() = runBlocking {
        dao.seed(entry("star"))
        val (store, vm) = storeWith("star")

        vm.toggleFavorite()
        assertTrue("the stored row must be flipped", dao.saved.single().isFavorite)

        vm.toggleFavorite()
        assertFalse("and flipped back again", dao.saved.single().isFavorite)
        store.clear()
    }

    @Test
    fun `removing an attachment the entry does not hold changes nothing`() = runBlocking {
        dao.seed(entry("e1"))
        val before = dao.saved.single().attachmentsJson
        val (store, vm) = storeWith("e1")

        vm.removeAttachment("/not/one/of/mine.jpg")
        store.clear()

        assertEquals("the row must be untouched", before, dao.saved.single().attachmentsJson)
    }

    // ---- test doubles -------------------------------------------------------

    private object NoWeather : WeatherRepository {
        override suspend fun weatherFor(place: DiaryAttachment.Place?): DiaryWeather =
            DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_SOURCE_OFFLINE_ONLY)
    }

    /**
     * A DAO whose reads can be held open on demand, so a test can prove a write
     * was still in flight when the calling scope went away.
     */
    private class GatedDiaryDao : DiaryDao {

        private val state = MutableStateFlow<List<DiaryEntity>>(emptyList())
        val saved: List<DiaryEntity> get() = state.value

        /** Signalled once a [delete] call has finished its body. */
        private val deleted = CountDownLatch(1)

        /** Non-null while reads are being held; awaited by [getById]. */
        private var gate: CompletableDeferred<Unit>? = null

        fun seed(vararg entries: DiaryEntity) {
            state.value = entries.toList()
        }

        fun holdReads() {
            gate = CompletableDeferred()
        }

        fun releaseReads() {
            gate?.complete(Unit)
            gate = null
        }

        fun awaitDelete(seconds: Long, unit: TimeUnit): Boolean = deleted.await(seconds, unit)

        override suspend fun upsert(entry: DiaryEntity) {
            state.value = state.value.filterNot { it.id == entry.id } + entry
        }

        override suspend fun delete(id: String) {
            state.value = state.value.filterNot { it.id == id }
            deleted.countDown()
        }

        override fun observeAll(): Flow<List<DiaryEntity>> = state

        override fun observeForDay(epochDay: Long): Flow<List<DiaryEntity>> =
            state.map { all -> all.filter { it.dateEpochDay == epochDay } }

        /**
         * `CompletableDeferred.await` is a *cancellable* suspension, so a
         * coroutine whose scope was cancelled while parked here resumes with a
         * `CancellationException` — which is precisely what lets this test
         * observe the cancellation race instead of merely asserting on it.
         */
        override fun observeById(id: String): Flow<DiaryEntity?> =
            state.map { all -> all.firstOrNull { it.id == id } }

        override suspend fun getById(id: String): DiaryEntity? {
            val answer = state.value.firstOrNull { it.id == id }
            gate?.await()
            return answer
        }

        override suspend fun getAllForBackup(): List<DiaryEntity> = state.value
    }
}
