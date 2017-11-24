package com.missmess.emotionkeyboard.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.missmess.emotionkeyboard.KeyboardInfo;

public class KeyboardInfoActivity extends AppCompatActivity {

    private TextView tv_status;
    private KeyboardInfo keyboardInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard_info);

        tv_status = (TextView) findViewById(R.id.tv_status);

        keyboardInfo = KeyboardInfo.from(this);
        keyboardInfo.startListening();

        keyboardInfo.setOnKeyboardChangeListener(new KeyboardInfo.OnSoftKeyboardChangeListener() {
            @Override
            public void onSoftKeyboardStateChanged(boolean shown, int height) {
                if (shown) {
                    tv_status.setText(String.format("键盘弹出了，高度为 %d", height));
                } else {
                    tv_status.setText("键盘收起了");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardInfo.stopListening();
    }
}
