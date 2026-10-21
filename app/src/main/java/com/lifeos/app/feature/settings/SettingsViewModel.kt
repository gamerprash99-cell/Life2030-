package com.lifeos.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.ai.AiConsent
import com.lifeos.app.core.data.AppContainer
import com.lifeos.app.core.data.AppSettings
import com.lifeos.app.core.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings: appearance, money, habits heatmap window, notifications, AI consent,
 * data controls (export / backup / delete).
 *
 * The AI panel is the security-relevant one: consent is explicit, graded, and can
 * be revoked with one tap, which immediately disables every AI-derived string in
 * the app. There is no remote provider to call, and no key is stored anywhere.
 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    data class SettingsState(
        val settings: AppSettings = AppSettings(),
        val providerName: String = "",
        val providerId: String = "",
        val providerNeedsNetwork: Boolean = false,
        val statusMessage: String? = null,
        val busy: Boolean = false,
    )

    private val statusMessage = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)

    val state: StateFlow<SettingsState> = combine(
        container.settings.settings,
        statusMessage,
        busy,
    ) { settings, message, isBusy ->
        SettingsState(
            settings = settings,
            providerName = container.aiProvider.displayName,
            providerId = container.aiProvider.id,
            providerNeedsNetwork = container.aiProvider.requiresNetwork,
            statusMessage = message,
            busy = isBusy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.settings.setThemeMode(mode) }
    }

    fun setCurrency(code: String) {
        if (code.trim().length != 3) return
        viewModelScope.launch { container.settings.setCurrency(code.trim()) }
    }

    fun setHeatmapWeeks(weeks: Int) {
        viewModelScope.launch { container.settings.setHeatmapWeeks(weeks) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { container.settings.setNotificationsEnabled(enabled) }
    }

    fun setAiConsentLevel(level: Int) {
        viewModelScope.launch {
            container.settings.setAiConsentLevel(level)
            statusMessage.value = when (level) {
                AiConsent.OFF -> "AI features are off. Nothing is computed."
                else -> "AI features set to: ${AiConsent.levelName(level)}"
            }
        }
    }

    fun clearStatus() {
        statusMessage.value = null
    }

    /**
     * Builds the JSON export and hands it to [onReady] on the main thread. Nothing
     * leaves the device: the caller decides where the text goes (share sheet or a
     * user-chosen file). Failures are surfaced as a status message, never as a crash.
     */
    fun exportJson(onReady: (String) -> Unit) = runExport(onReady) { container.exporter.exportJson() }

    /** Plain-text variant of [exportJson] for quick sharing/printing. */
    fun exportText(onReady: (String) -> Unit) = runExport(onReady) { container.exporter.exportText() }

    private fun runExport(onReady: (String) -> Unit, build: suspend () -> String) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val payload = build()
                statusMessage.value = "Export ready (${payload.length} characters)."
                onReady(payload)
            } catch (error: Exception) {
                statusMessage.value = "Export failed: ${error.message ?: error::class.simpleName}"
            } finally {
                busy.value = false
            }
        }
    }
}
