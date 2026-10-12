package com.lifeos.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Non-destructive recovery screen shown when LifeOS cannot recover the
 * SQLCipher database key. This screen never offers to delete or recreate the
 * database — doing so would destroy the user's data. It explains the state and
 * offers a retry, which succeeds if the key material becomes available again.
 */
@Composable
fun DataKeyErrorScreen(technicalDetail: String?, onRetry: () -> Unit, onExit: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text("LifeOS couldn't unlock your data", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                "The encryption key for this device's encrypted database is missing or " +
                    "inaccessible. Your database has been preserved and nothing was deleted " +
                    "or rewritten.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                "To recover: restore a LifeOS backup, or restore the device key material if " +
                    "you migrated from another device. Then tap Retry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (!technicalDetail.isNullOrBlank()) {
                Text(
                    technicalDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Retry") }
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Exit LifeOS") }
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 20.dp))
        }
    }
}
