package com.github739c1ae2.wsapatch.hook;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XposedBridge;

@SuppressLint("ViewConstructor")
public class OverlayViewContainer extends FrameLayout {
    private final View mView;
    private final LayoutParams mViewParams;
    private WindowManager.LayoutParams mOriginalParams;
    private final WindowManager.LayoutParams mReplaceParams;
    /**
     * 是否在移动或调整大小
     * */
    private boolean mIsMovingOrResizing = false;
    /**
     * 是否正在被触摸
     * */
    private boolean mIsTouching = false;

    private int mOffsetX = 0, mOffsetY = 0;
    private int mLastX = 0, mLastY = 0;
    private final WindowManager mManager;

    public OverlayViewContainer(@NonNull Context context, View view, WindowManager manager, @NonNull WindowManager.LayoutParams params) {
        super(context);
        mView = view;
        mOriginalParams = params;
        mViewParams = new LayoutParams(params.width, params.height);
        mManager = manager;
        addView(mView, mViewParams);
        setVisibility(mView.getVisibility());
        mReplaceParams = new WindowManager.LayoutParams();
        mReplaceParams.copyFrom(params);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mEndMoveOrResizeRunnable = this::endMoveOrResize;

    public WindowManager.LayoutParams updateViewLayout(@NonNull final WindowManager.LayoutParams params) {
        XposedBridge.log("updateViewLayout: x: " + params.x + ", y: " + params.y + ", lastX: " + mLastX + ", lastY: " + mLastY + ", offsetX: " + mOffsetX + ", offsetY: " + mOffsetY);
        mOriginalParams = params;
        if (mIsMovingOrResizing) {
            mViewParams.leftMargin = mLastX  = params.x;
            mViewParams.topMargin = mLastY = params.y;
            mViewParams.width = params.width;
            mViewParams.height = params.height;
            mView.setLayoutParams(mViewParams);

            return mReplaceParams;
        }
        mReplaceParams.copyFrom(params);
        if (params.x != mLastX || params.y != mLastY) {
            beginMoveOrResize();
            // 已触摸的情况下会在停止触摸的时候自动结束移动或调整大小模式
            // 未触摸时需要手动结束移动或调整大小模式
            if (!mIsTouching) {
                mHandler.removeCallbacks(mEndMoveOrResizeRunnable);
                mHandler.postDelayed(mEndMoveOrResizeRunnable, 800);
            }
        }
        mViewParams.width = params.width;
        mViewParams.height = params.height;
        mView.setLayoutParams(mViewParams);

        return mReplaceParams;
    }

    private void beginMoveOrResize() {
        mIsMovingOrResizing = true;
        mOffsetX = -mLastX;
        mOffsetY = -mLastY;
        // 将容器的位置和大小应用到子视图
        mViewParams.leftMargin = mLastX = mOriginalParams.x;
        mViewParams.topMargin = mLastY = mOriginalParams.y;
        mReplaceParams.x = 0;
        mReplaceParams.y = 0;
        mReplaceParams.width = LayoutParams.WRAP_CONTENT;
        mReplaceParams.height = LayoutParams.WRAP_CONTENT;
    }

    /**
     * 结束移动或者调整大小
     * */
    private void endMoveOrResize() {
        mIsMovingOrResizing = false;

        // 将子视图的位置和大小应用到容器
        mReplaceParams.copyFrom(mOriginalParams);

        // 恢复子视图
        mViewParams.width = mOriginalParams.width;
        mViewParams.height = mOriginalParams.height;
        mViewParams.leftMargin = 0;
        mViewParams.topMargin = 0;
        mView.setLayoutParams(mViewParams);

        mOffsetX = mOffsetY = 0;

        mManager.updateViewLayout(this, mReplaceParams);
        setVisibility(INVISIBLE);
        post(() -> setVisibility(VISIBLE));
    }

    /**
     * 销毁
     * */
    public void destroy() {
        removeView(mView);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mIsTouching = true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            mIsTouching = false;
        }

        int pointerCount = ev.getPointerCount();
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[pointerCount];

        for (int i = 0; i < pointerCount; i++) {
            pointerProperties[i] = new MotionEvent.PointerProperties();
            pointerCoords[i] = new MotionEvent.PointerCoords();
            ev.getPointerProperties(i, pointerProperties[i]);
            ev.getPointerCoords(i, pointerCoords[i]);

            pointerCoords[i].x = ev.getRawX(i) + mOffsetX;
            pointerCoords[i].y = ev.getRawY(i) + mOffsetY;
        }

        MotionEvent newEvent = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(),
                ev.getAction(), ev.getPointerCount(),
                pointerProperties,
                pointerCoords,
                ev.getMetaState(), ev.getButtonState(), ev.getXPrecision(),
                ev.getYPrecision(), ev.getDeviceId(), ev.getEdgeFlags(),
                ev.getSource(), ev.getFlags());

        newEvent.offsetLocation(ev.getX() - ev.getRawX(), ev.getY() - ev.getRawY());
        mView.dispatchTouchEvent(newEvent);
        newEvent.recycle();

        if (newEvent.getAction() == MotionEvent.ACTION_UP && mIsMovingOrResizing) {
            endMoveOrResize();
        }
        return true;
    }

}
