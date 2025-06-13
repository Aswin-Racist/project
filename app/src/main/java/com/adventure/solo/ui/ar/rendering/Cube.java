package com.adventure.solo.ui.ar.rendering;

import android.opengl.GLES20;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Cube {
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final ByteBuffer indexBuffer;

    private final int program;

    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    static final int COORDS_PER_VERTEX = 3;
    private static final int VALUES_PER_COLOR = 4; // R,G,B,A
    private static final int BYTES_PER_FLOAT = 4;


    static float cubeCoords[] = {
        -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f, // Front
        -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f, -0.5f, -0.5f, // Back
        -0.5f,  0.5f,  0.5f,   0.5f,  0.5f,  0.5f,   0.5f,  0.5f, -0.5f,  -0.5f,  0.5f, -0.5f, // Top
        -0.5f, -0.5f,  0.5f,  -0.5f, -0.5f, -0.5f,   0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f, // Bottom
         0.5f, -0.5f,  0.5f,   0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f, // Right
        -0.5f, -0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,  -0.5f,  0.5f, -0.5f,  -0.5f, -0.5f, -0.5f  // Left
    };

    static float colors[] = {
        1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,
        1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,
        1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,
        1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,
        1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,
        1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f,   1.0f, 0.843f, 0.0f, 1.0f
    };

    static byte indices[] = {
        0, 1, 2,   0, 2, 3,    // Front face
        4, 5, 6,   4, 6, 7,    // Back face
        8, 9, 10,  8, 10, 11,   // Top face
        12, 13, 14, 12, 14, 15,  // Bottom face
        16, 17, 18, 16, 18, 19,  // Right face
        20, 21, 22, 20, 22, 23   // Left face
    };

    private final String vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "varying vec4 varyingColor;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  varyingColor = vColor;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec4 varyingColor;" +
        "void main() {" +
        "  gl_FragColor = varyingColor;" +
        "}";

    public Cube() {
        ByteBuffer bb = ByteBuffer.allocateDirect(cubeCoords.length * BYTES_PER_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cubeCoords);
        vertexBuffer.position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * BYTES_PER_FLOAT);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        indexBuffer = ByteBuffer.allocateDirect(indices.length);
        indexBuffer.put(indices);
        indexBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e("CubeShader", "Error linking program: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Error linking GL program for Cube.");
        }
    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.e("CubeShader", "Could not create shader, type " + type);
            return 0;
        }
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("CubeShader", "Could not compile shader, type " + type + ":");
            Log.e("CubeShader", GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        // Using explicit stride values as per subtask re-definition
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, COORDS_PER_VERTEX * BYTES_PER_FLOAT, vertexBuffer);

        colorHandle = GLES20.glGetAttribLocation(program, "vColor");
        GLES20.glEnableVertexAttribArray(colorHandle);
        // Using explicit stride values
        GLES20.glVertexAttribPointer(colorHandle, VALUES_PER_COLOR, GLES20.GL_FLOAT, false, VALUES_PER_COLOR * BYTES_PER_FLOAT, colorBuffer);

        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_BYTE, indexBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }
}
