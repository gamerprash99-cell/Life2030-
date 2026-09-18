package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.LifeOSTopBar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val MOODS = listOf("😊 Happy", "😌 Calm", "😔 Sad", "😤 Stressed", "🤩 Excited")

/**
 * Diary (Section 23): a quiet, local journal. Entries are written by the user
 * and saved only on explicit Save — there is intentionally NO dedicated AI
 * drafting here. AI assistance lives in the central "Ask LifeOS" surface and
 * in the Notes editor.
 */
class DiaryViewModel(
    private val diaryRepository: DiaryRepository
) : ViewModel() {
    val entries: StateFlow<List<DiaryEntity>> = diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(content: String, mood: String?) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val now = DateTimeUtils.today()
            val minutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
            diaryRepository.createEntry(
                title = null, content = content, mood = mood, tags = emptyList(),
                dateEpochDay = now.toEpochDay(), timeMinutes = minutes
            )
        }
    }

    fun deleteEntry(id: String) = viewModelScope.launch { diaryRepository.delete(id) }
}

@Composable
fun DiaryScreen(onBack: () -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryViewModel = viewModel(
        factory = LambdaViewModelFactory { DiaryViewModel(locator.diaryRepository) }
    )
    val entries by viewModel.entries.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<String?>(null) }
    var entryToDelete by remember { mutableStateOf<DiaryEntity?>(null) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            LifeOSTopBar("Diary", "Reflect and keep your journal local", onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "New entry") }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No diary entries yet", style = MaterialTheme.typography.titleMedium)
                Text("Tap + to write your first entry", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = com.lifeos.app.ui.theme.LifeOSSpacing.fabContentClearance
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay)) +
                                        (entry.mood?.let { "  •  $it" } ?: ""),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                TextButton(onClick = { entryToDelete = entry }) { Text("Delete") }
                            }
                            Text(entry.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New diary entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = content, onValueChange = { content = it },
                        placeholder = { Text("What happened today?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Mood", style = MaterialTheme.typography.labelMedium)
                    MOODS.forEach { m ->
                        TextButton(onClick = { mood = if (mood == m) null else m }) { Text(if (mood == m) "✓ $m" else m) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addEntry(content, mood); content = ""; mood = null; showAddDialog = false },
                    enabled = content.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("This permanently removes the diary entry from this device.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEntry(entry.id); entryToDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { entryToDelete = null }) { Text("Cancel") } }
        )
    }
}
