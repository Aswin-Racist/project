package com.adventure.solo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class SoloAdventureApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize OpenStreetMap configuration
        Configuration.getInstance().userAgentValue = packageName
    }
} 