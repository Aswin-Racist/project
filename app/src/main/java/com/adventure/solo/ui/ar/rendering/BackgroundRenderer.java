package com.adventure.solo.ui.ar.rendering;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.google.ar.core.Frame;
// Remove import com.google.ar.core.Session; as it's not directly used in this version of BackgroundRenderer
import java.io.IOException; // ShaderUtil might throw this, but it's handled there
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BackgroundRenderer {
    private static final String TAG = BackgroundRenderer.class.getSimpleName();

    private static final int COORDS_PER_VERTEX = 2; // OpenGL NDC
    private static final int TEXCOORDS_PER_VERTEX = 2;
    private static final int FLOAT_SIZE = 4; // Bytes per float

    private FloatBuffer quadCoords; // Buffer for normalized device coordinates
    private FloatBuffer quadTexCoords; // Buffer for texture coordinates

    private int quadProgram; // OpenGL ES program handle

    private int quadPositionAttrib; // Attribute location for vertex positions
    private int quadTexCoordAttrib; // Attribute location for texture coordinates
    private int textureId = -1; // GL texture ID for the camera image

    public BackgroundRenderer() {}

    public int getTextureId() {
        return textureId;
    }

    public void createOnGlThread(Context context) {
        // Generate the texture ID and bind it to the GL_TEXTURE_EXTERNAL_OES target.
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        // Set texture parameters for GL_TEXTURE_EXTERNAL_OES.
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        int numVertices = 4; // A quad has 4 vertices
        if (numVertices != QUAD_COORDS_NDC.length / COORDS_PER_VERTEX) {
            throw new RuntimeException("Unexpected number of vertices in BackgroundRenderer's NDC coordinates.");
        }

        // Initialize vertex buffer for quad.
        ByteBuffer bbCoords = ByteBuffer.allocateDirect(QUAD_COORDS_NDC.length * FLOAT_SIZE);
        bbCoords.order(ByteOrder.nativeOrder());
        quadCoords = bbCoords.asFloatBuffer();
        quadCoords.put(QUAD_COORDS_NDC);
        quadCoords.position(0);

        // Initialize texture coordinates buffer.
        // The size is dynamic based on device aspect ratio and display rotation,
        // so it's updated in draw().
        ByteBuffer bbTexCoords =
            ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoords.order(ByteOrder.nativeOrder());
        quadTexCoords = bbTexCoords.asFloatBuffer();

        // Load and compile shaders.
        final String vertexShader = ShaderUtil.loadShader(context, "shaders/screenquad.vert");
        final String fragmentShader = ShaderUtil.loadShader(context, "shaders/screenquad.frag");

        if (vertexShader == null || fragmentShader == null) {
            Log.e(TAG, "Failed to load shaders.");
            throw new RuntimeException("Failed to load shaders for BackgroundRenderer.");
        }

        quadProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(quadProgram, ShaderUtil.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader));
        GLES20.glAttachShader(quadProgram, ShaderUtil.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader));
        GLES20.glLinkProgram(quadProgram);
        GLES20.glUseProgram(quadProgram); // Should be done before glGetAttribLocation

        quadPositionAttrib = GLES20.glGetAttribLocation(quadProgram, "a_Position");
        quadTexCoordAttrib = GLES20.glGetAttribLocation(quadProgram, "a_TexCoord");
        // Check for errors:
        if (quadPositionAttrib == -1 || quadTexCoordAttrib == -1) {
             Log.e(TAG, "Could not get attribute locations for screen quad shader.");
        }
    }

    public void draw(Frame frame) {
        // Ensure frame and camera are valid.
        if (frame == null || frame.getCamera() == null) {
            return;
        }

        // If display geometry changed (e.g. device rotation), we must update the texture coordinates.
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformCoordinates2d(
                com.google.ar.core.Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                quadCoords, // Use the static NDC quadCoords as input
                com.google.ar.core.Coordinates2d.TEXTURE_NORMALIZED,
                quadTexCoords); // Output transformed texture coordinates
        }

        // ARCore frame timestamp is 0 when there is no new frame data.
        if (frame.getTimestamp() == 0) {
            return;
        }

        // Disable depth testing and depth write before drawing the camera feed.
        // The camera feed is always drawn behind other objects.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUseProgram(quadProgram);

        // Set shader uniforms and attributes.
        GLES20.glVertexAttribPointer(
            quadPositionAttrib, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadCoords);
        GLES20.glVertexAttribPointer(
            quadTexCoordAttrib, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadTexCoords);

        GLES20.glEnableVertexAttribArray(quadPositionAttrib);
        GLES20.glEnableVertexAttribArray(quadTexCoordAttrib);

        // Draw the quad.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Clean up GL state.
        GLES20.glDisableVertexAttribArray(quadPositionAttrib);
        GLES20.glDisableVertexAttribArray(quadTexCoordAttrib);
        GLES20.glDepthMask(true); // Re-enable depth write
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // Re-enable depth test
    }

    // Normalized Device Coordinates for a full-screen quad.
    // These are static because the camera image should always fill the screen.
    private static final float[] QUAD_COORDS_NDC =
        new float[] {
            -1.0f, -1.0f, // Bottom Left
            +1.0f, -1.0f, // Bottom Right
            -1.0f, +1.0f, // Top Left
            +1.0f, +1.0f, // Top Right
        };
}
