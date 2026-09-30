package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import java.time.LocalDate
import java.time.LocalTime

private const val MAX_MEMORY_CHARACTERS = 1000

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiaryEditor(
    dayEpochDay: Long,
    editing: DiaryEntity?,
    timeMinutes: Int?,
    content: String,
    onContentChange: (String) -> Unit,
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
    // One scroll state for the whole writing surface. Keeping it here (rather
    // than on an inner field) is what makes the caret come back into view and
    // the position survive the keyboard opening and closing.
    val contentScroll = rememberScrollState()

    LaunchedEffect(editing?.id) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    BackHandler(onBack = onDismiss)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // ONE writing surface, top to bottom: header, thin date/time strip, then
        // the white card that takes every remaining pixel.
        //
        // The IME inset is consumed here, once, at the root — and the navigation
        // bar is not part of this budget (see LifeOSNavHost), so this padding is
        // the only thing between the card and the keyboard. Nothing here is a
        // hardcoded height: the card's size is whatever the header, the strip and
        // the keyboard leave, so it shrinks with the IME and grows back when the
        // keyboard closes.
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.diaryHeaderVertical),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = DiaryInkViolet)
                }
                Text(
                    if (isEditing) "Edit Memory" else "New Memory",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                    fontWeight = FontWeight.Bold,
                    color = DiaryInkViolet,
                    modifier = Modifier.weight(1f)
                )
                SaveChangesAction(enabled = canSave, onClick = onSave)
            }

            DateTimeStrip(
                dateEpochDay = dayEpochDay,
                timeMinutes = timeMinutes,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true }
            )

            DiaryPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = LifeOSSpacing.screenPadding),
                cornerRadius = 22
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = DiaryInkViolet)
                    Spacer(Modifier.weight(1f))
                    Text(content.length.toString() + "/" + MAX_MEMORY_CHARACTERS, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant )
                }
                Spacer(Modifier.height(10.dp))
                // The card's single scroll viewport. The text and the media that
                // belongs to it are one content flow, so a photo, a voice note or
                // a place is part of the memory and can never end up outside it.
                // `imeNestedScroll` keeps this viewport glued to the keyboard
                // while the IME animates, and BasicTextField brings its own caret
                // into view inside it, so the active line is never behind the
                // keyboard.
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .imeNestedScroll()
                            .verticalScroll(contentScroll),
                        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.diaryEditorSection)
                    ) {
                        // The field sizes to its own text, so a short entry stays
                        // short and a long one grows to its full height and then
                        // scrolls on the card's viewport.
                        BasicTextField(
                            value = content,
                            onValueChange = { value -> onContentChange(value.take(MAX_MEMORY_CHARACTERS)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = LifeOSSpacing.diaryEditorTextMinHeight)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp),
                            cursorBrush = SolidColor(DiaryActionViolet),
                            decorationBox = { inner ->
                                Box {
                                    if (content.isEmpty()) Text("Write whatever is on your mind…", style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                                    inner()
                                }
                            }
                        )
                        attachments()
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Pinned at the foot of the card: the actions stay reachable for
                // a long entry, and can never cover the text, the caret or the
                // character counter.
                editorActions()
            }
        }
    }

    if (showDatePicker) {
        SimpleDiaryDatePicker(
            initial = DateTimeUtils.epochDayToLocalDate(dayEpochDay),
            onDismiss = { showDatePicker = false },
            onConfirm = { selected ->
                onDateChange(selected.toEpochDay())
                showDatePicker = false
            }
        )
    }
    if (showTimePicker) {
        SimpleDiaryTimePicker(
            initial = DateTimeUtils.minutesToLocalTime(timeMinutes ?: DateTimeUtils.nowMinutesOfDay()),
            onDismiss = { showTimePicker = false },
            onConfirm = { selected ->
                onTimeChange(selected.hour * 60 + selected.minute)
                showTimePicker = false
            }
        )
    }
}

/**
 * The date and the time as a single compact strip: date on the left, time on the
 * right, roughly a third of the height of the stacked two-column card it replaces.
 *
 * Both halves stay independently tappable and open exactly the pickers they
 * always did — only the presentation changed. The values keep their existing
 * typography and violet accents, and the date ellipsises rather than wrapping so
 * a long day string can never push the time off the strip at a large font scale.
 */
@Composable
private fun DateTimeStrip(dateEpochDay: Long, timeMinutes: Int?, onDateClick: () -> Unit, onTimeClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = LifeOSSpacing.screenPadding),
        shape = RoundedCornerShape(18.dp),
        color = DiaryActionViolet.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, DiaryHairline)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable(onClick = onDateClick)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dateEpochDay)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryInkViolet,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(10.dp))
            Row(
                Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onTimeClick)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    DateTimeUtils.formatMinutes(timeMinutes ?: 0),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryInkViolet,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SaveChangesAction(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).background(if (enabled) DiaryActionViolet else DiarySaveDisabled).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 15.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Save changes", style = MaterialTheme.typography.labelLarge, color = if (enabled) Color.White else DiaryInkViolet.copy(alpha = 0.42f))
    }
}

@Composable
private fun SimpleDiaryDatePicker(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var selected by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Selected: ${DateTimeUtils.formatFullDate(selected)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { selected = selected.minusDays(1) }) { Text("− 1 day") }
                    Button(onClick = { selected = selected.plusDays(1) }) { Text("+ 1 day") }
                }
                Text("Use the five-day strip to jump quickly between recent diary days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Use date", color = DiaryActionViolet) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SimpleDiaryTimePicker(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var hour by remember(initial) { mutableStateOf(initial.hour) }
    var minute by remember(initial) { mutableStateOf(initial.minute - initial.minute % 5) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(DateTimeUtils.formatMinutes(hour * 60 + minute), style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { hour = (hour - 1 + 24) % 24 }) { Text("Hour −") }
                    Button(onClick = { hour = (hour + 1) % 24 }) { Text("Hour +") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { minute = (minute - 5 + 60) % 60 }) { Text("− 5 min") }
                    Button(onClick = { minute = (minute + 5) % 60 }) { Text("+ 5 min") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) { Text("Use time", color = DiaryActionViolet) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SaveMemoryAction(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(Modifier.fillMaxWidth().clip(CircleShape).background(if (enabled) DiaryActionViolet else DiarySaveDisabled).clickable(enabled = enabled, onClick = onClick).padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
        Text(if (enabled) "Save memory →" else "Save memory", style = MaterialTheme.typography.labelLarge, color = if (enabled) Color.White else DiaryInkViolet.copy(alpha = 0.4f))
    }
}

@Composable
fun DiaryEditorOverlay(visible: Boolean, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(300)) { it / 14 },
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) + androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(200)) { it / 20 }
    ) { content() }
}