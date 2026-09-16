package com.lifeos.app.ui.settings

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.data.repository.BackupRepository
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSAccentPink
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(private val settingsStore: SettingsStore, private val backupRepository: BackupRepository) : ViewModel() {
    val appLockType = settingsStore.appLockType
    val aiFeaturesEnabled = settingsStore.aiFeaturesEnabled
    val darkThemeEnabled = settingsStore.darkThemeEnabled
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status
    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile: StateFlow<File?> = _lastExportedFile

    fun setAiFeaturesEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setAiFeaturesEnabled(enabled) }
    fun setDarkThemeEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setDarkThemeEnabled(enabled) }
    fun exportBackup(directory: File) = viewModelScope.launch {
        runCatching { backupRepository.exportToFile(directory, "0.1.0") }
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
fun SettingsScreen(onOpenAppLockSetup: () -> Unit) {
    val locator = LocalServiceLocator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: SettingsViewModel = viewModel(factory = LambdaViewModelFactory { SettingsViewModel(locator.settingsStore, locator.backupRepository) })
    val appLockType by vm.appLockType.collectAsState(initial = AppLockType.NONE)
    val aiEnabled by vm.aiFeaturesEnabled.collectAsState(initial = false)
    val darkTheme by vm.darkThemeEnabled.collectAsState(initial = false)
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LifeOSTopBar("Settings & Security", "Preferences, authentication and local privacy")
            Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                LifeOSCard(Modifier.fillMaxWidth(), tint = LifeOSAccentLavender) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
                            Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(12.dp))
                        }
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Zero cloud telemetry", style = MaterialTheme.typography.titleMedium)
                            Text("LifeOS data and intelligence remain on-device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                LifeOSSectionHeader("Security")
                SettingsRow(
                    Icons.Filled.Lock,
                    "App Lock",
                    if (appLockType == AppLockType.PIN) "Secure LifeOS PIN" else "Off",
                    onOpenAppLockSetup
                )

                LifeOSSectionHeader("Appearance")
                LifeOSCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIcon(Icons.Filled.Palette)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Slate Night", style = MaterialTheme.typography.titleMedium)
                            Text(if (darkTheme) "Dark theme enabled" else "Lavender Day Calm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = darkTheme, onCheckedChange = vm::setDarkThemeEnabled)
                    }
                }

                LifeOSSectionHeader("Privacy & Local Intelligence")
                LifeOSCard(Modifier.fillMaxWidth(), tint = LifeOSAccentPink) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIcon(Icons.Filled.AutoAwesome)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Local Intelligence", style = MaterialTheme.typography.titleMedium)
                            Text("Rules, lexicons, statistics and reports run offline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = aiEnabled, onCheckedChange = vm::setAiFeaturesEnabled)
                    }
                }

                LifeOSSectionHeader("Notifications / Reminders")
                RemindersCard()

                LifeOSSectionHeader("Backup & Local Storage")
                LifeOSCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIcon(Icons.Filled.Backup)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("LifeOS snapshot", style = MaterialTheme.typography.titleMedium)
                                Text("Export and restore through Android Storage Access Framework.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(onClick = { vm.exportBackup(context.filesDir) }, modifier = Modifier.fillMaxWidth()) { Text("Export LifeOS backup") }
                        TextButton(onClick = { restoreLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth()) { Text("Restore LifeOS backup") }
                        if (exported != null) {
                            TextButton(onClick = {
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exported!!)
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share LifeOS backup"))
                            }, modifier = Modifier.fillMaxWidth()) { Text("Share exported backup") }
                        }
                        status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }

                LifeOSSectionHeader("About")
                Text("LifeOS · Offline-first · Private by default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 18.dp))
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    LifeOSCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIcon(icon)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)).padding(10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RemindersCard() {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null
    LifeOSCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIcon(Icons.Filled.Notifications)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Task & habit reminders", style = MaterialTheme.typography.titleMedium)
                Text("Notifications are requested only when enabled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = permission?.isGranted ?: true, onCheckedChange = { enabled -> if (enabled) { permission?.request?.invoke(); NotificationHelper.ensureChannel(context) } })
        }
    }
}
