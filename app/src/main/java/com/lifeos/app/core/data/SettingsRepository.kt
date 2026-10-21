package com.lifeos.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "lifeos_settings")

/** Theme preference: follow system, or force dark / light. */
enum class ThemeMode(val code: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromCode(code: String?): ThemeMode =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

/**
 * App settings, stored in Jetpack DataStore (preferences) — not in Room, because
 * settings are a single small document, never queried or joined.
 *
 * Defaults are chosen so a fresh install behaves exactly like the spec: system
 * theme, USD is NOT assumed — currency comes from the device locale and can be
 * changed in Settings, AI is off until consent is given.
 */
class SettingsRepository(private val context: Context) {

    private val store get() = context.settingsStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromCode(prefs[KEY_THEME]),
            currencyCode = prefs[KEY_CURRENCY] ?: defaultCurrency(),
            heatmapWeeks = (prefs[KEY_HEATMAP_WEEKS] ?: 12).coerceIn(5, 26),
            onboardingDone = prefs[KEY_ONBOARDING_DONE] ?: false,
            aiConsentLevel = (prefs[KEY_AI_CONSENT] ?: 0).coerceIn(0, 3),
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            firstLaunchAt = prefs[KEY_FIRST_LAUNCH_AT] ?: 0L,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[KEY_THEME] = mode.code }

    suspend fun setCurrency(code: String) = store.edit {
        it[KEY_CURRENCY] = code.uppercase(Locale.ROOT).take(3)
    }

    suspend fun setHeatmapWeeks(weeks: Int) = store.edit {
        it[KEY_HEATMAP_WEEKS] = weeks.coerceIn(5, 26)
    }

    suspend fun setOnboardingDone(done: Boolean) = store.edit {
        it[KEY_ONBOARDING_DONE] = done
        if (done && (it[KEY_FIRST_LAUNCH_AT] ?: 0L) == 0L) {
            it[KEY_FIRST_LAUNCH_AT] = System.currentTimeMillis()
        }
    }

    /**
     * AI consent is sticky and monotonic-in-scope: raising it is explicit user
     * action; lowering it takes effect immediately (see SecurityViewModel).
     */
    suspend fun setAiConsentLevel(level: Int) = store.edit {
        it[KEY_AI_CONSENT] = level.coerceIn(0, 3)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) = store.edit {
        it[KEY_NOTIFICATIONS] = enabled
    }

    private fun defaultCurrency(): String {
        val locale: Locale = Locale.getDefault()
        return try {
            java.util.Currency.getInstance(locale).currencyCode
        } catch (_: Exception) {
            "USD"
        }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_CURRENCY = stringPreferencesKey("currency_code")
        private val KEY_HEATMAP_WEEKS = intPreferencesKey("heatmap_weeks")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_AI_CONSENT = intPreferencesKey("ai_consent_level")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_FIRST_LAUNCH_AT =
            androidx.datastore.preferences.core.longPreferencesKey("first_launch_at")
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyCode: String = "USD",
    val heatmapWeeks: Int = 12,
    val onboardingDone: Boolean = false,
    /** 0 = off, 1 = summaries on device, 2 = + tagging, 3 = + suggestions. */
    val aiConsentLevel: Int = 0,
    val notificationsEnabled: Boolean = true,
    val firstLaunchAt: Long = 0L,
)
