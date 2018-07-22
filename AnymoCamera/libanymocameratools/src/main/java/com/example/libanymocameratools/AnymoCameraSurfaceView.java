package com.example.libanymocameratools;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.example.libanymocameratools.utils.DisplayUtil;

/**
 * Created by Anymo on 2018/7/12.
 */

public class AnymoCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "AnymoCameraSurfaceView";
    Context mContext;
    SurfaceHolder mSurfaceHolder;

    public AnymoCameraSurfaceView(Context context) {
        this(context, null);
    }

    public AnymoCameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnymoCameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        //View的onDraw函数是protected的。
//        protected 表明被它修饰的成员变量为保护类型，在同一个包里和 public 类型是一样的，也是能够访问到的。但是如果在不同包里的 protected 类型的成员变量就只能通过子类来访问，这个修饰符是区别于其他的修饰符的。
//        所以SurfaceView可以访问。而SurfaceView里并没有重写onDraw，
//        所以View子类的子类可以重写onDraw，但不能访问，所以像你说的：
//
//        即使写了onDraw函数，也不会自动调用，需要自己调用。
//        setWillNotDraw(false);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);//translucent半透明 transparent透明
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceHolder;
    }

    int widthScreen, heightScreen;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i(TAG, "onSizeChanged: w: " + w + " h: " + h);
        setSurfaceViewSize("4:3");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, "onMeasure: ");
        Point p = DisplayUtil.getScreenMetrics(mContext);
        widthScreen = p.x;
        heightScreen = p.y;
        int v = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getStatusBarHeight(), getResources().getDisplayMetrics());
        setMeasuredDimension(widthScreen + v, heightScreen );

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(TAG, "onDraw: ");
        setSurfaceViewSize("4:3");
        invalidate();
    }

    private int getStatusBarHeight() {
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen","android");
        int height = resources.getDimensionPixelSize(resourceId);
        Log.v("dbw", "Status height:" + height);
        return height;
    }

    /**
     * 根据分辨率设置预览SurfaceView的大小以防止变形
     *
     */
    public void setSurfaceViewSize(String surfaceSize) {
        Log.i(TAG, "setSurfaceViewSize: " + surfaceSize);
        ViewGroup.LayoutParams params = this.getLayoutParams();
        if (surfaceSize.equals("16:9")) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else if (surfaceSize.equals("4:3")) {
            params.height = 4 * widthScreen / 3;
            Log.i(TAG, "setSurfaceViewSize: height: " + params.height);
        }
        this.setLayoutParams(params);
    }
}
