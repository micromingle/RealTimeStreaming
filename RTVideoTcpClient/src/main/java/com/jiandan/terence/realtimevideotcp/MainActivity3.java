package com.jiandan.terence.realtimevideotcp;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import static com.jiandan.terence.realtimevideotcp.TlvBox.IMAGE;

public class MainActivity3 extends AppCompatActivity implements Camera.PreviewCallback {

    private GLSurfaceView mGLSurfaceView;
    GLRenderer mRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main3);
        mGLSurfaceView = findViewById(R.id.my_gl_surface_view);
        mRender = new GLES20Renderer();
        mGLSurfaceView.setRenderer(new MyRenderer(this));
        //mGLSurfaceView.setRenderMode(RENDERMODE_WHEN_DIRTY);
        // mHolder = mSurfaceView.getHolder();
        // mHolder.setFormat(PixelFormat.TRANSPARENT);
//surfceview放置在顶层，即始终位于最上层
        //mSurfaceView.setZOrderOnTop(true);
        //mHolder.setFixedSize(200, 200);
        // mHolder.addCallback(MainActivity.this);

    }

    private boolean hasGLES20() {
        ActivityManager am = (ActivityManager)
                getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return info.reqGlEsVersion >= 0x20000;
    }


    /**
     * 设置摄像头方向
     *
     * @param camera
     */
    public void setCameraDisplay(Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


    int count = 0;

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {



    }


}
