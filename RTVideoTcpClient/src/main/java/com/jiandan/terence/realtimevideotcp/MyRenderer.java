package com.jiandan.terence.realtimevideotcp;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {

    private static String TAG = "LastRenderer";
    private int[] yuvTextures = {-1, -1, -1};
    String fragmentShader =
            "#ifdef GL_ES\n" +
                    "precision highp float;\n" +
                    "#endif\n" +

                    "varying vec2 v_texCoord;\n" +
                    "uniform sampler2D y_texture;\n" +
                    "uniform sampler2D uv_texture;\n" +

                    "void main (void){\n" +
                    "   float r, g, b, y, u, v;\n" +

                    //We had put the Y values of each pixel to the R,G,B components by
                    //GL_LUMINANCE, that's why we're pulling it from the R component,
                    //we could also use G or B
                    "   y = texture2D(y_texture, v_texCoord).r;\n" +

                    //We had put the U and V values of each pixel to the A and R,G,B
                    //components of the texture respectively using GL_LUMINANCE_ALPHA.
                    //Since U,V bytes are interspread in the texture, this is probably
                    //the fastest way to use them in the shader
                    "   u = texture2D(uv_texture, v_texCoord).a - 0.5;\n" +
                    "   v = texture2D(uv_texture, v_texCoord).r - 0.5;\n" +

                    //The numbers are just YUV to RGB conversion constants
                    "   r = y + 1.13983*v;\n" +
                    "   g = y - 0.39465*u - 0.58060*v;\n" +
                    "   b = y + 2.03211*u;\n" +

                    //We finally set the RGB color of our pixel
                    "   gl_FragColor = vec4(r, g, b, 1.0);\n" +
                    "}\n";

    //Our vertex shader code; nothing special
    String vertexShader =
            "attribute vec4 a_position;                         \n" +
                    "attribute vec2 a_texCoord;                         \n" +
                    "varying vec2 v_texCoord;                           \n" +

                    "void main(){                                       \n" +
                    "   gl_Position = a_position;                       \n" +
                    "   v_texCoord = a_texCoord;                        \n" +
                    "}                                                  \n";
    public static final int recWidth = 512;
    public static final int recHeight = 384;

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
    private boolean isStarted = false;

    private final float[] mVerticesData = {
            -1.f, 1.f, 0.0f, // Position 0
            0.0f, 0.0f, // TexCoord 0
            -1.f, -1.f, 0.0f, // Position 1
            0.0f, 1.0f, // TexCoord 1
            1.f, -1.f, 0.0f, // Position 2
            1.0f, 1.0f, // TexCoord 2
            1.f, 1.f, 0.0f, // Position 3
            1.0f, 0.0f // TexCoord 3
    };

    //	private final float[] verticesData = {
//			-1.f, 1.f, // Position 0
//			0.0f, 0.0f, // TexCoord 0
//			-1.f, -1.f, // Position 1
//			0.0f, 1.0f, // TexCoord 1
//			1.f, -1.f, // Position 2
//			1.0f, 1.0f, // TexCoord 2
//			1.f, 1.f, // Position 3
//			1.0f, 0.0f // TexCoord 3
//	};
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

    public MyRenderer(Activity activity) {
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

    public void setYuvData(byte[] data) {
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
    public void onSurfaceChanged(GL10 gl, int width, int height) {
       //  GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "on surface created");
        // Define a simple shader program for our point.
        final String vShaderStr = vertexShader;
        final String fShaderStr = readTextFileFromRawResource(activity, R.raw.f_convert);
//        frameBuffer = IntBuffer.allocate(1);
//        renderBuffer = IntBuffer.allocate(1);
//
//
//        GLES20.glGenFramebuffers(1, frameBuffer);
//        GLES20.glGenRenderbuffers(1, renderBuffer);
//      //  GLES20.glActiveTexture(GLES20.GL_ACTIVE_TEXTURE);
//        //glGetIntegerv(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, ...)
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer.get(0));
//        GLES20.glClear(0);
//        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffer.get(0));
//
//        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
//                recWidth, recHeight);
//
//        parameterBufferHeigth = IntBuffer.allocate(1);
//        parameterBufferWidth = IntBuffer.allocate(1);
//        GLES20.glGetRenderbufferParameteriv(GLES20.GL_RENDERBUFFER, GLES20.GL_RENDERBUFFER_WIDTH, parameterBufferWidth);
//        GLES20.glGetRenderbufferParameteriv(GLES20.GL_RENDERBUFFER, GLES20.GL_RENDERBUFFER_HEIGHT, parameterBufferHeigth);
//        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, renderBuffer.get(0));
//        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
//            Log.d(TAG, "gl frame buffer status != frame buffer complete");
//        }
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//        GLES20.glClear(0);
//        checkNoGLES2Error();
        //   GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        mProgramObject = loadProgram(vShaderStr, fShaderStr);

        // Get the attribute locations
        mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "a_position");
        checkNoGLES2Error("glGetAttribLocation");
        mTexCoordLoc = GLES20.glGetAttribLocation(mProgramObject, "a_texCoord");
        checkNoGLES2Error("glGetAttribLocation");
        // Generate 3 texture ids for Y/U/V and place them into |textures|.
        GLES20.glGenTextures(yuvTextures.length, yuvTextures, 0);
        Log.d(TAG, "  texture=" + Arrays.toString(yuvTextures));
        checkNoGLES2Error("glGenTextures");
        //  checkNoGLES2Error();
        // Use the program object
        //  GLES20.glUseProgram(mProgramObject);
        // checkNoGLES2Error();
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        checkNoGLES2Error();
    }

//    @Override
//    public final void onDrawFrame(GL10 gl) {
//        Log.d("debug", "on Draw frame");
//        // Clear the color buffer
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//
//        // Use the program object
//        GLES20.glUseProgram(mProgramObject);
//
//        // Load the vertex position
//        mVertices.position(0);
//        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
//        // Load the texture coordinate
//        mVertices.position(3);
//        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
//
//        GLES20.glEnableVertexAttribArray(mPositionLoc);
//        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
//
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureNames[0]);
//        GLES20.glUniform1i(yTexture, 0);
//        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0,
//                0, recWidth, recHeight, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureNames[0]);
//        GLES20.glUniform1i(uTexture, 1);
//        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0,0,
//                recWidth/2, recHeight/2,  GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uBuffer);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureNames[0]);
//        GLES20.glUniform1i(vTexture, 2);
//        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0,0,
//                recWidth/2, recHeight/2, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//       // GLES20.glActiveTexture(GLES20.GL_TEXTURE1 + 1);
//       // GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureNames[0]);
//       // GLES20.glUniform1i(vTexture, 1);
//
//       GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mIndices);
//        //GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 6);
//    }


//    @Override
//    public final void onDrawFrame(GL10 gl) {
//        Log.d("debug", "on Draw frame");
//        // Clear the color buffer
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//
//        // Use the program object
//        GLES20.glUseProgram(mProgramObject);
//
//        // Load the vertex position
//        mVertices.position(0);
//        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
//        // Load the texture coordinate
//        mVertices.position(3);
//        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, mVertices);
//
//        GLES20.glEnableVertexAttribArray(mPositionLoc);
//        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//      //  GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureNames[0]);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTextureNames[0]);
//        GLES20.glUniform1i(yTexture, 1);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
//                recWidth, recHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
////        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0,
////                0, recWidth, recHeight, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTextureNames[0]);
////        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0,0,
////                recWidth/2, recWidth/2,  GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uBuffer);
//        GLES20.glUniform1i(uTexture, 2);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
//                recWidth/2, recHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uBuffer);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE12);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTextureNames[0]);
//        GLES20.glUniform1i(vTexture, 3);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
//                recWidth/2, recHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer);
////        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0,0,
////                recWidth/2, recWidth/2,  GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mIndices);
//    }

    @Override
    public final void onDrawFrame(GL10 gl) {
     //   if (!isStarted) return;
        Log.d(TAG, "on Draw frame");
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgramObject);
        checkNoGLES2Error("glUseProgram");
        // Load the vertex position
        mVertices.position(0);
        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false, 4 * 5, mVertices);
        // Load the texture coordinate
        mVertices.position(3);
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 4 * 5, mVertices);

        GLES20.glEnableVertexAttribArray(mPositionLoc);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
        createTextures(mProgramObject);
//        composeData();
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, mIndices);
    }
    // private boolean isChanged=false;

    private void createTextures(int program) {
        Log.d(TAG, "  YuvImageRenderer.createTextures");
        // this.program = program;
        //   GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        checkNoGLES2Error("glActiveTexture");
        for (int i = 0; i < yuvTextures.length; i++) {
            int w = (i == 0) ? recWidth : recWidth / 2;
            int h = (i == 0) ? recHeight : recHeight / 2;
            Log.d(TAG, "  i =" + i);
           // GLES20.glEnable(GLES20.GL_TEXTURE_2D);
          //  checkNoGLES2Error();
          //  GLES20.glActiveTexture(GLES20.GL_TEXTURE0+i);
         //   checkNoGLES2Error();

            ByteBuffer data;
            if (i == 0) {
              //  ;   GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                int ytexture = GLES20.glGetUniformLocation(mProgramObject, "y_texture");
                checkNoGLES2Error();
                GLES20.glUniform1i(ytexture, 0);
                checkNoGLES2Error();
                data = yBuffer;
            } else if (i == 1) {
              //  GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                data = uBuffer;
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramObject, "u_texture"), 1);
                checkNoGLES2Error();
            } else {
                data = vBuffer;
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramObject, "v_texture"), 2);
                checkNoGLES2Error();
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[i]);
            checkNoGLES2Error();
            Log.d(TAG, "data size =" + data.array().length);
            if (!isStarted) {
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                        w, h, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                        null);
            } else {
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                        w, h, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                        data);
            }
            checkNoGLES2Error("glTexImage2D");
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            checkNoGLES2Error("glTexParameterf");
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            checkNoGLES2Error("glTexParameterf");
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            checkNoGLES2Error("glTexParameterf");
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            checkNoGLES2Error("glTexParameterf");
        }
        checkNoGLES2Error();
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

    // Poor-man's assert(): die with |msg| unless |condition| is true.
    private static void abortUnless(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }

    // Assert that no OpenGL ES 2.0 error has been raised.
    private static void checkNoGLES2Error() {
        checkNoGLES2Error("");
    }

    // Assert that no OpenGL ES 2.0 error has been raised.
    private static void checkNoGLES2Error(String op) {
        int error = GLES20.glGetError();
        abortUnless(error == GLES20.GL_NO_ERROR, "LastRenderer   GLES20 error: " + error + " op: " + op);
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
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        checkNoGLES2Error("loadShader");
        return shader;
    }

    public int loadProgram(String vertShaderSrc, String fragShaderSrc) {
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
        checkNoGLES2Error("glCreateProgram");
        if (programObject == 0) {
            return 0;
        }

        GLES20.glAttachShader(programObject, vertexShader);
        checkNoGLES2Error("glAttachShader");
        GLES20.glAttachShader(programObject, fragmentShader);
        checkNoGLES2Error("glAttachShader");
        // Link the program
        GLES20.glLinkProgram(programObject);
        checkNoGLES2Error("glLinkProgram");
        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);
        checkNoGLES2Error("glGetProgramiv");
        if (linked[0] == 0) {
            Log.e(TAG, "Error linking program:");
            Log.e(TAG, GLES20.glGetProgramInfoLog(programObject));
            GLES20.glDeleteProgram(programObject);
            return 0;
        }

        // Free up no longer needed shader resources
        GLES20.glDeleteShader(vertexShader);
        checkNoGLES2Error("glDeleteShader");
        GLES20.glDeleteShader(fragmentShader);
        checkNoGLES2Error("glDeleteShader");
        return programObject;
    }

}

