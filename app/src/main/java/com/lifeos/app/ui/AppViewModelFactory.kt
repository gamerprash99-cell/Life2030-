package com.lifeos.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lifeos.app.core.data.AppContainer

/**
 * Composition-local access to the manual DI container.
 * ViewModels are created with [AppViewModelFactory], which reads the same graph.
 */
val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap the app in LocalAppContainer.Provider")
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when {
            modelClass.isAssignableFrom(com.lifeos.app.feature.home.HomeViewModel::class.java) ->
                com.lifeos.app.feature.home.HomeViewModel(container)
            modelClass.isAssignableFrom(com.lifeos.app.feature.notes.NotesViewModel::class.java) ->
                com.lifeos.app.feature.notes.NotesViewModel(container)
            modelClass.isAssignableFrom(com.lifeos.app.feature.capture.CaptureViewModel::class.java) ->
                com.lifeos.app.feature.capture.CaptureViewModel(container)
            modelClass.isAssignableFrom(com.lifeos.app.feature.tracking.TrackingViewModel::class.java) ->
                com.lifeos.app.feature.tracking.TrackingViewModel(container)
            modelClass.isAssignableFrom(com.lifeos.app.feature.insights.InsightsViewModel::class.java) ->
                com.lifeos.app.feature.insights.InsightsViewModel(container)
            modelClass.isAssignableFrom(com.lifeos.app.feature.life.LifeViewModel::class.java) ->
                com.lifeos.app.feature.life.LifeViewModel(container)
            modelClass.isAssignableFrom(com.lifeos.app.feature.search.SearchViewModel::class.java) ->
                com.lifeos.app.feature.search.SearchViewModel(container)
            modelClass.isAssignableFrom(com.lifeos.app.feature.settings.SettingsViewModel::class.java) ->
                com.lifeos.app.feature.settings.SettingsViewModel(container)
            else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
        }
        return viewModel as T
    }
}
