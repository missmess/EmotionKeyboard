package com.missmess.emotionkeyboard;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

/**
 * 获取键盘高度和状态信息的类。
 *
 * @author wl
 * @since 2017/11/23 10:50
 */
public class KeyboardInfo {
    private static final String SHARE_PREFERENCE_NAME = "EmotionKeyboard";
    private static final String SHARE_PREFERENCE_SOFT_INPUT_HEIGHT = "soft_input_height";
    private static final int DEFAULT_SOFT_KEYBOARD_HEIGHT = 787;
    private final TheGlobalLayoutListener mGlobalLayoutListener;

    private SharedPreferences mSp;
    private OnSoftKeyboardChangeListener mListener;
    private Activity mActivity;
    private View mDecorView;
    private boolean isKeyboardShowing;// 键盘是否正在显示，只有isListening=true才有效。
    private boolean isListening;
    private int mSoftKeyboardHeight = 0;

    private KeyboardInfo(Activity activity) {
        mActivity = activity;
        mDecorView = activity.getWindow().getDecorView();
        mSp = activity.getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        mGlobalLayoutListener = new TheGlobalLayoutListener();
    }

    public static KeyboardInfo from(Activity activity) {
        return new KeyboardInfo(activity);
    }

    /**
     * 设置软键盘状态改变监听器
     * @param listener OnSoftKeyboardChangeListener
     */
    public void setOnKeyboardChangeListener(OnSoftKeyboardChangeListener listener) {
        this.mListener = listener;
    }

    /**
     * 开始监听软键盘状态的变化。通常在activity的onResume方法中调用
     */
    public void startListening() {
        if (isListening)
            return;

        isListening = true;
        mDecorView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
    }

    /**
     * 停止监听软键盘状态的变化。通常在activity的onPause方法中调用
     */
    public void stopListening() {
        if (!isListening)
            return;

        isListening = false;
        mDecorView.getViewTreeObserver().removeGlobalOnLayoutListener(mGlobalLayoutListener);
    }

    /**
     * 获取键盘高度。分为三步：
     *
     * <ol>
     *     <li>如果调用了 {@link #startListening()} 后并且打开过键盘，就总能获取到最后一次打开的
     *     键盘的高度，并返回。没有调用 {@link #startListening()} 或者没打开过键盘，看第2步。</li>
     *     <li>如果当前键盘正打开着，将会直接获取到该键盘高度。如果没有打开键盘，看第3步</li>
     *     <li>直接获取缓存中的键盘高度值，这个缓存数值是最后一次成功获取到的键盘高度。如果还是没
     *     有值，则会取默认值 {@link #DEFAULT_SOFT_KEYBOARD_HEIGHT}。</li>
     * </ol>
     * @return 键盘高度
     */
    public int getSoftKeyboardHeight() {
        if (mSoftKeyboardHeight > 0) {
            return mSoftKeyboardHeight;
        }

        int keyboardHeight = getSoftInputHeightInternal();
        if (keyboardHeight > 0) {
            return keyboardHeight;
        }

        return getCachedKeyboardHeight();
    }

    /**
     * 键盘是否正在显示。
     * @return
     */
    public boolean isKeyboardShowing() {
        if (isListening) {
            return isKeyboardShowing;
        } else {
            return getSoftInputHeightInternal() != 0;
        }
    }

    private void globalLayoutChanged() {
        if (!checkSoftInputModeAvailable())
            return;

        int softInputHeight = getSoftInputHeightInternal();
        if (softInputHeight > 0) { //取到了键盘高度
            if (!isKeyboardShowing) {
                mSoftKeyboardHeight = softInputHeight;
                isKeyboardShowing = true;

                if (mListener != null) {
                    mListener.onSoftKeyboardStateChanged(true, softInputHeight);
                }
            }
        } else {
            if (isKeyboardShowing) {
                isKeyboardShowing = false;

                if (mListener != null) {
                    mListener.onSoftKeyboardStateChanged(false, 0);
                }
            }
        }
    }

    private boolean checkSoftInputModeAvailable() {
        int softInputMode = mActivity.getWindow().getAttributes().softInputMode;
        if ((softInputMode & WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING) {
            // 当前模式activity为adjustNothing模式
            Log.w("KeyboardInfo", "softInputMode of this activity contains adjustNothing, can not " +
                    "obtain height of soft-keyboard");
            return false;
        }
        return true;
    }

    /**
     * 尝试直接获取软键盘的高度，如果不大于0代表当前软键盘没有打开或者未取到。大于0则取到了正确
     * 的键盘高度。
     * @return >0 或者 == 0
     */
    private int getSoftInputHeightInternal() {
        Rect r = new Rect();
        /*
         * decorView是window中的最顶层view，可以从window中通过getDecorView获取到decorView。
         * 通过decorView获取到程序显示的区域，包括标题栏，但不包括状态栏。
         */
        mDecorView.getWindowVisibleDisplayFrame(r);
        // 获取屏幕的高度
        int screenHeight = mDecorView.getRootView().getHeight();
        // 计算软键盘的高度
        int softInputHeight = screenHeight - r.bottom;

        /*
         * 某些Android版本下，没有显示软键盘时减出来的高度总是144，而不是零，
         * 这是因为高度是包括了虚拟按键栏的(例如华为系列)，所以在API Level高于20时，
         * 我们需要减去底部虚拟按键栏的高度（如果有的话）
         */
        if (Build.VERSION.SDK_INT >= 20) {
            // When SDK Level >= 20 (Android L), the softInputHeight will contain the height of softButtonsBar (if has)
            softInputHeight = softInputHeight - getSoftButtonsBarHeight();
        }

        if (softInputHeight < 0) {
            softInputHeight = 0;
            Log.w("KeyboardInfo", "Warning: value of softInputHeight is below zero!");
        }

        if (softInputHeight > 0) {
            // 缓存一下
            saveKeyboardHeightCache(softInputHeight);
        }
        return softInputHeight;
    }

    /**
     * 底部虚拟导航按键栏的高度
     * @return int
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getSoftButtonsBarHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        //这个方法获取可能不是真实屏幕的高度
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        //获取当前屏幕的真实高度
        mActivity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }

    private void saveKeyboardHeightCache(int softInputHeight) {
        mSp.edit().putInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, softInputHeight).apply();
    }

    /**
     * 获取软键盘高度，由于第一次直接弹出表情时会出现小问题，787是一个均值，作为临时解决方案
     * @return int
     */
    private int getCachedKeyboardHeight(){
        return mSp.getInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, DEFAULT_SOFT_KEYBOARD_HEIGHT);
    }

    private class TheGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {
            globalLayoutChanged();
        }
    }

    /**
     * 软键盘状态改变的监听器，只在activity的softInputMode不是adjustNothing模式的时候可用。
     */
    public interface OnSoftKeyboardChangeListener {
        /**
         * 键盘状态改变时回调
         * @param shown true - 键盘展示出来了，false - 键盘收起了
         * @param height shown为true时，表示刚展示出来的键盘高度；shown为false时，为0
         */
        void onSoftKeyboardStateChanged(boolean shown, int height);
    }
}
