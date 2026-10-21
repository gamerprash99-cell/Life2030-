package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lifeos.app.core.data.ThemeMode
import com.lifeos.app.core.designsystem.theme.LifeOSTheme
import com.lifeos.app.ui.LifeOsApp
import com.lifeos.app.ui.LocalAppContainer

/**
 * Single-activity host. The activity itself owns only theme resolution; screens
 * never touch Android framework APIs directly (see ARCHITECTURE.md).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as LifeOsApplication).container

        setContent {
            val settings by container.settings.settings
                .collectAsState(initial = com.lifeos.app.core.data.AppSettings())

            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                LifeOSTheme(darkTheme = dark) {
                    LifeOsApp()
                }
            }
        }
    }
}
