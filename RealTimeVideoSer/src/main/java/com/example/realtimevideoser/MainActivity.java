package com.example.realtimevideoser;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity implements Callback {

	private DatagramSocket mSocket;
	private int mCamWidth = 480;
	private int mCamHeight = 320;
	private TextView tv;
	// private SurfaceView sfv;
	private String DevInfo = android.os.Build.MODEL + " Android " + android.os.Build.VERSION.RELEASE;
	ReceiveSoundsThread rst = new ReceiveSoundsThread();
	private SurfaceHolder holder;
	private String TAG="MainActivity3";
	private Handler mHandler =new Handler(){
        @Override
        public void handleMessage(Message msg) {
            tv.setText((String)msg.obj);
            Log.d(TAG,"setting text");
            super.handleMessage(msg);
        }
    };
    public static String getPackedId(byte[] b) {
        return ByteBuffer.wrap(b, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() + "";
    }
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		tv = (TextView) findViewById(R.id.sfv);
		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		// 判断wifi是否开启
		if (!wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
		}
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String ip = intToIp(ipAddress);
		setTitle("服务端IP：" + ip);

		// sfv = (SurfaceView) findViewById(R.id.sf);
		// holder = sfv.getHolder();
		// holder.addCallback(this);
		try {
			mSocket = new DatagramSocket(AppConfig.VPort);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            long start=System.currentTimeMillis();
                            int bufferSize=mCamWidth * mCamHeight * 3 / 2;
                            //bufferSize=10000;
                            byte[] message = new byte[bufferSize];
                            Log.d(TAG,"receive buffer size = " +message.length);
                            DatagramPacket datagramPacket = new DatagramPacket(message, message.length);
                            mSocket.receive(datagramPacket);
                            byte[] bytes = datagramPacket.getData();
                            byte[] data = new byte[message.length - 4];
                            final byte[] packetId = new byte[4];//包的标识id
                            System.arraycopy(bytes, 0, packetId, 0, packetId.length);
                            System.arraycopy(bytes, packetId.length, data, 0, data.length);


                            Log.d(TAG,"receive size = " +bytes.length);
                            Log.d(TAG,"receive cost time ="+(System.currentTimeMillis()-start));
                            if (bytes.length > 0) {
                                final Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                // Matrix matrix=new Matrix();
                                // matrix.postRotate(-90);
                                // // 重新绘制Bitmap
                                // final Bitmap bitmap = Bitmap.createBitmap(bm, 0,
                                // 0, bm.getWidth(),bm.getHeight(), matrix, true);
//                                Message message1=mHandler.obtainMessage(0,getPackedId(packetId));
//                                message1.sendToTarget();
                                runOnUiThread(new Runnable() {
                                    @SuppressLint("NewApi")
                                    public void run() {
                                        tv.setText(getPackedId(packetId));
                                        Log.d(TAG,"setting text");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		//rst.start();
		//rst.setRunning(true);

	}

	private String intToIp(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
	}

	class ReceiveSoundsThread extends Thread {
		private AudioTrack player = null;
		private boolean isRunning = false;
		private byte[] recordBytes = new byte[670];

		public ReceiveSoundsThread() {
			// 播放器
			int playerBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
			player = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, playerBufferSize, AudioTrack.MODE_STREAM);
		}

		@Override
		public synchronized void run() {
			super.run();

			try {
				@SuppressWarnings("resource")
				DatagramSocket serverSocket = new DatagramSocket(AppConfig.Port);
				while (true) {
					if (isRunning) {
						DatagramPacket receivePacket = new DatagramPacket(recordBytes, recordBytes.length);
						serverSocket.receive(receivePacket);

						byte[] data = receivePacket.getData();

						byte[] head = new byte[30];
						byte[] body = new byte[640];

						// 获得包头
						for (int i = 0; i < head.length; i++) {
							head[i] = data[i];
						}

						// 获得包体
						for (int i = 0; i < body.length; i++) {
							body[i] = data[i + 30];
						}

						// 获得头信息 通过头信息判断是否是自己发出的语音
						String thisDevInfo = new String(head).trim();
						System.out.println(thisDevInfo);

						if (!thisDevInfo.equals(DevInfo)) {
							player.write(body, 0, body.length);
							player.play();
						}
					}
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void setRunning(boolean isRunning) {
			this.isRunning = isRunning;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						long start=System.currentTimeMillis();
						byte[] message = new byte[mCamWidth * mCamHeight * 3 / 2];
						DatagramPacket datagramPacket = new DatagramPacket(message, message.length);
						mSocket.receive(datagramPacket);
						byte[] bytes = datagramPacket.getData();
						if (bytes.length > 0) {
							final Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
							// Matrix matrix=new Matrix();
							// matrix.postRotate(-90);
							// // 重新绘制Bitmap
							// final Bitmap bitmap = Bitmap.createBitmap(bm, 0,
							// 0, bm.getWidth(),bm.getHeight(), matrix, true);
							Canvas can = holder.lockCanvas();
							can.drawBitmap(bm, 0, 0, null);
							holder.unlockCanvasAndPost(can);
							// 重新锁定一次,"持久化"上次所绘制的内容
							holder.lockCanvas(new Rect(0, 0, 0, 0));
							holder.unlockCanvasAndPost(can);
						}
						Log.d(TAG,"receive cost time ="+(System.currentTimeMillis()-start));
					} catch (Exception e) {
					}
				}
			}
		}).start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
	}
}
