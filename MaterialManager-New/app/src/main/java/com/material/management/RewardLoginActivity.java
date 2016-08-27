package com.material.management;


import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.material.management.component.FlipAnimation;
import com.material.management.data.RewardInfo;
import com.material.management.dialog.CropImageDialog;
import com.material.management.dialog.SelectPhotoDialog;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.LogUtility;
import com.material.management.utils.Utility;
import com.picasso.Picasso;

import java.io.File;

public class RewardLoginActivity extends MMActivity implements DialogInterface.OnClickListener {
    private static final int REQ_CAMERA_TAKE_PIC = 1;
    private static final int REQ_SELECT_PICTURE = 2;

    private Menu mOptionMenu;
    private RelativeLayout mRlAddRewardLayout;
    private ImageView mIvAddRewardFrontPhoto;
    private ImageView mIvAddRewardBackPhoto;
    private ImageView mIvChangeRewardFace;
    private TextView mTvAddBardCode;
    private AutoCompleteTextView mActvCardName;
    private AutoCompleteTextView mActvComment;

    private BitmapFactory.Options mOptions = null;
    private FlipAnimation mFlipAnimation;
    private SelectPhotoDialog mSelectPhotoDialog;
    private CropImageDialog mCropImgDialog;
    private RewardInfo mOldRewardInfo;
    private Bitmap mNewestFrontBitmap;
    private Bitmap mNewestBackBitmap;
    private Bitmap mBarcodeBitmap;
    private String mBarcode = "";
    private String mBarcodeFormat = "";
    private int mCurRewardFaceResId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = mInflater.inflate(R.layout.activity_login_reward, null, false);
        setContentView(mLayout);
        changeLayoutConfig(mLayout);

        findView();
        initListener();
        init();
    }

    private void findView() {
        mRlAddRewardLayout = (RelativeLayout) findViewById(R.id.rl_add_photo_layout);
        mIvAddRewardFrontPhoto = (ImageView) findViewById(R.id.iv_add_reward_front_photo);
        mIvAddRewardBackPhoto = (ImageView) findViewById(R.id.iv_add_reward_back_photo);
        mIvChangeRewardFace = (ImageView) findViewById(R.id.iv_change_reward_face);
        mTvAddBardCode = (TextView) findViewById(R.id.tv_material_barcode);
        mActvCardName = (AutoCompleteTextView) findViewById(R.id.act_card_name);
        mActvComment = (AutoCompleteTextView) findViewById(R.id.actv_item_note);
    }

    private void initListener() {
        mIvAddRewardFrontPhoto.setOnClickListener(this);
        mIvAddRewardBackPhoto.setOnClickListener(this);
        mIvChangeRewardFace.setOnClickListener(this);
        mTvAddBardCode.setOnClickListener(this);
    }

    private void init() {
        Intent intent = getIntent();
        mOptions = new BitmapFactory.Options();
        mOldRewardInfo = intent.getParcelableExtra("reward_info");
        mFlipAnimation = new FlipAnimation(mIvAddRewardFrontPhoto, mIvAddRewardBackPhoto);
        mCurRewardFaceResId = R.id.iv_add_reward_front_photo;
        ActionBar actionBar = getActionBar();

        mOptions.inDensity = mMetrics.densityDpi;
        mOptions.inScaled = false;
        mOptions.inPurgeable = true;
        mOptions.inInputShareable = true;

        Utility.changeHomeAsUp(this, R.drawable.ic_ab_back_holo_dark_am);
        actionBar.setTitle(intent.getStringExtra("title"));
        actionBar.setDisplayHomeAsUpEnabled(true);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE|ActionBar.DISPLAY_HOME_AS_UP);
        }

        initOldData();
    }

    private void initOldData() {
        if (mOldRewardInfo == null) {
            return;
        }
        mActvCardName.setText(mOldRewardInfo.getName());
        mActvComment.setText(mOldRewardInfo.getComment());
        setBarcodeInfo(mOldRewardInfo.getBarCodeFormat(), mOldRewardInfo.getBarCode());
        Picasso.with(this).cancelRequest(mIvAddRewardFrontPhoto);
        Picasso.with(this).load(new File(mOldRewardInfo.getFrontPhotoPath())).fit().into(mIvAddRewardFrontPhoto);
        Picasso.with(this).load(new File(mOldRewardInfo.getBackPhotoPath())).fit().into(mIvAddRewardBackPhoto);
    }

    private void setBarcodeInfo(String barcodeFormat, String barcode) {
        if (barcode == null || barcodeFormat == null || barcode.isEmpty() || barcodeFormat.isEmpty()) {
            return;
        }
        try {
                        /* Restore to default */
            mBarcode = "";
            Drawable defaultBarcodeImg = getResources().getDrawable(R.drawable.selector_barcode);

            defaultBarcodeImg.setBounds(0, 0, defaultBarcodeImg.getIntrinsicWidth(),
                    defaultBarcodeImg.getIntrinsicHeight());
            mTvAddBardCode.setText("x xxxxxx xxxxxx x");
            mTvAddBardCode.setCompoundDrawables(null, defaultBarcodeImg, null, null);
            Utility.releaseBitmaps(mBarcodeBitmap);

            mBarcodeBitmap = null;
            mBarcode = barcode;
            mBarcodeFormat = barcodeFormat;
            mBarcodeBitmap = BarCodeUtility.encodeAsBitmap(barcode,
                    BarcodeFormat.valueOf(mBarcodeFormat), 600, 300);
            Drawable barcodeDrawable = new BitmapDrawable(getResources(), mBarcodeBitmap);

            barcodeDrawable.setBounds(0, 0, barcodeDrawable.getIntrinsicWidth(),
                    barcodeDrawable.getIntrinsicHeight());
            mTvAddBardCode.setText(barcode);
            mTvAddBardCode.setCompoundDrawables(null, barcodeDrawable, null, null);
        } catch (WriterException e) {
            LogUtility.printStackTrace(e);
        }
    }

    @Override
    protected void onStart() {
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_action_bar_menu, menu);
        mOptionMenu = menu;

        setMenuItemVisibility(R.id.action_search, false);
        setMenuItemVisibility(R.id.menu_action_add, true);
        setMenuItemVisibility(R.id.menu_action_cancel, true);
        setMenuItemVisibility(R.id.menu_action_new, false);
        setMenuItemVisibility(R.id.menu_action_receipt_grocery_login, false);
        setMenuItemVisibility(R.id.menu_sort_by_date, false);
        setMenuItemVisibility(R.id.menu_sort_by_name, false);
        setMenuItemVisibility(R.id.menu_sort_by_place, false);
        setMenuItemVisibility(R.id.menu_grid_1x1, false);
        setMenuItemVisibility(R.id.menu_grid_2x1, false);
        setMenuItemVisibility(R.id.menu_clear_expired_items, false);

        return super.onCreateOptionsMenu(menu);
    }

    public void setMenuItemVisibility(int id, boolean visible) {
        if (mOptionMenu != null) {
            MenuItem item = mOptionMenu.findItem(id);

            if (item != null)
                item.setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
            }
            break;

            case R.id.menu_action_add: {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermissions(PERM_REQ_WRITE_EXT_STORAGE, getString(R.string.perm_rationale_write_ext_storage), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return super.onOptionsItemSelected(item);
                }

                RewardInfo rewardInfo = new RewardInfo();

                rewardInfo.setName(mActvCardName.getText().toString());
                rewardInfo.setCardType(RewardInfo.RewardCardType.REWARD_CARD.type());
                rewardInfo.setBarCode(mBarcode);
                rewardInfo.setBarCodeFormat(mBarcodeFormat);
                rewardInfo.setFrontRewardPhoto(mNewestFrontBitmap);
                rewardInfo.setBackRewardPhoto(mNewestBackBitmap);
                rewardInfo.setFrontPhotoPath(mNewestFrontBitmap != null ? "" : (mOldRewardInfo != null ? mOldRewardInfo.getFrontPhotoPath() : ""));
                rewardInfo.setBackPhotoPath(mNewestBackBitmap != null ? "" : (mOldRewardInfo != null ? mOldRewardInfo.getBackPhotoPath() : ""));
                rewardInfo.setComment(mActvComment.getText().toString());

                if (mOldRewardInfo != null) {
                    DBUtility.deleteRewardCard(mOldRewardInfo);
                }
                DBUtility.insertRewardCard(rewardInfo);

                showToast(getString(R.string.data_save_success));
                onBackPressed();
            }
            break;

            case R.id.menu_action_cancel: {
                clearUserData();
                mImm.hideSoftInputFromWindow(mLayout.getApplicationWindowToken(), 0);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearUserData() {
        Drawable rewardFrontDrawable = mIvAddRewardFrontPhoto.getDrawable();
        Drawable rewardBackDrawable = mIvAddRewardBackPhoto.getDrawable();
        Drawable[] compoundDrawables = mTvAddBardCode.getCompoundDrawables();
        Bitmap barcodeBitmap = (compoundDrawables != null && compoundDrawables.length > 0) ? ((compoundDrawables[1] instanceof BitmapDrawable) ? ((BitmapDrawable) compoundDrawables[1]).getBitmap() : null) : null;
        Bitmap rewardFrontBitmap = rewardFrontDrawable != null && (rewardFrontDrawable instanceof BitmapDrawable) ? ((BitmapDrawable) rewardFrontDrawable).getBitmap() : null;
        Bitmap rewardBackBitmap = rewardBackDrawable != null && (rewardBackDrawable instanceof BitmapDrawable) ? ((BitmapDrawable) rewardBackDrawable).getBitmap() : null;
        Drawable defaultBarcodeImg = mResources.getDrawable(R.drawable.selector_barcode);
        mSelectPhotoDialog = null;
        mCropImgDialog = null;

        defaultBarcodeImg.setBounds(0, 0, defaultBarcodeImg.getIntrinsicWidth(), defaultBarcodeImg.getIntrinsicHeight());

        mActvCardName.setText("");
        mActvComment.setText("");
        mTvAddBardCode.setText("x xxxxxx xxxxxx x");
        mTvAddBardCode.setCompoundDrawables(null, defaultBarcodeImg, null, null);
        mIvAddRewardFrontPhoto.setImageResource(R.drawable.selector_add_photo_status);
        mIvAddRewardBackPhoto.setImageResource(R.drawable.selector_add_photo_status);
        Utility.releaseBitmaps(rewardFrontBitmap);
        Utility.releaseBitmaps(rewardBackBitmap);
        Utility.releaseBitmaps(barcodeBitmap);
        Utility.releaseBitmaps(mNewestFrontBitmap);
        Utility.releaseBitmaps(mNewestBackBitmap);
        Utility.forceGC(true);
    }

    @Override
    public void onBackPressed() {
        clearUserData();
        mTvAddBardCode.setCompoundDrawables(null, null, null, null);
        mIvAddRewardFrontPhoto.setImageDrawable(null);

        super.onBackPressed();
    }

    private void flipCard() {
        if (mIvAddRewardFrontPhoto.getVisibility() == View.GONE || mFlipAnimation.isEqualToView(mIvAddRewardFrontPhoto)) {
            mFlipAnimation.reverse();
        }

        if (mCurRewardFaceResId == R.id.iv_add_reward_front_photo) {
            mCurRewardFaceResId = R.id.iv_add_reward_back_photo;
        } else if (mCurRewardFaceResId == R.id.iv_add_reward_back_photo) {
            mCurRewardFaceResId = R.id.iv_add_reward_front_photo;
        }

        mRlAddRewardLayout.startAnimation(mFlipAnimation);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.iv_add_reward_front_photo:
            case R.id.iv_add_reward_back_photo: {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermissions(MMActivity.PERM_REQ_WRITE_EXT_STORAGE, getString(R.string.perm_rationale_write_ext_storage), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return;
                }

                mSelectPhotoDialog = new SelectPhotoDialog(this, getString(R.string.title_select_photo), new String[]{
                        getString(R.string.title_select_photo_from_album),
                        getString(R.string.title_select_photo_from_camera)}, this);

                mSelectPhotoDialog.show();
            }
            break;

            case R.id.iv_change_reward_face: {
                flipCard();
            }
            break;

            case R.id.tv_material_barcode: {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermissionGranted(Manifest.permission.CAMERA)) {
                    requestPermissions(MMActivity.PERM_REQ_CAMERA, getString(R.string.perm_rationale_camera), Manifest.permission.CAMERA);
                    return;
                }

                IntentIntegrator integrator = new IntentIntegrator(this);

                integrator.initiateScan();
            }
            break;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if (which < 0) {
            if (AlertDialog.BUTTON_POSITIVE == which) {
                if (mCropImgDialog != null && mCropImgDialog.isDialogShowing()) {
                    /* Recycle the original bitmap from camera intent extra. */
                    if (mCurRewardFaceResId == R.id.iv_add_reward_front_photo) {
                        mIvAddRewardFrontPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestFrontBitmap);

                        mNewestFrontBitmap = mCropImgDialog.getCroppedImage();

                        mIvAddRewardFrontPhoto.setImageBitmap(mNewestFrontBitmap);
                    } else if (mCurRewardFaceResId == R.id.iv_add_reward_back_photo) {
                        mIvAddRewardBackPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestBackBitmap);

                        mNewestBackBitmap = mCropImgDialog.getCroppedImage();

                        mIvAddRewardBackPhoto.setImageBitmap(mNewestBackBitmap);
                    }

                    mCropImgDialog.setShowState(false);
                }
            } else if (AlertDialog.BUTTON_NEGATIVE == which) {
                if (mCropImgDialog != null) {
                    if (mCurRewardFaceResId == R.id.iv_add_reward_front_photo) {
                        mIvAddRewardFrontPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestFrontBitmap);
                    } else if (mCurRewardFaceResId == R.id.iv_add_reward_back_photo) {
                        mIvAddRewardBackPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestBackBitmap);
                    }
                    mCropImgDialog.setShowState(false);
                }
            }
        } else {
            if (mSelectPhotoDialog != null) {
                mSelectPhotoDialog.setShowState(false);
                Utility.forceGC(true);
                if (which == 0) {
                    /* from album */
                    Intent albumIntent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    startActivityForResult(
                            Intent.createChooser(albumIntent, getString(R.string.title_image_chooser_title)),
                            REQ_SELECT_PICTURE);
                } else if (which == 1) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermissionGranted(Manifest.permission.CAMERA)) {
                        requestPermissions(MMActivity.PERM_REQ_CAMERA, getString(R.string.perm_rationale_camera), Manifest.permission.CAMERA);
                        return;
                    }

                    /* from camera */
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(FileUtility.TEMP_PHOTO_FILE));
                    startActivityForResult(takePictureIntent, REQ_CAMERA_TAKE_PIC);
                }
            }
        }

        mSelectPhotoDialog = null;
        mCropImgDialog = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQ_CAMERA_TAKE_PIC: {

                if (Activity.RESULT_OK == resultCode) {
                    try {
                        /* Restore to original icon */
                        if (mCurRewardFaceResId == R.id.iv_add_reward_front_photo) {
                            mIvAddRewardFrontPhoto.setImageResource(R.drawable.selector_add_photo_status);
                            Utility.releaseBitmaps(mNewestFrontBitmap);
                            mNewestFrontBitmap = null;

                            mNewestFrontBitmap = BitmapFactory.decodeFile(FileUtility.TEMP_PHOTO_FILE.getAbsolutePath(), mOptions);
                        } else if (mCurRewardFaceResId == R.id.iv_add_reward_back_photo) {
                            mIvAddRewardBackPhoto.setImageResource(R.drawable.selector_add_photo_status);
                            Utility.releaseBitmaps(mNewestBackBitmap);
                            mNewestBackBitmap = null;

                            mNewestBackBitmap = BitmapFactory.decodeFile(FileUtility.TEMP_PHOTO_FILE.getAbsolutePath(), mOptions);
                        }
                    } catch (OutOfMemoryError e) {
                        LogUtility.printError(e);
                        Utility.forceGC(false);
                    }

                    if (mNewestFrontBitmap != null || mNewestBackBitmap != null) {
                        if (mCurRewardFaceResId == R.id.iv_add_reward_front_photo) {
                            mCropImgDialog = new CropImageDialog(this, mNewestFrontBitmap, this);
                        } else if (mCurRewardFaceResId == R.id.iv_add_reward_back_photo) {
                            mCropImgDialog = new CropImageDialog(this, mNewestBackBitmap, this);
                        }

                        mCropImgDialog.show();
                    }
                }
            }
            break;

            case REQ_SELECT_PICTURE: {
                if (Activity.RESULT_OK == resultCode && intent != null && intent.getData() != null) {
                /* Restore to original icon */
                    if (mCurRewardFaceResId == R.id.iv_add_reward_front_photo) {
                        mIvAddRewardFrontPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestFrontBitmap);
                        mNewestFrontBitmap = null;
                    } else if (mCurRewardFaceResId == R.id.iv_add_reward_back_photo) {
                        mIvAddRewardBackPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestBackBitmap);
                        mNewestBackBitmap = null;
                    }

                    Uri selectedImageUri = intent.getData();
                    String selectedImagePath = Utility.getPathFromUri(selectedImageUri);

                     /* FIXME: duplicate decode image */
                    try {
                        if (selectedImagePath != null) {
                            if (mCurRewardFaceResId == R.id.iv_add_reward_front_photo) {
                                mNewestFrontBitmap = BitmapFactory.decodeFile(selectedImagePath, mOptions);
                            } else if (mCurRewardFaceResId == R.id.iv_add_reward_back_photo) {
                                mNewestBackBitmap = BitmapFactory.decodeFile(selectedImagePath, mOptions);
                            }
                        }
                    } catch (OutOfMemoryError e) {
                    /* A workaround to avoid the OOM */
                        LogUtility.printError(e);
                        Utility.forceGC(false);
                    }

                /* Error handling */
                    if (mNewestFrontBitmap != null || mNewestBackBitmap != null) {
                        if (mCurRewardFaceResId == R.id.iv_add_reward_front_photo) {
                            mCropImgDialog = new CropImageDialog(this, mNewestFrontBitmap, this);
                        } else if (mCurRewardFaceResId == R.id.iv_add_reward_back_photo) {
                            mCropImgDialog = new CropImageDialog(this, mNewestBackBitmap, this);
                        }
                        mCropImgDialog.show();
                    }
                }
            }
            break;

            case IntentIntegrator.REQUEST_CODE: {
            /* For barcode scanner */
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

                if (scanResult != null) {
                    String barcode = scanResult.getContents();
                    String barcodeFormat = scanResult.getFormatName();

                    setBarcodeInfo(barcodeFormat, barcode);
                }
            }
            break;
        }
    }
}
