package com.adhan.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdhanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialization logic here
    }
}
