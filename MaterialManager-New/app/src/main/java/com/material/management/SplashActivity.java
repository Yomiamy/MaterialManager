package com.material.management;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends Activity {

    private long mSplashDispDuration = 5000;
    private Handler mHandler = new Handler();
    private Runnable mSplashScreenRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);

            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_layout);

        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(mSplashScreenRunnable, mSplashDispDuration);
    }
}
