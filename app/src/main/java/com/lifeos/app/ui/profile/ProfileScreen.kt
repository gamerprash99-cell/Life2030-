package com.lifeos.app.ui.profile

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing

@Composable
fun ProfileScreen(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxWidth().padding(padding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LifeOSTopBar("Profile", "Your LifeOS space", onBack = onBack)
            Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LifeOSCard(Modifier.fillMaxWidth(), tint = LifeOSAccentLavender) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) { Icon(Icons.Filled.Person, contentDescription = "Profile", tint = LifeOSPrimary, modifier = Modifier.padding(16.dp)) }
                        Column(Modifier.padding(start = 14.dp)) { Text("Your LifeOS Profile", style = MaterialTheme.typography.titleLarge); Text("Personal space · stored on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                ProfileRow(Icons.Filled.Lock, "App Lock", "Protect LifeOS with your PIN")
                ProfileRow(Icons.Filled.Backup, "Local Backup", "Your backup stays under your control")
                LifeOSCard(Modifier.fillMaxWidth(), onClick = onOpenSettings) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Settings, contentDescription = null, tint = LifeOSPrimary); Column(Modifier.padding(start = 14.dp)) { Text("Settings", style = MaterialTheme.typography.titleMedium); Text("Preferences, reminders and privacy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            }
        }
    }
}

@Composable
private fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    LifeOSCard(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) { Icon(icon, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(10.dp)) }; Column(Modifier.padding(start = 12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}
