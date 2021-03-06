package com.jiandan.terence.realtimevideotcp.csdn;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.jiandan.terence.realtimevideotcp.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LastRenderer implements GLSurfaceView.Renderer {
    public static final int recWidth = 512;
    public static final int recHeight = 512;

    private static final int U_INDEX = recWidth * recHeight;
    private static final int V_INDEX = recWidth * recHeight * 5 / 4;
    private static final int LENGTH = recWidth * recHeight;
    private static final int LENGTH_4 = recWidth * recHeight / 4;

    private int previewFrameWidth = 256;
    private int previewFrameHeight = 256;

    private int[] yTextureNames;
    private int[] uTextureNames;
    private int[] vTextureNames;

    private Activity activity;

    private FloatBuffer mVertices;
    private ShortBuffer mIndices;

    private int mProgramObject;
    private int mPositionLoc;
    private int mTexCoordLoc;

    private int yTexture;
    private int uTexture;
    private int vTexture;
    String vertexShader =
            "attribute vec4 a_position;                         \n" +
                    "attribute vec2 a_texCoord;                         \n" +
                    "varying vec2 v_texCoord;                           \n" +

                    "void main(){                                       \n" +
                    "   gl_Position = a_position;                       \n" +
                    "   v_texCoord = a_texCoord;                        \n" +
                    "}                                                  \n";

    private final float[] mVerticesData = {-1.f, 1.f, 0.0f, // Position 0
            0.0f, 0.0f, // TexCoord 0
            -1.f, -1.f, 0.0f, // Position 1
            0.0f, 1.0f, // TexCoord 1
            1.f, -1.f, 0.0f, // Position 2
            1.0f, 1.0f, // TexCoord 2
            1.f, 1.f, 0.0f, // Position 3
            1.0f, 0.0f // TexCoord 3
    };
    private final short[] mIndicesData = {0, 1, 2, 0, 2, 3};

    private ByteBuffer yBuffer;
    private ByteBuffer uBuffer;
    private ByteBuffer vBuffer;

    private IntBuffer frameBuffer;
    private IntBuffer renderBuffer;
    private IntBuffer parameterBufferWidth;
    private IntBuffer parameterBufferHeigth;

    byte[] ydata = new byte[LENGTH];
    byte[] uData = new byte[LENGTH_4];
    byte[] vData = new byte[LENGTH_4];

    public LastRenderer(Activity activity) {
        this.activity = activity;

        mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);

        mIndices = ByteBuffer.allocateDirect(mIndicesData.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(mIndicesData).position(0);

        yBuffer = ByteBuffer.allocateDirect(LENGTH);
        uBuffer = ByteBuffer.allocateDirect(LENGTH_4/* * 2*/);
        vBuffer = ByteBuffer.allocateDirect(LENGTH_4);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glActiveTexture(GLES20.GL_ACTIVE_TEXTURE);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("debug", "on surface created");
        // Define a simple shader program for our point.
        final String vShaderStr = vertexShader;//readTextFileFromRawResource(activity, R.raw.v_simple);
        final String fShaderStr = readTextFileFromRawResource(activity, R.raw.f_convert);
        frameBuffer = IntBuffer.allocate(1);
        renderBuffer = IntBuffer.allocate(1);

       // GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        GLES20.glGenFramebuffers(1, frameBuffer);
        GLES20.glGenRenderbuffers(1, renderBuffer);
        GLES20.glActiveTexture(GLES20.GL_ACTIVE_TEXTURE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer.get(0));
        GLES20.glClear(0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffer.get(0));

        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                recWidth, recHeight);

        parameterBufferHeigth = IntBuffer.allocate(1);
        parameterBufferWidth = IntBuffer.allocate(1);
        GLES20.glGetRenderbufferParameteriv(GLES20.GL_RENDERBUFFER, GLES20.GL_RENDERBUFFER_WIDTH, parameterBufferWidth);
        GLES20.glGetRenderbufferParameteriv(GLES20.GL_RENDERBUFFER, GLES20.GL_RENDERBUFFER_HEIGHT, parameterBufferHeigth);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, renderBuffer.get(0));
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.d("debug", "gl frame buffer status != frame buffer complete");
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClear(0);

        mProgramObject = loadProgram(vShaderStr, fShaderStr);

        // Get the attribute locations
        mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "a_position");
        mTexCoordLoc = GLES20.glGetAttribLocation(mProgramObject, "a_texCoord");

       // GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        checkNoGLES2Error();

        yTexture = GLES20.glGetUniformLocation(mProgramObject, "y_texture");
        yTextureNames = new int[1];
        checkNoGLES2Error();

        GLES20.glGenTextures(1, yTextureNames, 0);
        int yTextureName = yTextureNames[0];
        checkNoGLES2Error();

       // GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        uTexture = GLES20.glGetUniformLocation(mProgramObject, "u_texture");
        uTextureNames = new int[1];
        GLES20.glGenTextures(1, uTextureNames, 0);
        int uTextureName = uTextureNames[0];
        checkNoGLES2Error();

      //  GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        vTexture = GLES20.glGetUniformLocation(mProgramObject, "v_texture");
        vTextureNames = new int[1];
        GLES20.glGenTextures(1, vTextureNames, 0);
        int vTextureName = vTextureNames[0];
        checkNoGLES2Error();

        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        checkNoGLES2Error();
    }

    @Override
    public final void onDrawFrame(GL10 gl) {
        Log.d("debug", "on Draw frame");
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Use the program object
        GLES20.glUseProgram(mProgramObject);
        checkNoGLES2Error();
        // Load the vertex position
        mVertices.position(0);
        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
        // Load the texture coordinate
        mVertices.position(3);
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, mVertices);

        GLES20.glEnableVertexAttribArray(mPositionLoc);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureNames[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                recWidth, recHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureNames[0]);
        GLES20.glUniform1i(yTexture, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureNames[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                recWidth / 2, recHeight / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uBuffer);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1 + 2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureNames[0]);
        GLES20.glUniform1i(uTexture, 2);
        checkNoGLES2Error();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureNames[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                recWidth / 2, recHeight / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1 + 1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureNames[0]);
        GLES20.glUniform1i(vTexture, 1);
        checkNoGLES2Error();
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mIndices);
    }

    private static void abortUnless(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }

    // Assert that no OpenGL ES 2.0 error has been raised.
    private static void checkNoGLES2Error() {
        int error = GLES20.glGetError();
        abortUnless(error == GLES20.GL_NO_ERROR, "GLES20 error: " + error);
    }

    public void setPreviewFrameSize(int realWidth, int realHeight) {
        previewFrameHeight = realHeight;
        previewFrameWidth = realWidth;
    }

    public static String readTextFileFromRawResource(final Context context, final int resourceId) {
        final InputStream inputStream = context.getResources().openRawResource(resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        return body.toString();
    }

    public static int loadShader(int type, String shaderSrc) {
        int shader;
        int[] compiled = new int[1];

        // Create the shader object
        shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            return 0;
        }
        // Load the shader source
        GLES20.glShaderSource(shader, shaderSrc);
        // Compile the shader
        GLES20.glCompileShader(shader);
        // Check the compile status
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            Log.e("ESShader", GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public static int loadProgram(String vertShaderSrc, String fragShaderSrc) {
        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);
        if (vertexShader == 0) {
            return 0;
        }

        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader);
            return 0;
        }

        // Create the program object
        programObject = GLES20.glCreateProgram();

        if (programObject == 0) {
            return 0;
        }

        GLES20.glAttachShader(programObject, vertexShader);
        GLES20.glAttachShader(programObject, fragmentShader);

        // Link the program
        GLES20.glLinkProgram(programObject);

        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            Log.e("ESShader", "Error linking program:");
            Log.e("ESShader", GLES20.glGetProgramInfoLog(programObject));
            GLES20.glDeleteProgram(programObject);
            return 0;
        }

        // Free up no longer needed shader resources
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return programObject;
    }

    public void setYuvData(byte[] data) {

        System.arraycopy(data, 0, ydata, 0, LENGTH);
        yBuffer.put(ydata);
        yBuffer.position(0);

        System.arraycopy(data, U_INDEX, uData, 0, LENGTH_4);
        uBuffer.put(uData);
        uBuffer.position(0);

        System.arraycopy(data, V_INDEX, vData, 0, LENGTH_4);
        vBuffer.put(vData);
        vBuffer.position(0);
    }
}