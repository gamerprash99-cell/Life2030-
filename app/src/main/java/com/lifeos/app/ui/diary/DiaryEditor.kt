package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * The writing surface: a full-page editorial sheet, not a bottom sheet and not
 * a form. It is presented over the whole screen (it intentionally covers the
 * bottom bar) so the only things competing with the words are the day, the
 * mood, and the save action.
 *
 * Nothing is written until [onSave] fires — the editor itself never touches the
 * database, and [saving] disables the action so a double tap cannot create a
 * duplicate entry.
 */
@Composable
fun DiaryEditor(
    dayEpochDay: Long,
    editing: DiaryEntity?,
    timeMinutes: Int?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (content: String, mood: String?) -> Unit,
    onDelete: () -> Unit
) {
    val isEditing = editing != null
    // Keyed on the entry id so switching between entries never leaks the
    // previous entry's text or mood into the new one.
    var content by rememberSaveable(editing?.id) { mutableStateOf(editing?.content.orEmpty()) }
    var mood by rememberSaveable(editing?.id) { mutableStateOf(editing?.mood) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Diary is a writing surface: once the editor opens, the cursor is ready and
    // the keyboard follows, removing the extra tap before the first thought.
    LaunchedEffect(editing?.id) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BackHandler(onBack = onDismiss)

    val canSave = content.isNotBlank() && !saving

    // Bottom inset = whichever of the keyboard / navigation bar is taller.
    // Taking the union is what stops the old `systemBars` + `imePadding` pair
    // from padding the navigation bar twice once the keyboard is up.
    val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
    val topInsets = WindowInsets.safeDrawing.exclude(bottomInsets)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                // Keeps the save row above the keyboard; the text area above it
                // shrinks, so nothing is ever covered.
                .windowInsetsPadding(topInsets)
                .windowInsetsPadding(bottomInsets)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = if (isEditing) "Close editor" else "Discard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(Modifier.weight(1f))
                if (isEditing) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Column(
                Modifier.padding(horizontal = LifeOSSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorEyebrow(dayEpochDay = dayEpochDay, isEditing = isEditing)

                Text(
                    DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dayEpochDay)),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // The exact minute this memory will be filed under, shown before
                // the save rather than discovered after it. It is captured when
                // the editor opens, so it does not shift while the user writes.
                timeMinutes?.let { minute ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            if (isEditing) {
                                "Written at ${DateTimeUtils.formatMinutes(minute)}"
                            } else {
                                DateTimeUtils.formatMinutes(minute)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                MoodSelector(
                    selectedKey = mood,
                    onSelect = { key -> mood = if (mood == key) null else key },
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(DiaryHairline))
            }

            Spacer(Modifier.height(18.dp))

            // Borderless field: the page's whitespace is the container.
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .focusRequester(focusRequester)
                    .padding(horizontal = LifeOSSpacing.screenPadding),
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
                ),
                cursorBrush = SolidColor(DiaryInkViolet),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                "Write whatever is on your mind…",
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = LifeOSSpacing.screenPadding,
                        end = LifeOSSpacing.screenPadding,
                        top = 10.dp,
                        bottom = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (val count = content.trim().length) {
                        0 -> "0 characters"
                        1 -> "1 character"
                        else -> "${count} characters"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(Modifier.weight(1f))
                SaveMemoryAction(enabled = canSave, onClick = { onSave(content, mood) })
            }
        }
    }
}

/**
 * "Save memory →" as a tinted inline action rather than a full-width filled
 * button, so it reads as part of the page instead of a form submit.
 */
@Composable
fun SaveMemoryAction(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (enabled) DiaryLavender else DiaryHairline.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (enabled) "Save memory →" else "Save memory",
            style = MaterialTheme.typography.labelLarge,
            color = DiaryInkViolet.copy(alpha = if (enabled) 1f else 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

/** Fades the whole editor in and out of the day view. */
@Composable
fun DiaryEditorOverlay(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 14 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 20 }
    ) { content() }
}
