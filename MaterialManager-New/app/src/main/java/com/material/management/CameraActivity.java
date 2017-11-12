package com.material.management;


import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.material.management.utils.BitmapUtility;
import com.material.management.utils.FileUtility;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;

public class CameraActivity extends MMActivity {

    private static final int DECODED_BITMAP_MAX_H = 1000;
    private static final int DECODED_BITMAP_MAX_W = 1000;

    private CameraView mCvCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_camera_layout);

        initView();
        initListener();
    }

    private void initListener() {
        mCvCameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                super.onPictureTaken(jpeg);
                CameraUtils.decodeBitmap(jpeg, DECODED_BITMAP_MAX_W, DECODED_BITMAP_MAX_H, bitmap -> {
                    showProgressDialog(null, mResources.getString(R.string.title_photo_write_progress));
                    new Thread(() -> {
                        BitmapUtility.getInstance().writeBitmapToFile(bitmap, FileUtility.TEMP_PHOTO_FILE, "jpg");
                        closeProgressDialog();
                        setResult(RESULT_OK);
                        finish();
                    }).start();
                });
            }
        });
    }


    private void initView() {
        mCvCameraView = findViewById(R.id.cv_camera_view);
    }

    public void onCapturePhotoClick(View view) {
        mCvCameraView.capturePicture();
    }

    public void onToggleCameraClick(View view) {
        mCvCameraView.toggleFacing();
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
