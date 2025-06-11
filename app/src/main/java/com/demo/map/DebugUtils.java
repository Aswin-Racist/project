package com.demo.map;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

public class DebugUtils {
    private static final String TAG = "DebugUtils";
    
    public static void logAssets(Context context) {
        try {
            // List all files in the assets directory
            Log.d(TAG, "Root assets: " + Arrays.toString(context.getAssets().list("")));
            
            // List files in the avatars directory
            String[] avatars = context.getAssets().list("drawable/avatars");
            if (avatars != null) {
                Log.d(TAG, "Avatars found: " + Arrays.toString(avatars));
            } else {
                Log.e(TAG, "No avatars directory found in assets");
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error listing assets", e);
        }
    }
}
