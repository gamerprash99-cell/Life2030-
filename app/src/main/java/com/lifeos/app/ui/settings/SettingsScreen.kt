package com.lifeos.app.ui.settings

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.R
import com.lifeos.app.BuildConfig
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.reminders.ReminderScheduler
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.core.util.NotificationAccess
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.core.util.PermissionManager
import com.lifeos.app.core.util.PermissionStatus
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.data.repository.BackupRepository
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaCard
import com.lifeos.app.ui.theme.FigmaOnAccent
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.FigmaTertiary
import com.lifeos.app.ui.theme.FigmaTrack
import com.lifeos.app.ui.theme.FigmaWarning
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val appContext: android.content.Context,
    private val settingsStore: SettingsStore,
    private val backupRepository: BackupRepository,
    private val reminderRepository: com.lifeos.app.data.repository.ReminderRepository
) : ViewModel() {
    val appLockType = settingsStore.appLockType
    val darkThemeEnabled = settingsStore.darkThemeEnabled
    val remindersEnabled = settingsStore.remindersEnabled
    val autoLockEnabled = settingsStore.autoLockEnabled
    val autoCaptureExpenses = settingsStore.autoCaptureExpenses
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status
    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile: StateFlow<File?> = _lastExportedFile

    fun setDarkThemeEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setDarkThemeEnabled(enabled) }
    fun setAutoLockEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setAutoLockEnabled(enabled) }

    /**
     * Turning automatic capture on is only allowed once the system grant exists,
     * so the switch cannot promise something that would silently not happen.
     */
    fun setAutoCaptureExpenses(enabled: Boolean) = viewModelScope.launch {
        if (enabled && !NotificationAccess.isGranted(appContext)) return@launch
        settingsStore.setAutoCaptureExpenses(enabled)
    }

    /**
     * Persists the global reminder preference and makes it take effect:
     * turning reminders OFF cancels every armed exact alarm, turning them ON
     * re-arms every still-future task/habit reminder.
     */
    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setRemindersEnabled(enabled)
        ReminderScheduler.setRemindersEnabled(appContext, enabled)
        if (enabled) {
            NotificationHelper.ensureChannel(appContext)
            reminderRepository.rebuildAllActive()
        }
    }
    fun exportBackup(directory: File) = viewModelScope.launch {
        runCatching { backupRepository.exportToFile(directory, BuildConfig.VERSION_NAME) }
            .onSuccess { _lastExportedFile.value = it; _status.value = "Backup exported successfully." }
            .onFailure { _status.value = "Export failed: ${it.message ?: "Unknown error"}" }
    }
    fun importBackup(file: File) = viewModelScope.launch {
        try {
            runCatching { backupRepository.importFromFile(file) }
                .onSuccess { _status.value = "Backup restored successfully." }
                .onFailure { _status.value = "Restore failed: ${it.message ?: "Invalid backup"}" }
        } finally {
            file.delete()
        }
    }
}

@Composable
fun SettingsScreen(onOpenAppLockSetup: () -> Unit, onBack: () -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: SettingsViewModel = viewModel(factory = LambdaViewModelFactory {
        SettingsViewModel(context.applicationContext, locator.settingsStore, locator.backupRepository, locator.reminderRepository)
    })
    val appLockType by vm.appLockType.collectAsState(initial = AppLockType.NONE)
    val darkTheme by vm.darkThemeEnabled.collectAsState(initial = false)
    val remindersEnabled by vm.remindersEnabled.collectAsState(initial = true)
    val autoLockEnabled by vm.autoLockEnabled.collectAsState(initial = true)
    val autoCaptureExpenses by vm.autoCaptureExpenses.collectAsState(initial = false)
    // Re-read on resume: the grant can be given or revoked in system Settings
    // while this screen is in the background.
    var notificationAccessGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    val captureLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(captureLifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = NotificationAccess.isGranted(context)
            }
        }
        captureLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { captureLifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val status by vm.status.collectAsState()
    val exported by vm.lastExportedFile.collectAsState()

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val temp = File(context.cacheDir, "lifeos-restore.json")
                    context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use(input::copyTo) } ?: error("Unable to read selected file")
                    vm.importBackup(temp)
                }
            }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxWidth().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            LifeOSTopBar(
                title = "Your space, protected",
                subtitle = "Settings / Your life. Your data.",
                onBack = onBack
            )
            Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)) {
                StoredOnDeviceCard()

                LifeOSSectionHeader("Security & privacy")
                LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingsRow(
                            icon = Icons.Filled.Lock,
                            title = "App lock",
                            subtitle = if (appLockType == AppLockType.PIN) "PIN required to open LifeOS" else "Off · tap to set a PIN",
                            onClick = onOpenAppLockSetup
                        )
                        SettingsRow(
                            icon = Icons.Filled.Lock,
                            title = "Auto-lock",
                            subtitle = "Require your PIN after LifeOS sits in the background.",
                            trail = {
                                Switch(
                                    checked = autoLockEnabled,
                                    onCheckedChange = vm::setAutoLockEnabled,
                                    enabled = appLockType == AppLockType.PIN
                                )
                            }
                        )
                    }
                }

                LifeOSSectionHeader("Appearance")
                LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
                    SettingsRow(
                        icon = Icons.Filled.Palette,
                        title = "Slate Night",
                        subtitle = if (darkTheme) "Dark theme enabled" else "Lavender Day Calm",
                        trail = { Switch(checked = darkTheme, onCheckedChange = vm::setDarkThemeEnabled) }
                    )
                }

                LifeOSSectionHeader("Notifications & automation")
                RemindersCard(enabled = remindersEnabled, onToggle = vm::setRemindersEnabled)
                ExactAlarmsRemindersRow()
                AutoCaptureCard(
                    enabled = autoCaptureExpenses,
                    accessGranted = notificationAccessGranted,
                    onToggle = vm::setAutoCaptureExpenses,
                    onGrantAccess = {
                        runCatching { context.startActivity(NotificationAccess.appSettingsIntent(context)) }
                    }
                )

                LifeOSSectionHeader("Backup & restore", if (exported != null) "Snapshot ready" else "No backup yet")
                BackupRestoreCard(
                    status = status,
                    backupFile = exported,
                    onCreate = { vm.exportBackup(context.filesDir) },
                    onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                    onShare = { file ->
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "Share LifeOS backup"
                            )
                        )
                    }
                )
                BackupWarningCard()

                LifeOSSectionHeader("About")
                Text(
                    "LifeOS ${BuildConfig.VERSION_NAME} · Offline-first · Private by default",
                    style = MaterialTheme.typography.bodySmall,
                    color = FigmaTertiary,
                    modifier = Modifier.padding(bottom = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trail: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = FigmaText
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FigmaTertiary)
        }
        when {
            trail != null -> trail()
            onClick != null -> Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = FigmaTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** The 42dp feature plate: a faint wash, radius 14, mint glyph. */
@Composable
private fun SettingsIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FigmaTrack),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = FigmaAccent, modifier = Modifier.size(20.dp))
    }
}

/**
 * STORED ON THIS DEVICE — the Figma storage card. The size is a real walk of the
 * app's own internal directories (databases, files, preferences), measured off
 * the main thread. The redesign's usage *bar* is deliberately not drawn: there is
 * no honest per-app capacity to fill to, and a made-up denominator would be a
 * decoration pretending to be a measurement.
 */
@Composable
private fun StoredOnDeviceCard() {
    val context = LocalContext.current
    val sizeBytes by produceState<Long?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { internalDataSize(context) }
    }
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Filled.Storage, contentDescription = null, tint = FigmaTertiary, modifier = Modifier.size(16.dp))
                Text(
                    "Stored on this device",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    color = FigmaText
                )
            }
            Text(
                sizeBytes?.let(::formatBytes) ?: "Measuring…",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Medium),
                color = FigmaAccent
            )
            Text("Internal app storage · databases, files and preferences", style = MaterialTheme.typography.bodySmall, color = FigmaTertiary)
            Text("No automatic uploads · Core features work offline", style = MaterialTheme.typography.bodySmall, color = FigmaTertiary)
        }
    }
}

/**
 * BACKUP & RESTORE — the Figma card. "Create backup" runs the existing export,
 * "Restore backup" opens the existing Storage Access Framework picker, and the
 * share row appears only once a snapshot exists. The wording keeps the app's
 * honest "plaintext JSON" caveat rather than the frame's "password-protected",
 * which this build does not provide.
 */
@Composable
private fun BackupRestoreCard(
    status: String?,
    backupFile: File?,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
    onShare: (File) -> Unit
) {
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Keep a copy of your records, media and settings somewhere safe. Exports are plaintext JSON — only share them with people you trust.",
                style = MaterialTheme.typography.bodySmall,
                color = FigmaTertiary
            )
            LifeOSPrimaryButton(
                text = "Create backup",
                icon = Icons.Filled.Backup,
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth()
            )
            SettingsRow(
                icon = Icons.Filled.Restore,
                title = "Restore backup",
                subtitle = "Choose a LifeOS backup file · reviewed before replacing",
                onClick = onRestore
            )
            if (backupFile != null) {
                SettingsRow(
                    icon = Icons.Filled.Backup,
                    title = "Share exported backup",
                    subtitle = "Send the last snapshot to another app",
                    onClick = { onShare(backupFile) }
                )
            }
            Text(
                "Restores are validated against the current format before anything is replaced.",
                style = MaterialTheme.typography.bodySmall,
                color = FigmaTertiary
            )
            status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = FigmaAccent)
            }
        }
    }
}

/** The Figma warning note, in the amber the frame uses, with its local-data caveat. */
@Composable
private fun BackupWarningCard() {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FigmaWarning.copy(alpha = 0.05f))
            .border(BorderStroke(1.dp, FigmaWarning.copy(alpha = 0.16f)), shape)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = FigmaWarning, modifier = Modifier.size(16.dp))
            Text(
                "Local doesn't mean loss-proof.",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = FigmaWarning
            )
        }
        Text(
            "Device loss, failure, factory reset or uninstall can erase local data. Save a backup outside this device.",
            style = MaterialTheme.typography.bodySmall,
            color = FigmaTertiary
        )
    }
}

/** Recursively sums a directory tree, tolerating anything unreadable. */
private fun internalDataSize(context: android.content.Context): Long {
    fun dirSize(dir: File?): Long =
        if (dir == null || !dir.exists()) 0L
        else dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    val parent = context.filesDir.parentFile
    return dirSize(context.filesDir) +
        dirSize(parent?.let { File(it, "databases") }) +
        dirSize(parent?.let { File(it, "shared_prefs") })
}

/** Human-readable byte size, using binary units. */
private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> "%.0f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

@Composable
private fun RemindersCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null

    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIcon(Icons.Filled.Notifications)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Task & habit reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            !enabled -> "All reminders are silenced."
                            permission != null && !permission.isGranted -> "Reminders are on · notifications not allowed yet."
                            else -> "Notifications are requested only when enabled."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (permission != null && !permission.isGranted) {
                                // A permanently-denied permission can no longer be
                                // requested (the OS silently no-ops), so route the
                                // user to system Settings instead of "asking" nothing.
                                if (permission.status == PermissionStatus.PERMANENTLY_DENIED) permission.openSettings()
                                else permission.request()
                            }
                            NotificationHelper.ensureChannel(context)
                        }
                        onToggle(checked)
                    }
                )
            }
            // Schedule delivery is driven by the shared ReminderScheduler: exact
            // (setExactAndAllowWhileIdle) when SCHEDULE_EXACT_ALARM is granted or
            // not required, otherwise the permission-free inexact fallback.
        }
    }
}

/**
 * Settings → automatic expense capture.
 *
 * Two separate things are true here and the copy keeps them apart: the user has
 * *asked* for capture (the stored preference) and the system has *granted*
 * access. Only the combination produces expenses, so the card reports both and
 * only offers the grant request when it is actually missing.
 *
 * The explanation is deliberately specific — on-device, no bank connection, no
 * upload, high-confidence payments only, and it can be switched off — because
 * "read your notifications" deserves more than a switch and a shrug.
 */
@Composable
private fun AutoCaptureCard(
    enabled: Boolean,
    accessGranted: Boolean,
    onToggle: (Boolean) -> Unit,
    onGrantAccess: () -> Unit
) {
    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIcon(Icons.Filled.NotificationsActive)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Automatic expense capture", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            enabled && accessGranted -> "On · reading payment notifications on this device."
                            enabled -> "Waiting for notification access."
                            accessGranted -> "Off · notification access is granted."
                            else -> "Off · notification access not granted."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled && accessGranted, onCheckedChange = { checked ->
                    if (checked && !accessGranted) onGrantAccess() else onToggle(checked)
                })
            }
            Text(
                "LifeOS reads payment notifications on this device and records the amount, merchant, " +
                    "category, payment method and time. It is matched on-device, never uploaded, and LifeOS " +
                    "is not connected to your bank. Only notifications it reads with high confidence are " +
                    "recorded; anything ambiguous is left for you to add yourself. Notification text is " +
                    "never stored.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!accessGranted) {
                androidx.compose.material3.TextButton(onClick = onGrantAccess, modifier = Modifier.fillMaxWidth()) {
                    Text("Open notification access settings")
                }
            } else if (enabled) {
                androidx.compose.material3.TextButton(onClick = { onToggle(false) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Turn off automatic capture")
                }
            }
        }
    }
}

/**
 * Settings → "Alarms & reminders": read-only status of LifeOS's
 * `SCHEDULE_EXACT_ALARM` special access. Tapping it deep-links the user to the
 * Android "Alarms & reminders" system page (API 31+); on older Android the
 * permission does not exist, so the row shows a static "not required" note and
 * is not tappable. There is deliberately no in-app fake switch — the grant can
 * only be toggled by the user inside the system page. The state is re-read
 * whenever the screen resumes so returning from system Settings immediately
 * reflects the current grant.
 */
@Composable
private fun ExactAlarmsRemindersRow() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canScheduleExact by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExact = ReminderScheduler.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requiresPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val statusText = context.getString(
        when {
            !requiresPermission -> R.string.settings_alarms_reminders_not_required
            canScheduleExact -> R.string.settings_alarms_reminders_allowed
            else -> R.string.settings_alarms_reminders_not_allowed
        }
    )

    SettingsRow(
        icon = Icons.Filled.Alarm,
        title = context.getString(R.string.settings_alarms_reminders_title),
        subtitle = "${context.getString(R.string.settings_alarms_reminders_subtitle)}\n$statusText",
        onClick = if (requiresPermission) {
            { PermissionManager.openExactAlarmSettings(context) }
        } else {
            null
        }
    )
}
