package com.demo.map;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

public class AssetUtils {
    private static final String TAG = "AssetUtils";
    
    public static void checkAvatarAssets(Context context) {
        try {
            // List all files in the assets directory
            Log.d(TAG, "Root assets: " + Arrays.toString(context.getAssets().list("")));
            
            // Check if avatars directory exists
            String[] files = context.getAssets().list("");
            boolean hasAvatarsDir = false;
            for (String file : files) {
                if (file.equals("drawable/avatars")) {
                    hasAvatarsDir = true;
                    break;
                }
            }
            
            if (!hasAvatarsDir) {
                Log.e(TAG, "ERROR: 'avatars' directory not found in assets");
                return;
            }
            
            // List files in the avatars directory
            String[] avatars = context.getAssets().list("drawable/avatars");
            Log.d(TAG, "Avatars directory contents: " + Arrays.toString(avatars));
            
            // Check for required avatar files
            String[] requiredAvatars = {"blackboy.png", "blackgirl.png", "whiteboy.png", "whitegirl.png"};
            for (String avatar : requiredAvatars) {
                try {
                    String[] avatarFiles = context.getAssets().list("drawable/avatars");
                    boolean found = false;
                    for (String file : avatarFiles) {
                        if (file.equals(avatar)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        Log.d(TAG, "✓ Found avatar: " + avatar);
                    } else {
                        Log.e(TAG, "✗ Missing avatar: " + avatar);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error checking avatar: " + avatar, e);
                }
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error checking assets", e);
        }
    }
}
