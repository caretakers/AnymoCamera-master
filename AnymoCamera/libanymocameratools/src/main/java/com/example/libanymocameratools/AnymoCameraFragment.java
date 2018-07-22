package com.example.libanymocameratools;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.libanymocameratools.utils.CameraUtils;
import com.example.libanymocameratools.utils.FileUtil;
import com.example.libanymocameratools.utils.ThreadPoolManager;

import java.io.IOException;

/**
 * Created by Anymo on 2018/7/12.
 */

public class AnymoCameraFragment extends Fragment implements View.OnTouchListener, View.OnClickListener {

    private final static String TAG = "AnymoCameraFragment";
    private Context context;
    private AnymoCameraSurfaceView sfv;
    private ImageView iv_take_photo;
    private ImageView iv_flip_lens;
    private Vibrator vibrator;
    private View view;
    private Point point;
    private float startDis;
    private final CameraUtils cameraUtils;
    private final ThreadPoolManager threadPoolManager;

    public AnymoCameraFragment() {
        cameraUtils = CameraUtils.getInstance();
        threadPoolManager = ThreadPoolManager.getInstance();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.anymo_camera_fragment, container, false);
        sfv = view.findViewById(R.id.acsv);
        iv_take_photo = view.findViewById(R.id.iv_take_photo);
        iv_flip_lens = view.findViewById(R.id.iv_flip_lens);
        iv_take_photo.setOnClickListener(this);
        iv_flip_lens.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        iv_take_photo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        iv_take_photo.setImageResource(R.mipmap.take_photo_press);
                        break;
                    case MotionEvent.ACTION_UP:
                        iv_take_photo.setImageResource(R.mipmap.take_photo);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        iv_flip_lens.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        iv_flip_lens.setImageResource(R.mipmap.flip_lens_press);
                        break;
                    case MotionEvent.ACTION_UP:
                        iv_flip_lens.setImageResource(R.mipmap.flip_lens);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        //设置ontouch,防止fragment穿透
        view.setOnTouchListener(this);
//        sfv.setSurfaceViewSize("4:3");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //震动
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        threadPoolManager.execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(50);
                    cameraUtils.setSurfaceView(sfv)
                            .openShot()
                            .startPreview();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraUtils.closeCamera();
    }


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.iv_take_photo) {
            cameraUtils.takePhoto(new FileUtil.OnTakePhotoSucessListener() {
                @Override
                public void success() {
                    Log.i(TAG, "success: ");
                }

                @Override
                public void fail() {
                    Log.i(TAG, "fail: ");
                }
            });

        } else if (i == R.id.iv_flip_lens) {
            try {
                cameraUtils.switchShot();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                startDis = distance(event);
                break;
            case MotionEvent.ACTION_MOVE:
                //只有同时触屏两个点的时候才执行
                if (event.getPointerCount() < 2) {
                    Log.i(TAG, "onTouch: MotionEvent.ACTION_MOVE");
                    return true;
                }
                float endDis = distance(event);// 结束距离
                //每变化10f zoom变1
                int scale = (int) ((endDis - startDis) / 10f);
                if (scale >= 1 || scale <= -1) {
                    int zoom = cameraUtils.getZoom() + scale;
                    //zoom不能超出范围
                    if (zoom > cameraUtils.getMaxZoom())
                        zoom = cameraUtils.getMaxZoom();
                    if (zoom < 0) zoom = 0;
                    cameraUtils.setZoom(zoom);
                    //将最后一次的距离设为当前距离
                    startDis = endDis;
                }
                break;
            case MotionEvent.ACTION_UP:
                int x = (int) event.getX();
                int y = (int) event.getY();
                point = new Point(x, y);
                cameraUtils.onFocus(point, new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                        if (b) {
                            vibrator.vibrate(100);
                        }
                    }
                });
                break;
            default:
                break;
        }
        return true;

    }


    /**
     * 计算两个手指间的距离
     */
    private float distance(MotionEvent event) {
        float sqrt = 0;
        try {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            /** 使用勾股定理返回两点之间的距离 */
            sqrt = (float) Math.sqrt(dx * dx + dy * dy);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
        return sqrt;
    }

}
