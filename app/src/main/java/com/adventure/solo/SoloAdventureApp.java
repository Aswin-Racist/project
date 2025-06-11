package com.adventure.solo;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;
import org.osmdroid.config.Configuration;

@HiltAndroidApp
public class SoloAdventureApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize OpenStreetMap configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
    }
} 