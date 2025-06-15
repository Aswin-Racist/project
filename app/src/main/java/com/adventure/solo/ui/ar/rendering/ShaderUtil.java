package com.adventure.solo.ui.ar.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets; // Specify charset

public class ShaderUtil {
    private static final String TAG = ShaderUtil.class.getSimpleName();

    public static String loadShader(Context context, String shaderFileName) {
        StringBuilder shaderSource = new StringBuilder();
        try {
            // Ensure the assets folder and path are correct.
            // The path should be relative to the 'assets' directory.
            // e.g., if shaders are in 'assets/shaders/', then shaderFileName should be 'shaders/yourshader.glsl'
            InputStream inputStream = context.getAssets().open(shaderFileName);
            // It's good practice to specify charset
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                shaderSource.append(line).append("\n"); // Append newline for better readability in logs
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not read shader file: " + shaderFileName, e);
            // Consider re-throwing or returning null to indicate failure clearly.
            return null;
        }
        return shaderSource.toString();
    }

    public static int compileShader(int type, String shaderCode) {
        if (shaderCode == null || shaderCode.isEmpty()) {
            Log.e(TAG, "Shader code is null or empty for type: " + type);
            return 0; // Return 0 to indicate failure
        }
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader type " + type + ": " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0; // Explicitly set to 0 on failure
        }
        if (shader == 0) {
            // This exception helps in immediately identifying shader compilation issues.
            throw new RuntimeException("Error creating shader of type " + type + ".");
        }
        return shader;
    }
}
