package com.lifeos.app.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.repository.CaptureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CaptureDetailViewModel(
    private val captureRepository: CaptureRepository,
    private val captureId: String
) : ViewModel() {
    private val _capture = MutableStateFlow<CaptureEntity?>(null)
    val capture: StateFlow<CaptureEntity?> = _capture

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    fun load() {
        viewModelScope.launch {
            _capture.value = captureRepository.getById(captureId)
            _loaded.value = true
        }
    }

    fun delete() {
        viewModelScope.launch {
            captureRepository.delete(captureId)
            _deleted.value = true
        }
    }
}

@Composable
fun CaptureDetailScreen(captureId: String, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: CaptureDetailViewModel = viewModel(
        factory = LambdaViewModelFactory { CaptureDetailViewModel(locator.captureRepository, captureId) }
    )
    LaunchedEffect(captureId) { viewModel.load() }

    val capture by viewModel.capture.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(captureTitle(capture?.type)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (capture != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            !loaded -> Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            capture == null -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("This item could not be found — it may have already been deleted.", style = MaterialTheme.typography.bodyMedium)
            }

            else -> {
                val item = capture!!
                Column(
                    modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item.filePath?.let { path ->
                        when (item.type) {
                            CaptureType.PHOTO -> PhotoPreview(path, modifier = Modifier.fillMaxWidth())
                            CaptureType.VIDEO -> VideoPreview(path, modifier = Modifier.fillMaxWidth())
                            CaptureType.AUDIO -> AudioPreview(path, modifier = Modifier.fillMaxWidth())
                            CaptureType.THOUGHT -> Unit
                        }
                    }

                    item.caption?.let { caption ->
                        Text(caption, style = MaterialTheme.typography.bodyLarge)
                    }

                    Text(
                        "${DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(item.dateEpochDay))} · ${DateTimeUtils.formatMinutes(item.timeMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this item?") },
            text = { Text("This will permanently remove the captured file and its record. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun captureTitle(type: CaptureType?): String = when (type) {
    CaptureType.PHOTO -> "Photo"
    CaptureType.VIDEO -> "Video"
    CaptureType.AUDIO -> "Audio"
    CaptureType.THOUGHT -> "Thought"
    null -> "Capture"
}
