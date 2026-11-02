package com.lifeos.app.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import com.lifeos.app.core.util.MediaStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * The voice recorder's live state.
 *
 * Every number in here is measured: [elapsedMillis] comes from a monotonic
 * clock that excludes paused time, and [amplitude] is the platform's reading of
 * the microphone for the sample that just happened. Nothing is simulated, so a
 * waveform drawn from this is a picture of real audio input — and a paused
 * recorder reports [Paused] rather than a frozen "recording".
 */
sealed interface VoiceRecordingState {
    data object Idle : VoiceRecordingState

    /** A take is live. [waveform] is the recent amplitude history, oldest first. */
    data class Recording(
        val elapsedMillis: Long,
        val amplitude: Int,
        val waveform: List<Int>
    ) : VoiceRecordingState

    /**
     * A take is held open but the microphone is not being read. [elapsedMillis]
     * is the time actually recorded so far, so resuming continues the clip
     * instead of starting a second one.
     */
    data class Paused(
        val elapsedMillis: Long,
        val waveform: List<Int>
    ) : VoiceRecordingState

    /** A finished take: the real file, its measured length and its real size. */
    data class Finished(
        val filePath: String,
        val durationMillis: Long,
        val sizeBytes: Long,
        val waveform: List<Int>
    ) : VoiceRecordingState

    /** The take was abandoned; whatever was written has been deleted. */
    data class Failed(val reason: VoiceRecordingFailure) : VoiceRecordingState
}

enum class VoiceRecordingFailure {
    /** The platform refused to start (mic busy, revoked mid-flight, IO error). */
    START_FAILED,
    STOP_FAILED,
    /** Released before a usable clip existed — an accidental tap, not a take. */
    TOO_SHORT,
    /** No microphone on this device, or it could not be opened at all. */
    NO_MICROPHONE
}

/**
 * The recorded length of a take that can be paused and resumed.
 *
 * Kept separate from [LifeOSAudioRecorder] and driven by an injectable clock so
 * the arithmetic — the part that is easy to get wrong (counting paused time, or
 * losing the segment before a pause) — is unit-testable on a plain JVM.
 */
class RecordingChronometer(private val clock: () -> Long = SystemClock::elapsedRealtime) {

    private var accumulatedMillis: Long = 0L
    private var segmentStartedAt: Long? = null

    val isRunning: Boolean get() = segmentStartedAt != null

    fun start() {
        accumulatedMillis = 0L
        segmentStartedAt = clock()
    }

    fun pause() {
        val started = segmentStartedAt ?: return
        accumulatedMillis += (clock() - started).coerceAtLeast(0L)
        segmentStartedAt = null
    }

    fun resume() {
        if (segmentStartedAt == null) segmentStartedAt = clock()
    }

    /** Total recorded time so far, excluding every paused stretch. */
    fun elapsedMillis(): Long {
        val started = segmentStartedAt ?: return accumulatedMillis
        return accumulatedMillis + (clock() - started).coerceAtLeast(0L)
    }

    fun reset() {
        accumulatedMillis = 0L
        segmentStartedAt = null
    }
}

/**
 * Real microphone recording for LifeOS voice captures, on the platform
 * [MediaRecorder].
 *
 * Why this and not the diary's [DiaryAudioRecorder]: a voice memory needs a
 * transport (pause/resume), a live level for the waveform, and a saved size —
 * the diary recorder is a press-and-hold note-taker. Both write AAC/MP4 into
 * app-private storage and neither touches the network.
 *
 * Contracts this class holds to:
 *  - `RECORD_AUDIO` is checked by the *caller* before [start]; the system
 *    dialog is therefore always a consequence of a user tap.
 *  - [start] on an already-live take is a no-op that returns the current state,
 *    so a double tap can never open a second microphone session.
 *  - Every failure path releases the recorder and deletes the partial file: a
 *    failed take can never leave a corrupt clip referenced by a capture.
 *  - [release] is safe to call from a lifecycle callback and twice in a row.
 */
class LifeOSAudioRecorder(private val context: Context) {

    private val _state = MutableStateFlow<VoiceRecordingState>(VoiceRecordingState.Idle)
    val state: StateFlow<VoiceRecordingState> = _state.asStateFlow()

    private val chronometer = RecordingChronometer()
    private val waveform = ArrayDeque<Int>()

    private var recorder: MediaRecorder? = null
    private var targetFile: File? = null

    /** True while the microphone is actually open (recording or paused). */
    val hasLiveTake: Boolean
        get() = _state.value is VoiceRecordingState.Recording || _state.value is VoiceRecordingState.Paused

    /**
     * Begins a real recording into app-private storage. The file is only handed
     * back once the recorder has genuinely started.
     */
    fun start(): VoiceRecordingState {
        when (_state.value) {
            is VoiceRecordingState.Recording, is VoiceRecordingState.Paused -> return _state.value
            else -> Unit
        }

        val file = MediaStorage.newCaptureAudioFile(context)
        val created = createRecorder(file)
        if (created == null) {
            _state.value = VoiceRecordingState.Failed(VoiceRecordingFailure.START_FAILED)
            return _state.value
        }

        recorder = created
        targetFile = file
        waveform.clear()
        chronometer.start()
        _state.value = VoiceRecordingState.Recording(
            elapsedMillis = 0L,
            amplitude = 0,
            waveform = emptyList()
        )
        return _state.value
    }

    private fun createRecorder(file: File): MediaRecorder? {
        var instance: MediaRecorder? = null
        return try {
            instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            instance.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(AUDIO_BIT_RATE)
                setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            instance
        } catch (_: Exception) {
            // A half-configured recorder holds the mic open; release it before
            // giving up, and remove the empty file prepare() may have created.
            runCatching { instance?.release() }
            file.delete()
            null
        }
    }

    /** Holds the microphone closed without ending the take. */
    fun pause(): VoiceRecordingState {
        val active = _state.value
        if (active !is VoiceRecordingState.Recording) return active
        val paused = runCatching {
            recorder?.pause()
            true
        }.getOrDefault(false)
        if (!paused) {
            // The platform refused to pause (an old device, or a recorder that
            // already died). Ending the take honestly is better than showing a
            // "paused" state that is still recording.
            return stop()
        }
        chronometer.pause()
        _state.value = VoiceRecordingState.Paused(
            elapsedMillis = chronometer.elapsedMillis(),
            waveform = waveform.toList()
        )
        return _state.value
    }

    /** Continues the same clip after a [pause]. */
    fun resume(): VoiceRecordingState {
        val active = _state.value
        if (active !is VoiceRecordingState.Paused) return active
        val resumed = runCatching {
            recorder?.resume()
            true
        }.getOrDefault(false)
        if (!resumed) return stop()
        chronometer.resume()
        _state.value = VoiceRecordingState.Recording(
            elapsedMillis = chronometer.elapsedMillis(),
            amplitude = 0,
            waveform = waveform.toList()
        )
        return _state.value
    }

    /**
     * Polls the clock and the microphone level. Call from a UI ticker while a
     * take is live; returns the state so a composable can render a real
     * duration and a real input level.
     */
    fun tick(): VoiceRecordingState {
        val active = _state.value
        if (active !is VoiceRecordingState.Recording) return active
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrElse { 0 }
        waveform.addLast(amplitude.toWaveformLevel())
        while (waveform.size > WAVEFORM_BUCKETS) waveform.removeFirst()
        _state.value = VoiceRecordingState.Recording(
            elapsedMillis = chronometer.elapsedMillis(),
            amplitude = amplitude,
            waveform = waveform.toList()
        )
        return _state.value
    }

    /**
     * Ends the take and returns the finished clip, or a [VoiceRecordingState.Failed]
     * explaining why there is none. A take shorter than [MIN_DURATION_MILLIS] is
     * discarded as an accidental tap rather than persisted as an unplayable clip.
     */
    fun stop(): VoiceRecordingState {
        val active = recorder
        val file = targetFile
        val elapsed = chronometer.elapsedMillis()

        if (active == null || file == null) {
            _state.value = VoiceRecordingState.Idle
            return _state.value
        }

        // Detach first: whether stop() throws or not, a released recorder must
        // never be reachable from here again.
        recorder = null
        targetFile = null
        chronometer.pause()

        val stopped = runCatching { active.stop() }.isSuccess
        runCatching { active.release() }

        if (!stopped) return fail(file, VoiceRecordingFailure.STOP_FAILED)
        if (elapsed < MIN_DURATION_MILLIS) return fail(file, VoiceRecordingFailure.TOO_SHORT)
        if (!file.exists() || file.length() == 0L) return fail(file, VoiceRecordingFailure.STOP_FAILED)

        val finished = VoiceRecordingState.Finished(
            filePath = file.absolutePath,
            durationMillis = elapsed,
            sizeBytes = file.length(),
            waveform = waveform.toList()
        )
        _state.value = finished
        return finished
    }

    private fun fail(file: File, reason: VoiceRecordingFailure): VoiceRecordingState {
        file.delete()
        chronometer.reset()
        waveform.clear()
        val failure = VoiceRecordingState.Failed(reason)
        _state.value = failure
        return failure
    }

    /** Abandons any live take and deletes whatever was written. */
    fun cancel() {
        recorder?.let { active ->
            runCatching { active.stop() }
            runCatching { active.release() }
        }
        recorder = null
        targetFile?.delete()
        targetFile = null
        chronometer.reset()
        waveform.clear()
        _state.value = VoiceRecordingState.Idle
    }

    /**
     * Lifecycle-safe teardown. A take that was never saved is deleted: leaving
     * the screen mid-recording must not leave an orphan file behind.
     */
    fun release() {
        when (_state.value) {
            is VoiceRecordingState.Recording, is VoiceRecordingState.Paused -> cancel()
            else -> Unit
        }
    }

    /** Deletes a finished-but-discarded take, keeping the recorder reusable. */
    fun discardFinished() {
        val finished = _state.value
        if (finished is VoiceRecordingState.Finished) MediaStorage.deleteIfExists(finished.filePath)
        chronometer.reset()
        waveform.clear()
        _state.value = VoiceRecordingState.Idle
    }

    /** `maxAmplitude` is 0..32767; the UI wants a 0..100 bar height. */
    private fun Int.toWaveformLevel(): Int = ((this.coerceIn(0, MAX_AMPLITUDE) * 100) / MAX_AMPLITUDE)

    companion object {
        const val AUDIO_BIT_RATE = 96_000
        const val AUDIO_SAMPLE_RATE = 44_100

        /** AAC in an MP4 container — the platform's own encoder defaults. */
        const val MIME_TYPE = "audio/mp4"

        /** Enough history to read as a waveform without growing unbounded. */
        const val WAVEFORM_BUCKETS = 72

        /** Anything shorter than this is a mis-tap, not a voice memory. */
        const val MIN_DURATION_MILLIS = 700L

        private const val MAX_AMPLITUDE = 32_767
    }
}
