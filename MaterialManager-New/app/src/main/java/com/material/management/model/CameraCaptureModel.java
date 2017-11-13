package com.material.management.model;


import com.material.management.R;
import com.material.management.presenter.CameraCapturePresenter;
import com.material.management.utils.BitmapUtility;
import com.material.management.utils.FileUtility;
import com.otaliastudios.cameraview.CameraUtils;

public class CameraCaptureModel {

    private static final int DECODED_BITMAP_MAX_H = 1000;
    private static final int DECODED_BITMAP_MAX_W = 1000;

    private CameraCapturePresenter mPresenter;

    public CameraCaptureModel(CameraCapturePresenter presenter) {
        this.mPresenter = presenter;
    }

    public void onHandlePictureData(byte[] picByteAry) {
        CameraUtils.decodeBitmap(picByteAry, DECODED_BITMAP_MAX_W, DECODED_BITMAP_MAX_H, bitmap -> {
            new Thread(() -> {
                BitmapUtility.getInstance().writeBitmapToFile(bitmap, FileUtility.TEMP_PHOTO_FILE, "jpg");
                mPresenter.onHandlePictureDataFinish();
            }).start();
        });
    }
}
