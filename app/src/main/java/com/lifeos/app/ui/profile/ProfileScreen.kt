package com.lifeos.app.ui.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifeos.app.BuildConfig
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.components.ProfileAvatar
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppLock: () -> Unit = onOpenSettings
) {
    val locator = LocalServiceLocator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val name by locator.settingsStore.profileName.collectAsState(initial = null)
    var showNameDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        scope.launch { locator.settingsStore.setProfilePhotoUri(uri.toString()) }
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxWidth().padding(padding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LifeOSTopBar("Profile", "Your LifeOS space", onBack = onBack)
            Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LifeOSCard(Modifier.fillMaxWidth(), tint = LifeOSAccentLavender) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            size = 72.dp,
                            onClick = { photoPicker.launch(arrayOf("image/*")) }
                        )
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(name?.takeIf { it.isNotBlank() } ?: "Your LifeOS Profile", style = MaterialTheme.typography.titleLarge)
                            Text("Personal space · stored on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap your photo to change it", style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary, modifier = Modifier.padding(top = 4.dp))
                        }
                        Surface(onClick = { showNameDialog = true }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit display name", tint = LifeOSPrimary, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
                ProfileRow(Icons.Filled.Lock, "App Lock", "Protect LifeOS with your 4-digit PIN", onOpenAppLock)
                ProfileRow(Icons.Filled.Backup, "Local Backup", "Your backup stays under your control", onOpenSettings)
                LifeOSCard(Modifier.fillMaxWidth(), onClick = onOpenSettings) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Settings, contentDescription = null, tint = LifeOSPrimary); Column(Modifier.padding(start = 14.dp)) { Text("Settings", style = MaterialTheme.typography.titleMedium); Text("Preferences, reminders and privacy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                ProfileRow(Icons.Filled.Info, "About LifeOS", "Version ${BuildConfig.VERSION_NAME}", { showAboutDialog = true })
            }
        }
    }

    if (showNameDialog) {
        var draft by remember { mutableStateOf(name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Display name") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 40) draft = it },
                    singleLine = true,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { locator.settingsStore.setProfileName(draft) }
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About LifeOS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LifeOS ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
                    Text(
                        "A private, offline-first life manager. Your data is stored only on this device, " +
                            "encrypted at rest, and never uploaded. No account, no internet permission, no telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    LifeOSCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
                Icon(icon, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(10.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
