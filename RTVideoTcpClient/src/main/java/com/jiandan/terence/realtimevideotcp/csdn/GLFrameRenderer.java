package com.jiandan.terence.realtimevideotcp.csdn;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.util.DisplayMetrics;
import android.util.Log;


public class GLFrameRenderer implements Renderer {
    private int mCamWidth = 512;
    private int mCamHeight = 512;

   // private ISimplePlayer mParentAct;
    private GLSurfaceView mTargetSurface;
    private GLProgram prog = new GLProgram(0);
    private int mScreenWidth, mScreenHeight;
    private int mVideoWidth, mVideoHeight;
    private ByteBuffer y;
    private ByteBuffer u;
    private ByteBuffer v;
    private  final int LENGTH = mCamWidth * mCamHeight;
    private  final int LENGTH_4 = mCamWidth * mCamHeight / 4;

    byte[] ydata = new byte[LENGTH];
    byte[] uData = new byte[LENGTH_4];
    byte[] vData = new byte[LENGTH_4];
    private  final int U_INDEX = mCamWidth * mCamHeight;
    private  final int V_INDEX = mCamWidth * mCamHeight * 5 / 4;
    public GLFrameRenderer(ISimplePlayer callback, GLSurfaceView surface, DisplayMetrics dm) {
        //mParentAct = callback;
        mTargetSurface = surface;
        mScreenWidth = mCamWidth;
        mScreenHeight = mCamHeight;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("GLFrameRenderer","GLFrameRenderer :: onSurfaceCreated");
        if (!prog.isProgramBuilt()) {
            prog.buildProgram();
            Log.d("GLFrameRenderer","GLFrameRenderer :: buildProgram done");
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("GLFrameRenderer","GLFrameRenderer :: onSurfaceChanged");
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (y != null) {
                // reset position, have to be done
                y.position(0);
                u.position(0);
                v.position(0);
                prog.buildTextures(y, u, v, mVideoWidth, mVideoHeight);
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                checkGlError("glClear");
                prog.drawFrame();
            }
        }
    }
    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("GLFrameRenderer","***** " + op + ": glError " + error, null);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
    /**
     * this method will be called from native code, it happens when the video is about to play or
     * the video size changes.
     */
    public void update(int w, int h) {
        Log.d("GLFrameRenderer","INIT E");
        if (w > 0 && h > 0) {
            // 调整比例
            if (mScreenWidth > 0 && mScreenHeight > 0) {
                float f1 = 1f * mScreenHeight / mScreenWidth;
                float f2 = 1f * h / w;
                if (f1 == f2) {
                    prog.createBuffers(GLProgram.squareVertices);
                } else if (f1 < f2) {
                    float widScale = f1 / f2;
                    prog.createBuffers(new float[] { -widScale, -1.0f, widScale, -1.0f, -widScale, 1.0f, widScale,
                            1.0f, });
                } else {
                    float heightScale = f2 / f1;
                    prog.createBuffers(new float[] { -1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f,
                            heightScale, });
                }
            }
            // 初始化容器
            if (w != mVideoWidth && h != mVideoHeight) {
                this.mVideoWidth = w;
                this.mVideoHeight = h;
                int yarraySize = w * h;
                int uvarraySize = yarraySize / 4;
                synchronized (this) {
                    y = ByteBuffer.allocate(yarraySize);
                    u = ByteBuffer.allocate(uvarraySize);
                    v = ByteBuffer.allocate(uvarraySize);
                }
            }
        }

        //mParentAct.onPlayStart();
        Log.d("GLFrameRenderer","INIT X");
    }

    /**
     * this method will be called from native code, it's used for passing yuv data to me.
     */
    public void update(byte[] data) {
        synchronized (this) {
            y.clear();
            u.clear();
            v.clear();
//            y.put(ydata, 0, ydata.length);
//            u.put(udata, 0, udata.length);
//            v.put(vdata, 0, vdata.length);
            System.arraycopy(data, 0, ydata, 0, LENGTH);
            y.put(ydata);
            y.position(0);

            System.arraycopy(data, U_INDEX, uData, 0, LENGTH_4);
            u.put(uData);
            u.position(0);

            System.arraycopy(data, V_INDEX, vData, 0, LENGTH_4);
            v.put(vData);
            v.position(0);
        }

        // request to render
        mTargetSurface.requestRender();
    }

    /**
     * this method will be called from native code, it's used for passing play state to activity.
     */
    public void updateState(int state) {
        Log.d("GLFrameRenderer","updateState E = " + state);
        //if (mParentAct != null) {
            //mParentAct.onReceiveState(state);
       // }
        Log.d("GLFrameRenderer","updateState X");
    }
}
