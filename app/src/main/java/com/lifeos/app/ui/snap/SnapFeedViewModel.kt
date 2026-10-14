package com.lifeos.app.ui.snap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.repository.CaptureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Drives Home's "Today's Snaps" strip: the photos the user captured today,
 * newest first. Derived from the same capture stream the Timeline reads, so a
 * snap posted from the camera appears here (and on the Timeline) with no refresh.
 */
class SnapFeedViewModel(
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val todayEpochDay: Long = DateTimeUtils.today().toEpochDay()

    val todaysSnaps: StateFlow<List<CaptureEntity>> = run {
        val range = DateTimeUtils.dayRangeMillis(todayEpochDay, todayEpochDay)
        captureRepository.observeAll()
            .map { captures ->
                captures
                    .filter { it.type == CaptureType.PHOTO && it.createdAt in range.first..range.second }
                    .sortedByDescending { it.createdAt }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }
}
