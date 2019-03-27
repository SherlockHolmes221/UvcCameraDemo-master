package com.serenegiant.usbcameracommon;


import java.nio.ByteBuffer;

public interface UvcCameraDataCallBack {

    /**
     * @param data        摄像头回调原始数据
     */
    void getData(byte[] data,ByteBuffer frame);
}
