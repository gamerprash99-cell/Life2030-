package com.lifeos.app.ui.snap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.ui.components.prismBrush
import com.lifeos.app.ui.theme.LifeOSRadius
import java.io.File
import kotlinx.coroutines.launch

/**
 * The post-capture composer. Shows the just-taken photo full-bleed, lets the
 * user add a one-line caption and a mood, then writes a PHOTO capture — which
 * is what makes the moment appear on the Life Timeline (see BuildTimelineUseCase)
 * and in Home's "Today's snaps" strip.
 */
@Composable
fun SnapComposeView(
    filePath: String,
    onRetake: () -> Unit,
    onClose: () -> Unit,
    onPosted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<SnapMood?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imageFile = remember(filePath) { File(filePath) }
    LaunchedEffect(filePath) {
        if (!imageFile.exists()) error = "That snap is no longer on disk."
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(460.dp)
                .clip(RoundedCornerShape(bottomStart = LifeOSRadius.xlarge, bottomEnd = LifeOSRadius.xlarge))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageFile,
                contentDescription = "Your snap",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ComposePillButton("Retake", Icons.Filled.Replay, onRetake)
                ComposePillButton("Done", null, onClose)
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("Post your moment", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(
                "It lands on your Life Timeline — a little piece of today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 140) caption = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What's this moment?") },
                placeholder = { Text("Morning snap ☀️") },
                singleLine = true,
                shape = RoundedCornerShape(LifeOSRadius.large),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(Modifier.height(18.dp))
            Text("MOOD", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnapMood.entries.forEach { option ->
                    MoodChip(
                        mood = option,
                        selected = mood == option,
                        onClick = { mood = if (mood == option) null else option }
                    )
                }
            }

            AnimatedVisibility(visible = error != null) {
                Text(
                    error ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(22.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(LifeOSRadius.pill))
                    .background(prismBrush(horizontal = true))
                    .clickable(enabled = !saving) {
                        saving = true
                        error = null
                        scope.launch {
                            runCatching {
                                locator.captureRepository.create(
                                    type = CaptureType.PHOTO,
                                    text = caption.trim().ifBlank { null },
                                    filePath = filePath,
                                    mimeType = "image/jpeg",
                                    moodKey = mood?.key
                                )
                            }.onSuccess { onPosted() }
                                .onFailure { error = "Couldn't post your snap. ${it.message ?: ""}".trim(); saving = false }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (saving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Post to Timeline", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoodChip(mood: SnapMood, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) mood.accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val border = if (selected) mood.accent else Color.Transparent
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(mood.emoji, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ComposePillButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(LifeOSRadius.pill))
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon?.let { Icon(it, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}
