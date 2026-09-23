package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.ui.navigation.LifeOSNavHost
import com.lifeos.app.ui.onboarding.OnboardingScreen
import com.lifeos.app.ui.security.AppLockScreen
import com.lifeos.app.ui.security.DataKeyErrorScreen
import com.lifeos.app.ui.theme.LifeOSTheme
import java.io.File
import kotlinx.coroutines.launch

/** Grace period before auto-lock engages after the app leaves the foreground. */
private const val AUTO_LOCK_GRACE_MILLIS = 30_000L

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LifeOSApplication

        setContent {
            val initState by app.databaseState.collectAsState(initial = DatabaseInit.Initializing)
            val locator = app.serviceLocator
            val error = (initState as? DatabaseInit.Error)?.cause ?: app.initializationError
            val context = LocalContext.current

            when {
                locator == null || error != null -> DataKeyErrorScreen(
                    technicalDetail = error?.message,
                    onRetry = { app.retryInitialization() },
                    onExit = { (context as? ComponentActivity)?.finish() }
                )

                initState != DatabaseInit.Ready -> BrandSplash()
                else -> {
                    val serviceLocator = locator
                    val darkTheme by serviceLocator.settingsStore.darkThemeEnabled.collectAsState(initial = false)

                    LifeOSTheme(darkTheme = darkTheme) {
                        CompositionLocalProvider(LocalServiceLocator provides serviceLocator) {
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                OnboardingGate { AppLockGate { LifeOSNavHost() } }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight instant first frame shown while the encrypted database opens on
 * a background thread (see [LifeOSApplication.databaseState]). Purely static —
 * no database, no DataStore — so it renders without a stall and replaces the
 * previous black/frozen window.
 */
@Composable
private fun BrandSplash() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LifeOS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unlocking your data…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Shows the one-time onboarding flow before anything else, gated by SettingsStore.onboardingComplete. */
@Composable
private fun OnboardingGate(content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val onboardingComplete by locator.settingsStore.onboardingComplete.collectAsState(initial = false)
    var restoreStatus by remember { mutableStateOf<String?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                restoreStatus = "Validating backup…"
                runCatching {
                    val temp = File(context.cacheDir, "lifeos-onboarding-restore.json")
                    context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use(input::copyTo) }
                        ?: error("Unable to read selected file")
                    locator.backupRepository.importFromFile(temp)
                    temp.delete()
                }.onSuccess {
                    restoreStatus = "Backup restored. Welcome back to LifeOS."
                    locator.settingsStore.setOnboardingComplete(true)
                }.onFailure {
                    restoreStatus = "Restore failed: ${it.message ?: "Invalid LifeOS backup"}"
                }
            }
        }
    }

    if (onboardingComplete) {
        content()
    } else {
        OnboardingScreen(
            onFinish = { scope.launch { locator.settingsStore.setOnboardingComplete(true) } },
            onRestoreBackup = { restoreLauncher.launch(arrayOf("application/json")) },
            restoreStatus = restoreStatus
        )
    }
}

/**
 * Gates the whole app behind App Lock when [AppLockType] != NONE.
 *
 * Auto-lock (Section 6): when auto-lock is enabled and the app has been in the
 * background for longer than [AUTO_LOCK_GRACE_MILLIS], the next foreground
 * visit requires the PIN again. The short grace period prevents an external
 * file picker or permission dialog from locking the user out mid-flow.
 */
@Composable
private fun AppLockGate(content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val lockType by locator.settingsStore.appLockType.collectAsState(initial = AppLockType.NONE)
    val autoLockEnabled by locator.settingsStore.autoLockEnabled.collectAsState(initial = true)
    val lifecycleOwner = LocalLifecycleOwner.current

    var unlocked by remember { mutableStateOf(false) }
    var backgroundedAt by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(lifecycleOwner, lockType, autoLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    val since = backgroundedAt
                    if (lockType != AppLockType.NONE && autoLockEnabled && since != null &&
                        System.currentTimeMillis() - since > AUTO_LOCK_GRACE_MILLIS
                    ) {
                        unlocked = false
                    }
                    backgroundedAt = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Changing the lock configuration must require a fresh unlock.
    androidx.compose.runtime.LaunchedEffect(lockType) { unlocked = false }

    when {
        lockType == AppLockType.NONE -> content()
        unlocked -> content()
        else -> AppLockScreen(lockType = lockType, onUnlocked = { unlocked = true })
    }
}
