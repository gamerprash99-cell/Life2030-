package com.lifeos.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.theme.LifeOSPrimary

/**
 * The single source of truth for the user's avatar in LifeOS. Shows the photo
 * persisted in SettingsStore when present (stored as an app-private file path);
 * falls back to a letter avatar built from the display name's first letter when
 * there is no photo — or when a previously stored photo can no longer be
 * loaded — so the header never renders as a blank circle. Tapping is optional
 * so the same component works as a button (Home header) or as a static element
 * (Profile card).
 */
@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val locator = LocalServiceLocator.current
    val photoPath by locator.settingsStore.profilePhotoUri.collectAsState(initial = null)
    val name by locator.settingsStore.profileName.collectAsState(initial = null)
    var photoBroken by remember(photoPath) { mutableStateOf(false) }

    val shape = CircleShape
    val containerModifier = modifier
        .size(size)
        .clip(shape)

    val avatar: @Composable () -> Unit = {
        val path = photoPath?.takeIf { it.isNotBlank() }
        if (path != null && !photoBroken) {
            AsyncImage(
                model = path,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                onError = { photoBroken = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LetterAvatar(name = name, shape = shape)
        }
    }

    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, modifier = containerModifier) { avatar() }
    } else {
        Box(modifier = containerModifier) { avatar() }
    }
}

@Composable
private fun LetterAvatar(name: String?, shape: Shape) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .32f),
        shape = shape,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
            if (initial.isEmpty()) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = LifeOSPrimary,
                    modifier = Modifier.fillMaxSize(0.56f)
                )
            } else {
                Text(
                    initial,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSPrimary
                )
            }
        }
    }
}
