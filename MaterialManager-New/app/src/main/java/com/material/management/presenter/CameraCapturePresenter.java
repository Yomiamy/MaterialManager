package com.material.management.presenter;


import com.material.management.CameraActivity;
import com.material.management.R;
import com.material.management.model.CameraCaptureModel;

public class CameraCapturePresenter {

    private CameraActivity mCameraActivity;
    private CameraCaptureModel mModel = new CameraCaptureModel(this);

    public CameraCapturePresenter(CameraActivity activity) {
        this.mCameraActivity = activity;
    }

    public void onPictureTakened(byte[] picByteAry) {
        mCameraActivity.showProgressDialog(null, mCameraActivity.getString(R.string.title_photo_write_progress));
        mModel.onHandlePictureData(picByteAry);
    }

    public void onHandlePictureDataFinish() {
        mCameraActivity.onPictureTakenFinish();
        mCameraActivity.closeProgressDialog();
    }
}
