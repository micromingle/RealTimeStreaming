package com.test.realtimevideo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.R.layout;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements Callback, PreviewCallback {

	private Camera mCamera;
	private Parameters mParametars;
	private int mCamWidth = 512;
	private int mCamHeight = 384;
	protected String ipname;
	protected int mPort = AppConfig.VPort;
	protected DatagramSocket mSocket;
	private boolean isPreview = false;
	private SurfaceHolder mHolder;
	private String DevInfo = android.os.Build.MODEL + " Android " + android.os.Build.VERSION.RELEASE;
	SendSoundsThread sst;
	private EditText et_ip;
	private SurfaceView surfaceView;
	protected boolean isRun=false;
    private String TAG="MainActivity3";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		surfaceView = (SurfaceView) findViewById(R.id.sfv);
		mHolder = surfaceView.getHolder();
		mHolder.setFixedSize(200, 200);
		mHolder.addCallback(MainActivity.this);
		try {
			mSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println("onCreate===" + e);
		}
		View view = LayoutInflater.from(this).inflate(R.layout.dialog, null, false);
		et_ip = (EditText) view.findViewById(R.id.et_ip);
		AlertDialog alertDialog = new AlertDialog.Builder(this).setTitle("设置服务端地址").setView(view)
				.setNegativeButton("确定", new OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						ipname = et_ip.getText().toString().trim();
						if (ipname.isEmpty()) {
							Toast.makeText(MainActivity.this, "地址不能空", Toast.LENGTH_LONG).show();
						} else {
							sst = new SendSoundsThread();
							surfaceView.setVisibility(View.VISIBLE);
							isRun=true;
							//sst.start();
						}
					}
				}).create();
		alertDialog.show();
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
			mParametars.setFlashMode(Parameters.FLASH_MODE_OFF); // 无闪光灯
			mParametars.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
			mParametars.setSceneMode(Parameters.SCENE_MODE_AUTO);
			mParametars.setFocusMode(Parameters.FOCUS_MODE_AUTO);
			mParametars.setPreviewFormat(ImageFormat.NV21);
			mParametars.setPictureSize(mCamWidth, mCamHeight);
            //mParametars.getSupportedPreviewSizes();
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
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info);
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
		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
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
		sst.setRunning(true);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			System.out.println("surfaceChanged===" + e);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		isRun=false;
		sst.setRunning(false);
	}

	int count=0;
	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera) {
        if(count%4==0) {
            Size size = paramCamera.getParameters().getPreviewSize();
            long start=System.currentTimeMillis();
            try {

                // 调用image.compressToJpeg（）将YUV格式图像数据data转为jpg格式
                YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                if (image != null) {
                    final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                    image.compressToJpeg(new Rect(0, 0, size.width, size.height), 20, outstream);
                    outstream.flush();
                    sendVideo(outstream);
                }
                Log.d(TAG,"encode cost time ="+(System.currentTimeMillis()-start));
            } catch (Exception ex) {
                System.out.println("onPreviewFrame===" + ex);
                mSocket.close();
            }
        }
        count++;
	}

	/**
	 * 发送视频
	 * 
	 * @param outstream
	 */
	private void sendVideo(final ByteArrayOutputStream outstream) {
		// 启用线程将图像数据发送出去
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (isRun) {
					try {
                        long start=System.currentTimeMillis();
                        // 将图像数据通过Socket发送出去
						byte[] data = outstream.toByteArray();
						DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ipname),
								mPort);
						mSocket.send(packet);
                        Log.d(TAG,"send size = " +data.length);
                        Log.d(TAG,"send cost time ="+(System.currentTimeMillis()-start));
					} catch (IOException e) {
						System.out.println("onPreviewFrame.Thread===" + e);
					}
				}
			}
		}).start();
	}

    private void sendVideo2(final byte[] dataToSend ) {
        // 启用线程将图像数据发送出去
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isRun) {
                    try {
                        long start=System.currentTimeMillis();
                        // 将图像数据通过Socket发送出去
                        byte[] data =dataToSend;// outstream.toByteArray();
                        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ipname),
                                mPort);
                        mSocket.send(packet);
                        Log.d(TAG,"send size = " +data.length);
                        Log.d(TAG,"send cost time ="+(System.currentTimeMillis()-start));
                    } catch (IOException e) {
                        System.out.println("onPreviewFrame.Thread===" + e);
                    }
                }
            }
        }).start();
    }

	class SendSoundsThread extends Thread {
		private AudioRecord recorder = null;
		private boolean isRunning = false;
		private byte[] recordBytes = new byte[640];

		public SendSoundsThread() {
			super();

			// 录音机
			int recordBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, recordBufferSize);
		}

		@Override
		public synchronized void run() {
			super.run();
			recorder.startRecording();

			while (true) {
				if (isRunning) {
					try {
						DatagramSocket clientSocket = new DatagramSocket();
						InetAddress IP = InetAddress.getByName(ipname);// 向这个网络广播

						// 获取音频数据
						recorder.read(recordBytes, 0, recordBytes.length);

						// 构建数据包 头+体
						DataPacket dataPacket = new DataPacket(DevInfo.getBytes(), recordBytes);

						// 构建数据报
						DatagramPacket sendPacket = new DatagramPacket(dataPacket.getAllData(),
								dataPacket.getAllData().length, IP, AppConfig.Port);

						// 发送
						clientSocket.send(sendPacket);
						clientSocket.close();
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		public void setRunning(boolean isRunning) {
			this.isRunning = isRunning;
		}
	}
}
