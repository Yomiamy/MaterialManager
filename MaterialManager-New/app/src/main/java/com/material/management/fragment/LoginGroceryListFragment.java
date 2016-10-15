package com.material.management.fragment;

import android.Manifest;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.loopj.android.http.RequestParams;
import com.material.management.MMActivity;
import com.material.management.MMFragment;
import com.material.management.MainActivity;
import com.material.management.Observer;
import com.material.management.StoreMapActivity;
import com.material.management.R;
import com.material.management.api.module.ConnectionControl;
import com.material.management.data.GroceryItem;
import com.material.management.data.GroceryListData;
import com.material.management.data.StoreData;
import com.material.management.utils.DBUtility;
import com.material.management.utils.LogUtility;
import com.material.management.utils.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoginGroceryListFragment extends MMFragment implements Observer, TimePickerDialog.OnTimeSetListener, CompoundButton.OnCheckedChangeListener {

    public static final String ACTION_BAR_BTN_ACTION_ADD = "add_material";
    public static final String ACTION_BAR_BTN_ACTION_CLEAR = "clear_user_input";
    private static final String REQ_PLACE_SEARCH = "geo_transform";
    private static final String REQ_QUERY_RECEIPT_INFO = "query_receipt_info";
    private static final String TIMEPICKER_TAG = "timepicker";
    private static MainActivity sActivity;

    private View mLayout;
    private RelativeLayout mRlLoginListByReceipt;
    private AutoCompleteTextView mActGroceryListName;
    private AutoCompleteTextView mActStoreName;
    private AutoCompleteTextView mActAddress;
    private AutoCompleteTextView mActPhone;
    private ImageView mIvReceiptBarcode;
    private ImageView mIvStoreAddress;
    private ImageView mIvPhone;
    private ImageView mIvNavig;
    private CheckBox mCbNearbyAlert;
    private Button mBtnSun, mBtnPMSun;
    private Button mBtnMon, mBtnPMMon;
    private Button mBtnTue, mBtnPMTue;
    private Button mBtnWed, mBtnPMWed;
    private Button mBtnThu, mBtnPMThu;
    private Button mBtnFri, mBtnPMFri;
    private Button mBtnSat, mBtnPMSat;
    private Button mBtnCurTimeSetting;

    private TimePickerDialog mTimePickerDialog = null;
    private ArrayAdapter<String> mTextHistAdapter = null;
    private ArrayList<String> mTextHistoryList;
    private ArrayList<GroceryItem> mReceiptItemList = null;
    private String mTitle;
    private String mCurModifiedAddress;
    private StoreData mSelectedStoreData = null;
    private String mReceiptNum = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayout = inflater.inflate(R.layout.fragment_grocery_login, container, false);
        sActivity = (MainActivity) getActivity();

        findView();
        init();
        setListener();

        return mLayout;
    }

    @Override
    public void onResume() {
        sendScreenAnalytics(getString(R.string.ga_app_view_grocery_list_login_fragment));

        super.onResume();
    }

    private void findView() {
        mRlLoginListByReceipt = (RelativeLayout) mLayout.findViewById(R.id.rl_login_grocery_by_receipt);
        mIvReceiptBarcode = (ImageView) mLayout.findViewById(R.id.iv_login_receipt_barcode);
        mIvPhone = (ImageView) mLayout.findViewById(R.id.iv_phoneButton);
        mIvNavig = (ImageView) mLayout.findViewById(R.id.iv_navigateButton);
        mIvStoreAddress = (ImageView) mLayout.findViewById(R.id.iv_store_address);
        mActGroceryListName = (AutoCompleteTextView) mLayout.findViewById(R.id.act_grocery_list_name);
        mActStoreName = (AutoCompleteTextView) mLayout.findViewById(R.id.act_store_name);
        mActAddress = (AutoCompleteTextView) mLayout.findViewById(R.id.act_addressText);
        mActPhone = (AutoCompleteTextView) mLayout.findViewById(R.id.act_phoneText);
        mCbNearbyAlert = (CheckBox) mLayout.findViewById(R.id.cb_nearby_alert_enable);
        mBtnSun = (Button) mLayout.findViewById(R.id.btn_sunTimings);
        mBtnPMSun = (Button) mLayout.findViewById(R.id.btn_sunPMTimings);
        mBtnMon = (Button) mLayout.findViewById(R.id.btn_monTimings);
        mBtnPMMon = (Button) mLayout.findViewById(R.id.btn_monPMTimings);
        mBtnTue = (Button) mLayout.findViewById(R.id.btn_tueTimings);
        mBtnPMTue = (Button) mLayout.findViewById(R.id.btn_tuePMTimings);
        mBtnWed = (Button) mLayout.findViewById(R.id.btn_wedTimings);
        mBtnPMWed = (Button) mLayout.findViewById(R.id.btn_wedPMTimings);
        mBtnThu = (Button) mLayout.findViewById(R.id.btn_thuTimings);
        mBtnPMThu = (Button) mLayout.findViewById(R.id.btn_thuPMTimings);
        mBtnFri = (Button) mLayout.findViewById(R.id.btn_friTimings);
        mBtnPMFri = (Button) mLayout.findViewById(R.id.btn_friPMTimings);
        mBtnSat = (Button) mLayout.findViewById(R.id.btn_satTimings);
        mBtnPMSat = (Button) mLayout.findViewById(R.id.btn_satPMTimings);

    }

    private void init() {
        CharSequence actionBarTitle = mOwnerActivity.getActionBar().getTitle();
        /* A workaround to avoid the NullPointerException for actionbar title. */
        mTitle = (actionBarTitle != null) ? actionBarTitle.toString() : "";
        Calendar calendar = Calendar.getInstance();
        mTimePickerDialog = TimePickerDialog.newInstance(this, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);

        if (!mDeviceInfo.getLocale().equalsIgnoreCase("TW")) {
            mRlLoginListByReceipt.setVisibility(View.GONE);
        }
        update(null);
        initAutoCompleteData();
        clearUserData();
        changeLayoutConfig(mLayout);
    }

    private void setListener() {
        mIvReceiptBarcode.setOnClickListener(this);
        mIvPhone.setOnClickListener(this);
        mIvNavig.setOnClickListener(this);
        mIvStoreAddress.setOnClickListener(this);
        mBtnSun.setOnClickListener(this);
        mBtnPMSun.setOnClickListener(this);
        mBtnMon.setOnClickListener(this);
        mBtnPMMon.setOnClickListener(this);
        mBtnTue.setOnClickListener(this);
        mBtnPMTue.setOnClickListener(this);
        mBtnWed.setOnClickListener(this);
        mBtnPMWed.setOnClickListener(this);
        mBtnThu.setOnClickListener(this);
        mBtnPMThu.setOnClickListener(this);
        mBtnFri.setOnClickListener(this);
        mBtnPMFri.setOnClickListener(this);
        mBtnSat.setOnClickListener(this);
        mBtnPMSat.setOnClickListener(this);
        mCbNearbyAlert.setOnCheckedChangeListener(this);
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

    private void initAutoCompleteData() {
        if (mTextHistAdapter == null) {
            mTextHistoryList = new ArrayList<String>();

            mTextHistoryList.addAll(Arrays.asList(Utility.getStringValueForKey(Utility.INPUT_TEXT_HISTORY).split(":")));

            mTextHistAdapter = new ArrayAdapter<String>(sActivity, android.R.layout.simple_dropdown_item_1line, mTextHistoryList);
        } else {
            mTextHistAdapter.clear();
            mTextHistAdapter.addAll(mTextHistoryList);
        }
        mActGroceryListName.setAdapter(mTextHistAdapter);
        mActStoreName.setAdapter(mTextHistAdapter);
        mActAddress.setAdapter(mTextHistAdapter);
        mActPhone.setAdapter(mTextHistAdapter);
        mTextHistAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.cb_nearby_alert_enable && isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !mOwnerActivity.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            mOwnerActivity.requestPermissions(MMActivity.PERM_REQ_ACCESS_FINE_LOCATION, getString(R.string.perm_rationale_location), Manifest.permission.ACCESS_FINE_LOCATION);
            buttonView.setChecked(false);
            return;
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        v.startAnimation(AnimationUtils.loadAnimation(sActivity, R.anim.anim_press_bounce));
        switch (id) {
            case R.id.iv_login_receipt_barcode: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && (!mOwnerActivity.isPermissionGranted(Manifest.permission.CAMERA) || !mOwnerActivity.isPermissionGranted(Manifest.permission.READ_PHONE_STATE))) {
                    if (!mOwnerActivity.isPermissionGranted(Manifest.permission.CAMERA)) {
                        mOwnerActivity.requestPermissions(MMActivity.PERM_REQ_CAMERA, mResources.getString(R.string.perm_rationale_camera), Manifest.permission.CAMERA);
                    }

                    if (!mOwnerActivity.isPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {
                        mOwnerActivity.requestPermissions(MMActivity.PERM_REQ_READ_PHONE_STATE, getString(R.string.perm_rationale_read_phone_state), Manifest.permission.READ_PHONE_STATE);
                    }
                    return;
                }
                showToast(mResources.getString(R.string.receipt_scan_hint_msg));
                IntentIntegrator integrator = new IntentIntegrator(LoginGroceryListFragment.this);
                integrator.initiateScan();
            }
            break;

            case R.id.iv_phoneButton: {
                String phone = Uri.encode(mActPhone.getText().toString());

                if (phone.isEmpty()) {
                    showToast(getString(R.string.grocery_login_err_empty_phone));
                } else {
                    Intent intent = new Intent(Intent.ACTION_DIAL);

                    intent.setData(Uri.parse("tel:" + phone));
                    startActivity(intent);
                }
            }
            break;

            case R.id.iv_navigateButton: {
                mCurModifiedAddress = mActAddress.getText().toString();

                if (mCurModifiedAddress.isEmpty()) {
                    showToast(getString(R.string.grocery_login_err_empty_store_address));
                } else {
                    RequestParams reqParams = new RequestParams();

                    reqParams.put("address", mCurModifiedAddress);
                    reqParams.put("sensor", Boolean.toString(true));
                    reqParams.put("language", mDeviceInfo.getLanguage() + "-" + mDeviceInfo.getLocale());

                    mControl.getData(ConnectionControl.GEO_ADDRESS_TRANSFORM, LoginGroceryListFragment.this, reqParams, REQ_PLACE_SEARCH);

                    showProgressDialog(null, getString(R.string.grocery_login_msg_geo_trans));
                }
            }
            break;

            case R.id.iv_store_address: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !mOwnerActivity.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    mOwnerActivity.requestPermissions(MMActivity.PERM_REQ_ACCESS_FINE_LOCATION, getString(R.string.perm_rationale_location), Manifest.permission.ACCESS_FINE_LOCATION);
                    return;
                }

                Intent intent = new Intent(sActivity, StoreMapActivity.class);

                intent.putExtra("title", mTitle);
                startActivityForResult(intent, 0);
            }
            break;

            case R.id.btn_sunTimings:
            case R.id.btn_sunPMTimings:
            case R.id.btn_monTimings:
            case R.id.btn_monPMTimings:
            case R.id.btn_tueTimings:
            case R.id.btn_tuePMTimings:
            case R.id.btn_wedTimings:
            case R.id.btn_wedPMTimings:
            case R.id.btn_thuTimings:
            case R.id.btn_thuPMTimings:
            case R.id.btn_friTimings:
            case R.id.btn_friPMTimings:
            case R.id.btn_satTimings:
            case R.id.btn_satPMTimings: {
                mBtnCurTimeSetting = (Button) v;
                mTimePickerDialog.show(mOwnerActivity.getFragmentManager(), TIMEPICKER_TAG);
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            // Get barcode and request the receipt information
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

            if (scanResult != null) {
                String code = scanResult.getContents();
                String codeFormat = scanResult.getFormatName();

                if (code != null && codeFormat != null) {
                    LogUtility.printLogD("randy", "barcode format = " + codeFormat);
                    LogUtility.printLogD("randy", "barcode = " + code);
                    LogUtility.printLogD("randy", "UUID = " + Utility.getUUID(mOwnerActivity));

                    RequestParams params = new RequestParams();

                    // TODO: It maybe need to be refactored
                    if (codeFormat.equals(BarcodeFormat.QR_CODE.name())) {
                        if (code.length() < 77) {
                            showToast(mResources.getString(R.string.grocery_login_err_receipt_code_format_incorrect));
                            return;
                        }
                        params.put("type", "QRCode");
                        params.put("invNum", code.substring(0, 10));
                        params.put("encrypt", code.substring(53, 77));
                        params.put("sellerID", code.substring(45, 53));
                        params.put("invDate", Utility.convertTWDate(code.substring(10, 17), "yyyMMdd", "yyyy/MM/dd"));
                        params.put("randomNumber", code.substring(17, 21));
                    } else if (codeFormat.equals(BarcodeFormat.CODE_39)) {
                        if (code.length() < 19) {
                            showToast(mResources.getString(R.string.grocery_login_err_receipt_code_format_incorrect));
                            return;
                        }
                        params.put("type", "Barcode");
                        params.put("invTerm", code.substring(0, 5));
                        params.put("invNum", code.substring(5, 15));
                        // TODO: where is th invData in barcode?
                        // params.put("invDate", );
                        params.put("randomNumber", code.substring(15, 19));
                    }
                    params.put("version", "0.3");
                    params.put("action", "qryInvDetail");
                    params.put("generation", "V2");
                    params.put("UUID", Utility.getUUID(mOwnerActivity));
                    params.put("appID", mResources.getString(R.string.receipt_app_id));

                    showProgressDialog(null, mResources.getString(R.string.grocery_login_msg_query_receipt_info));
                    mControl.postData(ConnectionControl.TW_RECEIPT_INFO, LoginGroceryListFragment.this, params, REQ_QUERY_RECEIPT_INFO);
                }
            }
        } else {
            if (data == null) {
                mActStoreName.setText("");
                mActPhone.setText("");
                mActAddress.setText("");
                return;
            }
            mSelectedStoreData = data.getParcelableExtra("store_data");
            updateStoreInfo();
        }
    }

    private void updateStoreInfo() {
        mActStoreName.setText((mSelectedStoreData != null) ? mSelectedStoreData.getStoreName() : "");
        mActAddress.setText((mSelectedStoreData != null) ? mSelectedStoreData.getStoreAddress() : "");
        mActPhone.setText((mSelectedStoreData != null) ? mSelectedStoreData.getStorePhone() : "");
        String serviceTime = mSelectedStoreData.getStoreServiceTime();
        String defaultStr = getString(R.string.title_service_time_default);

        mBtnSun.setText(defaultStr);
        mBtnPMSun.setText(defaultStr);
        mBtnMon.setText(defaultStr);
        mBtnPMMon.setText(defaultStr);
        mBtnTue.setText(defaultStr);
        mBtnPMTue.setText(defaultStr);
        mBtnWed.setText(defaultStr);
        mBtnPMWed.setText(defaultStr);
        mBtnThu.setText(defaultStr);
        mBtnPMThu.setText(defaultStr);
        mBtnFri.setText(defaultStr);
        mBtnPMFri.setText(defaultStr);
        mBtnSat.setText(defaultStr);
        mBtnPMSat.setText(defaultStr);

        if (serviceTime != null && !serviceTime.isEmpty()) {
            String[] daysHours = serviceTime.split("\\|");
            Pattern patt = Pattern.compile("(\\d+)#(\\-?\\d+),(\\-?\\d+)");


            for (String dayHour : daysHours) {
                Matcher match = patt.matcher(dayHour);

                if (match.matches()) {
                    int day = Integer.parseInt(match.group(1));
                    String startTime = match.group(2);
                    String endTime = match.group(3);
                    startTime = (Integer.parseInt(startTime) < 0) ? defaultStr : startTime;
                    endTime = (Integer.parseInt(endTime) < 0) ? defaultStr : endTime;

                    switch (day) {
                        case 0:
                            mBtnSun.setText(startTime);
                            mBtnPMSun.setText(endTime);
                            break;
                        case 1:
                            mBtnMon.setText(startTime);
                            mBtnPMMon.setText(endTime);
                            break;
                        case 2:
                            mBtnTue.setText(startTime);
                            mBtnPMTue.setText(endTime);
                            break;
                        case 3:
                            mBtnWed.setText(startTime);
                            mBtnPMWed.setText(endTime);
                            break;
                        case 4:
                            mBtnThu.setText(startTime);
                            mBtnPMThu.setText(endTime);
                            break;
                        case 5:
                            mBtnFri.setText(startTime);
                            mBtnPMFri.setText(endTime);
                            break;
                        case 6:
                            mBtnSat.setText(startTime);
                            mBtnPMSat.setText(endTime);
                            break;
                    }
                }
            }
        }
    }

    private String fixTime(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }


    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
        mBtnCurTimeSetting.setText(fixTime(hourOfDay) + fixTime(minute));
    }

    @Override
    public void callbackFromController(JSONObject jsonObj, String req) {
        try {
            if (req.equals(REQ_PLACE_SEARCH)) {
                String status = jsonObj.getString("status");

                if (status.equals("OK")) {
                    JSONArray resultAry = jsonObj.getJSONArray("results");

                    for (int i = 0, len = resultAry.length(); i < len; i++) {
                        JSONObject geoJsonObject = resultAry.getJSONObject(i);
                        String formattedAddress = geoJsonObject.getString("formatted_address");

                        if (formattedAddress.contains(mCurModifiedAddress)) {
                            JSONObject locJsonObject = geoJsonObject.getJSONObject("geometry").getJSONObject("location");
                            String lat = locJsonObject.getString("lat");
                            String lng = locJsonObject.getString("lng");
                            String uri = String.format("geo:%s,%s?q=%s,%s(%s)", lat, lng, lat, lng, mCurModifiedAddress);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

                            startActivity(intent);
                            break;
                        }
                    }
                } else {
//                "OK" 表示沒有發生任何錯誤；地址的剖析已成功完成且至少傳回一個地理編碼。
//                "ZERO_RESULTS" 表示地理編碼成功，但是並未傳回任何結果。如果該地理編碼收到了不存在的 address 或遠端位置的 latlng，就有可能發生這種情況。
//                "OVER_QUERY_LIMIT" 表示您超過配額了。
//                "REQUEST_DENIED" 表示您的要求已遭拒絕，通常是因為缺少 sensor 參數。
//                "INVALID_REQUEST" 一般表示查詢 (address 或 latlng) 遺失了。
                    showToast(mResources.getString(R.string.grocery_login_err_dialog_msg));

                }
            } else if (req.equals(REQ_QUERY_RECEIPT_INFO)) {
                if (jsonObj != null && jsonObj.getInt("code") == 200) {
                    mReceiptNum = jsonObj.getString("invNum");
                    String receiptDate = jsonObj.getString("invDate");
                    String storeName = jsonObj.getString("sellerName");
                    String storeAddress = jsonObj.getString("sellerAddress");
                    JSONArray groceryDetailAry = jsonObj.getJSONArray("details");
                    mReceiptItemList = new ArrayList<>();
                    ArrayList<String> materialTypeList = DBUtility.selectMaterialTypeInfo();
                    Calendar purchaseDateCal = Calendar.getInstance();

                    // TODO: Need to be refactored.
                    purchaseDateCal.setTime(Utility.transStringToDate("yyyyMMdd", receiptDate));
                    for(int i = 0, len = groceryDetailAry.length() ; i < len ; i++) {
                        JSONObject detailJsonObj = groceryDetailAry.getJSONObject(i);
                        GroceryItem groceryItem = new GroceryItem();

                        // set the -1 as the default grocery list id
                        groceryItem.setGroceryListId(-1);
                        groceryItem.setGroceryPic(((BitmapDrawable) getResources().getDrawable(R.drawable.ic_no_image_available)).getBitmap());
                        groceryItem.setBarcode("");
                        groceryItem.setBarcodeFormat("");
                        groceryItem.setName(detailJsonObj.getString("description"));
                        // use the index 0 item as default grocery list item type
                        groceryItem.setGroceryType(materialTypeList.get(0));
                        groceryItem.setSize("");
                        groceryItem.setSizeUnit("");
                        groceryItem.setQty(detailJsonObj.getString("quantity"));
                        groceryItem.setPrice(detailJsonObj.getString("unitPrice"));
                        groceryItem.setComment("");
                        groceryItem.setPurchaceDate(purchaseDateCal);
                        groceryItem.setValidDate(Calendar.getInstance());
                        mReceiptItemList.add(groceryItem);
                    }

                    mActStoreName.setText(storeName);
                    mActGroceryListName.setText(storeName + " - " + receiptDate);
                    mActAddress.setText(storeAddress);
                } else {
                    showToast(mResources.getString(R.string.grocery_receipt_barcode_err_dialog_msg));
                }
            }
        } catch (JSONException e) {
            LogUtility.printStackTrace(e);
            showToast(mResources.getString(R.string.data_progressing_fail));
        } finally {
            closeProgressDialog();
        }
    }

    @Override
    public void callbackFromController(JSONObject result, Throwable throwable, String error) {
        showToast(mResources.getString(R.string.grocery_login_err_dialog_msg));
        closeProgressDialog();
    }

    @Override
    public void update(Object data) {
        if (sActivity == null) {
            return;
        }

        if (data != null) {
            if (data.equals(ACTION_BAR_BTN_ACTION_ADD) || data.equals(ACTION_BAR_BTN_ACTION_CLEAR)) {
                if (data.equals(ACTION_BAR_BTN_ACTION_ADD)) {
                    GroceryListData groceryListData = new GroceryListData();
                    StringBuilder serviceTimeStr = new StringBuilder("");

                    groceryListData.setGroceryListName(mActGroceryListName.getText().toString());
                    groceryListData.setIsAlertWhenNearBy(mCbNearbyAlert.isChecked() ? 1 : 0);
                    groceryListData.setStoreName(mActStoreName.getText().toString());
                    groceryListData.setAddress(mActAddress.getText().toString());
                    groceryListData.setPhone(mActPhone.getText().toString());
                    /* The service time for each is  [week day]:[start time]~[end time] and each day is separated by "|"*/
                    serviceTimeStr.append("1:").append(mBtnSun.getText().toString()).append("~").append(mBtnPMSun.getText()).append("|");
                    serviceTimeStr.append("2:").append(mBtnMon.getText().toString()).append("~").append(mBtnPMMon.getText()).append("|");
                    serviceTimeStr.append("3:").append(mBtnTue.getText().toString()).append("~").append(mBtnPMThu.getText()).append("|");
                    serviceTimeStr.append("4:").append(mBtnWed.getText().toString()).append("~").append(mBtnPMWed.getText()).append("|");
                    serviceTimeStr.append("5:").append(mBtnThu.getText().toString()).append("~").append(mBtnPMThu.getText()).append("|");
                    serviceTimeStr.append("6:").append(mBtnFri.getText().toString()).append("~").append(mBtnPMFri.getText()).append("|");
                    serviceTimeStr.append("7:").append(mBtnSat.getText().toString()).append("~").append(mBtnPMSat.getText());

                    /* replace the default string in service time string.*/
                    String serviceTimeDefaultInDb = getString(R.string.title_service_time_default_in_db);
                    String serviceTimeDefault = getString(R.string.title_service_time_default);
                    int index;
                    int len = serviceTimeDefault.length();

                    while ((index = serviceTimeStr.indexOf(serviceTimeDefault)) != -1) {
                        serviceTimeStr.replace(index, index + len, serviceTimeDefaultInDb);
                    }
                    groceryListData.setServiceTime(serviceTimeStr.toString());
                    groceryListData.setLat((mSelectedStoreData != null) ? mSelectedStoreData.getStoreLat() : "");
                    groceryListData.setLong((mSelectedStoreData != null) ? mSelectedStoreData.getStoreLong() : "");
                    groceryListData.setIsAlertWhenNearBy(mCbNearbyAlert.isChecked() ? 1 : 0);
                    groceryListData.setReceiptNum(TextUtils.isEmpty(mReceiptNum) ? "" : mReceiptNum);

                    updateTextHistory(groceryListData.getGroceryListName(), groceryListData.getStoreName(), groceryListData.getPhone(), groceryListData.getAddress());
                    DBUtility.insertGroceryListInfo(groceryListData);

                    // user scan the receipt data to fill grocery list data and items automatically
                    if(!TextUtils.isEmpty(mReceiptNum)) {
                        groceryListData = DBUtility.selectGroceryListInfoByReceiptNum(mReceiptNum);

                        // TODO: it maybe cause slow performance
                        for(GroceryItem item : mReceiptItemList) {
                            item.setGroceryListId(groceryListData.getId());

                            DBUtility.insertGroceryItemInfo(item);
                        }
                    }
                    mReceiptNum = null;
                    mReceiptItemList = null;
                    showToast(getString(R.string.data_save_success));
                } else if (data.equals(ACTION_BAR_BTN_ACTION_CLEAR)) {
                }
                clearUserData();
            }

            hideSoftInput();
        } else {
            sActivity.setMenuItemVisibility(R.id.action_search, false);
            sActivity.setMenuItemVisibility(R.id.menu_action_add, true);
            sActivity.setMenuItemVisibility(R.id.menu_action_cancel, true);
            sActivity.setMenuItemVisibility(R.id.menu_action_new, false);
            sActivity.setMenuItemVisibility(R.id.menu_sort_by_date, false);
            sActivity.setMenuItemVisibility(R.id.menu_sort_by_name, false);
            sActivity.setMenuItemVisibility(R.id.menu_sort_by_place, false);
            sActivity.setMenuItemVisibility(R.id.menu_grid_1x1, false);
            sActivity.setMenuItemVisibility(R.id.menu_grid_2x1, false);
            sActivity.setMenuItemVisibility(R.id.menu_clear_expired_items, false);
        }
    }


    private void clearUserData() {
        mActGroceryListName.setText("");
        mActStoreName.setText("");
        mActAddress.setText("");
        mActPhone.setText("");
        mCbNearbyAlert.setChecked(false);

        String defaultStr = getString(R.string.title_service_time_default);

        mBtnSun.setText(defaultStr);
        mBtnPMSun.setText(defaultStr);
        mBtnMon.setText(defaultStr);
        mBtnPMMon.setText(defaultStr);
        mBtnTue.setText(defaultStr);
        mBtnPMTue.setText(defaultStr);
        mBtnWed.setText(defaultStr);
        mBtnPMWed.setText(defaultStr);
        mBtnThu.setText(defaultStr);
        mBtnPMThu.setText(defaultStr);
        mBtnFri.setText(defaultStr);
        mBtnPMFri.setText(defaultStr);
        mBtnSat.setText(defaultStr);
        mBtnPMSat.setText(defaultStr);
    }
}
