package com.missmess.emotionkeyboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;

/**
 * 实现类似微信聊天界面的表情窗口控制，窗口跟软键盘同高，无缝切换效果。
 * <p>
 * 通过设置activity的根布局（setContentView的那个布局）为固定高度，即使键盘切换，decorView的高度变化
 * 了，但是根布局高度不变，只不过显示在键盘下面了。所以解决了原先键盘和表情布局切换需要不停设置visibility
 * 导致的一些糟糕的体验和bug。
 *
 * <p>
 * <b>注意如果你的activity根布局使用了fitSystemWindow
 * 属性，请调用{@link Builder#rootView(ViewGroup)}方法重新指定一个作为root。不然会出很严重的
 * 错误</b>
 *
 *
 * @author wl
 * @since 2017/12/08 13:57
 */
public class EmojiconKeyBoard implements KeyboardInfo.OnSoftKeyboardChangeListener {
    private Activity mActivity;
    private InputMethodManager mInputManager;//软键盘管理类
    private final KeyboardInfo mKeyboardInfo;
    private View mContentView;//内容布局view,即除了表情布局或者软键盘布局以外的布局，用于固定bar的高度，防止跳闪
    private EditText mEditText;
    private ArrayList<View> mEmotionLayouts;
    private int showingEmotionIndex = -1;
    private boolean mTouchContentHideAll = false;
    private TheContentViewToucher mContentToucher;
    private OnEmotionLayoutStateChangeListener mEmotionLayoutListener;
    private KeyboardInfo.OnSoftKeyboardChangeListener mKeyboardListener;
    private View mActivityRootView;
    private int mRootViewHeight;
    private View mStuffView;
    /** 键盘弹出收起时的过渡view引用 */
    private View mTransitView;

    EmojiconKeyBoard(Activity activity) {
        mActivity = activity;
        mInputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        mKeyboardInfo = KeyboardInfo.from(activity);
        mEmotionLayouts = new ArrayList<>();

        // 开始监听键盘变化
        mKeyboardInfo.startListening();
        mKeyboardInfo.setOnKeyboardChangeListener(this);
    }

    private void setOnKeyboardChangeListener(KeyboardInfo.OnSoftKeyboardChangeListener listener) {
        mKeyboardListener = listener;
    }

    private void setOnEmotionLayoutChangeListener(OnEmotionLayoutStateChangeListener listener) {
        mEmotionLayoutListener = listener;
    }

    private void bindContentView(View contentView) {
        mContentView = contentView;
    }

    private void bindEditText(EditText editText, View.OnTouchListener listener) {
        mEditText = editText;
        editText.requestFocus();
    }

    private void touchContentViewHideAllEnabled(View.OnTouchListener listener) {
        mTouchContentHideAll = true;
        mContentToucher = new TheContentViewToucher(listener);
    }

    private void addEmotionBtnAndLayout(View emotionBtn, View emotionLayout, View.OnClickListener listener) {
        if (mStuffView == null) {
            mStuffView = new View(mActivity);
            // 颜色透明，实际显示时取决于parent的颜色
            mStuffView.setBackgroundColor(Color.TRANSPARENT);
            ViewGroup parent = (ViewGroup) emotionLayout.getParent();
            parent.addView(mStuffView, ViewGroup.LayoutParams.MATCH_PARENT, 0);
            mStuffView.setVisibility(View.GONE);
        }

        mEmotionLayouts.add(emotionLayout);
        int index = mEmotionLayouts.size() - 1;
        emotionBtn.setOnClickListener(new TheEmotionClicker(index, listener));
    }

    private void setRootView(View rootView) {
        rootView.setFitsSystemWindows(false);
        mActivityRootView = rootView;
    }

    /**
     * 完成配置
     */
    private void setup(){
        if (mContentView == null) {
            throw new IllegalStateException("No content view bound now, call bindContentView first");
        }

        //设置软键盘的模式：SOFT_INPUT_ADJUST_RESIZE  这个属性表示Activity的主窗口总是会被调整大小，从而保证软键盘显示空间。
        //从而方便我们计算软键盘的高度
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        //隐藏软键盘
        hideSoftKeyboard();

        if (mActivityRootView == null) {
            setRootView(((ViewGroup) mActivity.findViewById(android.R.id.content)).getChildAt(0));
        }

        mActivityRootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int oldHeight = mRootViewHeight;
                int newHeight = bottom - top;
                // 设置rootView高度固定
                if (newHeight != oldHeight) {
                    mRootViewHeight = newHeight;
                    ViewGroup.LayoutParams lps = mActivityRootView.getLayoutParams();
                    lps.height = mRootViewHeight;
                    mActivityRootView.setLayoutParams(lps);
                }
            }
        });

        if (mTouchContentHideAll) {
            mContentView.setFocusable(true);
            mContentView.setFocusableInTouchMode(true);
            mContentView.setOnTouchListener(mContentToucher);
        }
    }

    /**
     * 获得正在显示的表情布局index。
     * @return index等于 {@link #addEmotionBtnAndLayout(View, View, View.OnClickListener)} 方法调用的
     * 顺序，从0开始。-1代表没有正在显示的表情布局
     */
    public int getShowingEmotionIndex() {
        return showingEmotionIndex;
    }

    /**
     * 是否有正在显示的表情布局
     * @return true - 有, false - 没有
     */
    public boolean isEmotionLayoutShowing() {
        return showingEmotionIndex != -1;
    }

    /**
     * 显示表情布局, index为添加的顺序
     * @param index index
     */
    public void showEmotionLayout(int index) {
        if (mTransitView != null) {
            mTransitView.setVisibility(View.GONE);
            mTransitView = null;
        }
        hideSoftKeyboard();

        showEmotionLayoutInternal(index);
    }

    private void showEmotionLayoutInternal(int index) {
        int oldIndex = showingEmotionIndex;
        if (index != oldIndex) {
            int softKeyboardHeight = mKeyboardInfo.getSoftKeyboardHeight();
            // 隐藏旧的
            if (oldIndex != -1) {
                View oldLayout = mEmotionLayouts.get(oldIndex);
                oldLayout.setVisibility(View.GONE);
            }
            // 显示新的
            View emotionLayout = mEmotionLayouts.get(index);
            emotionLayout.getLayoutParams().height = softKeyboardHeight;
            emotionLayout.setVisibility(View.VISIBLE);

            if (mEmotionLayoutListener != null) {
                mEmotionLayoutListener.onEmotionLayoutShow(emotionLayout, index, oldIndex);
            }
        }

        showingEmotionIndex = index;
    }

    /**
     * 隐藏全部表情布局
     */
    public void hideEmotionLayout() {
        int oldIndex = showingEmotionIndex;
        if (oldIndex != -1) {
            View emotionLayout = mEmotionLayouts.get(oldIndex);
            emotionLayout.setVisibility(View.GONE);

            if (mEmotionLayoutListener != null) {
                mEmotionLayoutListener.onEmotionLayoutHide(oldIndex);
            }
        }

        showingEmotionIndex = -1;
    }

    public boolean isSoftKeyboardShowing() {
        return mKeyboardInfo.isKeyboardShowing();
    }

    /**
     * 编辑框获取焦点，并显示软键盘
     */
    public void showSoftKeyboard() {
        if (!isSoftKeyboardShowing()) {
            mEditText.requestFocus();
            mInputManager.showSoftInput(mEditText, 0);
        }
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftKeyboard() {
        if (isSoftKeyboardShowing()) {
            mInputManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }
    }

    /**
     * 是否拦截返回事件，要在activity的 {@link Activity#onBackPressed()} 中调用。
     * @return true，拦截
     */
    public boolean interceptBackPress() {
        if (isEmotionLayoutShowing()) {
            hideEmotionLayout();
            // 拦截事件
            return true;
        }
        // 不拦截，关闭界面
        mKeyboardInfo.stopListening(); // 不再监听键盘变化
        return false;
    }

    @Override
    public void onSoftKeyboardStateChanged(boolean shown, int height) {
        if (shown) {
            // 打开键盘时：如果有表情键盘显示，为了平滑过渡，不做隐藏处理；如果没有表情键盘显示，则
            // 显示第一个表情键盘作为位置填充
            if (showingEmotionIndex != -1) {
                int oldIndex = showingEmotionIndex;
                mTransitView = mEmotionLayouts.get(showingEmotionIndex);
                // 为了平滑过渡，仅重置这个值
                showingEmotionIndex = -1;
                if (mEmotionLayoutListener != null) {
                    mEmotionLayoutListener.onEmotionLayoutHide(oldIndex);
                }
            } else {
                // 显示键盘高度位置的填充布局
                int softKeyboardHeight = mKeyboardInfo.getSoftKeyboardHeight();
                View stuff = mStuffView;
                stuff.getLayoutParams().height = softKeyboardHeight;
                stuff.setVisibility(View.VISIBLE);
                mTransitView = stuff;
            }
        } else {
            // 关闭键盘时，隐藏填充位置
            if (mTransitView != null) {
                mTransitView.setVisibility(View.GONE);
                mTransitView = null;
            }
        }

        if (mKeyboardListener != null)
            mKeyboardListener.onSoftKeyboardStateChanged(shown, height);
    }

    // 给表情按钮添加的OnClickListener
    private class TheEmotionClicker implements View.OnClickListener {
        private View.OnClickListener other;
        private int position;

        TheEmotionClicker(int position, View.OnClickListener other) {
            this.other = other;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            if (isEmotionLayoutShowing()) { //有表情布局显示着
                if (position == getShowingEmotionIndex()) { //显示的是当前的
                    showSoftKeyboard();
                } else { //显示的是其它的
                    showEmotionLayout(position);
                }
            } else { //未显示表情布局
                if (isSoftKeyboardShowing()) { //显示着键盘，隐藏键盘显示当前表情布局
                    showEmotionLayout(position); //显示当前表情布局，隐藏键盘
                } else { //什么都没显示
                    showEmotionLayout(position); //显示当前布局
                }
            }

            if (other != null)
                other.onClick(v);
        }
    }

    private class TheContentViewToucher implements View.OnTouchListener {
        private View.OnTouchListener other;

        TheContentViewToucher(View.OnTouchListener other) {
            this.other = other;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // 内容区域获取焦点
            mContentView.requestFocus();
            // 隐藏软键盘
            hideSoftKeyboard();
            // 隐藏表情布局
            hideEmotionLayout();

            return other != null && other.onTouch(v, event);
        }
    }


    /**
     * 表情布局状态改变监听器
     */
    public interface OnEmotionLayoutStateChangeListener {
        /**
         * 表情布局显示出来，或者表情布局切换了
         * @param newEmotionLayout 显示的表情布局
         * @param newEmotionLayoutIndex 显示的表情布局index
         * @param oldEmotionLayoutIndex 之前显示的表情布局index，如果之前没有显示为-1
         */
        void onEmotionLayoutShow(View newEmotionLayout, int newEmotionLayoutIndex, int oldEmotionLayoutIndex);

        /**
         * 表情布局隐藏了
         * @param oldEmotionLayoutIndex 隐藏的表情布局Index
         */
        void onEmotionLayoutHide(int oldEmotionLayoutIndex);
    }

    public static class Builder {
        private EmojiconKeyBoard impl;

        public Builder(Activity activity) {
            impl = new EmojiconKeyBoard(activity);
        }

        /**
         * 键盘状态监听
         * @param listener OnSoftKeyboardChangeListener
         * @return link call
         */
        public Builder keyboardStateCallback(KeyboardInfo.OnSoftKeyboardChangeListener listener) {
            impl.setOnKeyboardChangeListener(listener);
            return this;
        }

        /**
         * 表情布局状态监听
         * @param listener OnEmotionLayoutStateChangeListener
         * @return link call
         */
        public Builder emotionPanelStateCallback(OnEmotionLayoutStateChangeListener listener) {
            impl.setOnEmotionLayoutChangeListener(listener);
            return this;
        }

        /**
         * 如果你的activity根布局使用了{@link View#setFitsSystemWindows(boolean)}属性（通常一些改
         * 变状态栏颜色的工具类，会使用setFitsSystemWindows方法），使用这个方法指定你的根布局为另一
         * 个没有fitsSystemWindows属性的ViewGroup。
         * @param root 必须是聊天布局的parent ViewGroup
         * @return link call
         */
        public Builder rootView(ViewGroup root) {
            impl.setRootView(root);
            return this;
        }

        /**
         * 绑定内容view，即你的聊天内容主界面布局。
         * @param contentView contentView
         * @return link call
         */
        public Builder contentLayout(View contentView) {
            impl.bindContentView(contentView);
            return this;
        }

        /**
         * 绑定编辑框，由于会给EditText设置touch listener，所以如果要监听touch事件，调
         * {@link #bindEditText(EditText, View.OnTouchListener)}
         * @param editText editText
         * @return link call
         */
        public Builder editText(EditText editText) {
            return editText(editText, null);
        }

        /**
         * 绑定编辑框，由于会给EditText设置touch listener，所以如果要监听touch事件，在参数中传入
         * @param editText editText
         * @param listener 自己添加的touch listener，没有可传null
         * @return link call
         */
        public Builder editText(EditText editText, View.OnTouchListener listener) {
            impl.bindEditText(editText, listener);
            return this;
        }

        /**
         * 是否触摸聊天内容区域时，获取焦点，并隐藏键盘和表情布局。
         * @param listener 要给contentView设置touch监听，如果自己有需求监听touch事件，在这里传入，否则传null
         * @return link call
         */
        public Builder touchContentViewHideAllEnabled(View.OnTouchListener listener) {
            impl.touchContentViewHideAllEnabled(listener);
            return this;
        }

        /**
         * 添加表情按钮和对应的按钮弹出布局
         * @param emotionBtn emotionBtn
         * @param emotionLayout emotionLayout
         * @return link call
         */
        public Builder addEmotionBtnAndLayout(View emotionBtn, View emotionLayout) {
            return addEmotionBtnAndLayout(emotionBtn, emotionLayout, null);
        }

        /**
         * 添加表情按钮和对应的按钮弹出布局，由于会给emotionBtn设置点击事件，所以如果要自己添
         * 加OnClickListener，请在参数中传入
         * @param emotionBtn emotionBtn
         * @param emotionLayout emotionLayout
         * @param listener 自己需要添加的OnClickListener，没有可传null
         * @return link call
         */
        public Builder addEmotionBtnAndLayout(View emotionBtn, View emotionLayout, View.OnClickListener listener) {
            impl.addEmotionBtnAndLayout(emotionBtn, emotionLayout, listener);
            return this;
        }

        /**
         * 创建 {@link EmojiconKeyBoard}。
         * @return EmotionKeyboard
         */
        public EmojiconKeyBoard build() {
            impl.setup();
            return impl;
        }
    }
}
