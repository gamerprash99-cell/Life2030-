package com.lifeos.app

import android.app.Application
import com.lifeos.app.core.data.AppContainer

/**
 * Application entry point. The container is created once, eagerly enough for
 * first frame, and shared through [LocalAppContainer] in Compose.
 */
class LifeOsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
