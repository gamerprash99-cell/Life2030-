package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Full-page view of one diary entry. */
class DiaryDetailViewModel(
    private val entryId: String,
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    val entry: StateFlow<DiaryEntity?> = diaryRepository.observeAll()
        .map { all -> all.firstOrNull { it.id == entryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor

    fun startEdit() { _showEditor.value = true }

    fun dismissEditor() { _showEditor.value = false }

    fun save(content: String, mood: String?) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val current = entry.value ?: return@launch
            diaryRepository.updateEntry(current.id, current.title, content, mood, splitTags(current.tagsCsv))
            _showEditor.value = false
        }
    }

    fun delete(id: String) = viewModelScope.launch { diaryRepository.delete(id) }

    private fun splitTags(csv: String): List<String> = csv.split(',').map { it.trim() }.filter { it.isNotBlank() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryDetailScreen(entryId: String, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryDetailViewModel = viewModel(
        key = "diary-detail-$entryId",
        factory = LambdaViewModelFactory {
            DiaryDetailViewModel(entryId, locator.diaryRepository)
        }
    )
    val entry by viewModel.entry.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val wasVisible = remember { mutableStateOf(false) }
    LaunchedEffect(entry) {
        if (entry == null) {
            if (wasVisible.value) onBack()
        } else wasVisible.value = true
    }

    entry?.let { current ->
        val dateLabel = DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(current.dateEpochDay))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding,
                bottom = LifeOSSpacing.fabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item { LifeOSTopBar("Diary entry", onBack = onBack) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        current.title?.takeIf { it.isNotBlank() } ?: "A day, written down",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        EntryMoodPill(entry = current)
                        Text(
                            "${DateTimeUtils.formatDayOfWeek(DateTimeUtils.epochDayToLocalDate(current.dateEpochDay))} · ${formatTime(current.timeMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        current.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35f
                    )
                    val tags = remember(current.id, current.tagsCsv) {
                        current.tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.take(6)
                    }
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag -> ThemeKeywordChip(tag) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = viewModel::startEdit) { Text("Edit entry") }
                        TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }

        if (showEditor) {
            DiaryEditorSheet(
                editing = current,
                onDismiss = viewModel::dismissEditor,
                onSave = viewModel::save,
                onDelete = { viewModel.dismissEditor(); confirmDelete = true }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This permanently removes the diary entry from this device.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(entryId); confirmDelete = false; onBack() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ThemeKeywordChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = LifeOSPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

/** Mood shown as a tinted pill, from the stored mood only. */
@Composable
fun EntryMoodPill(entry: DiaryEntity) {
    val mood = DiaryMoods.fromStored(entry.mood)
    val label = mood?.let { "${it.emoji} ${it.label}" } ?: DiaryMoods.displayLabel(entry.mood)
    if (label.isEmpty()) return
    val color = mood?.let { DiaryMoods.colorOf(it.key) } ?: LifeOSPrimary
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

internal fun formatTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val am = h < 12
    val hour = ((h + 11) % 12) + 1
    return String.format(java.util.Locale.getDefault(), "%d:%02d %s", hour, m, if (am) "AM" else "PM")
}