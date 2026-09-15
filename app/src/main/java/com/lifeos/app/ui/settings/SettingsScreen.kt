package com.lifeos.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.data.repository.BackupRepository
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val backupRepository: BackupRepository
) : ViewModel() {
    val appLockType = settingsStore.appLockType
    val aiFeaturesEnabled = settingsStore.aiFeaturesEnabled

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus

    private val _lastExportedFile = MutableStateFlow<java.io.File?>(null)
    val lastExportedFile: StateFlow<java.io.File?> = _lastExportedFile

    fun setAiFeaturesEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setAiFeaturesEnabled(enabled) }

    fun exportBackup(directory: java.io.File) {
        viewModelScope.launch {
            try {
                val file = backupRepository.exportToFile(directory, appVersion = "0.1.0")
                _lastExportedFile.value = file
                _exportStatus.value = "Exported to ${file.absolutePath}"
            } catch (e: Exception) {
                _exportStatus.value = "Export failed: ${e.message}"
            }
        }
    }
}

@Composable
fun SettingsScreen(onOpenAppLockSetup: () -> Unit) {
    val locator = LocalServiceLocator.current
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = LambdaViewModelFactory { SettingsViewModel(locator.settingsStore, locator.backupRepository) }
    )

    val appLockType by viewModel.appLockType.collectAsState(initial = AppLockType.NONE)
    val aiFeaturesEnabled by viewModel.aiFeaturesEnabled.collectAsState(initial = false)
    val exportStatus by viewModel.exportStatus.collectAsState()
    val lastExportedFile by viewModel.lastExportedFile.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("App Lock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (appLockType) {
                                AppLockType.NONE -> "Off — anyone with your unlocked phone can open LifeOS"
                                AppLockType.BIOMETRIC -> "Biometric — device fingerprint/face required"
                                AppLockType.PIN -> "App PIN — a separate PIN just for LifeOS"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = onOpenAppLockSetup) { Text("Change") }
                }
            }

            RemindersCard()

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("AI Features", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = aiFeaturesEnabled, onCheckedChange = viewModel::setAiFeaturesEnabled)
                    }
                    Text(
                        "LifeOS AI (note actions, task extraction, weekly review, chat) runs entirely on " +
                            "this device — the LifeOS Intelligence Engine analyzes your notes, tasks, habits, " +
                            "and diary locally. Nothing is ever uploaded, and no internet connection or API key is required.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Backup & Export", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your life. Your data. Export everything as a JSON file stored on your device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { viewModel.exportBackup(context.filesDir) }) { Text("Export backup") }
                        if (lastExportedFile != null) {
                            Button(
                                onClick = {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", lastExportedFile!!
                                    )
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share LifeOS backup"))
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) { Text("Share") }
                        }
                    }
                    exportStatus?.let { Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }
    }
}

/**
 * Reminders toggle — requests POST_NOTIFICATIONS only when the user turns
 * this on (Android 13+, Rule #22: permissions requested only when needed).
 * Task/Habit reminders (see core/reminders/ReminderScheduler.kt) work
 * regardless of this toggle on older Android versions, which don't require
 * the runtime permission at all.
 */
@Composable
private fun RemindersCard() {
    val context = LocalContext.current
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Reminders", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = notificationPermission?.isGranted ?: true,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            notificationPermission?.request?.invoke()
                            NotificationHelper.ensureChannel(context)
                        }
                    }
                )
            }
            Text(
                "Enable notifications for task due dates and habit reminders you set.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
