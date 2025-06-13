package com.adventure.solo.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class FirebaseModule {

    @Provides
    @Singleton
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseDatabase provideFirebaseDatabase() {
        // Ensure FirebaseApp.initializeApp(context) is called in your Application class.
        // This URL should ideally come from your google-services.json or build config.
        // For Realtime Database, you might need to specify the URL if it's not the default one
        // associated with your google-services.json.
        // If using the default RTDB instance, FirebaseDatabase.getInstance() is often enough
        // once initializeApp is called.
        // Example: FirebaseDatabase.getInstance("https://<YOUR-PROJECT-ID>-default-rtdb.firebaseio.com/");
        // For now, let's assume the default instance is configured correctly via google-services.json
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        // You could also explicitly set the URL if needed:
        // FirebaseDatabase database = FirebaseDatabase.getInstance("YOUR_FIREBASE_REALTIME_DB_URL");
        // e.g. from R.string.firebase_database_url if defined in strings.xml and populated by google-services.json
        return database;
    }
}
