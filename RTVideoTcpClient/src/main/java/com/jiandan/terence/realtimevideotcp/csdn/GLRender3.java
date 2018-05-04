package com.jiandan.terence.realtimevideotcp.csdn;


import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.jiandan.terence.realtimevideotcp.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRender3
        implements Renderer, PreviewCallback {
    public static int mCamWidth = 1920;
    public static int mCamHeight = 1080;
    private static final int LENGTH = mCamWidth * mCamHeight;
    private static final int LENGTH_2 = mCamWidth * mCamHeight / 2;
    private static final int U_INDEX = LENGTH;
    private static final int V_INDEX = LENGTH * 5 / 4;
    private static final int LENGTH_4 = LENGTH / 4;
    private Activity activity;
    byte[] ydata = new byte[LENGTH];
    byte[] uData = new byte[LENGTH_4];
    byte[] vData = new byte[LENGTH_4];

    private FloatBuffer mVertices;
    private ShortBuffer mIndices;

    private int previewFrameWidth = 256;
    private int previewFrameHeight = 256;
    private int mProgramObject;
    private int mPositionLoc;
    private int mTexCoordLoc;
    //  private int mSamplerLoc;
    private int yTexture;
    private int uTexture;
    private int vTexture;
    private boolean isStart = false;
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

    private ByteBuffer frameData = null;
    private ByteBuffer yBuffer;
    private ByteBuffer uBuffer;
    private ByteBuffer vBuffer;

    public GLRender3(Activity activity) {
        this.activity = activity;

        mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);

        mIndices = ByteBuffer.allocateDirect(mIndicesData.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(mIndicesData).position(0);

        yBuffer = ByteBuffer.allocateDirect(LENGTH);
        uBuffer = ByteBuffer.allocateDirect(LENGTH_4);
        vBuffer = ByteBuffer.allocateDirect(LENGTH_4);
    }

    @Override
    public final void onDrawFrame(GL10 gl) {
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Use the program object
        GLES20.glUseProgram(mProgramObject);
        // Load the vertex position
        mVertices.position(0);
        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
        // Load the texture coordinate
        mVertices.position(3);
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, mVertices);

        GLES20.glEnableVertexAttribArray(mPositionLoc);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureName);
        GLES20.glUniform1i(uTexture, 1);
        if (!isStart) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA,
                    mCamWidth / 2, mCamHeight / 2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uBuffer);
        } else {
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    mCamWidth / 2, mCamHeight / 2, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uBuffer);
            checkNoGLES2Error();
        }
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureName);
        GLES20.glUniform1i(vTexture, 2);
        if (!isStart) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA,
                    mCamWidth / 2, mCamHeight / 2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, vBuffer);
        } else {
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    mCamWidth / 2, mCamHeight / 2, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, vBuffer);
            checkNoGLES2Error();
        }
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkNoGLES2Error();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureName);
        checkNoGLES2Error();
        GLES20.glUniform1i(yTexture, 0);
        if (!isStart) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                    mCamWidth, mCamHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
        } else {
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    mCamWidth, mCamHeight, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
            checkNoGLES2Error();
        }
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);



        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mIndices);
        //        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        checkNoGLES2Error();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    String vertexShader =
            "attribute vec4 a_position;                         \n" +
                    "attribute vec2 a_texCoord;                         \n" +
                    "varying vec2 v_texCoord;                           \n" +

                    "void main(){                                       \n" +
                    "   gl_Position = a_position;                       \n" +
                    "   v_texCoord = a_texCoord;                        \n" +
                    "}                                                  \n";
    int yTextureName;
    int uTextureName,vTextureName;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        // Define a simple shader program for our point.
        final String vShaderStr = vertexShader;
        final String fShaderStr = readTextFileFromRawResource(activity, R.raw.f_convert);

        // Load the shaders and get a linked program object
        mProgramObject = loadProgram(vShaderStr, fShaderStr);
        checkNoGLES2Error();
        // Get the attribute locations
        mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "a_position");
        checkNoGLES2Error();
        mTexCoordLoc = GLES20.glGetAttribLocation(mProgramObject, "a_texCoord");
        checkNoGLES2Error();
       // GLES20.glEnable(GLES20.GL_TEXTURE_2D);
       // checkNoGLES2Error();
        yTexture = GLES20.glGetUniformLocation(mProgramObject, "y_texture");
        checkNoGLES2Error();
        int[] yTextureNames = new int[1];
        GLES20.glGenTextures(1, yTextureNames, 0);
        checkNoGLES2Error();
        yTextureName = yTextureNames[0];
        // GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //  GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureName);

        //GLES20.glEnable(GLES20.GL_TEXTURE_2D);
//        checkNoGLES2Error();
        uTexture = GLES20.glGetUniformLocation(mProgramObject, "v_texture");
        checkNoGLES2Error();
        int[] uTextureNames = new int[1];
        GLES20.glGenTextures(1, uTextureNames, 0);
        checkNoGLES2Error();
        uTextureName = uTextureNames[0];
        //  GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        //   GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureName);
        vTexture = GLES20.glGetUniformLocation(mProgramObject, "u_texture");
        checkNoGLES2Error();
        int[] vTextureNames = new int[1];
        GLES20.glGenTextures(1, vTextureNames, 0);
        checkNoGLES2Error();
        vTextureName = vTextureNames[0];

        // Set the background clear color to black.
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        checkNoGLES2Error();
    }

    public void setPreviewFrameSize(int realWidth, int realHeight) {
        previewFrameHeight = realHeight;
        previewFrameWidth = realWidth;

//      frameData = GraphicsUtil.makeByteBuffer(previewFrameHeight * previewFrameWidth * 3);
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
        checkNoGLES2Error();
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
        checkNoGLES2Error();
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
        checkNoGLES2Error();
        return programObject;
    }

    public void setYuvData(byte[] data) {
       isStart = true;
//        yBuffer.put(data, 0, LENGTH);
//        yBuffer.position(0);
//
//        uBuffer.put(data, LENGTH, LENGTH / 2);
//        uBuffer.position(0);

            //   isStarted = true;
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        yBuffer.put(data, 0, LENGTH);
        yBuffer.position(0);

        uBuffer.put(data, LENGTH, LENGTH / 2);
        uBuffer.position(0);
    }

    // Poor-man's assert(): die with |msg| unless |condition| is true.
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

}