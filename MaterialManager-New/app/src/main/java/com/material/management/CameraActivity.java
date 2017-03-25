package com.material.management;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;
import com.material.management.utils.BitmapUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.LogUtility;
import com.material.management.utils.camerakit.FocusMarkerLayout;

public class CameraActivity extends MMActivity {

    private CameraView mCvCameraView;
    private FocusMarkerLayout mFmlFocusMarker;
    private ImageView mIvCapturePhoto;
    private ImageView mIvSwitchFrontBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_layout);

        initView();
        initListener();
        init();
    }

    private void initView() {
        mCvCameraView = (CameraView) findViewById(R.id.cv_camera_preview);
        mFmlFocusMarker = (FocusMarkerLayout) findViewById(R.id.fml_camera_focus_marker);
        mIvCapturePhoto = (ImageView) findViewById(R.id.iv_capture_photo);
        mIvSwitchFrontBack = (ImageView) findViewById(R.id.iv_switch_front_and_back_camera);
    }

    private void initListener() {
        mIvCapturePhoto.setOnClickListener(this);
        mIvSwitchFrontBack.setOnClickListener(this);
        mFmlFocusMarker.setOnTouchListener((v, event) -> {
            mFmlFocusMarker.focus(event.getX(), event.getY());
            return false;
        });
    }

    private void init() {

        mActionBar.setTitle(getString(R.string.camera_activity_actionbar_title));
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(true);

        mCvCameraView.setFacing(CameraKit.Constants.FACING_BACK);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCvCameraView.start();
    }

    @Override
    protected void onPause() {
        mCvCameraView.stop();

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();
        switch (id) {
            case R.id.iv_capture_photo: {
                mCvCameraView.setCameraListener(new CameraListener() {
                    @Override
                    public void onPictureTaken(byte[] jpeg) {
                        super.onPictureTaken(jpeg);

                        showProgressDialog(null, mResources.getString(R.string.title_photo_write_progress));
                        new Thread(() -> {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

                            BitmapUtility.getInstance().writeBitmapToFile(bitmap, FileUtility.TEMP_PHOTO_FILE, "jpg");
                            closeProgressDialog();
                            setResult(RESULT_OK);
                            finish();
                        }).start();
                    }
                });
                mCvCameraView.captureImage();
            }
            break;

            case R.id.iv_switch_front_and_back_camera: {
                mCvCameraView.toggleFacing();
            }
            break;
        }
    }
}