package com.lifeos.app.ui.diary

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.location.DeviceLocationProvider
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.media.DiaryAudioRecorder
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiarySaveDisabled
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.components.LifeOSTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The full-screen diary composer.
 *
 * This is the only place Diary asks for a runtime permission, and it only does
 * so in direct response to the user tapping the matching feature:
 *  - "Add photo"    → the Android Photo Picker, which needs **no** permission
 *  - "Add location" → ACCESS_COARSE/FINE_LOCATION, requested on tap
 *  - "Add voice note" → RECORD_AUDIO, requested on tap
 *
 * Nothing is requested at app launch, a granted permission is never re-asked,
 * and a permanently-denied permission routes to system Settings instead of a
 * dialog the OS would silently swallow.
 */
@Composable
fun DiaryEditorScreen(
    entryId: String?,
    onBack: () -> Unit = {},
    onOpenEntry: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val locator = LocalServiceLocator.current

    val viewModel: DiaryEditorViewModel = viewModel(
        key = "diary-editor-${entryId ?: "new"}",
        factory = LambdaViewModelFactory {
            DiaryEditorViewModel(
                diaryRepository = locator.diaryRepository,
                weatherRepository = locator.weatherRepository,
                locationProvider = DeviceLocationProvider(context.applicationContext),
                recorder = DiaryAudioRecorder(context.applicationContext),
                player = DiaryAudioPlayer(),
                appContext = context.applicationContext
            )
        }
    )

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(entryId) { viewModel.start(entryId) }

    // ---- permissions ----------------------------------------------------
    // `micPermission` / `locationPermission` drive what the chips *look* like.
    // They are deliberately not treated as truth: `remember(Unit)` does not
    // re-run when the user leaves for system Settings and comes back, so the
    // action handlers below re-read the real OS state at the point of use.
    val micPermission = remember(Unit) { mutableStateOf(checkSelf(context, Manifest.permission.RECORD_AUDIO)) }
    val locationPermission = remember(Unit) { mutableStateOf(checkSelf(context, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    var micPermanentlyDenied by remember { mutableStateOf(false) }
    var locationPermanentlyDenied by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micPermission.value = granted
        if (granted) viewModel.startRecording() else markPermanent(activity, Manifest.permission.RECORD_AUDIO) {
            micPermanentlyDenied = it
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        locationPermission.value = granted
        if (granted) {
            viewModel.attachLocation(isPermanentlyDenied = false)
        } else {
            val permanent = !results.keys.any { permission ->
                activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
            }
            locationPermanentlyDenied = permanent
            viewModel.attachLocation(isPermanentlyDenied = permanent)
        }
    }

    // ---- photo picker (no storage permission required) -------------------
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let(viewModel::attachPhoto) }

    // ---- live recording ticker ------------------------------------------
    val isRecording = state.recording is com.lifeos.app.core.media.RecordingState.Recording
    LaunchedEffect(isRecording) {
        while (isRecording) {
            viewModel.tickRecording()
            delay(RECORDING_TICK_MILLIS)
        }
    }

    // Stop the mic if the screen leaves composition mid-take.
    DisposableEffect(Unit) {
        onDispose { viewModel.cancelRecording() }
    }

    BackHandler {
        if (isRecording) viewModel.cancelRecording() else onBack()
    }

    // Real errors surface once, then clear.
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            LifeOSTopBar(
                title = if (state.isEditing) "Edit entry" else "New entry",
                subtitle = DateTimeUtils.formatFullDate(
                    DateTimeUtils.epochDayToLocalDate(state.dateEpochDay)
                ),
                onBack = { if (isRecording) viewModel.cancelRecording() else onBack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LifeOSSpacing.screenPadding)
                .padding(bottom = LifeOSSpacing.sectionSpacing),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            DateTimeSelectors(
                dateEpochDay = state.dateEpochDay,
                timeMinutes = state.timeMinutes,
                onDateChange = viewModel::onDateChange,
                onTimeChange = viewModel::onTimeChange
            )

            MoodPicker(
                selected = state.mood,
                onSelect = viewModel::onMoodChange
            )

            TextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Give this memory a title", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                colors = diaryFieldColors()
            )

            Column {
                TextField(
                    value = state.content,
                    onValueChange = { next ->
                        // Enforce the cap in the view model, not by silently
                        // truncating, so what is stored is what the user typed.
                        if (next.length <= MAX_CONTENT_LENGTH) viewModel.onContentChange(next)
                    },
                    modifier = Modifier.fillMaxWidth().height(CONTENT_FIELD_HEIGHT),
                    placeholder = { Text("Write whatever's on your mind..", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    colors = diaryFieldColors()
                )
                CharacterCounter(state.characterCount)
            }

            DiaryPhotoStrip(
                photos = state.photos,
                onAdd = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = viewModel::removePhoto
            )

            DiaryLocationRow(
                place = state.place,
                status = state.locationStatus,
                onAdd = {
                    // Re-read from the OS: the user may have just granted
                    // location in Settings and returned to us.
                    val grantedNow = checkSelf(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (grantedNow) {
                        locationPermission.value = true
                        viewModel.attachLocation(isPermanentlyDenied = false)
                    } else if (locationPermanentlyDenied) {
                        // Already asked twice: the OS would show nothing. Send
                        // the user to Settings rather than looking broken.
                        openAppSettings(context)
                    } else {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                },
                onClear = viewModel::clearLocation
            )

            DiaryWeatherRow(weather = state.weather)

            DiaryTagEditor(
                tags = state.tags,
                onAdd = viewModel::addTag,
                onRemove = viewModel::removeTag
            )

            DiaryVoiceNoteRow(
                voiceNote = state.voiceNote,
                recording = state.recording,
                playback = state.playback,
                onStartRecording = {
                    val grantedNow = checkSelf(context, Manifest.permission.RECORD_AUDIO)
                    when {
                        grantedNow -> {
                            micPermission.value = true
                            viewModel.startRecording()
                        }
                        micPermanentlyDenied -> openAppSettings(context)
                        else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = viewModel::stopRecording,
                onCancelRecording = viewModel::cancelRecording,
                onTogglePlayback = viewModel::togglePlayback,
                onRemove = viewModel::removeVoiceNote
            )

            SaveRow(
                isEditing = state.isEditing,
                isSaving = state.isSaving,
                canSave = state.canSave,
                onCancel = { if (isRecording) viewModel.cancelRecording() else onBack() },
                onSave = viewModel::save
            )
        }
    }

    // Reference: a real "Memory saved!" confirmation with "Add another memory".
    if (state.savedEntryId != null) {
        AlertDialog(
            onDismissRequest = { onBack() },
            title = { Text("Memory saved!", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your thoughts have been added to ${DateTimeUtils.formatFullDate(
                        DateTimeUtils.epochDayToLocalDate(state.dateEpochDay)
                    )}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val savedId = state.savedEntryId
                    onBack()
                    savedId?.let(onOpenEntry)
                }) { Text("View entry", color = DiaryInkViolet) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.startAnother() }) { Text("Add another memory") }
            }
        )
    }
}

/** Date + time chips. Both drive the real stored timestamp. */
@Composable
private fun DateTimeSelectors(
    dateEpochDay: Long,
    timeMinutes: Int,
    onDateChange: (Long) -> Unit,
    onTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectorChip(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
            value = DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dateEpochDay)),
            onClick = { showDatePicker = true }
        )
        SelectorChip(
            modifier = Modifier.weight(1f),
            icon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
            value = DateTimeUtils.formatMinutes(timeMinutes),
            onClick = { showTimePicker = true }
        )
    }

    if (showDatePicker) {
        val initial = DateTimeUtils.epochDayToLocalDate(dateEpochDay)
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(DateTimeUtils.zoneId()).toInstant().toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateChange(
                            java.time.Instant.ofEpochMilli(millis).atZone(DateTimeUtils.zoneId()).toLocalDate().toEpochDay()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK", color = DiaryInkViolet) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { androidx.compose.material3.DatePicker(state = pickerState) }
    }

    if (showTimePicker) {
        val initial = DateTimeUtils.minutesToLocalTime(timeMinutes)
        val timeState = androidx.compose.material3.rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = false
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(timeState.hour * 60 + timeState.minute)
                    showTimePicker = false
                }) { Text("OK", color = DiaryInkViolet) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.TimePicker(state = timeState)
                }
            }
        )
    }
}

@Composable
private fun SelectorChip(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    value: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(DiaryLavender.copy(alpha = 0.4f))
            .border(1.dp, DiaryHairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { icon() }
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = DiaryInkViolet,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** "How did the day feel?" — the same five moods the app has always stored. */
@Composable
private fun MoodPicker(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "How did the day feel?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DiaryMoods.OPTIONS.size) { index ->
                val option = DiaryMoods.OPTIONS[index]
                DiaryMoodChip(
                    option = option,
                    selected = selected == option.key,
                    onClick = { onSelect(if (selected == option.key) null else option.key) }
                )
            }
        }
    }
}

/** Tags with add/remove, persisted in the existing `tagsCsv` column. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DiaryTagEditor(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tags",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (draft.isNotBlank()) {
                TextButton(onClick = { onAdd(draft); draft = "" }) {
                    Text("Add tag", color = DiaryInkViolet)
                }
            }
        }

        if (tags.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DiaryLavender.copy(alpha = 0.55f))
                            .padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, color = DiaryInkViolet)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onRemove(tag) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", style = MaterialTheme.typography.labelSmall, color = DiaryInkViolet)
                        }
                    }
                }
            }
        }

        TextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add a tag", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = diaryFieldColors()
        )
    }
}

@Composable
private fun SaveRow(
    isEditing: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 4.dp, bottom = LifeOSSpacing.compactPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
        Button(
            onClick = onSave,
            enabled = canSave,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = DiaryLavender,
                contentColor = DiaryInkViolet,
                disabledContainerColor = DiarySaveDisabled,
                disabledContentColor = DiaryInkViolet.copy(alpha = 0.4f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp, pressedElevation = 0.dp, disabledElevation = 0.dp
            ),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.weight(1.4f)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = DiaryInkViolet
                )
            } else {
                Text(
                    if (isEditing) "Save changes" else "Save entry",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** "126 / 1000", coloured when the cap is reached. */
@Composable
private fun CharacterCounter(count: Int) {
    val over = count >= MAX_CONTENT_LENGTH
    Text(
        "$count/$MAX_CONTENT_LENGTH",
        style = MaterialTheme.typography.labelSmall,
        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.End
    )
}

@Composable
private fun diaryFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = DiaryInkViolet,
    unfocusedIndicatorColor = DiaryHairline
)

private fun checkSelf(context: android.content.Context, permission: String): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

private fun markPermanent(activity: Activity?, permission: String, onResult: (Boolean) -> Unit) {
    val canShow = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
    onResult(!canShow)
}

private fun openAppSettings(context: android.content.Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private const val MAX_CONTENT_LENGTH = 1000
private const val RECORDING_TICK_MILLIS = 100L
private val CONTENT_FIELD_HEIGHT = 200.dp
