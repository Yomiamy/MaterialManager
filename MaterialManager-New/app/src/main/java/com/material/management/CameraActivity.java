package com.material.management;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;
import com.material.management.utils.LogUtility;
import com.material.management.utils.camerakit.FocusMarkerLayout;

/**
 * Created by yomi on 2017/3/23.
 */

public class CameraActivity extends MMActivity {

    private CameraView mCvCameraView;
    private FocusMarkerLayout mFmlFocusMarker;
    private ImageView mIvCapturePhoto;
    private ImageView mIvToggleFlash;
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
        mIvToggleFlash = (ImageView) findViewById(R.id.iv_toggle_flash);
        mIvSwitchFrontBack = (ImageView) findViewById(R.id.iv_switch_front_and_back_camera);
    }

    private void initListener() {
        mIvCapturePhoto.setOnClickListener(this);
        mIvToggleFlash.setOnClickListener(this);
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
                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);


                        LogUtility.printLogD("randy", "");
                    }
                });
                mCvCameraView.setFlash(CameraKit.Constants.FLASH_ON);
                mCvCameraView.captureImage();
            }
            break;

            case R.id.iv_toggle_flash: {

            }
            break;

            case R.id.iv_switch_front_and_back_camera: {

            }
            break;
        }
    }
}
