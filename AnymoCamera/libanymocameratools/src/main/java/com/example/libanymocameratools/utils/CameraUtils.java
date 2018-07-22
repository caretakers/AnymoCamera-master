package com.example.libanymocameratools.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.libanymocameratools.interf.AnymoCameraListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anymo on 2018/7/12.
 */

public class CameraUtils implements AnymoCameraListener {

    private final static String TAG = "CameraUtils";
    private static CameraUtils cameraUtils;
    private SurfaceView surfaceView;
    private Camera camera;
    private boolean isPreviewing = false;
    private Camera.Parameters mParams;
    private float previewRate = 1.33f;
    /**
     * 当前缩放级别  默认为0
     */
    private int mZoom = 0;
    private static ThreadPoolManager threadPoolManager;
    private static FileUtil fileUtil;

    public static synchronized CameraUtils getInstance() {
        if (cameraUtils == null) {
            cameraUtils = new CameraUtils();
            threadPoolManager = ThreadPoolManager.getInstance();
            fileUtil = new FileUtil();
        }
        return cameraUtils;
    }

    public CameraUtils setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        return this;
    }

    private int cameraId = 0;

    @Override
    public CameraUtils openShot() {
        camera = Camera.open(cameraId);
        return this;
    }

    @Override
    public CameraUtils startPreview() throws IOException {
        //判断surfaceview
        if (surfaceView == null) {
            new Throwable("SurfaceView is null");
            return this;
        }
        //判断是否开启预览
        if (isPreviewing) {
            camera.stopPreview();
            return this;
        }
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (camera != null) {
            camera.setPreviewDisplay(surfaceHolder);
            initCamera();
        }
        return this;
    }

    private Bitmap b = null;

    public Bitmap getPreviewPicture() {
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                b = null;
                if (data != null) {
                    b = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图
                }
                Log.i(TAG, "onPreviewFrame: Preview image return");
            }
        });
        return b;
    }

    private boolean flag;
    private boolean isSwitchShot;

    public void switchShot() throws IOException {
        if (isSwitchShot) return;
        isSwitchShot = true;
        closeCamera();
        if (!flag) {
            cameraId = 1;
        } else {
            cameraId = 0;
        }
        openShot();
        startPreview();
        flag = !flag;
    }

    @Override
    public CameraUtils takePhoto(final FileUtil.OnTakePhotoSucessListener listener) {
        if (isPreviewing && (camera != null)) {
//            第一个，ShutterCallback接口，在拍摄瞬间瞬间被回调，通常用于播放“咔嚓”这样的音效；
//            第二个，PictureCallback接口，返回未经压缩的RAW类型照片；
//            第三个，PictureCallback接口，返回经过压缩的JPEG类型照片；
            threadPoolManager.execute(new Runnable() {
                @Override
                public void run() {
                    camera.takePicture(null, null, mRectJpegPictureCallback);
                    fileUtil.setOnTakePhotoSucessListener(listener);
                }
            });
        }
        return this;
    }

    /**
     * 拍摄指定区域的Rect,对jpeg图像数据的回调,最重要的一个回调
     */
    Camera.PictureCallback mRectJpegPictureCallback = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            Bitmap b = null;
            if (data != null) {
                b = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图
                camera.stopPreview();
                isPreviewing = false;
            }
            //保存图片到sdcard
            if (b != null) {
                //设置FOCUS_MODE_CONTINUOUS_VIDEO)之后，myParam.set("rotation", 90)失效。
                //图片竟然不能旋转了，故这里要旋转下
                Bitmap rotaBitmap = null;
                if (cameraId == 0) {
                    rotaBitmap = ImageUtil.getRotateBitmap(b, 90f);
                }else if (cameraId == 1){
                    rotaBitmap = ImageUtil.getRotateBitmap(b, -90f);
                }

                FileUtil.saveBitmap(rotaBitmap);
                if (rotaBitmap.isRecycled()) {
                    rotaBitmap.recycle();
                }
            }
            //再次进入预览
            camera.startPreview();
            isPreviewing = true;
            if (!b.isRecycled()) {
                b.recycle();
            }

        }
    };

    @Override
    public CameraUtils closeCamera() {
        if (null != camera) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            isPreviewing = false;
//            previewRate = -1f;
            camera.release();
            camera = null;
        }
        return this;
    }

    /**
     * 手动聚焦
     *
     * @param point 触屏坐标
     */
    @Override
    public Camera onFocus(Point point, Camera.AutoFocusCallback callback) {
        //一定要加
        camera.cancelAutoFocus();
        Camera.Parameters parameters = camera.getParameters();
        //不支持设置自定义聚焦，则使用自动聚焦，返回
        if (parameters.getMaxNumFocusAreas() <= 0) {
            camera.autoFocus(callback);
            new Throwable("not support manual focus");
            return camera;
        }
        List<Camera.Area> areas = new ArrayList<Camera.Area>();
        int left = point.x - 300;
        int top = point.y - 300;
        int right = point.x + 300;
        int bottom = point.y + 300;
        left = left < -1000 ? -1000 : left;
        top = top < -1000 ? -1000 : top;
        right = right > 1000 ? 1000 : right;
        bottom = bottom > 1000 ? 1000 : bottom;
        //一定要加
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        areas.add(new Camera.Area(new Rect(left, top, right, bottom), 100));
        parameters.setFocusAreas(areas);
        try {
            //本人使用的小米手机在设置聚焦区域的时候经常会出异常，看日志发现是框架层的字符串转int的时候出错了，
            //目测是小米修改了框架层代码导致，在此try掉，对实际聚焦效果没影响
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        camera.autoFocus(callback);
        return camera;
    }

    public enum PullState {
        FLASH_MODE_ON, FLASH_MODE_OFF, FLASH_MODE_AUTO, FLASH_MODE_RED_EYE, FLASH_MODE_TORCH
    }

    @Override
    public void setFlashMode(String flashMode) {
        if (camera == null) return;
        Camera.Parameters parameters = camera.getParameters();
        if (PullState.FLASH_MODE_ON.equals(flashMode)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        } else if (PullState.FLASH_MODE_OFF.equals(flashMode)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        } else if (PullState.FLASH_MODE_AUTO.equals(flashMode)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        } else if (PullState.FLASH_MODE_RED_EYE.equals(flashMode)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_RED_EYE);
        } else if (PullState.FLASH_MODE_TORCH.equals(flashMode)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
        camera.setParameters(parameters);
    }

    private void initCamera() {
        if (camera != null) {
            mParams = camera.getParameters();
            mParams.setPictureFormat(ImageFormat.JPEG);//设置拍照后存储的图片格式
            //设置PreviewSize和PictureSize
            Camera.Size pictureSize = CameraParamUtils.getInstance().getPropPictureSize(
                    mParams.getSupportedPictureSizes(), previewRate, 800);
            mParams.setPictureSize(pictureSize.width, pictureSize.height);
            Camera.Size previewSize = CameraParamUtils.getInstance().getPropPreviewSize(
                    mParams.getSupportedPreviewSizes(), previewRate, 800);
            mParams.setPreviewSize(previewSize.width, previewSize.height);
            //左横屏不用预览旋转,又横屏要旋转180度
            camera.setDisplayOrientation(90);
            List<String> focusModes = mParams.getSupportedFocusModes();
            Log.i(TAG, "initCamera: " + focusModes);
            if (focusModes.contains("continuous-picture")) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            camera.setParameters(mParams);
            camera.startPreview();//开启预览

            isPreviewing = true;
            isSwitchShot = false;
            mParams = camera.getParameters(); //重新get一次
            Log.i(TAG, "最终设置:PreviewSize--With = " + mParams.getPreviewSize().width
                    + "Height = " + mParams.getPreviewSize().height);
            Log.i(TAG, "最终设置:PictureSize--With = " + mParams.getPictureSize().width
                    + "Height = " + mParams.getPictureSize().height);
        }
    }

    /**
     * 获取最大缩放级别，最大为40
     */
    public int getMaxZoom() {
        if (camera == null) return -1;
        Camera.Parameters parameters = camera.getParameters();
        if (!parameters.isZoomSupported()) return -1;
        return parameters.getMaxZoom() > 40 ? 40 : parameters.getMaxZoom();
    }

    /**
     * 设置相机缩放级别
     */
    public void setZoom(int zoom) {
        Log.i(TAG, "setZoom: " + zoom);
        if (camera == null) return;
        Camera.Parameters parameters;
        //注意此处为录像模式下的setZoom方式。在Camera.unlock之后，调用getParameters方法会引起android框架底层的异常
        //stackoverflow上看到的解释是由于多线程同时访问Camera导致的冲突，所以在此使用录像前保存的mParameters。
        parameters = camera.getParameters();

        if (!parameters.isZoomSupported()) return;
        parameters.setZoom(zoom);
        camera.setParameters(parameters);
        mZoom = zoom;
    }

    public int getZoom() {
        return mZoom;
    }
}
