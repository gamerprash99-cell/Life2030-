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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
 * a form, so the only things competing with the words are the day and the save
 * action.
 *
 * It fills the whole of its own surface, which — because the app's `NavHost`
 * already sits inside the bottom-bar `Scaffold` — is the diary's content area
 * *above* the navigation bar, not the entire physical screen. The bottom bar
 * stays visible underneath and has already consumed the system navigation-bar
 * inset itself, so the editor must not add that inset a second time; the only
 * two insets it owns are the status bar at the top (this page draws behind it,
 * because `MainActivity` calls `enableEdgeToEdge()`) and the keyboard at the
 * bottom.
 *
 * Everything below the top bar lives in ONE scrollable column, sized by its
 * content:
 *
 *  - The text field is `heightIn(min = …)` and no longer `weight(1f)`. It used to
 *    claim every spare dp on the page and scroll *itself*, which is what made
 *    an empty editor look like a cavern and pinned the attachments to the
 *    screen bottom, far from the words they belong to.
 *  - One `imePadding()` on the outer column, not `windowInsetsPadding(ime)`
 *    around a non-scrolling page. The keyboard now shrinks the scrollable
 *    content area, so the focused field scrolls into view (Compose 1.7 does this
 *    for text fields inside a scrollable container) and everything below it
 *    stays reachable by scrolling. Nothing is covered and nothing jumps when
 *    the keyboard closes.
 *
 * Nothing is written until [onSave] fires — the editor itself never touches the
 * database, and [canSave] (which the caller takes from the editor state) is the
 * one authority on whether the action is live, so a double tap cannot create a
 * duplicate entry and the button is never shown enabled while saving would be
 * refused.
 *
 * The words are *owned by the caller* ([content] / [onContentChange]) rather
 * than held in local state, because the real draft also has to carry photos, a
 * voice note and a place — all of which live in one editor view model. Keeping
 * the text here too would split the draft in half and let the attachments and
 * the words disagree.
 *
 * [mood] is shown but never edited: the picker was removed, and a memory that
 * already has a stored mood still says so (see [StoredMoodChip]). Historical
 * mood values are never rewritten.
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // The page's own insets, below. Scaffold's defaults are consumed here
        // instead: its bottom inset would push the save action up by a second
        // navigation bar, because the app's bottom bar already took it.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            EditorTopBar(
                isEditing = isEditing,
                canSave = canSave,
                onBack = onDismiss,
                onSave = onSave,
                onDelete = onDelete
            )
        }
    ) { innerPadding ->
        // One scrollable page, and only one: the keyboard takes its height out of
        // this container rather than squashing the form, the top bar stays put,
        // the focused field scrolls into view (Compose 1.7 brings a text field
        // inside a scrollable container into view on focus) and the sections
        // below stay reachable by scrolling. Nothing is covered by the keyboard
        // and nothing jumps when it closes.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = LifeOSSpacing.screenPadding,
                    end = LifeOSSpacing.screenPadding,
                    bottom = LifeOSSpacing.sectionSpacing
                ),
            // One rhythm for the whole page. The five separate steps this
            // replaced (an 18dp spacer, a 10dp spacedBy, a 4dp spacer, a 2dp
            // spacer and a 10/10 padded row) added up to a blank band above the
            // attachments.
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            EditorDayBlock(
                dayEpochDay = dayEpochDay,
                isEditing = isEditing,
                timeMinutes = timeMinutes,
                mood = mood
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // A floor for the writing area, not a height: the field grows
                // with what is written and the page scrolls, so long entries are
                // never clipped.
                BasicTextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = EDITOR_MIN_HEIGHT)
                        .focusRequester(focusRequester),
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
                    ),
                    cursorBrush = SolidColor(DiaryInkViolet),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = EDITOR_MIN_HEIGHT)
                        ) {
                            if (content.isEmpty()) {
                                Text(
                                    "Write whatever is on your mind…",
                                    style = MaterialTheme.typography.bodyLarge
                                        .copy(lineHeight = 28.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.55f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Text(
                    characterCountLabel(content),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            attachments()
        }
    }
}

/**
 * The editor's own top bar: dismiss on the left, the screen's name centred, and
 * the one save action on the right. Compact, and the only place the status-bar
 * inset is applied.
 */
@Composable
private fun EditorTopBar(
    isEditing: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = if (isEditing) "Close editor" else "Discard",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Weighted rather than centred between two fixed halves: the actions
        // side is as wide as it needs to be (a label and, when editing, Delete),
        // so giving it a fixed share of the row would clip the save action on a
        // small screen at a large font scale. The title absorbs the difference
        // and ellipsizes instead.
        Text(
            text = if (isEditing) "Edit Memory" else "New Memory",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = DiaryInkViolet,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isEditing) {
                TextButton(onClick = onDelete) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }
            }
            SaveMemoryAction(enabled = canSave, onClick = onSave)
        }
    }
}

/**
 * Which memory is being written, and when: the day, the exact minute it will be
 * filed under, and — only for a memory that already has one — its stored mood.
 *
 * The minute is captured when the editor opens, so it does not shift while the
 * user writes, and it is shown before the save rather than discovered after it.
 */
@Composable
private fun EditorDayBlock(
    dayEpochDay: Long,
    isEditing: Boolean,
    timeMinutes: Int?,
    mood: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        EditorEyebrow(dayEpochDay = dayEpochDay, isEditing = isEditing)

        Text(
            DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dayEpochDay)),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Drawn only when there is something to say. An always-present row would
        // leave its own gap above the rule on a memory with neither a minute nor
        // a stored mood.
        if (timeMinutes != null || !mood.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                if (!mood.isNullOrBlank()) {
                    StoredMoodChip(moodKey = mood)
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DiaryHairline)
        )
    }
}

/** "Save changes" as a tinted inline action rather than a form submit. */
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
            .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            // The label never changes with state: swapping it resized the pill
            // on every keystroke that changed `canSave`.
            "Save changes",
            style = MaterialTheme.typography.labelLarge,
            // White on the action violet; the disabled state is a muted ink on
            // the pale disabled pill, so "you can't save yet" never looks like
            // the live button.
            color = if (enabled) Color.White else DiaryInkViolet.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun characterCountLabel(content: String): String {
    val count = content.trim().length
    return if (count == 1) "1 character" else "$count characters"
}

/** Editorial caption above the editor, e.g. the day being written for. */
@Composable
fun EditorEyebrow(dayEpochDay: Long, isEditing: Boolean, modifier: Modifier = Modifier) {
    val date = DateTimeUtils.epochDayToLocalDate(dayEpochDay)
    val eyebrow = when {
        isEditing -> "Editing a memory"
        date == DateTimeUtils.today() -> "Today"
        else -> DateTimeUtils.formatFullDate(date)
    }
    Text(
        eyebrow.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = DiaryInkViolet.copy(alpha = 0.45f),
        letterSpacing = 1.6.sp,
        modifier = modifier
    )
}

/**
 * How much room the writing area keeps when it is empty. A floor, not a height:
 * the field grows with the text and the page scrolls, so this only decides what
 * an untouched editor looks like. It is the one deliberately-chosen vertical
 * value on the page, and it is small enough that the attachments below it stay
 * on the same screen.
 */
private val EDITOR_MIN_HEIGHT = 132.dp

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
