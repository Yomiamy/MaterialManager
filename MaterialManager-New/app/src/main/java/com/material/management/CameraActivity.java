package com.material.management;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraHostProvider;
import com.commonsware.cwac.camera.CameraView;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.material.management.utils.BitmapUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.LogUtility;
import com.material.management.utils.camerakit.FocusMarkerLayout;

public class CameraActivity extends MMActivity implements CameraHostProvider {

    private static final Interpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();

    private CameraView mCvCameraView;
    private View mVShutter;
    private FrameLayout mFlCamPreviewLayout;
    private FocusMarkerLayout mFmlFocusMarker;
    private ImageView mIvCapturePhoto;
    private ImageView mIvSwitchFrontBack;

    //    private CustomizedCameraHost mCamHost;
    private boolean mIsUseFrontCamFace = false;


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
        mFlCamPreviewLayout = (FrameLayout) findViewById(R.id.fl_camera_preview_layout);
        mVShutter = findViewById(R.id.v_shutter);
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
        if (mCvCameraView.isAutoFocusAvailable()) {
            mCvCameraView.autoFocus();
        }
        mFmlFocusMarker.setCameraView(mCvCameraView);

        mActionBar.setTitle(getString(R.string.camera_activity_actionbar_title));
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCvCameraView.onResume();
    }

    @Override
    protected void onPause() {
        mCvCameraView.onPause();

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

    private void animateShutter() {
        mVShutter.setVisibility(View.VISIBLE);
        mVShutter.setAlpha(0.f);

        ObjectAnimator alphaInAnim = ObjectAnimator.ofFloat(mVShutter, "alpha", 0f, 0.8f);
        alphaInAnim.setDuration(100);
        alphaInAnim.setStartDelay(100);
        alphaInAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator alphaOutAnim = ObjectAnimator.ofFloat(mVShutter, "alpha", 0.8f, 0f);
        alphaOutAnim.setDuration(200);
        alphaOutAnim.setInterpolator(DECELERATE_INTERPOLATOR);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(alphaInAnim, alphaOutAnim);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mVShutter.setVisibility(View.GONE);
            }
        });
        animatorSet.start();
    }

    @Override
    public CameraHost getCameraHost() {
        return new SimpleCameraHost(this) {
            private Camera.Size mPreviewSize;

            @Override
            public boolean useFullBleedPreview() {
                return true;
            }

            @Override
            public Camera.Size getPictureSize(PictureTransaction xact, Camera.Parameters parameters) {
                return mPreviewSize;
            }

            @Override
            public Camera.Parameters adjustPreviewParameters(Camera.Parameters parameters) {
                Camera.Parameters parameters1 = super.adjustPreviewParameters(parameters);
                mPreviewSize = parameters1.getPreviewSize();
                return parameters1;
            }

            @Override
            public void saveImage(PictureTransaction xact, final Bitmap bitmap) {
                showProgressDialog(null, mResources.getString(R.string.title_photo_write_progress));
                new Thread(() -> {
                    BitmapUtility.getInstance().writeBitmapToFile(bitmap, FileUtility.TEMP_PHOTO_FILE, "jpg");
                    closeProgressDialog();
                    setResult(RESULT_OK);
                    finish();
                }).start();
            }

            protected boolean useFrontFacingCamera() {
                return mIsUseFrontCamFace;
            }
        };
    }

    private void switchCamFace() {
        // do some change to the settings.
        mIsUseFrontCamFace = !mIsUseFrontCamFace;
        
        if (null != mCvCameraView) {
            mCvCameraView.onPause();
        }
        mFlCamPreviewLayout.removeAllViews();
        mCvCameraView = new CameraView(this);
        mCvCameraView.setHost(getCameraHost());
        mFlCamPreviewLayout.addView(mCvCameraView);
        mCvCameraView.onResume();
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();
        switch (id) {
            case R.id.iv_capture_photo: {
                mCvCameraView.takePicture(true, false);
                animateShutter();
            }
            break;

            case R.id.iv_switch_front_and_back_camera: {
                switchCamFace();
            }
            break;
        }
    }
}
