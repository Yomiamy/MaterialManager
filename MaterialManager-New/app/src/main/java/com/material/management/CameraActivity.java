package com.material.management;


import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.material.management.presenter.CameraCapturePresenter;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;

public class CameraActivity extends MMActivity {

    private CameraView mCvCameraView;
    private CameraCapturePresenter mPresenter = new CameraCapturePresenter(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_camera_layout);

        initView();
        initListener();
    }

    private void initView() {
        mCvCameraView = findViewById(R.id.cv_camera_view);
    }

    private void initListener() {
        mCvCameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                super.onPictureTaken(jpeg);
                mPresenter.onPictureTakened(jpeg);
            }
        });
    }

    public void onCapturePhotoClick(View view) {
        mCvCameraView.capturePicture();
    }

    public void onToggleCameraClick(View view) {
        mCvCameraView.toggleFacing();
    }

    public void onPictureTakenFinish() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCvCameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCvCameraView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCvCameraView.destroy();
    }

}
