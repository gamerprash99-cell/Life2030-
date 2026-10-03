package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.navigation.ComposerWindowOwner
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiarySaveDisabled
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate
import java.time.LocalTime

private const val MAX_MEMORY_CHARACTERS = 1000

/**
 * Leading for the composer's text: 1.75x the 16sp body size.
 *
 * Only the leading moved. Long entries were reading as a dense block at Phase 1's
 * 1.69x, and Compose 1.7 offers no true paragraph gap (`ParagraphStyle` carries a
 * line height, not space between paragraphs), so leading is the one lever that
 * reaches the density problem without enlarging the type.
 */
private val DIARY_EDITOR_LINE_HEIGHT = 28.sp

/**
 * The writing surface's minimum height, as a share of the viewport it is drawn
 * in — the height the card actually has right now, keyboard up or down.
 *
 * This has to be the *live* viewport. Deriving it from a remembered,
 * keyboard-closed height instead makes the surface taller than the box showing
 * it for as long as the keyboard is up: the card's scroll container then has to
 * scroll to reveal the surface at all, and because the text is anchored to the
 * top of that surface it goes off the top of the screen. The editor reads as
 * empty while the character counter below it still reports the real length.
 *
 * [minimum] is the floor for a cramped window, and a short entry. Above that the
 * surface claims [fill] of the page and a long memory simply grows past it and
 * scrolls.
 */
internal fun writingSurfaceMinHeight(
    viewport: Dp,
    minimum: Dp = LifeOSSpacing.diaryEditorTextMinHeight,
    fill: Float = LifeOSSpacing.diaryEditorWritingFill
): Dp = maxOf(minimum, viewport * fill)

/**
 * The character counter's label. The 1000-character business rule is unchanged;
 * only the wording is, because "412 of 1000" is read as a quantity where
 * "412/1000" was read as a fraction of a fraction.
 */
internal fun characterCounterLabel(
    length: Int,
    limit: Int = MAX_MEMORY_CHARACTERS
): String = "$length of $limit"

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiaryEditor(
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
    // One scroll state for the whole writing surface. Keeping it here (rather
    // than on an inner field) is what makes the caret come back into view and
    // the position survive the keyboard opening and closing.
    val contentScroll = rememberScrollState()
    // The composer is a focused flow: it owns the whole window while it is
    // composed, so LifeOSNavHost keeps the bottom navigation off and hands this
    // composable no bottom padding at all.
    ComposerWindowOwner()

    LaunchedEffect(editing?.id) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Back is two-stage while the editor is up, and only because the draft is at
    // risk: the first press closes the keyboard and leaves the editor exactly
    // where it is, so a long entry cannot be thrown away by a stray Back while
    // the user is still typing. Only once the keyboard is already hidden does
    // Back dismiss, which is the behaviour the composer had before the keyboard
    // was involved at all — the nav stack is untouched either way, because
    // dismissal is still the same `onDismiss` the two call sites already pass.
    //
    // Both handlers are registered and mutually exclusive rather than relying on
    // the IME's own Back handling: this app has `enableOnBackInvokedCallback`,
    // so which layer consumes the press is a platform detail. Deciding here makes
    // the behaviour the same on every API level.
    val imeVisible = WindowInsets.isImeVisible
    BackHandler(enabled = imeVisible) {
        // Falling back to dismissal if the controller is ever null matters: the
        // alternative is a Back press that consumes itself and does nothing, and
        // a Back press that does nothing is indistinguishable from a hung app.
        val controller = keyboardController
        if (controller != null) controller.hide() else onDismiss()
    }
    BackHandler(enabled = !imeVisible, onBack = onDismiss)

    // Because the Scaffold no longer contributes anything at the bottom, the
    // composer has to inset itself — and it has to pick the right inset in BOTH
    // keyboard states. `WindowInsets.ime` is zero while the keyboard is closed
    // (it is `AndroidWindowInsets(Type.ime())`, not a system-bar union), so a
    // plain `imePadding()` would leave the writing surface under the navigation
    // bar once the keyboard closed. Taking the union on the bottom edge keeps the
    // larger of the two, which is correct for gesture navigation (the keyboard
    // covers the gesture strip) and for 3-button navigation (the keyboard sits
    // above the bar), and it is never counted twice because the top edge is left
    // to the Scaffold.
    val bottomWindowInsets =
        WindowInsets.systemBars.only(WindowInsetsSides.Bottom).union(WindowInsets.ime)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // ONE writing surface, top to bottom: a compact header, the thin
        // date/time strip, then the white card that takes every remaining pixel.
        //
        // Nothing here is a hardcoded height. The card shrinks with the keyboard,
        // which is unavoidable — that is the space the keyboard took — and the
        // writing surface inside it is a share of whatever height the card has
        // left, so it can never be taller than the box that is showing it.
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(bottomWindowInsets)
        ) {
            // One 48dp row: back and Save both keep the app's minimum touch
            // target, and the title is one non-wrapping line between them, so it
            // can never grow a second row and squeeze the card at a large font
            // scale — and it can never push Save off the edge.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.diaryHeaderVertical),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(LifeOSSpacing.minTouchTarget)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = DiaryInkViolet)
                }
                Text(
                    if (isEditing) "Edit Memory" else "New Memory",
                    // titleLarge, not headlineMedium: the writing surface is the
                    // subject of this screen, the title is not.
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    fontWeight = FontWeight.Bold,
                    color = DiaryInkViolet,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                SaveChangesAction(isEditing = isEditing, enabled = canSave, onClick = onSave)
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
                // The card's own section label. The counter used to sit beside it,
                // which put a 12sp `onSurfaceVariant` figure ~200dp above the text it
                // describes; it now lives directly under the writing surface, where
                // it is read.
                DiarySectionLabel("Your memory")
                Spacer(Modifier.height(LifeOSSpacing.diaryEditorSection))
                // The card's single scroll viewport. The text and the media that
                // belongs to it are one content flow, so a photo, a voice note or
                // a place is part of the memory and can never end up outside it.
                // `imeNestedScroll` keeps this viewport glued to the keyboard
                // while the IME animates, and BasicTextField brings its own caret
                // into view inside it, so the active line is never behind the
                // keyboard.
                //
                // `BoxWithConstraints` is what supplies the writing surface's
                // floor: the height this viewport has *right now*, after the IME
                // inset above has been subtracted. Reading it from the incoming
                // constraints rather than from a remembered measurement is the
                // whole fix — a remembered keyboard-closed height made the surface
                // taller than this viewport, and scrolling a viewport shorter than
                // its own content pushed the text off the top of the screen. There
                // is no state here to go stale on rotation or on a font-scale
                // change either, because constraints report both directly.
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val writingMinHeight = writingSurfaceMinHeight(maxHeight)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .imeNestedScroll()
                            .verticalScroll(contentScroll),
                        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.diaryEditorSection)
                    ) {
                        // Optional title, above the body. Persisted and loaded
                        // since before this screen existed — the ViewModel
                        // handler and the `title` column were both already
                        // there — but nothing ever called them, so a title could
                        // only arrive from an import or a backup.
                        //
                        // Optional and blank-by-default on purpose: requiring it
                        // would be a behaviour change for every existing user,
                        // and most memories are a paragraph, not a headline.
                        DiaryTitleField(title = title, onTitleChange = onTitleChange)
                        // The writing surface: a rounded, tinted page inside the
                        // card, so the place you write on is bounded and clearly
                        // separate from the memory's metadata underneath.
                        WritingSurface(Modifier.heightIn(min = writingMinHeight)) {
                            // The field sizes to its own text, so a short entry
                            // stays short and a long one grows to its full height
                            // and then scrolls on the card's viewport.
                            //
                            // The selection handle and highlight are themed here
                            // rather than left to the defaults, which are drawn in
                            // `colorScheme.primary` — a pale lavender that all but
                            // vanished against this surface's own lavender wash.
                            // This is the standard CompositionLocal and nothing
                            // more: selection, the long-press toolbar and every
                            // accessibility affordance of BasicTextField are
                            // untouched.
                            val selectionColors = TextSelectionColors(
                                handleColor = DiaryActionViolet,
                                backgroundColor = DiaryLavender.copy(alpha = 0.35f)
                            )
                            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                                BasicTextField(
                                    value = content,
                                    onValueChange = { value -> onContentChange(value.take(MAX_MEMORY_CHARACTERS)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    // 16sp with 1.75x leading. The size is untouched
                                    // from Phase 1 — the extra leading is what stops
                                    // a 1000-character memory from reading as a
                                    // dense block, and it is the only paragraph
                                    // separation Compose can express here: 1.7's
                                    // `ParagraphStyle` carries a line height, not a
                                    // space between paragraphs.
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = DIARY_EDITOR_LINE_HEIGHT),
                                    cursorBrush = SolidColor(DiaryActionViolet),
                                    decorationBox = { inner ->
                                        Box {
                                            if (content.isEmpty()) Text("Write whatever is on your mind…", style = MaterialTheme.typography.bodyLarge.copy(lineHeight = DIARY_EDITOR_LINE_HEIGHT), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                                            inner()
                                        }
                                    }
                                )
                            }
                        }
                        // The counter sits immediately under the text it counts, and
                        // before the metadata, so the card reads in the order it is
                        // actually used: writing, then how much of it there is, then
                        // what is attached to it.
                        CharacterCounter(length = content.length)
                        // Mood, once the words are down. Its handler existed and
                        // had no caller, so this is the wiring rather than new
                        // state — and it is what makes the calendar's day dots,
                        // the insights chart and the mood distribution have
                        // anything to show at all.
                        DiaryMoodSelector(selectedMoodKey = mood, onMoodChange = onMoodChange)
                        // A hairline between the writing and everything that
                        // describes it. Without this the metadata simply
                        // continued off the bottom of the writing surface, so
                        // on a long entry the first tag or photo read as part of
                        // the text. `attachments()` is still one slot with one
                        // caller-facing signature, so both host screens are
                        // unchanged, and the 20dp the column's own `spacedBy`
                        // applies supplies every gap below the line.
                        //
                        // There is deliberately no umbrella label here. The slot
                        // used to sit under a single "Details" heading, but
                        // every one of its children already names itself, so
                        // that produced a nested sandwich (Details > Tags /
                        // Attachments > Photos / Voice note / Location). The slot
                        // now presents those two labelled groups of its own, so
                        // the separation this divider draws is stated more
                        // precisely than one umbrella label could.
                        HorizontalDivider(color = DiaryHairline)
                        attachments()
                    }
                }
                // The card footer: the actions are already structurally pinned
                // (the scrolling viewport above carries `weight(1f)`, so nothing
                // in it can displace them), and this hairline plus the wider
                // spacer give that fixed position a visible edge. Without it the
                // 8dp gap read as one more gap in the scrolling column and the
                // controls looked like they belonged to the writing surface, at
                // an arbitrary distance below the last line of a short entry.
                HorizontalDivider(color = DiaryHairline)
                Spacer(Modifier.height(16.dp))
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
 * The character counter, directly under the writing surface.
 *
 * It was the weakest element in the card — `labelMedium` in `onSurfaceVariant`,
 * parked in the top-right corner a couple of hundred dp above the text whose
 * length it reports. It is now `labelLarge` in the Diary ink, so it is actually
 * legible, and it sits against the right edge of the text it counts. It is still
 * smaller than the `bodyLarge` it describes, so the text stays the subject.
 *
 * The 1000-character rule is untouched: the `take` in the field's `onValueChange`
 * and the identical guard in `DiaryEditorViewModel` are the only places a limit is
 * enforced, and both are unchanged. This composable only reports.
 */
@Composable
private fun CharacterCounter(length: Int, limit: Int = MAX_MEMORY_CHARACTERS) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = characterCounterLabel(length, limit),
            style = MaterialTheme.typography.labelLarge,
            color = DiaryInkViolet,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * The writing surface itself: a rounded, faintly tinted page inside the white
 * memory card.
 *
 * The card is near-white on a near-white background, so before this the text sat
 * directly on the card with nothing around it and the writing area had no
 * boundary at all. A hairline edge plus a pale lavender wash — both existing
 * Diary tokens, and the same rounded-surface language the voice/location rows
 * already use — separates it from the card's own label row above and from the
 * memory's metadata below. Its edge is flush with those rows' own panels and its
 * inner padding is the shared `compactPadding`, so everything in the card lines
 * up on one left edge.
 */
@Composable
private fun WritingSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DiaryLavender.copy(alpha = 0.22f))
            .border(1.dp, DiaryHairline, shape)
            .padding(LifeOSSpacing.compactPadding)
    ) {
        content()
    }
}

/**
 * The date and the time as a single compact strip: date on the left, time on the
 * right, roughly a third of the height of the stacked two-column card it replaces.
 *
 * The date is the primary item (semibold, in the Diary ink); the time is the
 * secondary one (regular, in the muted colour). Both are stepped down to
 * `labelLarge`, because `d MMMM yyyy` and `h:mm a` together no longer fit a small
 * phone at `bodyLarge` — the date was being ellipsised mid-word, which is the
 * opposite of readable. The strip has no vertical padding of its own: its height
 * is exactly the minimum touch height of a half, so it stays as short as before
 * while each half becomes a real target instead of a ~26dp one. Both halves still
 * open exactly the picker they always did.
 */
@Composable
private fun DateTimeStrip(dateEpochDay: Long, timeMinutes: Int?, onDateClick: () -> Unit, onTimeClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = LifeOSSpacing.screenPadding),
        shape = RoundedCornerShape(16.dp),
        color = DiaryActionViolet.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, DiaryHairline)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = LifeOSSpacing.diaryDateStripMinTouch)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDateClick)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dateEpochDay)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryInkViolet,
                    // Two lines, not one. The full `d MMMM yyyy` is worth
                    // showing in full, and at a large font scale it stopped
                    // fitting the strip's weighted width and was cut mid-word
                    // ("26 Septem…"). It is a sentence of two words plus a
                    // year, so it wraps cleanly at the spaces instead and
                    // stays complete and tappable; the strip is above the
                    // weighted card, so the extra line only ever costs the
                    // writing surface the height it needs. `Ellipsis` stays
                    // as the clamp for scales too large even two lines to
                    // hold. Nothing about the formatter changed.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            Row(
                Modifier
                    .defaultMinSize(minHeight = LifeOSSpacing.diaryDateStripMinTouch)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onTimeClick)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    DateTimeUtils.formatMinutes(timeMinutes ?: 0),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Save, as a tonal pill rather than a filled block: it is the composer's primary
 * action but it is still secondary to the writing surface, and a saturated
 * rectangle in the same band as the top of the editor competed with the text.
 * `defaultMinSize` keeps it on the app's 48dp minimum touch target, so the row it
 * shares with the back button and the title is exactly one target tall.
 */
@Composable
private fun SaveChangesAction(isEditing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
            .clip(CircleShape)
            .background(if (enabled) DiaryLavender.copy(alpha = 0.72f) else DiarySaveDisabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isEditing) "Save changes" else "Save memory",
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) DiaryInkViolet else DiaryInkViolet.copy(alpha = 0.42f),
            maxLines = 1,
            softWrap = false
        )
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
fun DiaryEditorOverlay(visible: Boolean, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(300)) { it / 14 },
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) + androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(200)) { it / 20 }
    ) { content() }
}