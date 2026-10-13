package com.lifeos.app.ui.snap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.ui.components.prismBrush
import com.lifeos.app.ui.theme.LifeOSRadius
import java.io.File
import kotlinx.coroutines.launch

/**
 * Read-back of a single Snap. Opened from the Timeline (and the Home strip) by
 * capture id. Owns favorite + delete so the whole lifecycle of a snap lives in
 * one screen.
 */
@Composable
fun SnapViewerScreen(
    captureId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    val capture by remember(captureId) { locator.captureRepository.observeById(captureId) }
        .collectAsState(initial = null)
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val item = capture ?: run {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this snap?") },
            text = { Text("The photo and its moment are removed from your timeline. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        locator.captureRepository.delete(item.id)
                        onBack()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep") }
            }
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black), Alignment.Center) {
            if (item.type == CaptureType.PHOTO && item.filePath != null) {
                AsyncImage(
                    model = File(item.filePath!!),
                    contentDescription = item.text ?: "Your snap",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(LifeOSRadius.xlarge))
                        .background(prismBrush())
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        item.text ?: "A saved moment",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
            }

            ViewerRoundButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Back",
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
            )
            Column(
                Modifier.align(Alignment.TopEnd).padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                ViewerRoundButton(
                    if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (item.isFavorite) "Remove from favorites" else "Add to favorites",
                    active = item.isFavorite,
                    onClick = { scope.launch { locator.captureRepository.setFavorite(item.id, !item.isFavorite) } }
                )
                Spacer(Modifier.height(8.dp))
                ViewerRoundButton(Icons.Filled.DeleteOutline, "Delete snap", onClick = { confirmDelete = true })
            }
        }

        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            SnapMood.fromKey(item.moodKey)?.let { mood ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(LifeOSRadius.pill))
                        .background(mood.accent.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("${mood.emoji}  ${mood.label}", style = MaterialTheme.typography.labelMedium, color = mood.accent)
                }
                Spacer(Modifier.height(10.dp))
            }
            Text(
                item.text ?: "A moment captured",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                DateTimeUtils.formatMinutes(DateTimeUtils.minutesOfDay(item.createdAt)) +
                    (item.placeLabel?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ViewerRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription,
            tint = if (active) Color(0xFFE0245E) else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
