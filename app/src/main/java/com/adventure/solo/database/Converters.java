package com.adventure.solo.database;

import androidx.room.TypeConverter;
import com.adventure.solo.model.ClueType; // Ensure ClueType is in the correct package

public class Converters {
    @TypeConverter
    public static String fromClueType(ClueType clueType) {
        return clueType == null ? null : clueType.name();
    }

    @TypeConverter
    public static ClueType toClueType(String name) {
        if (name == null) {
            return null;
        }
        try {
            return ClueType.valueOf(name);
        } catch (IllegalArgumentException e) {
            // Optionally log this error or return a default ClueType
            // For example, return ClueType.LOCATION if 'name' is an old invalid 'type' string
            // For now, returning null if not a valid ClueType enum constant.
            android.util.Log.w("Converters", "Cannot convert string '" + name + "' to ClueType. Returning null.", e);
            return null;
        }
    }
}
