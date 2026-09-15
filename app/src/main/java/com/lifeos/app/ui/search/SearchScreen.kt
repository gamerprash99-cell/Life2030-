package com.lifeos.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchResults(
    val notes: List<String> = emptyList(),
    val tasks: List<String> = emptyList(),
    val expenses: List<String> = emptyList(),
    val diary: List<String> = emptyList()
)

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
            if (query.isBlank()) { _results.value = SearchResults(); return@launch }
            _results.value = SearchResults(
                notes = noteRepo.search(query).map { it.title.ifBlank { "Untitled note" } },
                tasks = taskRepo.search(query).map { it.title },
                expenses = expenseRepo.search(query).map { "${it.merchant ?: it.category} — ₹${it.amount}" },
                diary = diaryRepo.search(query).map { it.content.take(60) }
            )
        }
    }
}

@Composable
fun SearchScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: SearchViewModel = viewModel(
        factory = LambdaViewModelFactory {
            SearchViewModel(locator.noteRepository, locator.taskRepository, locator.expenseRepository, locator.diaryRepository)
        }
    )
    var query by remember { mutableStateOf("") }
    val results by viewModel.results.collectAsState()

    LaunchedEffect(query) { viewModel.search(query) }

    Scaffold(topBar = { TopAppBar(title = { Text("Search everything") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search notes, tasks, expenses, diary…") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(contentPadding = PaddingValues(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (results.notes.isNotEmpty()) item { ResultSection("Notes", results.notes) }
                if (results.tasks.isNotEmpty()) item { ResultSection("Tasks", results.tasks) }
                if (results.expenses.isNotEmpty()) item { ResultSection("Expenses", results.expenses) }
                if (results.diary.isNotEmpty()) item { ResultSection("Diary", results.diary) }
            }
        }
    }
}

@Composable
private fun ResultSection(title: String, items: List<String>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            items.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
