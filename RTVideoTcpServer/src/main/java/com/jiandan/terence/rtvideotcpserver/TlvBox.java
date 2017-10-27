package com.jiandan.terence.rtvideotcpserver;/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.util.Log;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TlvBox {
    public static final int LENGTH = 0x05;
    public static final int WIDTH = 0x06;
    public static final int HEIGHT = 0x07;
    public static final int IMAGE = 0x08;
    public static final int CONTENT = 0x09;
    private static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static String TAG="com.jiandan.terence.rtvideotcpserver.TlvBox";
    private SparseArray<byte[]> mObjects;
    private int mTotalBytes = 0;

    public TlvBox() {
        mObjects = new SparseArray<byte[]>();
    }

    public static int getTotalSize(byte[] buffer,int offset,int length) {
        int parsed = 0;
        while (parsed < length) {
            int type = ByteBuffer.wrap(buffer,offset+parsed,4).order(DEFAULT_BYTE_ORDER).getInt();
            parsed += 4;
            int size = ByteBuffer.wrap(buffer,offset+parsed,4).order(DEFAULT_BYTE_ORDER).getInt();
            parsed += 4;
            int totalSize=size+8;
           return totalSize;
        }
       return 0;
    }
    
    public static TlvBox parse(byte[] buffer,int offset,int length) {
        Log.d(TAG, String.format("buffer size %s  length %s ",buffer.length,length));
        TlvBox box = new TlvBox();
        int parsed = 0;
        while (parsed < length) {
            int type = ByteBuffer.wrap(buffer,offset+parsed,4).order(DEFAULT_BYTE_ORDER).getInt();
            parsed += 4;
            int size = ByteBuffer.wrap(buffer,offset+parsed,4).order(DEFAULT_BYTE_ORDER).getInt();
            parsed += 4;
            Log.d(TAG, String.format("buffer size %s  size %s ",buffer.length,size));
            byte[] value = new byte[size];

            System.arraycopy(buffer, offset+parsed, value, 0, size);
            box.putBytesValue(type,value);
            parsed += size;
        }        
        
        return box;
    }
    
    public byte[] serialize() {
        int offset = 0;
        byte[] result = new byte[mTotalBytes];  
        for(int i=0; i<mObjects.size(); i++) {
            int key = mObjects.keyAt(i);
            byte[] bytes = mObjects.get(key);
            byte[] type   = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(key).array();
            byte[] length = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(bytes.length).array();
            System.arraycopy(type, 0, result, offset, type.length);
            offset += 4;
            System.arraycopy(length, 0, result, offset, length.length);
            offset += 4;
            System.arraycopy(bytes, 0, result, offset, bytes.length);
            offset += bytes.length;
        }
        return result;
    }
    
    public void putByteValue(int type,byte value) {
        byte[] bytes = new byte[1];        
        bytes[0] = value;
        putBytesValue(type,bytes);
    }
        
    public void putShortValue(int type,short value) {
        byte[] bytes = ByteBuffer.allocate(2).order(DEFAULT_BYTE_ORDER).putShort(value).array();
        putBytesValue(type,bytes);
    }
    
    public void putIntValue(int type,int value) {
        byte[] bytes = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(value).array();
        putBytesValue(type,bytes);
    }
    
    public void putLongValue(int type,long value) {
        byte[] bytes = ByteBuffer.allocate(8).order(DEFAULT_BYTE_ORDER).putLong(value).array();
        putBytesValue(type,bytes);
    }
    
    public void putFloatValue(int type,float value) {
        byte[] bytes = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putFloat(value).array();
        putBytesValue(type,bytes);
    }
    
    public void putDoubleValue(int type,double value) {
        byte[] bytes = ByteBuffer.allocate(8).order(DEFAULT_BYTE_ORDER).putDouble(value).array();
        putBytesValue(type,bytes);
    }
    
    public void putStringValue(int type,String value) {
        putBytesValue(type,value.getBytes());        
    }

    public void putObjectValue(int type,TlvBox value) {        
        putBytesValue(type,value.serialize());
    }
    
    public void putBytesValue(int type,byte[] value) {
        mObjects.put(type, value);
        mTotalBytes += value.length + 8;
    }
    
    public Byte getByteValue(int type) {
        byte[] bytes = mObjects.get(type);
        if (bytes == null) {
            return null;
        }
        return bytes[0];
    }
        
    public Short getShortValue(int type) {
        byte[] bytes = mObjects.get(type);
        if (bytes == null) {
            return null;
        }
        return ByteBuffer.wrap(bytes).order(DEFAULT_BYTE_ORDER).getShort();
    }
    
    public Integer getIntValue(int type) {
        byte[] bytes = mObjects.get(type);
        if (bytes == null) {
            return null;
        }
        return ByteBuffer.wrap(bytes).order(DEFAULT_BYTE_ORDER).getInt();
    }
    
    public Long getLongValue(int type) {
        byte[] bytes = mObjects.get(type);
        if (bytes == null) {
            return null;
        }
        return ByteBuffer.wrap(bytes).order(DEFAULT_BYTE_ORDER).getLong();
    }
    
    public Float getFloatValue(int type) {
        byte[] bytes = mObjects.get(type);
        if (bytes == null) {
            return null;
        }
        return ByteBuffer.wrap(bytes).order(DEFAULT_BYTE_ORDER).getFloat();
    }
    
    public Double getDoubleValue(int type) {
        byte[] bytes = mObjects.get(type);
        if (bytes == null) {
            return null;
        }
        return ByteBuffer.wrap(bytes).order(DEFAULT_BYTE_ORDER).getDouble();
    }
    
    public TlvBox getObjectValue(int type) {
        byte[] bytes = mObjects.get(type);
        if (bytes == null) {
            return null;
        }
        return TlvBox.parse(bytes, 0, bytes.length);
    }
    
    public String getStringValue(int type) {
        byte[] bytes = mObjects.get(type);
        if (bytes == null) {
            return null;
        }
        return new String(bytes).trim();
    }
    
    public byte[] getBytesValue(int type) {
        byte[] bytes = mObjects.get(type);
        return bytes;
    }
}
