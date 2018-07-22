package com.example.libanymocameratools.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 对拍完照的图片的保存路径
 */
public class FileUtil {
    private static final String TAG = "FileUtil： ";
    //文件路径
    private static final File parentPath = Environment.getExternalStorageDirectory();
    private static String storagePath = "";
    //文件夹名称
    private static final String DST_FOLDER_NAME = "PlayCamera";
    //图片名称
    public static String jpegName;

    /**
     * 初始化保存路径
     *
     * @return
     */
    private static String initPath() {
        if (storagePath.equals("")) {
            storagePath = parentPath.getAbsolutePath() + "/" + DST_FOLDER_NAME;
            File f = new File(storagePath);
            if (!f.exists()) {
                f.mkdir();
            }
        }
        return storagePath;
    }

    /**
     * 保存Bitmap到sdcard
     *
     * @param b
     */
    public static void saveBitmap(Bitmap b) {
        String path = initPath();
        long dataTake = System.currentTimeMillis();
        jpegName = path + "/" + dataTake + ".jpg";
        Log.i(TAG , "saveBitmap:jpegName = " + jpegName);
        if (b != null) {
            try {
                FileOutputStream fout = new FileOutputStream(jpegName);
                BufferedOutputStream bos = new BufferedOutputStream(fout);
                b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
                Log.i(TAG , "saveBitmap成功");
                if (listener != null) {
                    listener.success();
                }
            } catch (IOException e) {
                Log.i(TAG , "saveBitmap:失败");
                e.printStackTrace();
            }
        }else {
            Log.i(TAG , "bitmap为null");
            if (listener != null) {
                listener.fail();
            }
        }
    }

    private static OnTakePhotoSucessListener listener;
    /**
     * 返回照片是否保存成功的结果的接口
     */
    public interface OnTakePhotoSucessListener {
        void success();
        void fail();
    }
    public void setOnTakePhotoSucessListener(OnTakePhotoSucessListener listener) {
        this.listener = listener;
    }
}
