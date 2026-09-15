package com.lifeos.app.ui.diary

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.lifeos.app.core.ai.AiResult
import com.lifeos.app.core.ai.AiRepository
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val MOODS = listOf("😊 Happy", "😌 Calm", "😔 Sad", "😤 Stressed", "🤩 Excited")

class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val aiRepository: AiRepository
) : ViewModel() {
    val entries: StateFlow<List<DiaryEntity>> = diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError

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

    /**
     * Turns rough notes into a drafted diary entry via AI. The resulting
     * entry is saved with aiGenerated=true / isReviewed=false (Rule #8) —
     * it shows up flagged "needs review" until the user taps Approve.
     */
    fun draftWithAi(rawThoughts: String, mood: String?) {
        if (rawThoughts.isBlank()) return
        viewModelScope.launch {
            _aiBusy.value = true
            _aiError.value = null
            when (val result = aiRepository.draftDiaryEntry(rawThoughts)) {
                is AiResult.Success -> {
                    val now = DateTimeUtils.today()
                    val minutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                    diaryRepository.createEntry(
                        title = null, content = result.text, mood = mood, tags = emptyList(),
                        dateEpochDay = now.toEpochDay(), timeMinutes = minutes, aiGenerated = true
                    )
                }
                is AiResult.Error -> _aiError.value = "Couldn't draft this entry: ${result.message}"
            }
            _aiBusy.value = false
        }
    }

    fun approveDraft(id: String) = viewModelScope.launch { diaryRepository.approveAiDraft(id) }

    fun dismissAiError() { _aiError.value = null }
}

@Composable
fun DiaryScreen(onBack: () -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryViewModel = viewModel(
        factory = LambdaViewModelFactory { DiaryViewModel(locator.diaryRepository, locator.aiRepository) }
    )
    val entries by viewModel.entries.collectAsState()
    val aiBusy by viewModel.aiBusy.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diary") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
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
                            Text(
                                DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay)) +
                                    (entry.mood?.let { "  •  $it" } ?: ""),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(entry.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                            if (entry.aiGenerated && !entry.isReviewed) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text("AI draft — needs review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    TextButton(onClick = { viewModel.approveDraft(entry.id) }) { Text("Approve") }
                                }
                            }
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
                        TextButton(onClick = { mood = m }) { Text(if (mood == m) "✓ $m" else m) }
                    }

                    if (aiBusy) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            Text("Drafting…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    aiError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

                    TextButton(
                        onClick = { viewModel.draftWithAi(content, mood) },
                        enabled = content.isNotBlank() && !aiBusy
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Turn into a diary entry with AI")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.addEntry(content, mood); content = ""; mood = null; showAddDialog = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; viewModel.dismissAiError() }) { Text("Cancel") } }
        )
    }
}
