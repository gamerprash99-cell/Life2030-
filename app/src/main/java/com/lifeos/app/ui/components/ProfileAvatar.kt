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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.theme.LifeOSPrimary

/**
 * The single source of truth for the user's avatar in LifeOS. Shows the photo
 * persisted in SettingsStore when present, otherwise a neutral Person icon.
 * Tapping is optional so the same component works as a button (Home header)
 * or as a static element (Profile card).
 */
@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val locator = LocalServiceLocator.current
    val photoUri by locator.settingsStore.profilePhotoUri.collectAsState(initial = null)

    val shape = CircleShape
    val containerModifier = modifier
        .size(size)
        .clip(shape)

    val avatar: @Composable () -> Unit = {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .32f), shape = shape, modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = LifeOSPrimary,
                        modifier = Modifier.size(size * 0.56f)
                    )
                }
            }
        }
    }

    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, modifier = containerModifier) { avatar() }
    } else {
        Box(modifier = containerModifier) { avatar() }
    }
}
