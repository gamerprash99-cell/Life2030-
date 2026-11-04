package com.lifeos.app.ui.diary.new

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.diary.new.DiarySearchViewModel
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Diary Search — new visual design.
 *
 * Search input, search results, empty results. Reuses [DiarySearchViewModel].
 */
@Composable
fun DiaryNewSearchScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: DiarySearchViewModel = viewModel(
        factory = LambdaViewModelFactory {
            DiarySearchViewModel(locator.searchDiaryEntriesUseCase)
        }
    )

    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(DiaryNewCanvas)) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DiaryNewScreenPadding, vertical = DiaryNewSectionSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                Icon(
                    ArrowBack,
                    contentDescription = "Back",
                    tint = DiaryNewInk
                )
            }
            Text(
                "Search",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.Bold,
                    color = DiaryNewInk
                ),
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
            )
        }

        // Search field
        DiaryNewSearchField(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            onSubmit = viewModel::onSubmit,
            onClear = viewModel::onClearQuery
        )

        Spacer(Modifier.height(DiaryNewSectionSpacing))

        // Results
        if (state.isSearching) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DiaryNewSave)
            }
        } else if (state.showEmptyResult) {
            DiaryNewEmptySearchResults()
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding)
                    .fillMaxWidth()
            ) {
                if (state.results.isNotEmpty()) {
                    Text(
                        "${state.results.size} result${if (state.results.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryNewInkSoft
                        ),
                        modifier = Modifier.padding(bottom = DiaryNewSectionSpacing)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            state.results,
                            key = { _, entry -> entry.id }
                        ) { index, entry ->
                            DiaryNewSearchResultRow(
                                entry = entry,
                                query = state.query,
                                onClick = { onOpenEntry(entry.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = DiaryNewSectionSpacing)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryNewSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit
) {
    var text: String by remember { mutableStateOf(query) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = DiaryNewScreenPadding)
            .background(DiaryNewPaper)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DiaryNewBorder, RoundedCornerShape(12.dp))
    ) {
        TextField(
            value = text,
            onValueChange = {
                text = it
                onQueryChange(it)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onAny = { onSubmit(); keyboardController?.hide() }),
            leadingIcon = {
                Icon(
                    Search,
                    contentDescription = null,
                    tint = DiaryNewInkSoft,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (text.isNotBlank()) {
                    IconButton(onClick = { text = ""; onClear(); keyboardController?.hide() }) {
                        Icon(
                            Close,
                            contentDescription = "Clear",
                            tint = DiaryNewInkSoft,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Search your diary",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkMuted)
                )
            }
        )
    }
}

@Composable
private fun DiaryNewEmptySearchResults() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Box(
            Modifier.size(100.dp).clip(CircleShape).background(DiaryNewPanel),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Search,
                contentDescription = null,
                tint = DiaryNewInkMuted,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No results found",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = DiaryHandwriting,
                fontWeight = FontWeight.Bold,
                color = DiaryNewInk
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Try a different keyword or date.",
            style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
        )
    }
}

@Composable
private fun DiaryNewSearchResultRow(
    entry: DiaryEntity,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val attachments = DiaryAttachments.decode(entry.attachmentsJson)
    val photos = attachments.filterIsInstance<DiaryAttachment.Photo>()
    val mood = entry.mood?.takeIf { it.isNotBlank() }?.let { DiaryMoods.fromStored(it) }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(DiaryNewPaper)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DiaryNewBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mood?.let { m ->
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(moodColor(m.key)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        m.emoji,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } ?: Box(Modifier.size(32.dp))

            Spacer(Modifier.width(12.dp))

            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entry.title?.takeIf { it.isNotBlank() }?.let { title ->
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryNewInk
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Text(
                    entry.content.takeIf { it.isNotBlank() } ?: "No content yet",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.fillMaxWidth())

            Text(
                formatShortDateTime(entry.dateEpochDay, entry.timeMinutes),
                style = MaterialTheme.typography.labelSmall.copy(color = DiaryNewInkMuted)
            )
        }
    }
}

private fun formatShortDateTime(epochDay: Long, timeMinutes: Int): String {
    val date = java.time.Instant.ofEpochSecond(epochDay * 86400)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return java.time.format.DateTimeFormatter.ofPattern("d MMM")
        .withLocale(java.util.Locale.getDefault())
        .format(date) + " · " + DateTimeUtils.formatMinutes(timeMinutes)
}

private fun moodColor(key: String): Color = when (key) {
    "😊 Happy" -> DiaryNewMoodHappy
    "😌 Calm" -> DiaryNewMoodCalm
    "😔 Sad" -> DiaryNewMoodSad
    "😰 Anxious" -> DiaryNewMoodSad
    "😤 Stressed" -> DiaryNewMoodStressed
    "😠 Angry" -> DiaryNewMoodStressed
    "🤩 Excited" -> DiaryNewMoodExcited
    "🥱 Tired" -> DiaryNewMoodNeutral
    else -> DiaryNewMoodNeutral
}
