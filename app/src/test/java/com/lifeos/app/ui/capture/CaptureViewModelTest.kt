package com.lifeos.app.ui.capture

import com.lifeos.app.core.location.LocationFailure
import com.lifeos.app.core.location.LocationOutcome
import com.lifeos.app.core.location.LocationProvider
import com.lifeos.app.data.db.dao.CaptureDao
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.repository.CaptureRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The Capture hub's storage rules.
 *
 * The specific defect these pin down: the Audio tile used to be a stub that
 * never recorded, so the one path that mattered — a finished recording becoming
 * a real [CaptureType.AUDIO] row carrying its own measured duration — did not
 * exist. `saveAudio` is the second half of the fix (the recorder is the first),
 * so these tests hold the row shape and the honest success/failure states.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeCaptureDao
    private val createdScopes = mutableListOf<kotlinx.coroutines.CoroutineScope>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeCaptureDao()
    }

    @After
    fun tearDown() {
        createdScopes.forEach { it.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
        createdScopes.clear()
        Dispatchers.resetMain()
    }

    /** Storage work runs on the shared test scheduler, never a live IO pool. */
    private fun viewModel() = CaptureViewModel(
        captureRepository = CaptureRepository(dao, io = dispatcher),
        locationProvider = FakeLocationProvider()
    ).also { createdScopes += it.viewModelScope }

    @Test
    fun `saveAudio stores an audio row with the recorder's own duration`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.saveAudio("/data/files/captures/audio/voice_1.m4a", durationMillis = 4_200L)
        advanceUntilIdle()

        val stored = dao.rows.value.single()
        assertEquals(CaptureType.AUDIO, stored.type)
        assertEquals("/data/files/captures/audio/voice_1.m4a", stored.filePath)
        assertEquals(4_200L, stored.durationMillis)
        assertEquals("audio/mp4", stored.mimeType)

        val state = vm.state.value
        assertEquals(false, state.saving)
        assertNull(state.error)
        assertNotNull(state.confirmation)
        assertTrue("confirmation names what was stored", state.confirmation!!.contains("Voice note"))
    }

    @Test
    fun `saveAudio reports the real failure and keeps the clip on disk`() = runTest(dispatcher) {
        val vm = viewModel()
        dao.failNextUpsert = IllegalStateException("disk full")

        vm.saveAudio("/data/files/captures/audio/voice_2.m4a", durationMillis = 1_000L)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(false, state.saving)
        assertNull(state.confirmation)
        val error = state.error
        assertNotNull(error)
        // Honest what -> why -> how copy; the clip is not silently deleted.
        assertTrue(error!!.contains("disk full"))
        assertTrue(error.contains("still on this device"))
        assertTrue(dao.rows.value.isEmpty())
    }
}

/** Minimal in-memory [CaptureDao] — enough for the write paths under test. */
private class FakeCaptureDao : CaptureDao {
    val rows = MutableStateFlow<List<CaptureEntity>>(emptyList())
    var failNextUpsert: Throwable? = null

    override suspend fun upsert(capture: CaptureEntity) {
        failNextUpsert?.let { failNextUpsert = null; throw it }
        rows.value = rows.value.filterNot { it.id == capture.id } + capture
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override fun observeAll(): Flow<List<CaptureEntity>> = rows
    override fun observeForType(type: String): Flow<List<CaptureEntity>> = rows.map { list -> list.filter { it.type == type } }
    override fun observeFavorites(): Flow<List<CaptureEntity>> = rows.map { list -> list.filter { it.isFavorite } }
    override suspend fun getById(id: String): CaptureEntity? = rows.value.firstOrNull { it.id == id }
    override fun observeById(id: String): Flow<CaptureEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getAllForBackup(): List<CaptureEntity> = rows.value
    override suspend fun getAll(): List<CaptureEntity> = rows.value
    override suspend fun getAllInRange(fromMillis: Long, toMillis: Long): List<CaptureEntity> =
        rows.value.filter { it.createdAt in fromMillis..toMillis }
    override suspend fun search(pattern: String): List<CaptureEntity> =
        rows.value.filter { it.text?.contains(pattern.trim('%'), ignoreCase = true) == true }
    override suspend fun countForType(type: String): Int = rows.value.count { it.type == type }
}

private class FakeLocationProvider : LocationProvider {
    override fun hasLocationPermission(): Boolean = true
    override suspend fun currentPlace(): LocationOutcome =
        LocationOutcome.Failure(LocationFailure.NO_FIX)
}
