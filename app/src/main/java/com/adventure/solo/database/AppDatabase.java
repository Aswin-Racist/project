package com.adventure.solo.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters; // Added
import androidx.room.migration.Migration; // Added
import androidx.sqlite.db.SupportSQLiteDatabase; // Added

import com.adventure.solo.model.Quest;
import com.adventure.solo.model.Clue;
import com.adventure.solo.model.PlayerProfile;
import com.adventure.solo.model.QuestProgress;
import com.adventure.solo.model.ClueProgress;

@Database(entities = {Quest.class, Clue.class, PlayerProfile.class, QuestProgress.class, ClueProgress.class}, version = 3) // Version incremented
@TypeConverters(Converters.class) // Added
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "solo_adventure_db";

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add new columns to the 'clues' table
            // Default value for ClueType can be 'LOCATION' if most existing clues are location-based
            database.execSQL("ALTER TABLE clues ADD COLUMN clueType TEXT DEFAULT 'LOCATION'");
            database.execSQL("ALTER TABLE clues ADD COLUMN puzzleData TEXT DEFAULT NULL");
        }
    };

    public abstract QuestDao questDao();
    public abstract ClueDao clueDao();
    public abstract PlayerProfileDao playerProfileDao(); // Added
    public abstract QuestProgressDao questProgressDao(); // Added
    public abstract ClueProgressDao clueProgressDao();   // Added

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            //.fallbackToDestructiveMigration() // Remove or comment out fallback
                            .addMigrations(MIGRATION_2_3) // Add our migration
                            .build();
                }
            }
        }
        return INSTANCE;
    }
} 