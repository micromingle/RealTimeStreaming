package com.jiandan.terence.rtvideotcpserver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static com.jiandan.terence.rtvideotcpserver.TlvBox.IMAGE;

public class MainActivity extends AppCompatActivity {

    ServerSocket mServerSocket;
    Socket mSocket;
    ImageView mImageView;
    private String TAG="MainActivity4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 获取wifi服务
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // 判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        mImageView=findViewById(R.id.image_view);
        int ipAddress = wifiInfo.getIpAddress();
        String ip = Util.intToIp(ipAddress);
        setTitle("服务端IP：" + ip);
       //开启socket 监听

       new Thread(new Runnable() {
           @Override
           public void run() {
               while(true){
                   try {
                       if(mServerSocket==null){
                           mServerSocket = new ServerSocket(3000);
                       }
                       Log.d(TAG,"i am listening");
                       Socket socket = mServerSocket.accept();
                       ExecutorManager.getExecutor().execute(new ReceiveDataRunnable(socket));
                      // new Thread().start();
                       Log.d(TAG,"spawn a runnable");
                   } catch (IOException e) {
                       e.printStackTrace();
                   }

               }
           }
       }).start();

    }

    private class ReceiveDataRunnable implements Runnable{

        Socket mSocket;

        public ReceiveDataRunnable(Socket socket) {
            this.mSocket = socket;
        }

        @Override
        public void run() {
            try {
                //Socket socket=mServerSocket.accept();
                InputStream inputStream=mSocket.getInputStream();
                long start = System.currentTimeMillis();
                int headerLength = 12;
                byte[] contentInfo = new byte[headerLength];
                inputStream.read(contentInfo);
                int totalSize = TlvBox.getTotalSize(contentInfo, 0, contentInfo.length);
                Log.d(TAG, "totalSize =" + totalSize);
                Log.d(TAG, "receive success cost time =" + (System.currentTimeMillis() - start));
                byte[] buffer = new byte[totalSize - headerLength];
                int nIdx = 0;//contentInfo.length;
                int nTotalLen = buffer.length;
                int nReadLen = 0;

                while (nIdx < nTotalLen) {
                    nReadLen = inputStream.read(buffer, nIdx, nTotalLen - nIdx);
                    Log.d(TAG, "read length =" + nReadLen);
                    if (nReadLen > 0) {
                        nIdx = nIdx + nReadLen;
                        Log.d(TAG, "nIdx =" + nIdx);
                    } else {
                        break;
                    }
                }
                final byte[] allData = new byte[totalSize];
                Log.d(TAG, "reading data");
                System.arraycopy(contentInfo, 0, allData, 0, contentInfo.length);
                System.arraycopy(buffer, 0, allData, headerLength, buffer.length);
                byte[] rawdata=allData;
                Log.d(TAG, "receive len=" + rawdata.length);
                long start1=System.currentTimeMillis();
                TlvBox parsedBox = TlvBox.parse(rawdata,0, rawdata.length);
                byte[] imageBytes = parsedBox.getBytesValue(IMAGE);
               final Bitmap bm = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                Log.d(TAG, "decode cost time ="+(System.currentTimeMillis()-start1));
                inputStream.close();
                mSocket.close();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setImageBitmap(bm);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
