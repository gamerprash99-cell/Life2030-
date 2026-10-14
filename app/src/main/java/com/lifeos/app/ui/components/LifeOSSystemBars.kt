package com.lifeos.app.ui.components

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.lifeos.app.ui.theme.LifeOSBackgroundDark
import com.lifeos.app.ui.theme.LifeOSBackgroundLight

/**
 * Declares the system-bar appearance from **LifeOS's own resolved theme**,
 * rather than from the system's night-mode setting.
 *
 * Why this had to exist: `Activity.enableEdgeToEdge()` called with no arguments
 * resolves its bar styles through `SystemBarStyle.auto`, which picks the icon
 * polarity from the *system* dark-mode configuration. LifeOS's dark mode is its
 * own setting (`SettingsStore.darkThemeEnabled`, off by default), so on a
 * device in system dark mode the app painted its light lavender palette
 * (#FDF8FF) while the status bar still asked for *light* icons — a white clock,
 * white signal bars and a white battery on a near-white background. Flipping
 * dark mode in-app changed nothing either, because nothing re-evaluated the
 * style.
 *
 * `SystemBarStyle.light` = dark icons over a light scrim, `dark` = light icons
 * over a dark scrim, so the icons always contrast with what is actually painted
 * behind them. Both scrims are LifeOS's own background colour rather than the
 * framework default, which is what stops the navigation-bar / gesture strip from
 * being a grey band drawn on top of the LifeOS surface. On API 29+ the scrim is
 * fully transparent and the LifeOS background shows through the gesture area
 * directly; on API 26-28 the platform cannot make the navigation bar itself
 * transparent, so it applies this same scrim.
 *
 * Safe to call on every theme change: `enableEdgeToEdge` only sets window flags
 * and re-applies them, so this is idempotent and touches nothing in the
 * navigation or inset architecture.
 */
fun ComponentActivity.enableLifeOSEdgeToEdge(darkTheme: Boolean) {
    val scrim = (if (darkTheme) LifeOSBackgroundDark else LifeOSBackgroundLight).toArgb()
    val style = if (darkTheme) SystemBarStyle.dark(scrim) else SystemBarStyle.light(scrim, scrim)
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}
