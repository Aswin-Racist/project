package com.adventure.solo.ui.ar;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.WindowManager;
import com.google.ar.core.Session;

public class DisplayRotationHelper implements DisplayManager.DisplayListener {
    private boolean displayRotated = false;
    private final Context context;
    private final Display display;
    private int viewportWidth;  // Store viewport dimensions
    private int viewportHeight; // Store viewport dimensions


    public DisplayRotationHelper(Context context) {
        this.context = context;
        // It's better to get the display instance via ContextCompat or directly from WindowManager
        // For API level 30+ getSystemService(WindowManager.class).getDefaultDisplay() is deprecated
        // However, for simplicity in this context, we'll keep it if it works for target API.
        // A more robust way for newer APIs: context.getDisplay() if inside an Activity,
        // or ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            this.display = windowManager.getDefaultDisplay();
        } else {
            // Fallback or throw exception, as display is crucial
            throw new IllegalStateException("WindowManager service not available.");
        }
    }

    public void onResume() {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(this, null);
        }
    }

    public void onPause() {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(this);
        }
    }

    // Called from Renderer's onSurfaceChanged
    public void onSurfaceChanged(int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        displayRotated = true; // Mark as rotated to trigger update in session
    }

    // Called from Renderer's onDrawFrame, before rendering the AR frame
    public void updateSessionIfNeeded(Session session) {
        if (displayRotated && viewportWidth > 0 && viewportHeight > 0) {
            // Corrected to use stored viewportWidth and viewportHeight
            session.setDisplayGeometry(display.getRotation(), viewportWidth, viewportHeight);
            displayRotated = false;
        }
    }

    public int getRotation() {
        return display.getRotation();
    }

    @Override
    public void onDisplayAdded(int displayId) {}

    @Override
    public void onDisplayRemoved(int displayId) {}

    @Override
    public void onDisplayChanged(int displayId) {
        // This callback is triggered when display properties change, like rotation.
        displayRotated = true;
    }
}
