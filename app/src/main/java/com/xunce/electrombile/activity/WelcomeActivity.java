package com.xunce.electrombile.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.xunce.electrombile.R;

public class WelcomeActivity extends Activity {

    private Button btn_Sure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        initView();
    }

    private void initView(){
        btn_Sure = (Button)findViewById(R.id.btn_Sure);
        btn_Sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this,FragmentActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
