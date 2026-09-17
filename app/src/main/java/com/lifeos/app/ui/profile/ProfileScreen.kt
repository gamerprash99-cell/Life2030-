package com.lifeos.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSTopBar
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
    val scope = rememberCoroutineScope()
    val photoUri by produceState<String?>(initialValue = null) {
        locator.settingsStore.profilePhotoUri.collect { value = it }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selected ->
            scope.launch { locator.settingsStore.setProfilePhotoUri(selected.toString()) }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxWidth().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LifeOSTopBar("Profile", "Your LifeOS space", onBack = onBack)

            Column(
                Modifier.padding(horizontal = LifeOSSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LifeOSCard(Modifier.fillMaxWidth(), tint = LifeOSAccentLavender) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = { picker.launch("image/*") },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = "Set profile photo",
                                    tint = LifeOSPrimary,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text("Your LifeOS Profile", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Personal space · stored on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            onClick = { picker.launch("image/*") },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CameraAlt, null, tint = LifeOSPrimary, modifier = Modifier.size(16.dp))
                                Text("Edit", color = LifeOSPrimary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 5.dp))
                            }
                        }
                    }
                }

                ProfileRow(Icons.Filled.Lock, "App Lock", "Protect LifeOS with your PIN", onOpenAppLock)
                ProfileRow(Icons.Filled.Backup, "Local Backup", "Export or restore from Android storage", onOpenSettings)
                LifeOSCard(Modifier.fillMaxWidth(), onClick = onOpenSettings) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = LifeOSPrimary)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("Settings", style = MaterialTheme.typography.titleMedium)
                            Text("Preferences, reminders and privacy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                LifeOSCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VerifiedUser, null, tint = LifeOSPrimary)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("Private by design", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "LifeOS keeps notes, tasks, habits and captures on this device. Nothing is uploaded unless you explicitly export it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
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
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
