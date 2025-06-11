package com.demo.map;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String PREFS_NAME = "MapAppPrefs";
    private static final String KEY_AVATAR = "selected_avatar";
    private static final String DEFAULT_AVATAR = "whiteboy.png";

    public static void saveAvatar(Context context, String avatarName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_AVATAR, avatarName).apply();
    }

    public static String getAvatar(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_AVATAR, DEFAULT_AVATAR);
    }
}
