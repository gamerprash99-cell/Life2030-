package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.ui.navigation.ComposerWindowOwner
import com.lifeos.app.ui.theme.Diary2ActionViolet
import com.lifeos.app.ui.theme.Diary2BodyLineHeight
import com.lifeos.app.ui.theme.Diary2Canvas
import com.lifeos.app.ui.theme.Diary2EditorTextMinHeight
import com.lifeos.app.ui.theme.Diary2EditorWritingFill
import com.lifeos.app.ui.theme.Diary2Hairline
import com.lifeos.app.ui.theme.Diary2InkSoft
import com.lifeos.app.ui.theme.Diary2InkViolet
import com.lifeos.app.ui.theme.Diary2Lavender
import com.lifeos.app.ui.theme.Diary2MarginLine
import com.lifeos.app.ui.theme.Diary2MinTouchTarget
import com.lifeos.app.ui.theme.Diary2PaperCard
import com.lifeos.app.ui.theme.Diary2RadiusMd
import com.lifeos.app.ui.theme.Diary2RadiusSm
import com.lifeos.app.ui.theme.Diary2RuledLine
import com.lifeos.app.ui.theme.Diary2SaveDisabled
import com.lifeos.app.ui.theme.Diary2ScreenPadding
import com.lifeos.app.ui.theme.Diary2SectionSpacing
import com.lifeos.app.ui.theme.Diary2SurfaceLavender
import java.time.LocalDate
import java.time.LocalTime

private const val MAX_MEMORY_CHARACTERS = 1000

private val DIARY2_EDITOR_LINE_HEIGHT = 30.sp
private val DIARY2_EDITOR_RULE_SPACING = 30.dp

/**
 * LifeOS Diary Editor — Full-screen composer (v2).
 *
 * Premium writing surface: pastel pink canvas, handwritten date strip,
 * optional title, ruled writing surface, persistent bottom toolbar above
 * IME/gesture insets, contextual tool palettes above the toolbar.
 *
 * Uses the existing [DiaryEditorViewModel] — no data-layer change.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiaryEditorScreen2(
    editorViewModel: DiaryEditorViewModel,
    dayEpochDay: Long,
    editing: DiaryEntity?,
    timeMinutes: Int?,
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    mood: String?,
    onMoodChange: (String?) -> Unit,
    onDateChange: (Long) -> Unit,
    onTimeChange: (Int) -> Unit,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    attachments: @Composable () -> Unit = {},
    editorActions: @Composable () -> Unit = {}
) {
    val isEditing = editing != null
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val contentScroll = rememberScrollState()

    ComposerWindowOwner()

    var openPalette by remember { mutableStateOf(DiaryEditorPalette.NONE) }

    val currentAttachments = buildList {
        addAll(editorViewModel.state.value.photos)
        editorViewModel.state.value.voiceNote?.let { add(it) }
        editorViewModel.state.value.place?.let { add(it) }
        editorViewModel.state.value.theme?.let { add(it) }
        addAll(editorViewModel.state.value.stickers)
        editorViewModel.state.value.drawing?.let { add(it) }
        addAll(editorViewModel.state.value.importedAudio)
    }
    val entryTheme = DiaryAttachments.themeStyle(currentAttachments)

    LaunchedEffect(editing?.id) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val imeVisible = WindowInsets.isImeVisible
    BackHandler(enabled = imeVisible) {
        val controller = keyboardController
        if (controller != null) controller.hide() else onDismiss()
    }
    BackHandler(enabled = !imeVisible, onBack = onDismiss)

    val bottomWindowInsets =
        WindowInsets.systemBars.only(WindowInsetsSides.Bottom).union(WindowInsets.ime)

    Surface(Modifier.fillMaxSize(), color = Diary2Canvas) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(bottomWindowInsets)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Diary2ScreenPadding, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Diary2BackButton(onClick = onDismiss)
                Text(
                    text = if (isEditing) "Edit entry" else "New entry",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Diary2InkViolet,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                Diary2SaveAction(isEditing = isEditing, enabled = canSave, onClick = onSave)
            }

            Diary2DateTimeStrip(
                dateEpochDay = dayEpochDay,
                timeMinutes = timeMinutes,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true }
            )

            Diary2Panel(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = Diary2ScreenPadding),
                cornerRadius = 10.dp
            ) {
                Diary2SectionLabel("Your entry")
                Spacer(Modifier.height(Diary2SectionSpacing))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .imeNestedScroll()
                        .verticalScroll(contentScroll),
                    verticalArrangement = Arrangement.spacedBy(Diary2SectionSpacing)
                ) {
                    Diary2TitleField(title = title, onTitleChange = onTitleChange)

                    Diary2WritingSurface(Modifier.height(Diary2EditorTextMinHeight)) {
                        val selectionColors = TextSelectionColors(
                            handleColor = Diary2ActionViolet,
                            backgroundColor = Diary2Lavender.copy(alpha = 0.35f)
                        )
                        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                            BasicTextField(
                                value = content,
                                onValueChange = { value -> onContentChange(value.take(MAX_MEMORY_CHARACTERS)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = DIARY2_EDITOR_LINE_HEIGHT,
                                    fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                                ),
                                cursorBrush = SolidColor(Diary2ActionViolet),
                                decorationBox = { inner ->
                                    Box {
                                        if (content.isEmpty()) {
                                            Text(
                                                text = "Write whatever is on your mind…",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    lineHeight = DIARY2_EDITOR_LINE_HEIGHT,
                                                    fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                    }

                    Diary2CharacterCounter(length = content.length)
                    Diary2MoodSelectorV2(selectedMoodKey = mood, onMoodChange = onMoodChange)
                    HorizontalDivider(color = Diary2Hairline)
                    attachments()
                }

                HorizontalDivider(color = Diary2Hairline)
                Spacer(Modifier.height(16.dp))
                editorActions()

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Diary2ScreenPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Diary2PaletteToolButton(
                        label = "🎨 Theme",
                        selected = openPalette == DiaryEditorPalette.THEME,
                        onClick = { openPalette = DiaryEditorPalette.NONE }
                    )
                    Diary2PaletteToolButton(
                        label = "😊 Stickers",
                        selected = openPalette == DiaryEditorPalette.STICKER,
                        onClick = { openPalette = DiaryEditorPalette.NONE }
                    )
                    Diary2PaletteToolButton(
                        label = "🖊️ Drawing",
                        selected = openPalette == DiaryEditorPalette.DRAWING,
                        onClick = { openPalette = DiaryEditorPalette.NONE }
                    )
                    Diary2PaletteToolButton(
                        label = "📝 List",
                        selected = openPalette == DiaryEditorPalette.LIST,
                        onClick = { openPalette = DiaryEditorPalette.NONE }
                    )
                    Diary2PaletteToolButton(
                        label = "🎵 Media",
                        selected = openPalette == DiaryEditorPalette.MEDIA,
                        onClick = { openPalette = DiaryEditorPalette.NONE }
                    )
                    Diary2PaletteToolButton(
                        label = "🎵 Audio",
                        selected = openPalette == DiaryEditorPalette.AUDIO,
                        onClick = { openPalette = DiaryEditorPalette.NONE }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        SimpleDiaryDatePickerV2(
            initial = DateTimeUtils.epochDayToLocalDate(dayEpochDay),
            onDismiss = { showDatePicker = false },
            onConfirm = { selected ->
                onDateChange(selected.toEpochDay())
                showDatePicker = false
            }
        )
    }
    if (showTimePicker) {
        SimpleDiaryTimePickerV2(
            initial = DateTimeUtils.minutesToLocalTime(timeMinutes ?: DateTimeUtils.nowMinutesOfDay()),
            onDismiss = { showTimePicker = false },
            onConfirm = { selected ->
                onTimeChange(selected.hour * 60 + selected.minute)
                showTimePicker = false
            }
        )
    }
}

/* ============================================================================
   Editor sub-components (v2)
   ============================================================================ */

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Diary2DateTimeStrip(
    dateEpochDay: Long,
    timeMinutes: Int?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = Diary2ScreenPadding),
        shape = RoundedCornerShape(16.dp),
        color = Diary2ActionViolet.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Diary2Hairline)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = Diary2MinTouchTarget)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDateClick)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = Diary2ActionViolet,
                    modifier = androidx.compose.ui.Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dateEpochDay)),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = Diary2InkViolet,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            Row(
                Modifier
                    .defaultMinSize(minHeight = Diary2MinTouchTarget)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onTimeClick)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = Diary2InkSoft,
                    modifier = androidx.compose.ui.Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = DateTimeUtils.formatMinutes(timeMinutes ?: 0),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                    ),
                    color = Diary2InkSoft,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun Diary2TitleField(title: String, onTitleChange: (String) -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
        ),
        color = Diary2InkViolet,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun Diary2WritingSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Diary2PaperCard)
            .border(1.dp, Diary2Hairline, shape)
            .padding(com.lifeos.app.ui.theme.LifeOSSpacing.compactPadding)
    ) {
        content()
    }
}

@Composable
private fun Diary2CharacterCounter(length: Int, limit: Int = MAX_MEMORY_CHARACTERS) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = "$length of $limit",
            style = MaterialTheme.typography.labelLarge,
            color = Diary2InkViolet,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun Diary2SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Diary2InkViolet,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Diary2MoodSelectorV2(selectedMoodKey: String?, onMoodChange: (String?) -> Unit) {
    Diary2SectionLabel("Mood")
    val moods = com.lifeos.app.domain.model.DiaryMoods.OPTIONS
    for (mood in moods) {
        val selected = mood.key == selectedMoodKey
        Box(
            Modifier
                .clip(CircleShape)
                .background(if (selected) Diary2ActionViolet.copy(alpha = 0.15f) else Diary2Lavender.copy(alpha = 0.3f))
                .clickable(role = Role.Button, onClick = { onMoodChange(mood.key) })
                .size(Diary2MinTouchTarget)
            ) {
                Text(
                    text = mood.emoji,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) Diary2ActionViolet else Diary2InkViolet
                )
            }
        }
    }
@Composable
private fun Diary2PaletteToolButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (selected) Diary2ActionViolet else Diary2Lavender.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (selected) Diary2ActionViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SimpleDiaryDatePickerV2(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var selected by remember(initial) { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose date", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Selected: ${DateTimeUtils.formatFullDate(selected)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = { selected = selected.minusDays(1) }) { Text("− 1 day") }
                    androidx.compose.material3.TextButton(onClick = { selected = selected.plusDays(1) }) { Text("+ 1 day") }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onConfirm(selected) }) { Text("Use date", color = Diary2ActionViolet) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SimpleDiaryTimePickerV2(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var hour by remember(initial) { mutableStateOf(initial.hour) }
    var minute by remember(initial) { mutableStateOf(initial.minute - initial.minute % 5) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose time", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = DateTimeUtils.formatMinutes(hour * 60 + minute),
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = { hour = (hour - 1 + 24) % 24 }) { Text("Hour −") }
                    androidx.compose.material3.TextButton(onClick = { hour = (hour + 1) % 24 }) { Text("Hour +") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = { minute = (minute - 5 + 60) % 60 }) { Text("− 5 min") }
                    androidx.compose.material3.TextButton(onClick = { minute = (minute + 5) % 60 }) { Text("+ 5 min") }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) { Text("Use time", color = Diary2ActionViolet) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/* ============================================================================
   Helper: writing surface min height (v2)
   ============================================================================ */

internal fun writingSurface2MinHeight(
    viewport: Dp,
    minimum: Dp = Diary2EditorTextMinHeight,
    fill: Float = Diary2EditorWritingFill
): Dp = (viewport * fill).coerceAtLeast(minimum)

/* ============================================================================
   Panel (v2)
   ============================================================================ */

@Composable
private fun Diary2Panel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier
            .clip(shape)
            .background(Diary2PaperCard)
            .border(1.dp, Diary2Hairline, shape)
    ) {
        content()
    }
}

@Composable
private fun Diary2SaveAction(isEditing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = Diary2MinTouchTarget)
            .clip(CircleShape)
            .background(if (enabled) Diary2ActionViolet else Diary2SaveDisabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isEditing) "Save changes" else "Save entry",
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color.White else Diary2InkViolet.copy(alpha = 0.5f),
            maxLines = 1,
            softWrap = false
        )
    }
}
