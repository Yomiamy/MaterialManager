package com.material.management.dialog;

import com.material.management.R;
import com.material.management.component.cropper.CropImageView;
import com.material.management.utils.Utility;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class CropImageDialog extends AlertDialog.Builder implements View.OnClickListener {
    // Static final constants
    private static final int DEFAULT_ASPECT_RATIO_VALUES = 10;
    private static final int ROTATE_POSITIVE_NINETY_DEGREES = 90;
    private static final int ROTATE_NEGATIVE_NINETY_DEGREES = -90;
    // Instance variables
    private View mLayout;
    private RelativeLayout mRlCropImg;
    private CropImageView mCivSourceImage;
    private ImageView mIvTurnLeft;
    private ImageView mIvTurnRight;

    private Activity mOwnerActivity;
    private AlertDialog mDialog = null;
    private Bitmap mSrcImage;
    private boolean mIsShown;
    private OnClickListener mListener;
    private int mOrigWid;
    private int mOrigHeigh;
    private int mScaleW;
    private int mScaleH;
    private DisplayMetrics mMetrics = null;

    public CropImageDialog(Activity ownerActivity, Bitmap bitmap, OnClickListener listener) {
        super(ownerActivity, R.style.AlertDialogTheme);

        mOwnerActivity = ownerActivity;
        mSrcImage = bitmap;
        mIsShown = false;
        mListener = listener;
        mOrigHeigh = mSrcImage.getHeight();
        mOrigWid = mSrcImage.getWidth();

        init();
        initView();
        setTitle(mOwnerActivity.getString(R.string.title_crop_dialog_title));
        setPositiveButton(mOwnerActivity.getString(R.string.title_positive_btn_label), mListener);
        setNegativeButton(mOwnerActivity.getString(R.string.title_negative_btn_label), mListener);
    }

    private void init() {
        mMetrics = new DisplayMetrics();
        mOwnerActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        mScaleW = mMetrics.widthPixels;
        int imgW = mSrcImage.getWidth();
        int imgH = mSrcImage.getHeight();

        if (imgW >= 600 && imgH >= 400) {
            mScaleH = mScaleW * 2 / 3;
        } else if (imgW == imgH) {
            mScaleH = mScaleW;
        } else if (imgW != imgH) {
            mScaleH = (int) (mScaleW * ((float) imgH / imgW));
        }
    }

    private void initView() {
        Bitmap targetImage = Bitmap.createScaledBitmap(mSrcImage, mScaleW, mScaleH, false);

        if(mSrcImage != targetImage) {
            Utility.releaseBitmaps(mSrcImage);
            mSrcImage = null;
        }

        LayoutInflater layoutInflater = (LayoutInflater) Utility.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mLayout = layoutInflater.inflate(R.layout.dialog_crop_image_layout, null);
        mRlCropImg = (RelativeLayout) mLayout.findViewById(R.id.rl_crop_img);
        mCivSourceImage = (CropImageView) mLayout.findViewById(R.id.civ_source_image);
        // Sets the rotate button
        mIvTurnLeft = (ImageView) mLayout.findViewById(R.id.iv_turn_left);
        mIvTurnRight = (ImageView) mLayout.findViewById(R.id.iv_turn_right);

        mCivSourceImage.setMinimumWidth(mScaleW);
        mCivSourceImage.setMinimumHeight(mScaleH);
        mIvTurnLeft.setOnClickListener(this);
        mIvTurnRight.setOnClickListener(this);
        // Sets initial aspect ratio to 10/10, for demonstration purposes
        mCivSourceImage.setImageBitmap(targetImage);
        mCivSourceImage.setGuidelines(1);
        mCivSourceImage.setAspectRatio(DEFAULT_ASPECT_RATIO_VALUES, DEFAULT_ASPECT_RATIO_VALUES);
        setView(mLayout);
    }

    public boolean isDialogShowing() {
        return mIsShown;
    }

    public void setShowState(boolean isShowing) {
        mIsShown = isShowing;

        if(!isShowing && mDialog != null) {
            mDialog.dismiss();
        }
    }

    public Bitmap getCroppedImage() {
        Bitmap bitmap = null;
        try {
            bitmap = mCivSourceImage.getCroppedImage();
            int targetWidth = Math.min(mOrigWid, bitmap.getWidth());
            int targetHeight = Math.min(mOrigHeigh, bitmap.getHeight());

            return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
        } finally {
            /* it won't use the recycled bitmap after the dialog hided */
            mCivSourceImage.setImageBitmap(null);

            Utility.releaseBitmaps(bitmap);
            bitmap = null;

        }
    }

    @Override
    public AlertDialog show() {
        mIsShown = true;
        mDialog = super.show();
        Window window = mDialog.getWindow();

        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        return mDialog;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
            case R.id.iv_turn_left: {
                mCivSourceImage.rotateImage(ROTATE_NEGATIVE_NINETY_DEGREES);
            }
            break;
            case R.id.iv_turn_right: {
                mCivSourceImage.rotateImage(ROTATE_POSITIVE_NINETY_DEGREES);
            }
            break;
        }
    }
}
