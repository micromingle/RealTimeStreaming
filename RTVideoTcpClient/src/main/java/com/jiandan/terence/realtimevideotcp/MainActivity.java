package com.jiandan.terence.realtimevideotcp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private Camera mCamera;
    private Camera.Parameters mParametars;
    private int mCamWidth = 512;
    private int mCamHeight = 384;
    protected String ipname;
    protected int mPort = 1234;//= AppConfig.VPort;
    private boolean isPreview = false;
    private SurfaceHolder mHolder;
    private String DevInfo = android.os.Build.MODEL + " Android " + android.os.Build.VERSION.RELEASE;
    //   SendSoundsThread sst;
    private EditText mEtAddress;
    private Socket mSocket;
    private SurfaceView mSurfaceView;
    protected boolean isRun = false;
    private String TAG = "MainActivity3";
    Button mConnectButton;
    private String mIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSurfaceView = findViewById(R.id.surface_view);
        mHolder = mSurfaceView.getHolder();
        mHolder.setFixedSize(200, 200);
        mHolder.addCallback(MainActivity.this);
        mConnectButton = findViewById(R.id.btn_connect);
        mEtAddress = findViewById(R.id.et_address);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIp = mEtAddress.getText().toString();
                mSurfaceView.setVisibility(View.VISIBLE);
            }
        });
    }


    /**
     * 初始化相机
     */
    private void initCamera() {
        if (!isPreview) {
            mCamera = Camera.open();
        }
        if (mCamera != null && !isPreview) {
            mParametars = mCamera.getParameters();
            mParametars.setFlashMode(Camera.Parameters.FLASH_MODE_OFF); // 无闪光灯
            mParametars.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            mParametars.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            mParametars.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mParametars.setPreviewFormat(ImageFormat.NV21);
            mParametars.setPictureSize(mCamWidth, mCamHeight);
            mParametars.setPreviewSize(mCamWidth, mCamHeight);
            mParametars.setPreviewFpsRange(13, 15);
            mCamera.autoFocus(null);
            setCameraDisplay(mCamera);
            byte[] buf = new byte[mCamWidth * mCamHeight * 3 / 2];
            mCamera.addCallbackBuffer(buf);
            mCamera.setPreviewCallback(this);
            isPreview = true;
        }
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

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        initCamera();
        // sst.setRunning(true);
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            Log.d(TAG, "surfaceChanged");
        } catch (IOException e) {
            System.out.println("surfaceChanged===" + e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int format = parameters.getPreviewFormat();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;
        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 20, outputStream);
        byte[] imageData = outputStream.toByteArray();
        sendVideo(imageData);

    }

    /**
     * 发送视频
     *
     * @param imageData
     */
    private void sendVideo(final byte[] imageData) {
        // 启用线程将图像数据发送出去
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isRun) {
                    try {
                        long start = System.currentTimeMillis();
                        // 将图像数据通过Socket发送出去
                        byte[] data = imageData;
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(mIp, 3000));
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(data);
                        Log.d(TAG, "send size = " + data.length);
                        Log.d(TAG, "send cost time =" + (System.currentTimeMillis() - start));
                    } catch (IOException e) {
                        System.out.println("onPreviewFrame.Thread===" + e);
                    }
                }
            }
        }).start();
    }
}