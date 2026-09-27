package com.lifeos.app.ui.diary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.media.DevicePhotoImporter
import com.lifeos.app.core.media.DiaryAudioRecorder
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryTextStats
import com.lifeos.app.domain.model.DiaryWeather
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Full-page view of one diary entry. */
class DiaryDetailViewModel(
    private val entryId: String,
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    val entry: StateFlow<DiaryEntity?> = diaryRepository.observeAll()
        .map { all -> all.firstOrNull { it.id == entryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor

    fun startEdit() { _showEditor.value = true }

    fun dismissEditor() { _showEditor.value = false }

    /**
     * Deletes the entry the user asked to delete.
     *
     * The screen pops this destination the instant Delete is confirmed, which
     * clears the back-stack entry and therefore this ViewModel — and with it
     * `viewModelScope`. A plain `viewModelScope.launch { delete() }` is
     * therefore *racing* its own cancellation: on a slow, encrypted commit the
     * scope can be cancelled before the row goes, and the memory silently
     * reappears on the timeline after the user was told it was deleted.
     * `NonCancellable` closes the window — the request to delete is honoured
     * once the user has made it.
     */
    fun delete(id: String) = viewModelScope.launch {
        withContext(NonCancellable) { diaryRepository.delete(id) }
    }

    /** Flips the favourite flag; the list re-renders from the same Room flow. */
    fun toggleFavorite() = viewModelScope.launch { diaryRepository.toggleFavorite(entryId) }

    /**
     * Removes one attachment in place, so the detail screen's remove
     * affordances do what they look like they do. The repository no-ops on a
     * path this entry does not hold and deletes the file once unreferenced.
     */
    fun removeAttachment(filePath: String) =
        viewModelScope.launch { diaryRepository.removeAttachment(entryId, filePath) }

    private val _weather = MutableStateFlow<DiaryWeather>(
        DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_LOCATION)
    )

    /**
     * Weather for wherever this entry was written. Resolved from the entry's own
     * stored place, so it is a real reading of a real place or an honest
     * "unavailable" — this build has no permitted weather source.
     */
    val weather: StateFlow<DiaryWeather> = _weather

    fun refreshWeather() {
        viewModelScope.launch {
            val stored = entry.value ?: return@launch
            val place = DiaryAttachments.place(DiaryAttachments.decode(stored.attachmentsJson))
            _weather.value = weatherRepository.weatherFor(place)
        }
    }
}

/**
 * One memory, opened full page. Shares the timeline's editorial language — same
 * spine, same mood marker, same leading — so arriving here from the Diary list
 * or from the Timeline feels like the same surface seen closer.
 */
@Composable
fun DiaryDetailScreen(entryId: String, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryDetailViewModel = viewModel(
        key = "diary-detail-$entryId",
        factory = LambdaViewModelFactory {
            DiaryDetailViewModel(entryId, locator.diaryRepository, locator.weatherRepository)
        }
    )
    val entry by viewModel.entry.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    // Editing here uses the same real composer as the day view, so an edit made
    // from the detail page can add a photo or a voice note too.
    val editorViewModel: DiaryEditorViewModel = viewModel(
        key = "diary-detail-editor-$entryId",
        factory = LambdaViewModelFactory {
            DiaryEditorViewModel(
                diaryRepository = locator.diaryRepository,
                weatherRepository = locator.weatherRepository,
                locationProvider = locator.deviceLocationProvider,
                recorder = DiaryAudioRecorder(locator.appContext),
                player = DiaryAudioPlayer(),
                photoImporter = DevicePhotoImporter(locator.appContext)
            )
        }
    )
    val editorState by editorViewModel.state.collectAsStateWithLifecycle()

    // Playback of the *saved* voice note: a player owned by this screen, released
    // with it, so leaving the page cannot leave a decoder running.
    val player = remember { DiaryAudioPlayer() }
    val playback by player.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { player.release() } }

    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = showEditor, onBack = viewModel::dismissEditor)
    BackHandler(enabled = !showEditor, onBack = onBack)

    val wasVisible = remember { mutableStateOf(false) }
    LaunchedEffect(entry) {
        if (entry == null) {
            if (wasVisible.value) onBack()
        } else wasVisible.value = true
    }

    LaunchedEffect(entry?.attachmentsJson) { viewModel.refreshWeather() }

    LaunchedEffect(showEditor, entryId) {
        if (showEditor) editorViewModel.start(entryId)
    }

    // Close the composer once the write is actually committed. Without this the
    // overlay stays open after a successful save, and the save button remains
    // live, so one edit could be written again and again.
    LaunchedEffect(editorState.saveCount) {
        if (editorState.saveCount > 0) {
            editorViewModel.cancelRecording()
            viewModel.dismissEditor()
        }
    }

    // `enableEdgeToEdge()` in `MainActivity` plus a bottom-bar-only Scaffold
    // means this page inherits no top inset, so the back / favourite / Edit row
    // would sit under the status bar. The day view and the composer already
    // apply their own; this is the detail page doing the same.
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        entry?.let { current ->
            val date = DateTimeUtils.epochDayToLocalDate(current.dateEpochDay)
            val hasMood = !current.mood.isNullOrBlank()

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = LifeOSSpacing.fabContentClearance)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DiaryInkViolet
                        )
                    }
                    IconButton(onClick = viewModel::toggleFavorite, modifier = Modifier.size(44.dp)) {
                        Icon(
                            if (current.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (current.isFavorite) {
                                "Remove from favourites"
                            } else {
                                "Mark as favourite"
                            },
                            tint = if (current.isFavorite) DiaryInkViolet else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(Modifier.weight(1f))
                    TextButton(onClick = viewModel::startEdit) {
                        Text("Edit", color = DiaryActionViolet)
                    }
                }

                Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding)) {
                    EditorEyebrow(dayEpochDay = current.dateEpochDay, isEditing = false)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        DateTimeUtils.formatFullDate(date),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        DateTimeUtils.formatMinutes(current.timeMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Same gutter + spine as the timeline, so the memory keeps its
                // place in the day even when read on its own.
                Row(Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding)) {
                    Box(Modifier.width(14.dp), contentAlignment = Alignment.TopCenter) {
                        if (hasMood) MoodDot(moodKey = current.mood) else {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(DiaryHairline)
                            )
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        if (hasMood) {
                            Text(
                                DiaryMoods.displayLabel(current.mood).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = DiaryMoods.colorOf(current.mood),
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            current.content,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val tags = remember(current.id, current.tagsCsv) {
                            current.tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.take(6)
                        }
                        if (tags.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                tags.joinToString("  ·  "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Everything below is decoded from the entry's own
                // `attachmentsJson`, so what is shown is what was actually saved.
                val attachments = remember(current.id, current.attachmentsJson) {
                    DiaryAttachments.decode(current.attachmentsJson)
                }
                val photos = remember(attachments) { DiaryAttachments.photos(attachments) }
                val voiceNote = remember(attachments) { DiaryAttachments.voiceNote(attachments) }
                val place = remember(attachments) { DiaryAttachments.place(attachments) }

                // Rendered unconditionally, *not* only when `photos` is
                // non-empty. The strip carries the "add" tile, so gating it on
                // having photos removed the only way to add one: a memory
                // written without photos could never get any from its own
                // detail page, and the affordance the design calls for was
                // simply absent rather than present-and-empty. A memory with no
                // photos now shows the add tile on its own.
                Spacer(Modifier.height(18.dp))
                DiaryPhotoStrip(
                    photos = photos,
                    onAdd = viewModel::startEdit,
                    onRemove = { photo -> viewModel.removeAttachment(photo.filePath) },
                    modifier = Modifier.padding(horizontal = LifeOSSpacing.screenPadding)
                )

                if (voiceNote != null) {
                    Spacer(Modifier.height(18.dp))
                    DiaryVoiceNoteRow(
                        voiceNote = voiceNote,
                        recording = RecordingState.Idle,
                        playback = playback,
                        onStartRecording = {},
                        onStopRecording = {},
                        onCancelRecording = {},
                        onTogglePlayback = { player.toggle(voiceNote.filePath) },
                        onRemove = {
                            player.stop()
                            viewModel.removeAttachment(voiceNote.filePath)
                        },
                        modifier = Modifier.padding(horizontal = LifeOSSpacing.screenPadding)
                    )
                }

                // Nothing to say until there is a place, or a real reading for one.
                val hasWeather = weather !is DiaryWeather.Unavailable
                if (place != null || hasWeather) {
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding)) {
                        if (place != null) {
                            DiaryMetaRow(
                                icon = {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = "Location",
                                value = place.placeName.takeIf { it.isNotBlank() }
                                    ?: String.format(
                                        java.util.Locale.getDefault(),
                                        "%.4f, %.4f",
                                        place.latitude,
                                        place.longitude
                                    )
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        DiaryWeatherRow(weather = weather)
                    }
                }

                // Word count is derived from the stored body every time it is
                // shown, so it can never drift from what was actually written.
                Spacer(Modifier.height(18.dp))
                Text(
                    "${DiaryTextStats.wordCount(current.content)} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = LifeOSSpacing.screenPadding)
                )

                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LifeOSSpacing.screenPadding)
                        .height(1.dp)
                        .background(DiaryHairline)
                )
                Spacer(Modifier.height(4.dp))

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.compactPadding),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = {
                        shareEntry(context, current, photos)
                    }) {
                        Text("Share", color = DiaryActionViolet)
                    }
                    TextButton(onClick = {
                        copyEntry(context, current)
                        scope.launch { snackbarHostState.showSnackbar("Memory copied") }
                    }) {
                        Text("Copy", color = DiaryActionViolet)
                    }
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete memory", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        DiaryEditorOverlay(visible = showEditor) {
            entry?.let { current ->
                DiaryEditor(
                    dayEpochDay = current.dateEpochDay,
                    editing = current,
                    // This screen only ever edits an existing memory, so the time
                    // shown is the stored one and the edit keeps it.
                    timeMinutes = editorState.timeMinutes,
                    content = editorState.content,
                    onContentChange = editorViewModel::onContentChange,
                    mood = editorState.mood,
                    onMoodChange = editorViewModel::onMoodChange,
                    canSave = editorState.canSave,
                    onDismiss = {
                        editorViewModel.cancelRecording()
                        viewModel.dismissEditor()
                    },
                    onSave = editorViewModel::save,
                    onDelete = { editorViewModel.cancelRecording(); viewModel.dismissEditor(); confirmDelete = true },
                    attachments = { DiaryEditorAttachments(editorViewModel) }
                )
            }
        }

        // The `SnackbarHostState` above is only a *channel*; without a host
        // nothing ever draws it, so `showSnackbar` resolved into the void and
        // Copy — the one action here with no visible effect of its own — left
        // the user with no idea whether it had done anything. This is what
        // actually renders the confirmation.
        //
        // No `navigationBarsPadding()` here on purpose: this page sits inside
        // the app's `NavHost`, which the bottom-bar `Scaffold` has *already*
        // inset above a bar that consumes the nav-bar inset itself. Adding it
        // again would push the confirmation twice as far up as intended.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = LifeOSSpacing.screenPadding)
                .padding(bottom = 16.dp)
        )
    }

    entry?.let { current ->
        if (confirmDelete) {
            MemoryDeleteDialog(
                dayEpochDay = current.dateEpochDay,
                timeMinutes = current.timeMinutes,
                onConfirm = {
                    confirmDelete = false
                    viewModel.delete(entryId)
                    onBack()
                },
                onDismiss = { confirmDelete = false }
            )
        }
    }
}

/**
 * Shares the memory through the system chooser. The text is content the user
 * explicitly chose to share; a photo is handed over as a single-purpose
 * FileProvider URI with a one-shot read grant, so no storage permission is
 * involved and no other app receives a lasting path.
 */
private fun shareEntry(context: Context, entry: DiaryEntity, photos: List<DiaryAttachment.Photo>) {
    val text = buildString {
        entry.title?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        appendLine(entry.content)
        appendLine()
        append(
            "${DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay))} " +
                "at ${DateTimeUtils.formatMinutes(entry.timeMinutes)}"
        )
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    photos.firstOrNull()?.let { photo ->
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(photo.filePath))
        }.getOrNull()
        if (uri != null) {
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(
        Intent.createChooser(intent, "Share memory").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/** Copies the memory to the system clipboard. Entirely on-device. */
private fun copyEntry(context: Context, entry: DiaryEntity) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val text = buildString {
        entry.title?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        append(entry.content)
    }
    clipboard.setPrimaryClip(ClipData.newPlainText("Diary memory", text))
}
