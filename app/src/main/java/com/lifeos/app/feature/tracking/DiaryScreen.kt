package com.lifeos.app.feature.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.MinTouchTarget
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.Mood
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill
import com.lifeos.app.ui.components.SectionHeader

/**
 * Diary: one entry per local day (the database enforces uniqueness), optional
 * mood. Previous days are read-only history below the today composer.
 */
@Composable
fun DiaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.diaryState.collectAsStateWithLifecycle()

    // Seed the composer from the stored entry whenever the selected day changes.
    LaunchedEffect(state.selectedDate, state.entry?.updatedAt) {
        val entry = state.entry
        if (entry != null && state.draft.isEmpty()) {
            viewModel.setDiaryDraft(
                title = entry.title,
                body = entry.body,
                mood = entry.mood,
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "Diary", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        viewModel.selectDiaryDate(state.selectedDate.minusDays(1))
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous day")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Dates.relative(Dates.iso(state.selectedDate)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = Dates.humanLong(state.selectedDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.selectDiaryDate(state.selectedDate.plusDays(1)) },
                        enabled = state.selectedDate.isBefore(Dates.today()),
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next day")
                    }
                }
            }

            item {
                LifeOsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        OutlinedTextField(
                            value = state.draftTitle,
                            onValueChange = { viewModel.setDiaryDraft(title = it) },
                            label = { Text("Title (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = trackingFieldColors(),
                        )
                        OutlinedTextField(
                            value = state.draft,
                            onValueChange = { viewModel.setDiaryDraft(body = it) },
                            label = { Text("How was this day?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Spacing.huge * 4),
                            colors = trackingFieldColors(),
                        )
                        Text("Mood (optional)", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            items(Mood.entries.toList()) { mood ->
                                Pill(
                                    text = "${mood.emoji} ${mood.label}",
                                    selected = state.draftMood == mood.code,
                                    onClick = {
                                        viewModel.setDiaryDraft(
                                            mood = if (state.draftMood == mood.code) null else mood.code,
                                            clearMood = state.draftMood == mood.code,
                                        )
                                    },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = { viewModel.saveDiary() },
                                enabled = state.draft.isNotBlank() || state.draftTitle.isNotBlank(),
                            ) {
                                Text(if (state.hasEntry) "Update entry" else "Save entry")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Your entries",
                    subtitle = if (state.all.isEmpty()) "Nothing written yet" else
                        "${state.all.size} days recorded",
                )
            }

            if (state.all.isEmpty()) {
                item {
                    EmptyState(
                        title = "No diary entries yet",
                        subtitle = "Write about your day above. Entries are stored only on this device.",
                    )
                }
            } else {
                items(state.all, key = { it.id }) { entry ->
                    LifeOsCard(
                        onClick = { viewModel.selectDiaryDate(Dates.parseOrNull(entry.diaryDate) ?: Dates.today()) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(MinTouchTarget)
                                    .background(LifeOs.tokens.softPinkSurface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = entry.mood?.let { Mood.fromCode(it).emoji } ?: "·",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.size(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = entry.title.ifBlank {
                                        Dates.human(Dates.parseOrNull(entry.diaryDate) ?: Dates.today())
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = Dates.human(Dates.parseOrNull(entry.diaryDate) ?: Dates.today()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.selectedDate == Dates.parseOrNull(entry.diaryDate)) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Open",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
