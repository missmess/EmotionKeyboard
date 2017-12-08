package com.missmess.emotionkeyboard.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;

import com.missmess.emotionkeyboard.EmojiconKeyBoard;
import com.missmess.emotionkeyboard.KeyboardInfo;

public class WechatActivity extends AppCompatActivity {
    private EmojiconKeyBoard emotionKeyboard;
    private ScrollView ll_contentView;
    private EditText et_input;
    private View v_press_voice;
    private View btn_emoji;
    private View btn_more;
    private View btn_send;
    private View btn_voice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wechat);

        ll_contentView = (ScrollView) findViewById(R.id.ll_contentView);
        View ll_bottom_layout = findViewById(R.id.ll_bottom_layout);
        View ll_bottom_layout2 = findViewById(R.id.ll_bottom_layout2);
        btn_voice = findViewById(R.id.btn_voice);
        btn_emoji = findViewById(R.id.btn_emoji);
        btn_more = findViewById(R.id.btn_more);
        btn_send = findViewById(R.id.btn_send);
        v_press_voice = findViewById(R.id.v_press_voice);
        et_input = (EditText) findViewById(R.id.et_input);

        et_input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    btn_send.setVisibility(View.VISIBLE);
                    btn_more.setVisibility(View.GONE);
                } else {
                    btn_send.setVisibility(View.GONE);
                    btn_more.setVisibility(View.VISIBLE);
                }
            }
        });

        //绑定内容view
        //判断绑定那种EditView
        emotionKeyboard = new EmojiconKeyBoard.Builder(this)
                .contentLayout(ll_contentView)//绑定内容view
                .editText(et_input)//判断绑定那种EditView
                .addEmotionBtnAndLayout(btn_emoji, ll_bottom_layout)
                .addEmotionBtnAndLayout(btn_more, ll_bottom_layout2)
                .touchContentViewHideAllEnabled(null)
                .keyboardStateCallback(new KeyboardInfo.OnSoftKeyboardChangeListener() {
                    @Override
                    public void onSoftKeyboardStateChanged(boolean shown, int height) {
                        if (shown) {
                            scrollContentToBottom();
                        }
                    }
                })
                .emotionPanelStateCallback(new EmojiconKeyBoard.OnEmotionLayoutStateChangeListener() {
                    @Override
                    public void onEmotionLayoutShow(View newEmotionLayout, int newEmotionLayoutIndex, int oldEmotionLayoutIndex) {
                        scrollContentToBottom();
                        if (btn_voice.isActivated()) {
                            setVoiceActivate(false, false);
                        }

                        if (newEmotionLayoutIndex == 0) {
                            et_input.requestFocus();
                            btn_emoji.setActivated(true);
                        }
                        if (newEmotionLayoutIndex == 1) {
                            et_input.clearFocus();
                        }
                        if (oldEmotionLayoutIndex == 0) {
                            btn_emoji.setActivated(false);
                        }
                    }

                    @Override
                    public void onEmotionLayoutHide(int oldEmotionLayoutIndex) {
                        if (oldEmotionLayoutIndex == 0) {
                            btn_emoji.setActivated(false);
                        }
                    }
                })
                .build();

        btn_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emotionKeyboard.hideEmotionLayout();

                setVoiceActivate(!btn_voice.isActivated(), true);
            }
        });

        scrollContentToBottom();
    }

    private void setVoiceActivate(boolean activate, boolean keyboard) {
        if (btn_voice.isActivated() == activate)
            return;

        if (activate) {
            btn_voice.setActivated(true);
            v_press_voice.setVisibility(View.VISIBLE);
            et_input.setVisibility(View.GONE);
            emotionKeyboard.hideSoftKeyboard();
        } else {
            btn_voice.setActivated(false);
            v_press_voice.setVisibility(View.GONE);
            et_input.setVisibility(View.VISIBLE);
            if (keyboard) {
                emotionKeyboard.showSoftKeyboard();
            } else {
                et_input.requestFocus();
            }
        }
    }

    private void scrollContentToBottom() {
        ll_contentView.post(new Runnable() {
            @Override
            public void run() {
                ll_contentView.scrollTo(0, ll_contentView.getChildAt(0).getHeight());
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!emotionKeyboard.interceptBackPress()) {
            super.onBackPressed();
        }
    }

}
