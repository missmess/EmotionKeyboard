package com.missmess.emotionkeyboard.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.missmess.emotionkeyboard.EmotionKeyboard;
import com.missmess.emotionkeyboard.KeyboardInfo;

public class BaseFunctionActivity extends AppCompatActivity {

    private EmotionKeyboard emotionKeyboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_function);

        View ll_contentView = findViewById(R.id.ll_contentView);
        View ll_bottom_layout = findViewById(R.id.ll_bottom_layout);
        View ll_bottom_layout2 = findViewById(R.id.ll_bottom_layout2);
        View btn_emoji = findViewById(R.id.btn_emoji);
        View btn_more = findViewById(R.id.btn_more);
        EditText et_input = (EditText) findViewById(R.id.et_input);


        //绑定内容view
        //判断绑定那种EditView
        emotionKeyboard = new EmotionKeyboard.Builder(this)
                .contentLayout(ll_contentView)//绑定内容view
                .editText(et_input)//绑定EditView
                .addEmotionBtnAndLayout(btn_emoji, ll_bottom_layout)//添加第一个表情按钮布局
                .addEmotionBtnAndLayout(btn_more, ll_bottom_layout2)//第二个
                .touchContentViewHideAllEnabled(null)//是否在触摸内容view时获取焦点隐藏键盘
                .keyboardStateCallback(new KeyboardInfo.OnSoftKeyboardChangeListener() {
                    @Override
                    public void onSoftKeyboardStateChanged(boolean shown, int height) {
                        if (shown) {
                            Log.d("BaseFunctionActivity", "显示键盘");
                        } else {
                            Log.d("BaseFunctionActivity", "隐藏键盘");
                        }
                    }
                })
                .emotionPanelStateCallback(new EmotionKeyboard.OnEmotionLayoutStateChangeListener() {
                    @Override
                    public void onEmotionLayoutShow(View newEmotionLayout, int newEmotionLayoutIndex, int oldEmotionLayoutIndex) {
                        Log.d("BaseFunctionActivity", "显示index=" + newEmotionLayoutIndex + "的布局，隐藏index=" + oldEmotionLayoutIndex + "的布局");
                    }

                    @Override
                    public void onEmotionLayoutHide(int oldEmotionLayoutIndex) {
                        Log.d("BaseFunctionActivity", "隐藏布局");
                    }
                })
                .build();
    }

    @Override
    public void onBackPressed() {
        if (!emotionKeyboard.interceptBackPress()) {
            super.onBackPressed();
        }
    }
}
