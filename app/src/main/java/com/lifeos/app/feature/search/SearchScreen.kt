package com.lifeos.app.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.data.SearchResults
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.LifeEventType
import com.lifeos.app.core.domain.formatMoney
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.SectionHeader

/**
 * Global search: grouped by record type, each group independently capped at 25
 * rows. The query never reaches the database below two characters.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "Search", onBack = onBack)

        Column(Modifier.padding(horizontal = Spacing.lg)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search notes, tasks, expenses…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clear) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = LifeOs.tokens.border,
                ),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when {
                state.tooShort -> item {
                    EmptyState(
                        title = "Type at least two characters",
                        subtitle = "Search looks through your own notes, tasks, habits, expenses, " +
                            "diary entries, captures and timeline — everything on this device.",
                    )
                }

                state.results == null -> item {
                    EmptyState(
                        title = "Searching…",
                        subtitle = "Looking through your records.",
                    )
                }

                state.results?.isEmpty == true -> item {
                    EmptyState(
                        title = "No matches",
                        subtitle = "Nothing stored on this device matched “${state.query}”.",
                    )
                }

                else -> {
                    val results = state.results!!
                    resultGroups(results).forEach { group ->
                        if (group.items.isNotEmpty()) {
                            item(key = "header-${group.title}") {
                                SectionHeader(
                                    title = group.title,
                                    subtitle = if (group.capped) {
                                        "${group.items.size} shown (limit 25)"
                                    } else {
                                        "${group.items.size} found"
                                    },
                                )
                            }
                            items(group.items.size) { index ->
                                SearchRow(group.items[index])
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.xl)) }
        }
    }
}

private data class ResultGroup(
    val title: String,
    val items: List<Pair<String, String>>,
    val capped: Boolean,
)

/** Maps the grouped query result into display rows (title, subtitle) per group. */
private fun resultGroups(results: SearchResults): List<ResultGroup> = listOf(
    ResultGroup(
        "Notes",
        results.notes.map { it.title.ifBlank { "Untitled note" } to it.body.take(80) },
        results.notes.size >= 25,
    ),
    ResultGroup(
        "Tasks",
        results.tasks.map {
            it.title to (it.dueDate?.let { due -> "Due ${Dates.relative(due)}" } ?: "No due date")
        },
        results.tasks.size >= 25,
    ),
    ResultGroup(
        "Habits",
        results.habits.map { it.name to it.recurrence },
        results.habits.size >= 25,
    ),
    ResultGroup(
        "Expenses",
        results.expenses.map {
            formatMoney(it.amountMinor, it.currencyCode) to
                "${it.category} · ${Dates.relative(it.expenseDate)}"
        },
        results.expenses.size >= 25,
    ),
    ResultGroup(
        "Diary",
        results.diary.map {
            (it.title.ifBlank { it.body.take(60) }) to Dates.human(Dates.parse(it.diaryDate))
        },
        results.diary.size >= 25,
    ),
    ResultGroup(
        "Captures",
        results.captures.map {
            it.text.ifBlank { com.lifeos.app.core.domain.CaptureType.fromCode(it.type).label } to
                Dates.relative(it.occurredOn)
        },
        results.captures.size >= 25,
    ),
    ResultGroup(
        "Timeline",
        results.events.map {
            it.title to "${LifeEventType.fromCode(it.type).label} · ${Dates.relative(it.occurredOn)}"
        },
        results.events.size >= 25,
    ),
)

@Composable
private fun SearchRow(item: Pair<String, String>) {
    LifeOsCard {
        Column {
            Text(
                text = item.first.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.second.isNotBlank()) {
                Text(
                    text = item.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
