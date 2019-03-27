package com.vinda.ucvcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.usbcameracommon.UvcCameraDataCallBack;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;
import com.yuan.camera.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 显示多路摄像头
 */
public class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    private static final boolean DEBUG = true;
    private static final String TAG = "MainActivity";

    private static final float[] BANDWIDTH_FACTORS = {0.5f, 0.5f};

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;

    private UVCCameraHandler mHandlerFirst;
    private CameraViewInterface mUVCCameraViewFirst;
    private ImageButton mCaptureButtonFirst;
    private Surface mFirstPreviewSurface;

    private UVCCameraHandler mHandlerSecond;
    private CameraViewInterface mUVCCameraViewSecond;
    private ImageButton mCaptureButtonSecond;
    private Surface mSecondPreviewSurface;

    private UVCCameraHandler mHandlerThird;
    private CameraViewInterface mUVCCameraViewThird;
    private ImageButton mCaptureButtonThird;
    private Surface mThirdPreviewSurface;

    private UVCCameraHandler mHandlerFourth;
    private CameraViewInterface mUVCCameraViewFourth;
    private ImageButton mCaptureButtonFourth;
    private Surface mFourthPreviewSurface;

    private UVCCameraHandler mHandlerFifth;
    private CameraViewInterface mUVCCameraViewFifth;
    private ImageButton mCaptureButtonFifth;
    private Surface mFifthPreviewSurface;


    private UVCCameraHandler mHandlerSixth;
    private CameraViewInterface mUVCCameraViewSixth;
    private ImageButton mCaptureButtonSixth;
    private Surface mSixthPreviewSurface;


    private final Object mSync = new Object();
    private Boolean isRecord = false;
    private String tid;
    private TextView textView ;
    private int deviceId= 1001;
    //创建OkHttpClient对象
    OkHttpClient okHttpClient = new OkHttpClient();
    private String picPath;
    private Bitmap bitmap;

    //读写权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_view_camera);

        send();

        findViewById(R.id.RelativeLayout1).setOnClickListener(mOnClickListener);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        resultFirstCamera();
        resultSecondCamera();
        resultThirdCamera();
        resultFourthCamera();
        resultFifThCamrea();
        resultSixthCamera();

        textView = findViewById(R.id.textView1);

        //checkPermissionWriteExternalStorage();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }

    }

    private void send() {
        //开启线程，发送请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    Log.e("Tag","开始发送请求");
                    if (longPulling()) {
                        Log.e("longPulling", "true " );
                        //isRecord = true;
                        show("执行拍照指令");
                        bitmap = mUVCCameraViewFirst.captureStillImage();
                        if(bitmap == null)
                            Log.e(TAG, "run: null");
                        bitmap = comp(bitmap);
                        try{
                            picPath = saveToSDCard(bitmabToBytes(bitmap));
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        uploadPicture();
                    }else
                        Log.e("longPulling", "false");
                }
            }
        }).start();
    }

    protected boolean longPulling() {
        Log.e(TAG, "longPulling");
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String res = "";
        try {
            URL url = new URL("http://lt.good.rouis.tech/android/message/" + deviceId);
            connection = (HttpURLConnection) url.openConnection();
            //设置请求方法
            connection.setRequestMethod("GET");

            //返回输入流
            InputStream in = connection.getInputStream();
            //读取输入流
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            res = result.toString();
            JSONObject json = new JSONObject(res);
            Log.e("Tag", "res: " + res);
            String status = json.getString("status");
            Log.e("Tag", status );
            if(status.equals("2000")){
                tid = json.getJSONObject("content").getString("tid");
                Log.e("longpull",tid);
                return true;
            }
            return false;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {//关闭连接
                connection.disconnect();
            }
        }
        return false;
    }

    private void show(final String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append(textView.getText());
                sb.append("   ");
                sb.append(result);
                textView.setText(sb);
            }
        });
    }

    private void uploadPicture(){
        String targetUrl  = "http://lt.good.rouis.tech/android/upload";
        String[] sourceStrArray = picPath.split("/");
        String filename = sourceStrArray[sourceStrArray.length-1];
        Log.e("filename", filename );
        //上传的图
        try{
            Log.e("tid", tid);
            File file = new File(picPath);

            //获取了file


            //2.通过new MultipartBody build() 创建requestBody对象，
            RequestBody requestBody = new MultipartBody.Builder()
                    //设置类型是表单
                    .setType(MultipartBody.FORM)
                    //添加数据
                    .addFormDataPart("tid",tid)
                    .addFormDataPart("pic1",filename,
                            RequestBody.create(MediaType.parse("image/png"),file))
                    .build();
            //3.创建Request对象，设置URL地址，将RequestBody作为post方法的参数传入
            final Request request = new Request.Builder().url(targetUrl).post(requestBody).build();
            //4.创建一个call对象,参数就是Request请求对象
            Call call = okHttpClient.newCall(request);
            //5.请求加入调度,重写回调方法
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("onFailure", "onFailure");
                    Log.e("onFailure",e.getMessage());
                    Log.e("onFailure",e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String data = response.body().string();
                    Log.e("onResponse", data);

                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        Log.e("onResponse ",jsonObject.getString("msg") );
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            show("照片上传成功");
                        }
                    });
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 将拍下来的照片存放在SD卡中
     * @param data
     * @throws IOException
     */
    public static String saveToSDCard(byte[] data) throws IOException {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        // File fileFolder = new File(getTrueSDCardPath()
        //       + "/rebot/cache/");

        File sdCard = Environment.getExternalStorageDirectory();
        File fileFolder = new File(sdCard, "Pictures");

        if (!fileFolder.exists()) {
            fileFolder.mkdir();
        }
        File jpgFile = new File(fileFolder, filename);
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
        outputStream.write(data); // 写入sd卡中

        outputStream.close(); // 关闭输出流
        return jpgFile.getPath().toString();
    }

    public  static String createPath(){
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        // File fileFolder = new File(getTrueSDCardPath()
        //       + "/rebot/cache/");

        File sdCard = Environment.getExternalStorageDirectory();
        File fileFolder = new File(sdCard, "Pictures");

        if (!fileFolder.exists()) {
            fileFolder.mkdir();
        }
        File jpgFile = new File(fileFolder, filename);
        return jpgFile.getPath().toString();
    }


    public byte[] bitmabToBytes(Bitmap bitmap){
        //将图片转化为位图
        ///Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        //创建一个字节数组输出流,流的大小为size
        ByteArrayOutputStream baos= new ByteArrayOutputStream(size);
        try {
            //设置位图的压缩格式，质量为100%，并放入字节数组输出流中
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            //将字节数组输出流转化为字节数组byte[]
            byte[] imagedata = baos.toByteArray();
            return imagedata;
        }catch (Exception e){
        }finally {
            try {
                bitmap.recycle();
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    private Bitmap comp(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        if (baos.toByteArray().length / 1024 >
                1024) {//判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, 50, baos);//这里压缩50%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(isBm, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        float hh = 720f;
        float ww = 1280f;
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0) {
            be = 1;
        }
        newOpts.inSampleSize = be;//设置缩放比例
        newOpts.inPreferredConfig = Bitmap.Config.RGB_565;//降低图片从ARGB888到RGB565
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        isBm = new ByteArrayInputStream(baos.toByteArray());
        bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }

    private Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            options -= 10;//每次都减少10
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中

        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(
                baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    public Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            Log.e(TAG, "Bytes2Bimap"+b.length);
            Log.e(TAG, "Bytes2Bimap"+ByteBuffer.wrap(b).toString());
            return BitmapFactory.decodeByteArray(b, 0, b.length);

        } else {
            Log.e(TAG, "null");
            return null;
        }
    }


    /**
     * 带有回调数据的初始化
     */
    private void resultFirstCamera() {
        mUVCCameraViewFirst = (CameraViewInterface) findViewById(R.id.camera_view_first);
        //设置显示宽高
        mUVCCameraViewFirst.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        //((UVCCameraTextureView) mUVCCameraViewFirst).setOnClickListener(mOnClickListener);
        mCaptureButtonFirst = (ImageButton) findViewById(R.id.capture_button_first);
        //mCaptureButtonFirst.setOnClickListener(mOnClickListener);
        mCaptureButtonFirst.setVisibility(View.INVISIBLE);
        mHandlerFirst = UVCCameraHandler.createHandler(this, mUVCCameraViewFirst
                , UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT
                , BANDWIDTH_FACTORS[0], firstDataCallBack);
    }

    UvcCameraDataCallBack firstDataCallBack = new UvcCameraDataCallBack() {
        @Override
        public void getData(byte[] data,ByteBuffer frame) {
            //if (DEBUG) Log.v(TAG, "数据回调1");

            //try {
//                if (isRecord) {
//                    Log.e("save pic1", "save pic1");
//                    isRecord = false;
//                    //bitmap = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT,Bitmap.Config.RGB_565);
//
//                    //有问题
//                    Log.e("save pic2", "save pic2");
//                    bitmap = Bytes2Bimap(data);
//                    if(bitmap == null)
//                        Log.e("null1", "null1");
//                    Log.e("save pic3", "save pic3");
//                    bitmap = comp(bitmap);
//                    if(bitmap == null)
//                        Log.e("null1", "null2");
//                    Log.e("save pic3", "save pic3");
//                    try {
//                        picPath = saveToSDCard(bitmabToBytes(bitmap));
//                    }catch (Exception e){
//                        Log.e("error", e.toString());
//
//                        e.printStackTrace();
//                    }
//                    Log.e("save pic", "save pic");
//                    uploadPicture();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    };

    private void resultSecondCamera() {
        mUVCCameraViewSecond = (CameraViewInterface) findViewById(R.id.camera_view_second);
        mUVCCameraViewSecond.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        //((UVCCameraTextureView) mUVCCameraViewSecond).setOnClickListener(mOnClickListener);
        mCaptureButtonSecond = (ImageButton) findViewById(R.id.capture_button_second);
        //mCaptureButtonSecond.setOnClickListener(mOnClickListener);
        mCaptureButtonSecond.setVisibility(View.INVISIBLE);
        mHandlerSecond = UVCCameraHandler.createHandler(this, mUVCCameraViewSecond,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                BANDWIDTH_FACTORS[0],secondDataCallBack);
    }

    UvcCameraDataCallBack secondDataCallBack = new UvcCameraDataCallBack() {
        @Override
        public void getData(byte[] data,ByteBuffer frame) {
            if (DEBUG) Log.v(TAG, "数据回调2:" + data.length);
        }
    };

    private void resultThirdCamera() {
        mUVCCameraViewThird = (CameraViewInterface) findViewById(R.id.camera_view_third);
        mUVCCameraViewThird.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        //((UVCCameraTextureView) mUVCCameraViewThird).setOnClickListener(mOnClickListener);
        mCaptureButtonThird = (ImageButton) findViewById(R.id.capture_button_third);
        //mCaptureButtonThird.setOnClickListener(mOnClickListener);
        mCaptureButtonThird.setVisibility(View.INVISIBLE);
        mHandlerThird = UVCCameraHandler.createHandler(this, mUVCCameraViewThird,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                BANDWIDTH_FACTORS[1],thirdDataCallBack);
    }

    UvcCameraDataCallBack thirdDataCallBack = new UvcCameraDataCallBack() {
        @Override
        public void getData(byte[] data,ByteBuffer frame) {
            if (DEBUG) Log.v(TAG, "数据回调3:" + data.length);
        }
    };


    private void resultFourthCamera() {
        mUVCCameraViewFourth = (CameraViewInterface) findViewById(R.id.camera_view_fourth);
        mUVCCameraViewFourth.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        //((UVCCameraTextureView) mUVCCameraViewFourth).setOnClickListener(mOnClickListener);
        mCaptureButtonFourth = (ImageButton) findViewById(R.id.capture_button_fourth);
        // mCaptureButtonFourth.setOnClickListener(mOnClickListener);
        mCaptureButtonFourth.setVisibility(View.INVISIBLE);
        mHandlerFourth = UVCCameraHandler.createHandler(this, mUVCCameraViewFourth,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                BANDWIDTH_FACTORS[1],fourthDataCallBack);
    }

    UvcCameraDataCallBack fourthDataCallBack = new UvcCameraDataCallBack() {
        @Override
        public void getData(byte[] data,ByteBuffer frame) {
            if (DEBUG) Log.v(TAG, "数据回调4:" + data.length);
        }
    };

    private void resultFifThCamrea() {
        mUVCCameraViewFifth = (CameraViewInterface) findViewById(R.id.camera_view_fifth);
        mUVCCameraViewFifth.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        // ((UVCCameraTextureView) mUVCCameraViewFifth).setOnClickListener(mOnClickListener);
        mCaptureButtonFifth = (ImageButton) findViewById(R.id.capture_button_fifth);
        // mCaptureButtonFifth.setOnClickListener(mOnClickListener);
        mCaptureButtonFifth.setVisibility(View.INVISIBLE);
        mHandlerFifth = UVCCameraHandler.createHandler(this, mUVCCameraViewFifth,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                BANDWIDTH_FACTORS[1],fifthDataCallBack);
    }

    UvcCameraDataCallBack fifthDataCallBack = new UvcCameraDataCallBack() {
        @Override
        public void getData(byte[] data,ByteBuffer frame) {
            if (DEBUG) Log.v(TAG, "数据回调5:" + data.length);
        }
    };

    private void resultSixthCamera() {
        mUVCCameraViewSixth = (CameraViewInterface) findViewById(R.id.camera_view_sixth);
         mUVCCameraViewSixth.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        //((UVCCameraTextureView) mUVCCameraViewSixth).setOnClickListener(mOnClickListener);
        mCaptureButtonSixth = (ImageButton) findViewById(R.id.capture_button_sixth);
        //mCaptureButtonSixth.setOnClickListener(mOnClickListener);
        mCaptureButtonSixth.setVisibility(View.INVISIBLE);
        mHandlerSixth = UVCCameraHandler.createHandler(this, mUVCCameraViewSixth,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                BANDWIDTH_FACTORS[1],sixthDataCallBack);
    }

    UvcCameraDataCallBack sixthDataCallBack = new UvcCameraDataCallBack() {
        @Override
        public void getData(byte[] data,ByteBuffer frame) {
            if (DEBUG) Log.v(TAG, "数据回调6:" + data.length);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        if (mUVCCameraViewSecond != null)
            mUVCCameraViewSecond.onResume();
        if (mUVCCameraViewFirst != null)
            mUVCCameraViewFirst.onResume();
        if (mUVCCameraViewThird != null)
            mUVCCameraViewThird.onResume();
        if (mUVCCameraViewFourth != null)
            mUVCCameraViewFourth.onResume();
        if (mUVCCameraViewFifth != null)
            mUVCCameraViewFifth.onResume();
        if (mUVCCameraViewSixth != null)
            mUVCCameraViewSixth.onResume();
    }

    @Override
    protected void onStop() {
        mHandlerFirst.close();
        if (mUVCCameraViewFirst != null)
            mUVCCameraViewFirst.onPause();
        mCaptureButtonFirst.setVisibility(View.INVISIBLE);

        mHandlerSecond.close();
        if (mUVCCameraViewSecond != null)
            mUVCCameraViewSecond.onPause();
        mCaptureButtonSecond.setVisibility(View.INVISIBLE);

        mHandlerThird.close();
        if (mUVCCameraViewThird != null)
            mUVCCameraViewThird.onPause();
        mCaptureButtonThird.setVisibility(View.INVISIBLE);

        mHandlerFourth.close();
        if (mUVCCameraViewFourth != null)
            mUVCCameraViewFourth.onPause();
        mCaptureButtonFourth.setVisibility(View.INVISIBLE);

        mHandlerFifth.close();
        if (mUVCCameraViewFifth != null)
            mUVCCameraViewFifth.onPause();
        mCaptureButtonFifth.setVisibility(View.INVISIBLE);

        mHandlerSixth.close();
        if (mUVCCameraViewSixth != null)
            mUVCCameraViewSixth.onPause();
        mCaptureButtonSixth.setVisibility(View.INVISIBLE);

        mUSBMonitor.unregister();//usb管理器解绑
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mHandlerFirst != null) {
            mHandlerFirst = null;
        }

        if (mHandlerSecond != null) {
            mHandlerSecond = null;
        }

        if (mHandlerThird != null) {
            mHandlerThird = null;
        }

        if (mHandlerFourth != null) {
            mHandlerFourth = null;
        }

        if (mHandlerFifth != null) {
            mHandlerFifth = null;
        }

        if (mHandlerSixth != null) {
            mHandlerSixth = null;
        }

        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        mUVCCameraViewFirst = null;
        mCaptureButtonFirst = null;

        mUVCCameraViewSecond = null;
        mCaptureButtonSecond = null;

        mUVCCameraViewThird = null;
        mCaptureButtonThird = null;

        mUVCCameraViewFourth = null;
        mCaptureButtonFourth = null;

        mUVCCameraViewFifth = null;
        mCaptureButtonFifth = null;

        mUVCCameraViewSixth = null;
        mCaptureButtonSixth = null;
        super.onDestroy();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.camera_view_first:
                    if (mHandlerFirst != null) {
                        if (!mHandlerFirst.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mHandlerFirst.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.capture_button_first:
                    if (mHandlerFirst != null) {
                        if (mHandlerFirst.isOpened()) {
                            if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                                if (!mHandlerFirst.isRecording()) {
                                    mCaptureButtonFirst.setColorFilter(0xffff0000);    // turn red
                                    mHandlerFirst.startRecording();
                                } else {
                                    mCaptureButtonFirst.setColorFilter(0);    // return to default color
                                    mHandlerFirst.stopRecording();
                                }
                            }
                        }
                    }
                    break;
                case R.id.camera_view_second:
                    if (mHandlerSecond != null) {
                        if (!mHandlerSecond.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mHandlerSecond.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.capture_button_second:
                    if (mHandlerSecond != null) {
                        if (mHandlerSecond.isOpened()) {
                            if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                                if (!mHandlerSecond.isRecording()) {
                                    mCaptureButtonSecond.setColorFilter(0xffff0000);    // turn red
                                    mHandlerSecond.startRecording();
                                } else {
                                    mCaptureButtonSecond.setColorFilter(0);    // return to default color
                                    mHandlerSecond.stopRecording();
                                }
                            }
                        }
                    }
                    break;
                case R.id.camera_view_third:
                    if (mHandlerThird != null) {
                        if (!mHandlerThird.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mHandlerThird.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.capture_button_third:
                    if (mHandlerThird != null) {
                        if (mHandlerThird.isOpened()) {
                            if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                                if (!mHandlerThird.isRecording()) {
                                    mCaptureButtonThird.setColorFilter(0xffff0000);    // turn red
                                    mHandlerThird.startRecording();
                                } else {
                                    mCaptureButtonThird.setColorFilter(0);    // return to default color
                                    mHandlerThird.stopRecording();
                                }
                            }
                        }
                    }
                    break;
                case R.id.camera_view_fourth:
                    if (mHandlerFourth != null) {
                        if (!mHandlerFourth.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mHandlerFourth.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.capture_button_fourth:
                    if (mHandlerFourth != null) {
                        if (mHandlerFourth.isOpened()) {
                            if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                                if (!mHandlerFourth.isRecording()) {
                                    mCaptureButtonFourth.setColorFilter(0xffff0000);    // turn red
                                    mHandlerFourth.startRecording();
                                } else {
                                    mCaptureButtonFourth.setColorFilter(0);    // return to default color
                                    mHandlerFourth.stopRecording();
                                }
                            }
                        }
                    }
                    break;

                case R.id.camera_view_fifth:
                    if (mHandlerFifth != null) {
                        if (!mHandlerFifth.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mHandlerFifth.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.capture_button_fifth:
                    if (mHandlerFifth != null) {
                        if (mHandlerFifth.isOpened()) {
                            if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                                if (!mHandlerFifth.isRecording()) {
                                    mCaptureButtonFifth.setColorFilter(0xffff0000);    // turn red
                                    mHandlerFifth.startRecording();
                                } else {
                                    mCaptureButtonFifth.setColorFilter(0);    // return to default color
                                    mHandlerFifth.stopRecording();
                                }
                            }
                        }
                    }
                    break;

                case R.id.camera_view_sixth:
                    if (mHandlerSixth != null) {
                        if (!mHandlerSixth.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mHandlerSixth.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.capture_button_sixth:
                    if (mHandlerSixth != null) {
                        if (mHandlerSixth.isOpened()) {
                            if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                                if (!mHandlerSixth.isRecording()) {
                                    mCaptureButtonSixth.setColorFilter(0xffff0000);    // turn red
                                    mHandlerSixth.startRecording();
                                } else {
                                    mCaptureButtonSixth.setColorFilter(0);    // return to default color
                                    mHandlerSixth.stopRecording();
                                }
                            }
                        }
                    }
                    break;
            }
        }
    };

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (DEBUG) Log.e(TAG, "onAttach:");
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();

            //synchronized (mSync) {
                if (device.getDeviceClass() == 239 && device.getDeviceSubclass() == 2) {
                    mUSBMonitor.requestPermission(device);
                }
           //}
//            new Handler().postDelayed(new Runnable(){
//                public void run() {
//                    //execute the task
//                    Toast.makeText(MainActivity.this, "申请", Toast.LENGTH_SHORT).show();
//                    if (device.getDeviceClass() == 239 && device.getDeviceSubclass() == 2) {
//                        mUSBMonitor.requestPermission(device);
//                    }
//                }
//            }, 2000);
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            //设备连接成功
            if (DEBUG) Log.e(TAG, "onConnect:");
            Toast.makeText(MainActivity.this, "USB_DEVICE_CONNECT", Toast.LENGTH_SHORT).show();

            //synchronized (mSync) {
                if (!mHandlerFirst.isOpened()) {
                    mHandlerFirst.open(ctrlBlock);
                    final SurfaceTexture st = mUVCCameraViewFirst.getSurfaceTexture();
                    mHandlerFirst.startPreview(new Surface(st));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCaptureButtonFirst.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (!mHandlerSecond.isOpened()) {
                    mHandlerSecond.open(ctrlBlock);
                    final SurfaceTexture st = mUVCCameraViewSecond.getSurfaceTexture();
                    mHandlerSecond.startPreview(new Surface(st));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCaptureButtonSecond.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (!mHandlerThird.isOpened()) {
                    mHandlerThird.open(ctrlBlock);
                    final SurfaceTexture st = mUVCCameraViewThird.getSurfaceTexture();
                    mHandlerThird.startPreview(new Surface(st));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCaptureButtonThird.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (!mHandlerFourth.isOpened()) {
                    mHandlerFourth.open(ctrlBlock);
                    final SurfaceTexture st = mUVCCameraViewFourth.getSurfaceTexture();
                    mHandlerFourth.startPreview(new Surface(st));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCaptureButtonFourth.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (!mHandlerFifth.isOpened()) {
                    mHandlerFifth.open(ctrlBlock);
                    final SurfaceTexture st = mUVCCameraViewFifth.getSurfaceTexture();
                    mHandlerFifth.startPreview(new Surface(st));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCaptureButtonFifth.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (!mHandlerSixth.isOpened()) {
                    mHandlerSixth.open(ctrlBlock);
                    final SurfaceTexture st = mUVCCameraViewSixth.getSurfaceTexture();
                    mHandlerSixth.startPreview(new Surface(st));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCaptureButtonSixth.setVisibility(View.VISIBLE);
                        }
                    });
                }
            //}

        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.e(TAG, "onDisconnect:" );
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
            if ((mHandlerFirst != null) && !mHandlerFirst.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerFirst.close();
                        if (mFirstPreviewSurface != null) {
                            mFirstPreviewSurface.release();
                            mFirstPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            } else if ((mHandlerSecond != null) && !mHandlerSecond.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerSecond.close();
                        if (mSecondPreviewSurface != null) {
                            mSecondPreviewSurface.release();
                            mSecondPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            } else if ((mHandlerThird != null) && !mHandlerThird.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerThird.close();
                        if (mThirdPreviewSurface != null) {
                            mThirdPreviewSurface.release();
                            mThirdPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            } else if ((mHandlerFourth != null) && !mHandlerFourth.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerFourth.close();
                        if (mFourthPreviewSurface != null) {
                            mFourthPreviewSurface.release();
                            mFourthPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            } else if ((mHandlerFifth != null) && !mHandlerFifth.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerFifth.close();
                        if (mFifthPreviewSurface != null) {
                            mFifthPreviewSurface.release();
                            mFifthPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            } else if ((mHandlerSixth != null) && !mHandlerSixth.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerSixth.close();
                        if (mSixthPreviewSurface != null) {
                            mSixthPreviewSurface.release();
                            mSixthPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            }
        }

        @Override
        public void onDettach(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDettach:" + device);
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:");
        }
    };

    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setCameraButton();
                }
            }, 0);
        }
    }

    private void setCameraButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((mHandlerFirst != null) && !mHandlerFirst.isOpened() && (mCaptureButtonFirst != null)) {
                    mCaptureButtonFirst.setVisibility(View.INVISIBLE);
                }
                if ((mHandlerSecond != null) && !mHandlerSecond.isOpened() && (mCaptureButtonSecond != null)) {
                    mCaptureButtonSecond.setVisibility(View.INVISIBLE);
                }
                if ((mHandlerThird != null) && !mHandlerThird.isOpened() && (mCaptureButtonThird != null)) {
                    mCaptureButtonThird.setVisibility(View.INVISIBLE);
                }
                if ((mHandlerFourth != null) && !mHandlerFourth.isOpened() && (mCaptureButtonFourth != null)) {
                    mCaptureButtonFourth.setVisibility(View.INVISIBLE);
                }
                if ((mHandlerFifth != null) && !mHandlerFifth.isOpened() && (mCaptureButtonFifth != null)) {
                    mCaptureButtonFifth.setVisibility(View.INVISIBLE);
                }
                if ((mHandlerSixth != null) && !mHandlerSixth.isOpened() && (mCaptureButtonSixth != null)) {
                    mCaptureButtonSixth.setVisibility(View.INVISIBLE);
                }
            }
        }, 0);
    }
}
