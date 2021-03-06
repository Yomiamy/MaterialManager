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
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.android.datetimepicker.date.DatePickerDialog;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.material.management.data.Material;
import com.material.management.dialog.InputDialog;
import com.material.management.dialog.MultiChoiceDialog;
import com.material.management.dialog.SelectPhotoDialog;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.FabricUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.LogUtility;
import com.material.management.utils.Utility;
import com.picasso.Picasso;
import com.cropper.CropImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

public class MaterialModifyActivity extends MMActivity implements AdapterView.OnItemSelectedListener,
        DialogInterface.OnClickListener, DatePickerDialog.OnDateSetListener {

    private static final int REQ_CAMERA_TAKE_PIC = 1;
    private static final int REQ_SELECT_PICTURE = 2;
    private static final String DATEPICKER_TAG = "datepicker";
    private static Pattern sMaterialNameErrorPattern = Pattern.compile(".*[\\\\/?]+.*", Pattern.DOTALL);

    private View mLayout;
    private RelativeLayout mRlPurchaceDate;
    private RelativeLayout mRlValidateDate;
    private ImageView mIvAddPhoto;
    private ImageView mIvBarcode;
    private TextView mTvBarcodeTxt;
    private AutoCompleteTextView mActMaterialName;
    private Spinner mSpinMaterialCategory;
    private TextView mTvPurchaceDate;
    private TextView mTvValidDate;
    private EditText mEtNotificationDays;
    private AutoCompleteTextView mActMaterialPlace;
    private AutoCompleteTextView mActComment;
    private InputDialog mInputDialog;
    private MultiChoiceDialog mMultiChoiceDialog;
    private SelectPhotoDialog mSelectPhotoDialog;
    private DatePickerDialog mDatePickerDialog;

    private Material mOldMaterial;
    private Menu mOptionMenu;
    private Bitmap mNewestBitmap = null;
    private Bitmap mBarcodeBitmap = null;
    /* Store the newest recorded barcode */
    private String mBarcode = "";
    private String mBarcodeFormat = "";
    private String mDateFormat = "";
    private int mCurPressDateBtnId = -1;
    private BitmapFactory.Options mOptions = null;
    private Calendar mPurchaceDate;
    private Calendar mValidDate;
    private ArrayAdapter<String> mTextHistAdapter = null;
    private ArrayAdapter<String> mCategoryAdapter = null;
    private LinkedHashSet<String> mMaterialTypes = null;
    private ArrayList<String> mTextHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = mInflater.inflate(R.layout.fragment_login_material_layout, null, false);
        setContentView(mLayout);

        findView();
        setListener();
        init();
    }

    private void findView() {
        mIvAddPhoto = (ImageView) mLayout.findViewById(R.id.iv_add_photo);
        mIvBarcode = (ImageView) mLayout.findViewById(R.id.iv_barcode);
        mTvBarcodeTxt = (TextView) mLayout.findViewById(R.id.tv_barcode_txt);
        mActMaterialName = (AutoCompleteTextView) mLayout.findViewById(R.id.act_material_name);
        mSpinMaterialCategory = (Spinner) mLayout.findViewById(R.id.spin_material_category);
        mRlPurchaceDate = (RelativeLayout) mLayout.findViewById(R.id.rl_purchace_date_layout);
        mRlValidateDate = (RelativeLayout) mLayout.findViewById(R.id.rl_validate_date_layout);
        mTvPurchaceDate = (TextView) mLayout.findViewById(R.id.tv_purchace_date);
        mTvValidDate = (TextView) mLayout.findViewById(R.id.tv_valid_date);
        mEtNotificationDays = (EditText) mLayout.findViewById(R.id.et_notification_days);
        mActMaterialPlace = (AutoCompleteTextView) mLayout.findViewById(R.id.act_material_place);
        mActComment = (AutoCompleteTextView) mLayout.findViewById(R.id.act_comment);
    }

    private void setListener() {
        mIvAddPhoto.setOnClickListener(this);
        mIvBarcode.setOnClickListener(this);
        mRlPurchaceDate.setOnClickListener(this);
        mRlValidateDate.setOnClickListener(this);
        mSpinMaterialCategory.setOnItemSelectedListener(this);
    }

    private void init() {
        boolean isInitialized = Utility.getBooleanValueForKey(Utility.CATEGORY_IS_INITIALIZED);
        String composedDateFormat = Utility.getStringValueForKey(Utility.SHARE_PREF_KEY_COMPOSED_DATE_FORMAT_SYMBOL);
        mDateFormat = composedDateFormat.split(Utility.SYMBOL_COMPOSED_DATE_FORMAT)[0];
        mOldMaterial = getIntent().getParcelableExtra("material_item");
        mValidDate = mOldMaterial.getValidDate();
        mPurchaceDate = mOldMaterial.getPurchaceDate();
        mOptions = new BitmapFactory.Options();
        mOptions.inDensity = Utility.getDisplayMetrics().densityDpi;
        mOptions.inScaled = false;
        mOptions.inPurgeable = true;
        mOptions.inInputShareable = true;

        if (isInitialized)
            mMaterialTypes = new LinkedHashSet<>();
        else {
            mMaterialTypes = new LinkedHashSet<>(Arrays.asList(getResources().getStringArray(
                    R.array.default_material_type)));
            Utility.setBooleanValueForKey(Utility.CATEGORY_IS_INITIALIZED, true);
        }

        mActionBar.setTitle(getString(R.string.material_modify_actionbar_title));
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(true);
        Utility.changeHomeAsUp(this, R.drawable.ic_ab_back_holo_dark_am);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        }

        initSpinnerData();
        initAutoCompleteData();
        changeLayoutConfig(mLayout);
        mImm.hideSoftInputFromWindow(mLayout.getApplicationWindowToken(), 0);
        initOldData();
    }

    private void initOldData() {
        mActMaterialName.setText(mOldMaterial.getName());
        mSpinMaterialCategory.setSelection(((ArrayAdapter<String>) mSpinMaterialCategory.getAdapter()).getPosition(mOldMaterial.getMaterialType()));
        mTvValidDate.setText(Utility.transDateToString(mDateFormat, mValidDate.getTime()));
        mTvPurchaceDate.setText(Utility.transDateToString(mDateFormat, mPurchaceDate.getTime()));
        mEtNotificationDays.setText(Integer.toString(mOldMaterial.getNotificationDays()));
        mActMaterialPlace.setText(mOldMaterial.getMaterialPlace());
        mActComment.setText(mOldMaterial.getComment());
        Picasso.with(this).cancelRequest(mIvAddPhoto);
        Picasso.with(this).load(new File(mOldMaterial.getMaterialPicPath())).fit().into(mIvAddPhoto);
        setBarcodeInfo(mOldMaterial.getBarcodeFormat(), mOldMaterial.getBarcode());
    }

    private void setBarcodeInfo(String barcodeFormat, String barcode) {
        try {
            /* Pre-reset as default */
            mBarcode = "";
            mBarcodeFormat = "";
            Drawable barcodeDrawable = ContextCompat.getDrawable(mContext, R.drawable.selector_barcode);

            mIvBarcode.setImageDrawable(barcodeDrawable);
            mTvBarcodeTxt.setText("x xxxxxx xxxxxx x");
            Utility.releaseBitmaps(mBarcodeBitmap);
            mBarcodeBitmap = null;
            if (!TextUtils.isEmpty(barcodeFormat) && !TextUtils.isEmpty(barcode)) {
                mBarcode = barcode;
                mBarcodeFormat = barcodeFormat;
                mBarcodeBitmap = BarCodeUtility
                        .encodeAsBitmap(barcode, BarcodeFormat.valueOf(mBarcodeFormat), 600, 300);

                barcodeDrawable = new BitmapDrawable(mContext.getResources(), mBarcodeBitmap);

                mTvBarcodeTxt.setText(barcode);
                mIvBarcode.setImageDrawable(barcodeDrawable);
            }
        } catch (Exception e) {
            LogUtility.printStackTrace(e);
            FabricUtility.logException(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        if (mInputDialog != null && mInputDialog.isDialogShowing())
            mInputDialog.setShowState(false);
        if (mMultiChoiceDialog != null && mMultiChoiceDialog.isDialogShowing())
            mMultiChoiceDialog.setShowState(false);
        if (mSelectPhotoDialog != null && mSelectPhotoDialog.isDialogShowing())
            mSelectPhotoDialog.setShowState(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Utility.releaseBitmaps(mNewestBitmap);
        super.onDestroy();
    }

    private void initSpinnerData() {
        if (mMaterialTypes == null) {
            return;
        }

        for (String type : mMaterialTypes)
            DBUtility.insertMaterialTypeInfo(type);

        ArrayList<String> spinList = DBUtility.selectMaterialTypeInfo();

        spinList.add(getString(R.string.item_spinner_new_add));
        spinList.add(getString(R.string.item_spinner_del));

        if (spinList.size() == 2) {
            spinList.add(0, getString(R.string.item_spinner_empty));
        }

        if (mCategoryAdapter == null) {
            mCategoryAdapter = new ArrayAdapter<String>(this, R.layout.view_spinner_item_layout, spinList) {
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);

                    ((TextView) v).setGravity(Gravity.CENTER);
                    changeLayoutConfig(v);

                    return v;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);

                    changeLayoutConfig(v);

                    return v;
                }
            };
        } else {
            mCategoryAdapter.clear();
            mCategoryAdapter.addAll(spinList);
        }

        mSpinMaterialCategory.setAdapter(mCategoryAdapter);
        mCategoryAdapter.notifyDataSetChanged();
    }

    private void initAutoCompleteData() {
        if (mTextHistAdapter == null) {
            mTextHistoryList = new ArrayList<String>();

            mTextHistoryList.addAll(Arrays.asList(Utility.getStringValueForKey(Utility.INPUT_TEXT_HISTORY).split(":")));

            mTextHistAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, mTextHistoryList);
        } else {
            mTextHistAdapter.clear();
            mTextHistAdapter.addAll(mTextHistoryList);
        }
        mActMaterialName.setAdapter(mTextHistAdapter);
        mActMaterialPlace.setAdapter(mTextHistAdapter);
        mActComment.setAdapter(mTextHistAdapter);
        mTextHistAdapter.notifyDataSetChanged();
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
        setMenuItemVisibility(R.id.menu_sort_by_date, false);
        setMenuItemVisibility(R.id.menu_sort_by_name, false);
        setMenuItemVisibility(R.id.menu_sort_by_place, false);
        setMenuItemVisibility(R.id.menu_grid_1x1, false);
        setMenuItemVisibility(R.id.menu_grid_2x1, false);
        setMenuItemVisibility(R.id.menu_clear_expired_items, false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
            }
            break;

            case R.id.menu_action_add: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermissions(PERM_REQ_WRITE_EXT_STORAGE, getString(R.string.perm_rationale_write_ext_storage), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return super.onOptionsItemSelected(item);
                }

                Material material = new Material();
                String notificationDays = mEtNotificationDays.getText().toString().trim();

                material.setName(mActMaterialName.getText().toString());
                material.setBarcode((mBarcode != null && !mBarcode.isEmpty()) ? mBarcode : mOldMaterial.getBarcode());
                material.setBarcodeFormat((mBarcodeFormat != null && !mBarcodeFormat.isEmpty()) ? mBarcodeFormat : mOldMaterial.getBarcodeFormat());
                material.setMaterialType((String) mSpinMaterialCategory.getSelectedItem());
                material.setIsAsPhotoType(mOldMaterial.getIsAsPhotoType());
                material.setMaterialPlace(mActMaterialPlace.getText().toString());
                material.setPurchaceDate(mPurchaceDate);
                material.setValidDate(mValidDate);
                /* We consider the user has set up the valid date in LoginMaterialFragment. */
                material.setIsValidDateSetup(1);
                material.setNotificationDays(notificationDays.isEmpty() ? 0 : Integer.parseInt(notificationDays));
                material.setMaterialPic((mNewestBitmap != null) ? mNewestBitmap : mOldMaterial.getMaterialPic());
                material.setMaterialPicPath((mNewestBitmap != null) ? "" : mOldMaterial.getMaterialPicPath());
                material.setComment(mActComment.getText().toString());

                String msg = isAllowSave(material);
                if (msg == null) {
                    DBUtility.deleteMaterialInfo(mOldMaterial);
                    DBUtility.insertMaterialInfo(material);
                    clearUserData();
                    showToast(getString(R.string.data_save_success));
                    finish();
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme);

                    dialog.setTitle(getResources().getString(R.string.msg_error_dialog_title));
                    dialog.setMessage(msg);
                    dialog.setPositiveButton(getString(R.string.title_positive_btn_label), null);
                    dialog.show();
                }

                updateTextHistory(material.getName(), material.getMaterialPlace(), material.getComment());
            }
            break;

            case R.id.menu_action_cancel: {
                clearUserData();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQ_CAMERA_TAKE_PIC: {
                if (Activity.RESULT_OK == resultCode) {
                    try {
                       /* Restore to original icon */
                        mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                        Utility.releaseBitmaps(mNewestBitmap);
                    } catch (OutOfMemoryError e) {
                        LogUtility.printError(e);
                        Utility.forceGC(false);
                    }

                    if (FileUtility.TEMP_PHOTO_FILE.exists()) {
                        CropImage.activity(Uri.fromFile(FileUtility.TEMP_PHOTO_FILE))
                                .setActivityTitle(mResources.getString(R.string.app_name))
                                .start(this);
                    }
                }
            }
            break;

            case REQ_SELECT_PICTURE: {
                if (Activity.RESULT_OK == resultCode && intent != null && intent.getData() != null) {
                    /* Restore to original icon */
                    mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
                    Utility.releaseBitmaps(mNewestBitmap);
                    Uri selectedImageUri = intent.getData();

                    /* Error handling */
                    if (selectedImageUri != null) {
                        CropImage.activity(selectedImageUri)
                                .setActivityTitle(mResources.getString(R.string.app_name))
                                .start(this);
                    }
                }
            }
            break;

            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    CropImage.ActivityResult result = CropImage.getActivityResult(intent);
                    File photoFile = new File(Utility.getPathFromUri(result.getUri()));
                    mNewestBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), mOptions);

                    mIvAddPhoto.setImageBitmap(mNewestBitmap);
                }
            }
            break;

            case IntentIntegrator.REQUEST_CODE: {
            /* For barcode scanner */
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

                if (scanResult != null) {
                    String barcode = scanResult.getContents();
                    String barcodeFormat = scanResult.getFormatName();

                    if (!TextUtils.isEmpty(barcodeFormat) && !TextUtils.isEmpty(barcode)) {
                        setBarcodeInfo(barcodeFormat, barcode);
                    }
                }
            }
            break;
        }
    }

    public void setMenuItemVisibility(int id, boolean visible) {
        if (mOptionMenu != null) {
            MenuItem item = mOptionMenu.findItem(id);

            if (item != null)
                item.setVisible(visible);
        }
    }

    private void clearUserData() {
        if (mTvPurchaceDate == null || mTvValidDate == null
                || mIvAddPhoto == null || mActMaterialName == null
                || mActMaterialPlace == null || mActComment == null
                || mEtNotificationDays == null || mTvBarcodeTxt == null) {
            return;
        }

        mPurchaceDate = Calendar.getInstance();
        mValidDate = Calendar.getInstance();
        mNewestBitmap = ((BitmapDrawable) mResources.getDrawable(R.drawable.ic_no_image_available)).getBitmap();
        Drawable defaultBarcodeImg = mResources.getDrawable(R.drawable.selector_barcode);
        mBarcode = "";
        mBarcodeFormat = "";

        defaultBarcodeImg.setBounds(0, 0, defaultBarcodeImg.getIntrinsicWidth(), defaultBarcodeImg.getIntrinsicHeight());

        /* set the default time */
        mTvPurchaceDate.setText(Utility.transDateToString(mDateFormat, mPurchaceDate.getTime()));
        mTvValidDate.setText(Utility.transDateToString(mDateFormat, mValidDate.getTime()));
        mIvAddPhoto.setImageResource(R.drawable.selector_add_photo_status);
        mActMaterialName.setText("");
        mActMaterialPlace.setText("");
        mActComment.setText("");
        mEtNotificationDays.setText("");
        setBarcodeInfo(null, null);
    }

    private String isAllowSave(Material material) {
        StringBuilder msg = new StringBuilder(mResources.getString(R.string.msg_error_msg_title));
        String materialType = material.getMaterialType().trim();
        String materialName = material.getName().trim();
        boolean isAllow = true;

        if (materialType.isEmpty()
                || materialType.equals(this.getResources().getString(R.string.item_spinner_del))) {
            msg.append(getString(R.string.msg_error_no_spercify_material_type));
            isAllow = isAllow && false;
        }
        if (material.getPurchaceDate().after(material.getValidDate())) {
            msg.append(getString(R.string.msg_error_no_correct_valid_date));
            isAllow = isAllow && false;
        }

        if (materialName.isEmpty()) {
            msg.append(getString(R.string.msg_error_no_material_name));
            isAllow = isAllow && false;
        }

        if (sMaterialNameErrorPattern.matcher(materialName).matches()) {
            msg.append(getString(R.string.msg_error_special_material_naming));
            isAllow = isAllow && false;
        }

        return !isAllow ? msg.toString() : null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    /* which < 0, then the dialog has positive/negative button */
        if (which < 0) {
            if (AlertDialog.BUTTON_POSITIVE == which) {
                if (mInputDialog != null && mInputDialog.isDialogShowing()) {
                    String input = mInputDialog.getInputString();

                    if (input.trim().isEmpty()) {
                        mSpinMaterialCategory.setSelection(0);
                        return;
                    }

                    mMaterialTypes.add(input);
                    mInputDialog.setShowState(false);
                    initSpinnerData();

                    int index = ((ArrayAdapter<String>) mSpinMaterialCategory.getAdapter()).getPosition(input);
                    mSpinMaterialCategory.setSelection(index);
                } else if (mMultiChoiceDialog != null && mMultiChoiceDialog.isDialogShowing()) {
                    final String[] selectedItems = mMultiChoiceDialog.getSelectedItemsString();

                    if (selectedItems == null || selectedItems.length == 0) {
                        mSpinMaterialCategory.setSelection(0);
                        return;
                    }

                    mMultiChoiceDialog.setShowState(false);

                    AlertDialog.Builder subDialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme);

                    subDialog.setTitle(getResources().getString(R.string.msg_remind_title));
                    subDialog.setMessage(getResources().getString(R.string.msg_remind_delete_material_type_title));
                    subDialog.setPositiveButton(getString(R.string.title_positive_btn_label),
                            (dialoginterface, i) -> {
                                for (String item : selectedItems) {
                                    if (item.trim().isEmpty())
                                        continue;

                                    mMaterialTypes.remove(item);
                                    DBUtility.delMaterialTypeInfo(item);
                                    initSpinnerData();
                                    mSpinMaterialCategory.setSelection(0);
                                }
                            });
                    subDialog.setNegativeButton(getString(R.string.title_negative_btn_label), null);
                    subDialog.show();
                }
            } else if (AlertDialog.BUTTON_NEGATIVE == which) {
                if (mMultiChoiceDialog != null || mInputDialog != null) {
                    mSpinMaterialCategory.setSelection(0);
                }

                mDatePickerDialog = null;
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermissionGranted(Manifest.permission.CAMERA)) {
                        requestPermissions(MMActivity.PERM_REQ_CAMERA, getString(R.string.perm_rationale_camera), Manifest.permission.CAMERA);
                        return;
                    }

                    Intent intent = new Intent(this, CameraActivity.class);

                    startActivityForResult(intent, REQ_CAMERA_TAKE_PIC);
//                    /* from camera */
//                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                    Uri tmpPhotoUri = null;
//                    /**
//                     *  If your targetSdkVersion is 24 or higher, you can not use file: Uri values in Intents on Android 7.0+ devices.
//                     * */
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        tmpPhotoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", FileUtility.TEMP_PHOTO_FILE);
//                    } else {
//                        tmpPhotoUri = Uri.fromFile(FileUtility.TEMP_PHOTO_FILE);
//                    }
//                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tmpPhotoUri);
//                    startActivityForResult(takePictureIntent, REQ_CAMERA_TAKE_PIC);
                }
            }
        }

        mMultiChoiceDialog = null;
        mInputDialog = null;
        mSelectPhotoDialog = null;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.rl_purchace_date_layout:
            case R.id.rl_validate_date_layout: {
                mCurPressDateBtnId = id;
                Calendar calendar = Calendar.getInstance();
                mDatePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                mDatePickerDialog.show(getFragmentManager(), DATEPICKER_TAG);
            }
            break;

            case R.id.iv_barcode: {
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.initiateScan();
            }
            break;
            case R.id.iv_add_photo: {
                mSelectPhotoDialog = new SelectPhotoDialog(this, getString(R.string.title_select_photo), new String[]{
                        getString(R.string.title_select_photo_from_album),
                        getString(R.string.title_select_photo_from_camera)}, this);

                mSelectPhotoDialog.show();
            }
            break;
        }
    }

    private void updateTextHistory(String... textAry) {
        StringBuilder textHistory = new StringBuilder("");

        for (String text : textAry) {
            if (!mTextHistoryList.contains(text)) {
                mTextHistoryList.add(text);
            }
        }

        for (String text : mTextHistoryList) {
            textHistory.append(text);
            textHistory.append(":");
        }
        textHistory.deleteCharAt(textHistory.length() - 1);
        Utility.setStringValueForKey(Utility.INPUT_TEXT_HISTORY, textHistory.toString());
        initAutoCompleteData();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        String item = (String) adapterView.getSelectedItem();
        String firstItem = (String) adapterView.getItemAtPosition(0);
        boolean isCanDel = !firstItem.trim().isEmpty() ? true : false;

        if (item.equals(getString(R.string.item_spinner_new_add))) {
            mInputDialog = new InputDialog(this, getString(R.string.title_input_dialog),
                    getString(R.string.title_input_dialog_body), this);
            mInputDialog.show();
        } else if (item.equals(getString(R.string.item_spinner_del)) && isCanDel) {
            SpinnerAdapter adapter = mSpinMaterialCategory.getAdapter();
            int itemCount = adapter.getCount();
            String[] items = new String[itemCount - 2];

            for (int i = 0; i < itemCount - 2; i++)
                items[i] = (String) adapter.getItem(i);

            mMultiChoiceDialog = new MultiChoiceDialog(this, getString(R.string.title_single_choice_dialog),
                    items, this);
            mMultiChoiceDialog.show();
        } else if (item.equals(getString(R.string.item_spinner_del)) && !isCanDel)
            /* for fixing a bug, but need to be refactor */
            mSpinMaterialCategory.setSelection(0);
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();

        cal.set(year, monthOfYear, dayOfMonth);

        if (mCurPressDateBtnId >= 0) {
            if (mCurPressDateBtnId == R.id.rl_purchace_date_layout) {
                mPurchaceDate = cal;
                mTvPurchaceDate.setText(Utility.transDateToString(mDateFormat, cal.getTime()));
            } else if (mCurPressDateBtnId == R.id.rl_validate_date_layout) {
                mValidDate = cal;
                mTvValidDate.setText(Utility.transDateToString(mDateFormat, cal.getTime()));
            }
            mDatePickerDialog = null;
            mCurPressDateBtnId = -1;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
