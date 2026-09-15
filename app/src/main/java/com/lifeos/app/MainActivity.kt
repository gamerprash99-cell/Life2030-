package com.lifeos.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
class MainActivity : FragmentActivity() {
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
    val onboardingComplete by locator.settingsStore.onboardingComplete.collectAsState(initial = false)

    if (onboardingComplete) {
        content()
    } else {
        OnboardingScreen(onFinish = { scope.launch { locator.settingsStore.setOnboardingComplete(true) } })
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()

/**
 * Gates the whole app behind App Lock (Section 3/4 security pass) when
 * AppLockType != NONE. Delegates to AppLockScreen for both the biometric
 * and PIN paths, plus PIN recovery. Session stays unlocked until the
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
