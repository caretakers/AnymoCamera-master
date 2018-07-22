package com.example.libanymocameratools.interf;

import android.graphics.Point;
import android.hardware.Camera;

import com.example.libanymocameratools.utils.CameraUtils;
import com.example.libanymocameratools.utils.FileUtil;

import java.io.IOException;

/**
 * Created by Anymo on 2018/7/12.
 */

public interface AnymoCameraListener {

    CameraUtils openShot();

    CameraUtils startPreview() throws IOException;

    CameraUtils takePhoto(FileUtil.OnTakePhotoSucessListener listener);

    CameraUtils closeCamera();

    Camera onFocus(Point point, Camera.AutoFocusCallback callback);

    void setFlashMode(String flashMode);
}
