package com.missmess.emotionkeyboard;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * 类似微信聊天界面的表情窗口控制，窗口跟软键盘同高，无缝切换效果。
 * 核心代码参考的是别人的开源项目。不过增强了扩展性：
 * <ul>
 *     <li>新增 {@link KeyboardInfo} 类，封装并能很方便获取软键盘相关信息和状态。</li>
 *     <li>可以添加多个表情布局并进行切换。</li>
 *     <li>默认实现了聊天内容布局在LinearLayout中锁定的处理，如果你的聊天内容布局不在
 *     LinearLayout中，则可以自己实现 {@link ContentViewLocker} 来处理。</li>
 *     <li>提供 {@link #touchContentViewHideAllEnabled(View.OnTouchListener)} 方法，调用后
 *     触摸聊天内容区域可获取焦点，收起键盘。</li>
 *     <li>...</li>
 * </ul>
 * 也修复了几个bug：
 * <ul>
 *     <li>第一次进入app，点击表情按钮，再点击输入框弹出键盘，此时表情工具条没有顶在键盘
 *     上（没有requestLayout导致）</li>
 *     <li>第一次进入app，点击表情按钮，点击输入框弹出键盘，收起键盘，再点击表情按钮，表情布局高
 *     度还是不正确（因为源码是只有在点击表情按钮收键盘才会记录键盘高度）</li>
 *     <li>...</li>
 * </ul>
 * 做了一些优化：
 * <ul>
 * <li>开放了一些常用api</li>
 * <li>使用Build模式来配置</li>
 * <li>...</li>
 * </ul>
 *
 * @author wl
 * @since 2017/11/23 10:29
 * @see <a href ="https://github.com/dss886/Android-EmotionInputDetector">参考的是dss886的开源项目</a>
 */
public class EmotionKeyboard implements KeyboardInfo.OnSoftKeyboardChangeListener {
    private Activity mActivity;
    private InputMethodManager mInputManager;//软键盘管理类
    private final KeyboardInfo mKeyboardInfo;
    private View mContentView;//内容布局view,即除了表情布局或者软键盘布局以外的布局，用于固定bar的高度，防止跳闪
    private ContentViewLocker mLocker;
    private EditText mEditText;
    private ArrayList<View> mEmotionLayouts;
    private int showingEmotionIndex = -1;
    private boolean mTouchContentHideAll = false;
    private TheContentViewToucher mContentToucher;
    private OnEmotionLayoutStateChangeListener mEmotionLayoutListener;
    private KeyboardInfo.OnSoftKeyboardChangeListener mKeyboardListener;

    EmotionKeyboard(Activity activity) {
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

    private void bindContentView(View contentView, ContentViewLocker locker) {
        mContentView = contentView;
        mLocker = locker;
    }

    private void bindEditText(EditText editText, View.OnTouchListener listener) {
        mEditText = editText;
        editText.requestFocus();
        editText.setOnTouchListener(new TheEditTextToucher(listener));
    }

    private void touchContentViewHideAllEnabled(View.OnTouchListener listener) {
        mTouchContentHideAll = true;
        mContentToucher = new TheContentViewToucher(listener);
    }

    private void addEmotionBtnAndLayout(View emotionBtn, View emotionLayout, View.OnClickListener listener) {
        mEmotionLayouts.add(emotionLayout);
        int index = mEmotionLayouts.size() - 1;
        emotionBtn.setOnClickListener(new TheEmotionClicker(index, listener));
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

        if (mTouchContentHideAll) {
            mContentView.setFocusable(true);
            mContentView.setFocusableInTouchMode(true);
            mContentView.setOnTouchListener(mContentToucher);
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

    private void lockContentHeight() {
        mLocker.lockContentHeight(mContentView);
    }

    private void unlockContentHeightDelayed() {
        mContentView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLocker.unlockContentHeight(mContentView);
            }
        }, 200L);
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
        if (showingEmotionIndex != index) {
            int oldIndex = showingEmotionIndex;
            int softKeyboardHeight = mKeyboardInfo.getSoftKeyboardHeight();
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
        if (isEmotionLayoutShowing()) {
            int oldIndex = showingEmotionIndex;
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
        mEditText.requestFocus();
        mEditText.post(new Runnable() {
            @Override
            public void run() {
                mInputManager.showSoftInput(mEditText, 0);
            }
        });
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftKeyboard() {
        mInputManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    @Override
    public void onSoftKeyboardStateChanged(boolean shown, int height) {
        if (shown) {
            // 确保键盘弹出时，一定隐藏了表情布局
            hideEmotionLayout();
        }
        if (mKeyboardListener != null)
            mKeyboardListener.onSoftKeyboardStateChanged(shown, height);
    }

    public static class Builder {
        private EmotionKeyboard impl;

        public Builder(Activity activity) {
            impl = new EmotionKeyboard(activity);
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
         * 绑定内容view，即你的聊天内容主界面，使用默认的线性布局locker
         * @param contentView contentView
         * @return link call
         */
        public Builder contentLayout(View contentView) {
            return contentLayout(contentView, new LinearLayoutLocker());
        }

        /**
         * 绑定内容view，即你的聊天内容主界面
         * @param contentView contentView
         * @param locker 定义如何固定contentView的高度
         * @return link call
         */
        public Builder contentLayout(View contentView, ContentViewLocker locker) {
            impl.bindContentView(contentView, locker);
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
         * 创建 {@link EmotionKeyboard}。
         * @return EmotionKeyboard
         */
        public EmotionKeyboard build() {
            impl.setup();
            return impl;
        }
    }

    // 给表情按钮添加的OnClickListener
    private class TheEmotionClicker implements View.OnClickListener {
        private View.OnClickListener other;
        private int position;

        public TheEmotionClicker(int position, View.OnClickListener other) {
            this.other = other;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            if (isEmotionLayoutShowing()) { //有表情布局显示着
                if (position == getShowingEmotionIndex()) { //显示的是当前的
                    lockContentHeight();//显示软键盘时，锁定内容高度，防止跳闪。
                    hideEmotionLayout();//隐藏表情布局，显示软键盘
                    showSoftKeyboard();
                    unlockContentHeightDelayed();//软键盘显示后，释放内容高度
                } else { //显示的是其它的
                    hideEmotionLayout(); //隐藏其它的，显示当前的
                    showEmotionLayout(position);
                }
            } else { //未显示表情布局
                if (isSoftKeyboardShowing()) { //显示着键盘，隐藏键盘显示当前表情布局
                    lockContentHeight();
                    showEmotionLayout(position); //显示当前表情布局，隐藏键盘
                    hideSoftKeyboard();
                    unlockContentHeightDelayed();
                } else { //什么都没显示
                    showEmotionLayout(position); //显示当前布局
                }
            }

            if (other != null)
                other.onClick(v);
        }
    }

    // 给输入框添加的OnTouchListener
    private class TheEditTextToucher implements View.OnTouchListener {
        private View.OnTouchListener other;

        public TheEditTextToucher(View.OnTouchListener other) {
            this.other = other;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (isEmotionLayoutShowing()) {
                    lockContentHeight();//显示软键盘时，锁定内容高度，防止跳闪。
                    hideEmotionLayout();//隐藏表情布局，显示软键盘
                    showSoftKeyboard();
                    //软键盘显示后，释放内容高度
                    unlockContentHeightDelayed();
                }
            }
            return other != null && other.onTouch(v, event);
        }
    }
    
    private class TheContentViewToucher implements View.OnTouchListener {
        private View.OnTouchListener other;

        public TheContentViewToucher(View.OnTouchListener other) {
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
     * 用于键盘切换时固定contentView的高度的类，主要用于防止键盘切换时跳闪现象
     */
    public interface ContentViewLocker {
        /**
         * 在这里固定contentView的高度。
         * @param contentView contentView
         */
        void lockContentHeight(View contentView);

        /**
         * 在这里解除固定高度，让contentView填充剩余空间。
         * @param contentView contentView
         */
        void unlockContentHeight(View contentView);
    }

    /**
     * 用于ContentView是LinearLayout的一个子view，并且contentView设置了weight值的情况。
     */
    private static class LinearLayoutLocker implements ContentViewLocker {
        @Override
        public void lockContentHeight(View contentView) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) contentView.getLayoutParams();
            params.height = contentView.getHeight();
            params.weight = 0.0f;
            contentView.setLayoutParams(params);
        }

        @Override
        public void unlockContentHeight(View contentView) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) contentView.getLayoutParams();
            params.height = 0;
            params.weight = 1.0f;
            contentView.setLayoutParams(params);
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

    public interface onEmotionLayoutAndKeyboardStateChangeListener {
        void onShowKeyboard(int height);
        void onShowEmotionLayout(int index);
        void onChangeEmotionLayout(int newIndex, int oldIndex);
    }
}
