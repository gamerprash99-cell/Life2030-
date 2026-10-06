package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.core.util.StartupTrace
import com.lifeos.app.ui.components.enableLifeOSEdgeToEdge
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
        // Android's own startup screen. It must be installed before
        // super.onCreate so the system splash covers the encrypted-database
        // open; it stays on screen (via keepOnScreenCondition below) only while
        // the one-time SQLCipher open runs, then dismisses straight into the
        // real destination (Home / App Lock / Onboarding) — no custom
        // intermediate "Unlocking your data…" screen is ever drawn.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Goes edge-to-edge immediately, before the first frame, so the system
        // splash is not letterboxed by an opaque status/navigation bar. The
        // icon *polarity* is deliberately left to LifeOSSystemBars below: the
        // no-argument overload picks it from the system's night-mode setting,
        // which is not the same thing as the theme LifeOS paints.
        enableEdgeToEdge()

        val app = application as LifeOSApplication

        // Keep the native splash on screen while the database is still opening
        // on a background thread (see LifeOSApplication.launchDatabaseOpen).
        // The condition turns false for ReadY OR Error, so a key failure
        // dismisses into the non-destructive DataKeyErrorScreen.
        splashScreen.setKeepOnScreenCondition { app.databaseState.value == DatabaseInit.Initializing }

        // Traced so the Activity's own onCreate cost can be separated from the
        // database open in a Perfetto capture (see core/util/StartupTrace.kt).
        StartupTrace.section("lifeos:MainActivity.setContent") {
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

                    else -> {
                        val serviceLocator = locator
                        val darkTheme by serviceLocator.settingsStore.darkThemeEnabled.collectAsState(initial = false)

                        // Keep the status-bar / navigation-bar icons readable
                        // against what is actually painted behind them. Re-applied
                        // on every theme change, so flipping dark mode in Settings
                        // updates the bars too (see LifeOSSystemBars.kt).
                        SideEffect { enableLifeOSEdgeToEdge(darkTheme) }

                        LifeOSTheme(darkTheme = darkTheme) {
                            CompositionLocalProvider(LocalServiceLocator provides serviceLocator) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    // Deliberately gated on Ready, and *not* composed
                                    // behind the splash. Composing it anyway would build
                                    // Home's ViewModel, whose repository dependency chain
                                    // is exactly what forces `AppDatabase.getInstance()`
                                    // — so an overlay here would quietly move the
                                    // SQLCipher open back onto the main thread. Waiting
                                    // costs nothing: the native splash is on screen
                                    // either way, and it is dismissed the moment
                                    // databaseState flips to Ready.
                                    if (initState == DatabaseInit.Ready) {
                                        OnboardingGate {
                                            AppLockGate { LifeOSNavHost() }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
    // Fail-closed: the initial null means "lock state not yet known", so the
    // first frame renders the opaque gate instead of an unlocked preview. Only
    // a resolved value of NONE (or a completed unlock) may show content.
    val lockType by locator.settingsStore.appLockType.collectAsStateWithLifecycle(initialValue = null)
    val autoLockEnabled by locator.settingsStore.autoLockEnabled.collectAsStateWithLifecycle(initialValue = true)
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

    val resolved = lockType
    when {
        // Unknown: keep the whole app covered until the real lock state resolves.
        resolved == null -> AppLockGatePlaceholder()
        resolved == AppLockType.NONE -> content()
        unlocked -> content()
        else -> AppLockScreen(lockType = resolved, onUnlocked = { unlocked = true })
    }
}

/** Opaque placeholder drawn while the lock state is still loading — never content. */
@Composable
private fun AppLockGatePlaceholder() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
}
