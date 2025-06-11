package com.adventure.solo.ui.ar;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix; // For matrix operations
import android.util.Log;

import com.adventure.solo.ui.ar.rendering.BackgroundRenderer; // Needs to be created or sourced
import com.adventure.solo.ui.ar.rendering.Cube; // Your new Cube class
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.Camera; // ARCore Camera

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = MainRenderer.class.getSimpleName();

    private final Context context;
    private final DisplayRotationHelper displayRotationHelper;
    private final Runnable onObjectTappedCallback; // Renamed for clarity

    private Session session;
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private Cube cube;

    private boolean objectVisible = false; // Controlled by ARSceneFragment
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16]; // Model-View-Projection

    // Z-Near and Z-Far planes for projection matrix
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;


    public MainRenderer(Context context, DisplayRotationHelper displayRotationHelper, Runnable onObjectTapped) {
        this.context = context;
        this.displayRotationHelper = displayRotationHelper;
        this.onObjectTappedCallback = onObjectTapped;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setObjectVisible(boolean visible) {
        this.objectVisible = visible;
    }

    // Called from ARSceneFragment's touch listener
    public void handleTap() {
        Log.d(TAG, "MainRenderer handleTap. Object visible: " + objectVisible);
        if (objectVisible && onObjectTappedCallback != null) {
            // Could add hit testing here later if needed (e.g. raycast against cube bounds)
            onObjectTappedCallback.run();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // Enable depth testing for 3D rendering

        backgroundRenderer.createOnGlThread(context);
        cube = new Cube();
        // The Cube constructor already handles shader compilation and buffer setup.
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return; // Session is not yet available.
        }

        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            backgroundRenderer.draw(frame);

            // If tracking, and object is set to be visible, render it.
            if (camera.getTrackingState() == com.google.ar.core.TrackingState.TRACKING && objectVisible) {
                // Get projection matrix.
                camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);
                // Get camera matrix (view matrix).
                camera.getViewMatrix(viewMatrix, 0);

                // Set model matrix for the cube
                // Place it 2 meters in front (negative Z) of the camera and slightly down
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, 0f, -0.5f, -2f);
                // Example: Rotate the cube around the Y axis
                // float angle = (float) (System.currentTimeMillis() / 100 % 360); // Simple continuous rotation
                // Matrix.rotateM(modelMatrix, 0, angle, 0f, 1f, 0f);


                // Calculate MVP matrix: P * V * M
                // First, V * M
                Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
                // Then, P * (V*M)
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

                cube.draw(mvpMatrix);
            }

        } catch (Throwable t) {
            Log.e(TAG, "Exception on GL thread: " + t.getMessage(), t);
        }
    }
}
