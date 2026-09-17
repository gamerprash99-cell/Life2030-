package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.ui.navigation.LifeOSNavHost
import com.lifeos.app.ui.onboarding.OnboardingScreen
import com.lifeos.app.ui.security.AppLockScreen
import com.lifeos.app.ui.theme.LifeOSTheme
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serviceLocator = (application as LifeOSApplication).serviceLocator

        setContent {
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

/** Shows the one-time onboarding flow before anything else, gated by SettingsStore.onboardingComplete. */
@Composable
private fun OnboardingGate(content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScopeCompat()
    val context = androidx.compose.ui.platform.LocalContext.current
    val onboardingComplete by produceState<Boolean?>(initialValue = null) {
        locator.settingsStore.onboardingComplete.collect { value = it }
    }
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

    when (onboardingComplete) {
        true -> content()
        false -> OnboardingScreen(
            onFinish = { scope.launch { locator.settingsStore.setOnboardingComplete(true) } },
            onRestoreBackup = { restoreLauncher.launch(arrayOf("application/json")) },
            restoreStatus = restoreStatus
        )
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()

/**
 * Gates the whole app behind App Lock (Section 3/4 security pass) when
 * AppLockType != NONE. Delegates to AppLockScreen for the PIN path and
 * PIN recovery. Session stays unlocked until the
 * process is killed (matches the prior behavior — this is not a per-
 * background-return re-lock, which would be a separate product decision).
 */
@Composable
private fun AppLockGate(content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val lockType by locator.settingsStore.appLockType.collectAsState(initial = AppLockType.NONE)
    var unlocked by remember { mutableStateOf(false) }

    when {
        lockType == AppLockType.NONE -> content()
        unlocked -> content()
        else -> AppLockScreen(lockType = lockType, onUnlocked = { unlocked = true })
    }
}
