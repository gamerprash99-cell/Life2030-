package com.lifeos.app.core.di

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

val LocalServiceLocator = staticCompositionLocalOf<ServiceLocator> {
    error("ServiceLocator not provided — wrap the app in CompositionLocalProvider(LocalServiceLocator provides ...)")
}

/** Tiny generic ViewModelProvider.Factory so every ViewModel can take constructor params without Hilt. */
class LambdaViewModelFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>, extras: CreationExtras): VM = create() as VM
}
