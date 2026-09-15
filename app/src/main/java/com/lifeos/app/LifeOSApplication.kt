package com.lifeos.app

import android.app.Application
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationHelper

class LifeOSApplication : Application() {

    lateinit var serviceLocator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator.get(this)
        NotificationHelper.ensureChannel(this)
    }
}
