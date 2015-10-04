package com.material.management;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.material.management.dialog.CropImageDialog;
import com.material.management.dialog.SelectPhotoDialog;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.Utility;

public class RewardLoginActivity extends MMActivity implements DialogInterface.OnClickListener {
    private static final int REQ_CAMERA_TAKE_PIC = 1;
    private static final int REQ_SELECT_PICTURE = 2;

    private ImageView mIvAddRewardPhoto;
    private TextView mTvAddBardCode;
    private AutoCompleteTextView nActvCardName;
    private AutoCompleteTextView nActvCardNote;

    private BitmapFactory.Options mOptions = null;
    private SelectPhotoDialog mSelectPhotoDialog;
    private CropImageDialog mCropImgDialog;
    private Bitmap mNewestBitmap;
    private Bitmap mBarcodeBitmap;
    private String mBarcode;
    private String mBarcodeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = mInflater.inflate(R.layout.activity_login_reward, null, false);
        setContentView(mLayout);

        findView();
        initListener();
        init();
    }

    private void findView() {
        mIvAddRewardPhoto = (ImageView) findViewById(R.id.iv_add_reward_photo);
        mTvAddBardCode = (TextView) findViewById(R.id.tv_barcode);
        nActvCardName = (AutoCompleteTextView) findViewById(R.id.act_card_name);
        nActvCardNote = (AutoCompleteTextView) findViewById(R.id.actv_item_note);
    }

    private void initListener() {
        mIvAddRewardPhoto.setOnClickListener(this);
        mTvAddBardCode.setOnClickListener(this);
    }

    private void init() {
        Intent intent = getIntent();
        mOptions = new BitmapFactory.Options();

        mOptions.inDensity = Utility.getDisplayMetrics().densityDpi;
        mOptions.inScaled = false;
        mOptions.inPurgeable = true;
        mOptions.inInputShareable = true;

        setTitle(intent.getStringExtra("title"));
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Drawable drawable = mIvAddRewardPhoto.getDrawable();
        Bitmap bitmap = drawable != null && (drawable instanceof BitmapDrawable) ? ((BitmapDrawable) drawable).getBitmap() : null;

        mIvAddRewardPhoto.setImageDrawable(null);
        Utility.releaseBitmaps(bitmap);
        Utility.releaseBitmaps(mBarcodeBitmap);
        Utility.releaseBitmaps(mNewestBitmap);
        mSelectPhotoDialog = null;
        mCropImgDialog = null;

        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.iv_add_reward_photo: {
                mSelectPhotoDialog = new SelectPhotoDialog(this, getString(R.string.title_select_photo), new String[]{
                        getString(R.string.title_select_photo_from_album),
                        getString(R.string.title_select_photo_from_camera)}, this);

                mSelectPhotoDialog.show();
            }
            break;

            case R.id.tv_barcode: {
                IntentIntegrator integrator = new IntentIntegrator(this);

                integrator.initiateScan();
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if(which < 0) {
            if (AlertDialog.BUTTON_POSITIVE == which) {
                 if (mCropImgDialog != null && mCropImgDialog.isDialogShowing()) {
                    /* Recycle the original bitmap from camera intent extra. */

                    mIvAddRewardPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    mNewestBitmap = null;


                    Bitmap bitmap = mCropImgDialog.getCroppedImage();
                    mNewestBitmap = bitmap;

                     mIvAddRewardPhoto.setImageBitmap(bitmap);
                    mCropImgDialog.setShowState(false);
                }
            } else if (AlertDialog.BUTTON_NEGATIVE == which) {
                if (mCropImgDialog != null) {
                    mIvAddRewardPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    mNewestBitmap = null;
                    mCropImgDialog.setShowState(false);
                }
            }
        } else {
            if (mSelectPhotoDialog != null) {
                mSelectPhotoDialog.setShowState(false);

                if (which == 0) {
                    /* from album */
                    Intent albumIntent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    startActivityForResult(
                            Intent.createChooser(albumIntent, getString(R.string.title_image_chooser_title)),
                            REQ_SELECT_PICTURE);
                } else if (which == 1) {
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
                        mIvAddRewardPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestBitmap);
                        mNewestBitmap = null;

                        mNewestBitmap = BitmapFactory.decodeFile(FileUtility.TEMP_PHOTO_FILE.getAbsolutePath(), mOptions);
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        System.gc();
                    }

                    if (mNewestBitmap != null) {
                        mCropImgDialog = new CropImageDialog(this, mNewestBitmap, this);

                        mCropImgDialog.show();
                    }
                }
            }
            break;

            case REQ_SELECT_PICTURE: {
                if (Activity.RESULT_OK == resultCode && intent != null && intent.getData() != null) {
                /* Restore to original icon */
                    mIvAddRewardPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    mNewestBitmap = null;

                    Uri selectedImageUri = intent.getData();
                    String selectedImagePath = Utility.getPathFromUri(selectedImageUri);

                     /* FIXME: duplicate decode image */
                    try {
                        if (selectedImagePath != null) {
                            mNewestBitmap = BitmapFactory.decodeFile(selectedImagePath, mOptions);
                        }
                    } catch (OutOfMemoryError e) {
                    /* A workaround to avoid the OOM */
                        e.printStackTrace();
                        System.gc();
                    }

                /* Error handling */
                    if (mNewestBitmap != null) {
                        mCropImgDialog = new CropImageDialog(this, mNewestBitmap, this);

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

                    if (barcode != null && barcodeFormat != null) {
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
                            e.printStackTrace();
                        }
                    }
                }
            }
            break;
        }
    }
}
