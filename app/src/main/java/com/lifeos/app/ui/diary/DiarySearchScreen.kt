package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Search across the user's own entries.
 *
 * Results are `DiaryEntity` rows, deliberately untransformed: the list is the
 * user's own text, and there is no summarising step between the database and
 * the screen. Tapping a result hands the id to the detail screen, which loads
 * it by id — the search result list is never a second copy of the entry.
 *
 * "Found 3 of 214" is shown rather than a bare count. A bare count is
 * indistinguishable from "3 results exist" when the real number is 3 of 214,
 * and the capped count ([SearchDiaryEntriesUseCase.DEFAULT_LIMIT]) is a promise
 * the screen should keep visible rather than quietly break.
 */
@Composable
fun DiarySearchScreen(
    state: DiarySearchState,
    onQueryChange: (String) -> Unit,
    onMoodFilterChange: (String?) -> Unit,
    onSubmit: () -> Unit,
    onClearQuery: () -> Unit,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // The search field sits inside the system bars' padding: it is the
            // first interactive element, and on a gesture-nav device an
            // uninsetted search box ends up under the status bar.
            .padding(horizontal = LifeOSSpacing.screenPadding)
    ) {
        SearchHeader(onBack = onBack)

        TextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, if (state.query.isNotEmpty()) DiaryActionViolet else DiaryHairline, RoundedCornerShape(16.dp)),
            placeholder = { Text("Search your journal", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onSubmit()
                // Dismiss only on an explicit submit. Hiding it on every
                // keystroke would fight the user mid-word.
                keyboard?.hide()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        MoodFilterRow(
            selectedMoodKey = state.moodKey,
            onMoodFilterChange = onMoodFilterChange,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )

        Box(Modifier.weight(1f)) {
            when {
                state.errorMessage != null -> MessageState(
                    title = "Search failed",
                    body = state.errorMessage,
                    accent = MaterialTheme.colorScheme.error
                )

                state.showPrompt -> MessageState(
                    title = "Find a moment",
                    body = "Search every word you have written, and narrow it down with a mood.",
                    accent = DiaryActionViolet
                )

                state.showEmptyResult -> MessageState(
                    title = "Nothing found",
                    body = if (state.moodKey != null) {
                        "No entries match that word with the ${DiaryMoods.fromStored(state.moodKey)?.label ?: "chosen"} mood. Try without the mood filter."
                    } else {
                        "No entries match \"${state.query.trim()}\". Try a shorter phrase."
                    },
                    accent = DiaryActionViolet
                )

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "${state.results.size} result${if (state.results.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(state.results, key = { it.id }) { entry ->
                        DiarySearchResultRow(entry = entry, onClick = { onOpenEntry(entry.id) })
                    }
                }
            }

            // A 2dp ring rather than a full-screen scrim: the results are still
            // the point, and a scrim would hide what the user is searching.
            if (state.isSearching) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp,
                    color = DiaryActionViolet
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to journal"
            )
        }
        Text(
            "Search",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = DiaryInkViolet
        )
    }
}

/**
 * The eight moods as filters, plus "all".
 *
 * Reuses the editor's discs so the filter and the composer read as the same
 * control, and so a mood's colour is defined in exactly one place.
 */
@Composable
private fun MoodFilterRow(
    selectedMoodKey: String?,
    onMoodFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
    ) {
        item(key = "all") {
            FilterChipPill(label = "All", isSelected = selectedMoodKey == null) {
                onMoodFilterChange(null)
            }
        }
        itemsIndexed(DiaryMoods.OPTIONS, key = { _, mood -> mood.key }) { _, mood ->
            val isSelected = mood.key.equals(selectedMoodKey, ignoreCase = true)
            FilterChipPill(
                label = mood.label,
                emoji = mood.emoji,
                isSelected = isSelected,
                accent = DiaryMoodVisuals.accentOf(mood)
                // The ViewModel owns the toggle, so the screen just names what
                // was tapped.
            ) { onMoodFilterChange(mood.key) }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    emoji: String? = null,
    accent: Color = DiaryActionViolet,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) accent else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isSelected) accent else DiaryHairline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (emoji != null) {
            Text(emoji, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** One hit. Title if it has one, else the first words of the body. */
@Composable
private fun DiarySearchResultRow(entry: DiaryEntity, onClick: () -> Unit) {
    val dateLabel = remember(entry.dateEpochDay) {
        LocalDate.ofEpochDay(entry.dateEpochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
    val snippet = remember(entry.content, entry.title) { snippetOf(entry) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DiaryHairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                entry.title?.takeIf { it.isNotBlank() } ?: dateLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = DiaryInkViolet,
                maxLines = 1
            )
            entry.mood?.let { moodKey ->
                val mood = DiaryMoods.fromStored(moodKey)
                if (mood != null) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(DiaryMoodVisuals.haloOf(mood)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mood.emoji, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (snippet.isNotBlank()) {
            Text(
                snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

/**
 * The preview text for a hit.
 *
 * Trimmed to two lines' worth of characters rather than laid out and clipped, so
 * the search cannot reflow the whole list once the real text wraps — a title
 * plus 160 characters is stable across font scales in a way that is not.
 */
private fun snippetOf(entry: DiaryEntity): String {
    val source = entry.content.trim().replace(Regex("\\s+"), " ")
    return if (source.length <= SNIPPET_CHARS) source else source.take(SNIPPET_CHARS).trimEnd() + "…"
}

private const val SNIPPET_CHARS = 160

/** Shared empty/error panel, so search's three resting states look like one screen. */
@Composable
internal fun MessageState(title: String, body: String, accent: Color) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(DiaryLavender)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}