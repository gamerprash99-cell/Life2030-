package com.lifeos.app.ui.diary.new

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.verticalScroll
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
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryMood
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.navigation.ComposerWindowOwner
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.DiaryRuledLine
import com.lifeos.app.ui.theme.DiarySaveDisabled
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate
import java.time.LocalTime

private const val MAX_MEMORY_CHARACTERS = 1000

/** Which contextual tool palette is currently open, if any. */
enum class DiaryNewEditorPalette {
    NONE, THEME, STICKER, DRAWING, LIST, MEDIA, AUDIO
}

/**
 * Diary Editor — the full-screen composing surface.
 *
 * Completely new visual design: soft pink canvas, editorial date typography,
 * minimal writing surface, prominent pink Save action, compact anchored
 * toolbar, contextual palettes. Reuses [DiaryEditorViewModel] for all state
 * and persistence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryEditorScreen(
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

    var openPalette by remember { mutableStateOf(DiaryNewEditorPalette.NONE) }

    // Build the current draft's attachment list so the theme can be resolved.
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

    Surface(Modifier.fillMaxSize(), color = DiaryNewCanvas) {
        Column(
            Modifier
                .fillMaxSize()
                .diaryNewPaperBackground()
                .windowInsetsPadding(bottomWindowInsets)
        ) {
            // Top row: Back, title, Save
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding, vertical = DiaryNewCompactPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                    Icon(
                        ArrowBack,
                        contentDescription = "Back",
                        tint = DiaryNewInk
                    )
                }
                Text(
                    if (isEditing) "Edit entry" else "New entry",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = DiaryHandwriting,
                        fontWeight = FontWeight.Bold,
                        color = DiaryNewInk
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
                )
                DiaryNewSaveAction(isEditing = isEditing, enabled = canSave, onClick = onSave)
            }

            // Date row — editorial/handwritten treatment
            DiaryNewDateTimeRow(
                dateEpochDay = dayEpochDay,
                timeMinutes = timeMinutes ?: 0,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true }
            )

            // Writing card
            DiaryNewPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding),
                cornerRadius = 16
            ) {
                DiaryNewSectionLabel("Your entry")
                Spacer(Modifier.height(DiaryNewSectionSpacing))

                BoxWithConstraints(
                    Modifier.fillMaxWidth().fillMaxWidth()
                ) {
                    val writingMinHeight = writingSurfaceMinHeight(maxHeight)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .imeNestedScroll()
                            .verticalScroll(contentScroll),
                        verticalArrangement = Arrangement.spacedBy(DiaryNewSectionSpacing)
                    ) {
                        // Title field
                        DiaryNewTitleField(title = title, onTitleChange = onTitleChange)

                        // Writing surface
                        DiaryNewWritingSurface(Modifier.heightIn(min = writingMinHeight)) {
                            val selectionColors = TextSelectionColors(
                                handleColor = DiaryNewSave,
                                backgroundColor = DiaryNewLavender.copy(alpha = 0.35f)
                            )
                            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                                BasicTextField(
                                    value = content,
                                    onValueChange = { value -> onContentChange(value.take(MAX_MEMORY_CHARACTERS)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        lineHeight = 30.sp,
                                        fontFamily = DiaryHandwriting
                                    ),
                                    cursorBrush = SolidColor(DiaryNewSave),
                                    decorationBox = { inner ->
                                        Box {
                                            if (content.isEmpty()) {
                                                Text(
                                                    "Write whatever is on your mind…",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        lineHeight = 30.sp,
                                                        fontFamily = DiaryHandwriting
                                                    ),
                                                    color = DiaryNewInkMuted.copy(alpha = 0.55f)
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                DiaryNewCharacterCounter(length = content.length)

                // Mood selector
                DiaryNewMoodSelectorRow(
                    selectedMoodKey = mood,
                    onMoodChange = onMoodChange
                )

                HorizontalDivider(color = DiaryNewHairline)
                attachments()
            }

            HorizontalDivider(color = DiaryNewHairline)
            Spacer(Modifier.height(16.dp))
            editorActions()

            // Toolbar — compact tool buttons
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiaryNewPaletteToolButton(
                    label = "🎨 Theme",
                    selected = openPalette == DiaryNewEditorPalette.THEME,
                    onClick = {
                        openPalette = if (openPalette == DiaryNewEditorPalette.THEME) DiaryNewEditorPalette.NONE else DiaryNewEditorPalette.THEME
                    }
                )
                DiaryNewPaletteToolButton(
                    label = "😊 Stickers",
                    selected = openPalette == DiaryNewEditorPalette.STICKER,
                    onClick = {
                        openPalette = if (openPalette == DiaryNewEditorPalette.STICKER) DiaryNewEditorPalette.NONE else DiaryNewEditorPalette.STICKER
                    }
                )
                DiaryNewPaletteToolButton(
                    label = "🖊️ Drawing",
                    selected = openPalette == DiaryNewEditorPalette.DRAWING,
                    onClick = {
                        openPalette = if (openPalette == DiaryNewEditorPalette.DRAWING) DiaryNewEditorPalette.NONE else DiaryNewEditorPalette.DRAWING
                    }
                )
                DiaryNewPaletteToolButton(
                    label = "📝 List",
                    selected = openPalette == DiaryNewEditorPalette.LIST,
                    onClick = {
                        openPalette = if (openPalette == DiaryNewEditorPalette.LIST) DiaryNewEditorPalette.NONE else DiaryNewEditorPalette.LIST
                    }
                )
                DiaryNewPaletteToolButton(
                    label = "📷 Media",
                    selected = openPalette == DiaryNewEditorPalette.MEDIA,
                    onClick = {
                        openPalette = if (openPalette == DiaryNewEditorPalette.MEDIA) DiaryNewEditorPalette.NONE else DiaryNewEditorPalette.MEDIA
                    }
                )
                DiaryNewPaletteToolButton(
                    label = "🎵 Audio",
                    selected = openPalette == DiaryNewEditorPalette.AUDIO,
                    onClick = {
                        openPalette = if (openPalette == DiaryNewEditorPalette.AUDIO) DiaryNewEditorPalette.NONE else DiaryNewEditorPalette.AUDIO
                    }
                )
            }
        }

        // Contextual palette sheets
        when (openPalette) {
            DiaryNewEditorPalette.THEME -> DiaryNewThemePickerSheet(
                currentThemeKey = entryTheme?.key ?: "original",
                onThemeSelected = { theme ->
                    editorViewModel.setTheme(theme.key)
                    openPalette = DiaryNewEditorPalette.NONE
                },
                onApply = { openPalette = DiaryNewEditorPalette.NONE },
                onDismiss = { openPalette = DiaryNewEditorPalette.NONE }
            )
            DiaryNewEditorPalette.STICKER -> DiaryNewStickerPickerSheet(
                onStickerSelected = { sticker ->
                    editorViewModel.addSticker(sticker.assetKey, 0.5f, 0.5f)
                    openPalette = DiaryNewEditorPalette.NONE
                },
                onDismiss = { openPalette = DiaryNewEditorPalette.NONE }
            )
            DiaryNewEditorPalette.DRAWING -> {
                // Drawing sheet — opens full-screen drawing editor
                openPalette = DiaryNewEditorPalette.NONE
                // Handled by the drawing editor screen route
            }
            DiaryNewEditorPalette.LIST -> DiaryNewListPickerSheet(
                onTemplateSelected = { template ->
                    onContentChange((editorViewModel.state.value.content + "\n" + template.marker + " ").take(MAX_MEMORY_CHARACTERS))
                    openPalette = DiaryNewEditorPalette.NONE
                },
                onDismiss = { openPalette = DiaryNewEditorPalette.NONE }
            )
            DiaryNewEditorPalette.MEDIA, DiaryNewEditorPalette.AUDIO, DiaryNewEditorPalette.NONE -> Unit
        }

        if (showDatePicker) {
            DiaryNewDatePicker(
                initial = DateTimeUtils.epochDayToLocalDate(dayEpochDay),
                onDismiss = { showDatePicker = false },
                onConfirm = { selected ->
                    onDateChange(selected.toEpochDay())
                    showDatePicker = false
                }
            )
        }
        if (showTimePicker) {
            DiaryNewTimePicker(
                initial = DateTimeUtils.minutesToLocalTime(timeMinutes ?: DateTimeUtils.nowMinutesOfDay()),
                onDismiss = { showTimePicker = false },
                onConfirm = { selected ->
                    onTimeChange(selected.hour * 60 + selected.minute)
                    showTimePicker = false
                }
            )
        }
    }
}

// ---- Supporting composables ----

@Composable
fun DiaryNewSaveAction(isEditing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = DiaryNewMinTouchTarget)
            .clip(CircleShape)
            .background(if (enabled) DiaryNewSave else DiaryNewSaveDisabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isEditing) "Save changes" else "Save entry",
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color.White else DiaryNewInk.copy(alpha = 0.42f),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun DiaryNewDateTimeRow(
    dateEpochDay: Long,
    timeMinutes: Int,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = DiaryNewScreenPadding),
        shape = RoundedCornerShape(16.dp),
        color = DiaryNewSave.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, DiaryNewHairline)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = DiaryNewMinTouchTarget)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDateClick)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                        CalendarMonth,
                        contentDescription = null,
                        tint = DiaryNewSave,
                        modifier = Modifier.size(18.dp)
                    )
                Spacer(Modifier.size(6.dp))
                Text(
                    DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dateEpochDay)),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = DiaryHandwriting),
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryNewInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            Row(
                Modifier
                    .defaultMinSize(minHeight = DiaryNewMinTouchTarget)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onTimeClick)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = DiaryNewInkSoft,
                        modifier = Modifier.size(18.dp)
                    )
                Spacer(Modifier.size(6.dp))
                Text(
                    DateTimeUtils.formatMinutes(timeMinutes),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = DiaryHandwriting),
                    color = DiaryNewInkSoft,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun DiaryNewPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(DiaryNewPaper)
            .border(1.dp, DiaryNewBorder, RoundedCornerShape(cornerRadius.dp))
            .padding(DiaryNewCompactPadding),
        contentAlignment = Alignment.TopStart
    ) {
        content()
    }
}

@Composable
fun DiaryNewSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = DiaryNewInkSoft
        )
    )
}

@Composable
fun DiaryNewTitleField(title: String, onTitleChange: (String) -> Unit) {
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            fontFamily = DiaryHandwriting,
            fontWeight = FontWeight.Bold,
            color = DiaryNewInk
        ),
        decorationBox = { inner ->
            Box { inner() }
        }
    )
}

@Composable
fun DiaryNewWritingSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DiaryNewPaper)
            .border(1.dp, DiaryNewHairline, shape)
            .padding(DiaryNewCompactPadding)
    ) {
        content()
    }
}

@Composable
fun DiaryNewCharacterCounter(length: Int, limit: Int = MAX_MEMORY_CHARACTERS) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = "$length of $limit",
            style = MaterialTheme.typography.labelMedium.copy(color = DiaryNewInkSoft),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun DiaryNewMoodSelectorRow(
    selectedMoodKey: String?,
    onMoodChange: (String?) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Mood",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = DiaryNewInkSoft
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        DiaryMoods.OPTIONS.forEach { mood ->
            val isSelected = selectedMoodKey == mood.key
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) DiaryNewSave else DiaryNewMoodColor(mood))
                    .clickable(onClick = { onMoodChange(mood.key) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mood.emoji,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.fillMaxWidth())
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DiaryNewLavender.copy(alpha = 0.5f))
                .clickable(onClick = { onMoodChange(null) }),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "✖",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun DiaryNewPaletteToolButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(DiaryNewToolbarButtonSize)
            .clip(CircleShape)
            .background(if (selected) DiaryNewSave else DiaryNewLavender.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (selected) Color.White else DiaryNewInkSoft,
                maxLines = 1
            )
        }
    }
}

/** Mood chip background colour by mood. */
fun DiaryNewMoodColor(m: DiaryMood): Color = when (m.key) {
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

fun writingSurfaceMinHeight(
    viewport: Dp,
    minimum: Dp = LifeOSSpacing.diaryEditorTextMinHeight,
    fill: Float = LifeOSSpacing.diaryEditorWritingFill
): Dp = maxOf(minimum, viewport * fill)

/** Background painter for the Diary paper surface. */
fun Modifier.diaryNewPaperBackground(): Modifier = this.background(DiaryNewCanvas)
