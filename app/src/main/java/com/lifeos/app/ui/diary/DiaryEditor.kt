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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiarySaveDisabled
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * The writing surface: a full-page editorial sheet, not a bottom sheet and not
 * a form, so the only things competing with the words are the day, the mood, and
 * the save action.
 *
 * It fills the whole of its own surface, which — because the app's `NavHost`
 * already sits inside the bottom-bar `Scaffold` — is the diary's content area
 * *above* the navigation bar, not the entire physical screen. The bottom bar
 * stays visible underneath; the editor consumes the navigation-bar and IME
 * insets itself so the save action is never left under the keyboard.
 *
 * Nothing is written until [onSave] fires — the editor itself never touches the
 * database, and [canSave] (which the caller takes from the editor state) is the
 * one authority on whether the action is live, so a double tap cannot create a
 * duplicate entry and the button is never shown enabled while saving would be
 * refused.
 *
 * The words and the mood are *owned by the caller* ([content] / [onContentChange]
 * and [mood] / [onMoodChange]) rather than held in local state, because the real
 * draft also has to carry photos, a voice note and a place — all of which live
 * in one editor view model. Keeping the text here too would split the draft in
 * half and let the attachments and the words disagree.
 *
 * [attachments] is the seam for everything that hangs off an entry: photos, the
 * voice note, the place and the weather. It is a slot rather than a fixed set of
 * parameters so this composable stays the presentation layer and the editor
 * screen decides what those affordances do.
 */
@Composable
fun DiaryEditor(
    dayEpochDay: Long,
    editing: DiaryEntity?,
    timeMinutes: Int?,
    content: String,
    onContentChange: (String) -> Unit,
    mood: String?,
    onMoodChange: (String?) -> Unit,
    /**
     * Whether Save is genuinely available right now, taken straight from
     * `DiaryEditorState.canSave`.
     *
     * The editor used to re-derive this locally as `content.isNotBlank() &&
     * !saving`, which was a *weaker* rule than the ViewModel's. Two of the
     * ViewModel's own guards were missing from the button: the row still
     * loading, and a take still recording. In both cases the pill rendered
     * fully enabled and tapping it hit an early `return` in `save()` — a live
     * control that silently does nothing, which is worse than one that is
     * honestly disabled. The single source of truth is now the state property.
     */
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    attachments: @Composable () -> Unit = {}
) {
    val isEditing = editing != null

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Diary is a writing surface: once the editor opens, the cursor is ready and
    // the keyboard follows, removing the extra tap before the first thought.
    LaunchedEffect(editing?.id) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BackHandler(onBack = onDismiss)

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
                    onSelect = { key -> onMoodChange(if (mood == key) null else key) },
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(DiaryHairline))
            }

            Spacer(Modifier.height(18.dp))

            // Borderless field: the page's whitespace is the container.
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
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

            attachments()

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
                SaveMemoryAction(enabled = canSave, onClick = onSave)
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
            .background(if (enabled) DiaryActionViolet else DiarySaveDisabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (enabled) "Save memory →" else "Save memory",
            style = MaterialTheme.typography.labelLarge,
            // White on the action violet; the disabled state is a muted ink on
            // the pale disabled pill, so "you can't save yet" never looks like
            // the live button.
            color = if (enabled) Color.White else DiaryInkViolet.copy(alpha = 0.4f),
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
