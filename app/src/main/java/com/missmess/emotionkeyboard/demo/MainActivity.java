package com.missmess.emotionkeyboard.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void click1(View view) {
        startActivity(new Intent(this, BaseFunctionActivity.class));
    }

    public void click2(View view) {
        startActivity(new Intent(this, WechatActivity.class));
    }

    public void click3(View view) {
        startActivity(new Intent(this, KeyboardInfoActivity.class));
    }
}
