package com.lifeos.app.ui.snap

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.ui.components.lifeOSCanvas
import com.lifeos.app.ui.components.prismBrush
import com.lifeos.app.ui.theme.LifeOSRadius

/**
 * The whole Snap flow behind one route: request the camera permission only when
 * the user actually opens the camera (never at launch), take the photo, then
 * compose and post it. Keeping camera + compose in one composable with local
 * state means no captured file path has to be smuggled through nav arguments.
 */
@Composable
fun SnapScreen(
    onClose: () -> Unit,
    onPosted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf(SnapStage.CAMERA) }
    var capturedPath by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    BackHandler {
        if (stage == SnapStage.COMPOSE && capturedPath != null) {
            MediaStorage.deleteIfExists(capturedPath)
            capturedPath = null
            stage = SnapStage.CAMERA
        } else {
            onClose()
        }
    }

    when {
        !hasPermission -> SnapPermissionScreen(
            denied = permissionDenied,
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onClose = onClose,
            modifier = modifier
        )

        stage == SnapStage.CAMERA || capturedPath == null -> SnapCameraView(
            onCaptured = { path ->
                capturedPath = path
                stage = SnapStage.COMPOSE
            },
            onClose = onClose,
            modifier = modifier
        )

        else -> SnapComposeView(
            filePath = capturedPath!!,
            onRetake = {
                MediaStorage.deleteIfExists(capturedPath)
                capturedPath = null
                stage = SnapStage.CAMERA
            },
            onClose = onClose,
            onPosted = onPosted,
            modifier = modifier
        )
    }
}

@Composable
private fun SnapPermissionScreen(
    denied: Boolean,
    onRequest: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .lifeOSCanvas(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(prismBrush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (denied) "Camera is blocked" else "Snap your moment",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (denied) {
                    "Allow camera access in system settings to keep snapping your day."
                } else {
                    "LifeOS uses the camera only when you take a snap. Photos stay on your device."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            if (!denied) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(LifeOSRadius.pill))
                        .background(prismBrush(horizontal = true))
                        .clickable(onClick = onRequest),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Allow camera", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
                Spacer(Modifier.height(10.dp))
            }
            Text(
                "Not now",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(LifeOSRadius.pill))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}
