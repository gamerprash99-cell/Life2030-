package com.lifeos.app.ui.search

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSPageHeader
import com.lifeos.app.ui.components.LifeOSSearchField
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSSkeletonList
import com.lifeos.app.ui.components.lifeOSIsDark
import com.lifeos.app.ui.theme.LifeOSMint
import com.lifeos.app.ui.theme.lifeOSCanvasBrush
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val searched: Boolean = false,
    val notes: List<NoteEntity> = emptyList(),
    val diary: List<DiaryEntity> = emptyList(),
    val captures: List<CaptureEntity> = emptyList()
) {
    val total: Int get() = notes.size + diary.size + captures.size
}

/** Searches Notes, Diary and Captures in one pass. Local SQLite LIKE — instant, offline. */
class GlobalSearchViewModel(
    private val noteRepository: NoteRepository,
    private val diaryRepository: DiaryRepository,
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            // collectLatest cancels an in-flight search when the query changes,
            // so results always belong to the newest keystroke.
            queryFlow.collectLatest { q -> runSearch(q) }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        queryFlow.value = value
    }

    private suspend fun runSearch(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) {
            _state.update { it.copy(loading = false, searched = false, notes = emptyList(), diary = emptyList(), captures = emptyList()) }
            return
        }
        _state.update { it.copy(loading = true, searched = true) }
        val notes = runCatching { noteRepository.search(q) }.getOrDefault(emptyList())
        val diary = runCatching { diaryRepository.search(q) }.getOrDefault(emptyList())
        val captures = runCatching { captureRepository.search(q) }.getOrDefault(emptyList())
        _state.update { it.copy(loading = false, notes = notes, diary = diary, captures = captures) }
    }
}

/**
 * One field across the whole app. Results are grouped by module with honest
 * counts, and each row opens its source. Nothing is fuzzy or invented — it is
 * a straight text match over stored rows.
 */
@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenDiary: (String) -> Unit,
    onOpenCapture: (String) -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: GlobalSearchViewModel = viewModel(
        factory = LambdaViewModelFactory {
            GlobalSearchViewModel(locator.noteRepository, locator.diaryRepository, locator.captureRepository)
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dark = lifeOSIsDark()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(lifeOSCanvasBrush(dark)),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            LifeOSPageHeader(
                eyebrow = "Everything",
                title = "Search",
                onBack = onBack
            )
        }
        item {
            LifeOSSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Notes, diary, memories…",
                leadingIcon = Icons.Filled.Search
            )
        }
        if (state.searched && !state.loading) {
            item {
                LifeOSSectionHeader(
                    "Results",
                    actionLabel = "${state.total} total"
                )
            }
        }

        when {
            state.query.isBlank() -> item {
                LifeOSEmptyState(
                    title = "Search everything",
                    body = "One field across your notes, diary and captured memories.",
                    icon = Icons.Filled.Search
                )
            }
            state.loading -> item { LifeOSSkeletonList(rows = 4) }
            state.total == 0 -> item {
                LifeOSEmptyState(
                    title = "No matches",
                    body = "Nothing stored matches “${state.query}”.",
                    icon = Icons.Filled.Search
                )
            }
            else -> {
                if (state.notes.isNotEmpty()) {
                    item { SearchGroupHeader("Notes", state.notes.size) }
                    items(state.notes, key = { "n" + it.id }) { note ->
                        ResultRow(
                            icon = Icons.Filled.Description,
                            title = note.title.ifBlank { "Untitled note" },
                            subtitle = note.content.trim().take(140),
                            onClick = { onOpenNote(note.id) }
                        )
                    }
                }
                if (state.diary.isNotEmpty()) {
                    item { SearchGroupHeader("Diary", state.diary.size) }
                    items(state.diary, key = { "d" + it.id }) { entry ->
                        ResultRow(
                            icon = Icons.Filled.Book,
                            title = entry.title?.takeIf { it.isNotBlank() } ?: "Memory",
                            subtitle = entry.content.trim().take(140),
                            leading = formatDate(entry.dateEpochDay),
                            onClick = { onOpenDiary(entry.id) }
                        )
                    }
                }
                if (state.captures.isNotEmpty()) {
                    item { SearchGroupHeader("Memories", state.captures.size) }
                    items(state.captures, key = { "c" + it.id }) { capture ->
                        ResultRow(
                            icon = Icons.Filled.PhotoCamera,
                            title = capture.text?.takeIf { it.isNotBlank() } ?: capture.type.lowercase().replaceFirstChar { it.uppercase() },
                            subtitle = capture.placeLabel ?: capture.moodKey ?: capture.type.lowercase(),
                            onClick = { onOpenCapture(capture.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGroupHeader(title: String, count: Int) {
    LifeOSSectionHeader(title, actionLabel = "$count")
}

@Composable
private fun ResultRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: String? = null
) {
    LifeOSCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = LifeOSMint, modifier = Modifier.padding(end = 2.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            leading?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun formatDate(epochDay: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochDay * 86_400_000L))
