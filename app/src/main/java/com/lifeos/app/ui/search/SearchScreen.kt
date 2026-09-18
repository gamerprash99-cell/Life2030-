package com.lifeos.app.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.ui.components.LifeOSCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SearchCategory(val label: String) {
    NOTES("Notes"),
    TASKS("Tasks"),
    EXPENSES("Expenses"),
    DIARY("Diary")
}

data class SearchHit(
    val id: String,
    val title: String,
    val subtitle: String?,
    val category: SearchCategory
)

data class SearchResults(
    val hits: List<SearchHit> = emptyList(),
    val isSearching: Boolean = false
) {
    fun byCategory(category: SearchCategory): List<SearchHit> = hits.filter { it.category == category }
}

class SearchViewModel(
    private val noteRepo: NoteRepository,
    private val taskRepo: TaskRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository
) : ViewModel() {
    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results

    fun search(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.isBlank()) {
                _results.value = SearchResults()
                return@launch
            }
            _results.value = SearchResults(isSearching = true)
            val hits = mutableListOf<SearchHit>()
            noteRepo.search(trimmed).forEach { note ->
                hits += SearchHit(
                    id = note.id,
                    title = note.title.ifBlank { "Untitled note" },
                    subtitle = note.plainTextForSearch.take(80).takeIf { it.isNotBlank() },
                    category = SearchCategory.NOTES
                )
            }
            taskRepo.search(trimmed).forEach { task ->
                hits += SearchHit(
                    id = task.id,
                    title = task.title,
                    subtitle = task.category?.let { "#$it" },
                    category = SearchCategory.TASKS
                )
            }
            expenseRepo.search(trimmed).forEach { expense ->
                hits += SearchHit(
                    id = expense.id,
                    title = expense.merchant ?: expense.category,
                    subtitle = "₹${"%.2f".format(expense.amount)}",
                    category = SearchCategory.EXPENSES
                )
            }
            diaryRepo.search(trimmed).forEach { entry ->
                hits += SearchHit(
                    id = entry.id,
                    title = entry.content.take(50).ifBlank { "Diary entry" },
                    subtitle = null,
                    category = SearchCategory.DIARY
                )
            }
            _results.value = SearchResults(hits = hits)
        }
    }
}

@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onOpenHit: (SearchHit) -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: SearchViewModel = viewModel(
        factory = LambdaViewModelFactory {
            SearchViewModel(locator.noteRepository, locator.taskRepository, locator.expenseRepository, locator.diaryRepository)
        }
    )
    var query by remember { mutableStateOf("") }
    val results by viewModel.results.collectAsState()

    BackHandler(onBack = onBack)

    // LaunchedEffect is cancelled and restarted on every keystroke, so the
    // trailing delay acts as a 300 ms debounce: no search runs per character.
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(300)
        viewModel.search(query)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Search everything") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search notes, tasks, expenses, diary…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(contentPadding = PaddingValues(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchCategory.entries.forEach { category ->
                    val hits = results.byCategory(category)
                    if (hits.isNotEmpty()) {
                        item(key = "header-${category.name}") {
                            Text(category.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        items(hits, key = { "${it.category}-${it.id}" }) { hit ->
                            LifeOSCard(Modifier.fillMaxWidth(), onClick = { onOpenHit(hit) }) {
                                Column {
                                    Text(hit.title, style = MaterialTheme.typography.bodyLarge)
                                    hit.subtitle?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                if (!results.isSearching && query.isNotBlank() && results.hits.isEmpty()) {
                    item { Text("No matches for \"$query\".", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
