package com.material.management;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;
import com.material.management.api.module.ConnectionControl;
import com.material.management.api.module.ViewCallbackListener;
import com.material.management.data.DeviceInfo;
import com.material.management.utils.Utility;
import com.material.management.utils.permission.MultiPermissionsListener;
import com.picasso.Picasso;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;

public class MMActivity extends Activity implements ViewCallbackListener, View.OnClickListener {
    public static final String PERM_REQ_READ_EXT_STORAGE = "PERM_REQ_READ_EXT_STORAGE";
    public static final String PERM_REQ_WRITE_EXT_STORAGE = "PERM_REQ_WRITE_EXT_STORAGE";
    public static final String PERM_REQ_CAMERA = "PERM_REQ_CAMERA";
    public static final String PERM_REQ_ACCESS_FINE_LOCATION = "PERM_REQ_ACCESS_FINE_LOCATION";
    public static final String PERM_REQ_READ_PHONE_STATE = "PERM_REQ_READ_PHONE_STATE";

    protected View mLayout = null;
    protected Dialog mProgressDialog;
    protected Handler mHandler;
    protected Resources mResources;
    protected Context mContext;
    protected ConnectionControl mControl;
    protected LayoutInflater mInflater;
    protected DisplayMetrics mMetrics;
    protected InputMethodManager mImm = null;
    protected DeviceInfo mDeviceInfo;
    protected HashMap<Integer, Float> mOrigFontSizeMap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        mResources = getResources();
        mMetrics = new DisplayMetrics();
        mContext = this;
        mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mControl = ConnectionControl.getInstance();
        mDeviceInfo = Utility.getDeviceInfo();

        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    @Override
    protected void onResume() {
        if (!Utility.isApplicationInitialized()) {
            Intent intent = new Intent(this, MainActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Picasso.with(this).clearCache();
        Utility.forceGC(true);
    }

    /* It's been pair using*/
    public void showProgressDialog(String title, String content) {
        /* to avoid windowLeaked*/
        closeProgressDialog();
        mProgressDialog = ProgressDialog.show(this, title, content, true);
    }

    public void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    /* TODO: We that it use common layout and only setup message. But maybe it need to be custom in the future. */
    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = new Toast(MMActivity.this);
                View layout = mInflater.inflate(R.layout.view_default_toast, null);
                TextView tvToastMsg = (TextView) layout.findViewById(R.id.tv_toast_msg);

                tvToastMsg.setText(msg);
                toast.setView(layout);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public void showAlertDialog(final String title, final String msg,
                                final String btnPositText, final String btnNegatText,
                                final DialogInterface.OnClickListener positListener, final DialogInterface.OnClickListener negatListener,
                                final DialogInterface.OnDismissListener dismissListener) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MMActivity.this);

            builder.setTitle(title);
            builder.setMessage(msg);
            builder.setPositiveButton(btnPositText, positListener);
            builder.setNegativeButton(btnNegatText, negatListener);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                builder.setOnDismissListener(dismissListener);
            }

            Dialog dialog = builder.show();
                /* Use the id inside framework to get the message TextView*/
            TextView tvMsgText = (TextView) dialog.findViewById(android.R.id.message);

            tvMsgText.setGravity(Gravity.CENTER);

        });
    }

    public void hideKeyboard(View v) {
        mImm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    }

    @Override
    public void callbackFromController(JSONObject result) {

    }

    @Override
    public void callbackFromController(JSONObject result, String tag) {

    }

    @Override
    public void callbackFromController(JSONObject result, Throwable throwable, String error) {

    }

    @Override
    public void onClick(View v) {
        hideKeyboard(v);
    }

    /* Change layout configuration e.g.: Font size...*/
    protected void changeLayoutConfig(View layout) {
        if(mOrigFontSizeMap == null) {
            mOrigFontSizeMap = new HashMap<Integer, Float>();
        }

        storeViewFontSize(layout);
        changeFontSize(layout);
    }

    protected void storeViewFontSize(View view) {
        if(mOrigFontSizeMap == null) {
            return;
        }

        if(view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for(int i = 0, len = viewGroup.getChildCount() ; i < len ; i++) {
                storeViewFontSize(viewGroup.getChildAt(i));
            }
        } else if(view instanceof TextView && !mOrigFontSizeMap.containsKey(view.getId())){
            /* When view is added for the first time, then we use the text size as a baseline. */
            mOrigFontSizeMap.put(view.getId(), ((TextView) view).getTextSize());
        }
    }

    protected void changeFontSize(View view) {
        float adjustFontFactor = Float.parseFloat(Utility.getStringValueForKey(Utility.FONT_SIZE_SCALE_FACTOR));

        if(view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for(int i = 0, len = viewGroup.getChildCount() ; i < len ; i++) {
                changeFontSize(viewGroup.getChildAt(i));
            }
        } else if(view instanceof TextView){
            TextView targetView = (TextView) view;

            targetView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mOrigFontSizeMap.get(view.getId()) * adjustFontFactor);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected void startPermsSetting() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);

        intent.setData(uri);
        startActivityForResult(intent, 0);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean isPermissionGranted(String perm) {
        return checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(String tag, String permissionRationalMsg, String... permissions) {
        MultiPermissionsListener multiPermsListener = new MultiPermissionsListener(this, permissionRationalMsg, tag);

        if (Dexter.isRequestOngoing()) {
            return;
        }
        Dexter.checkPermissions(multiPermsListener, permissions);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showPermissionGranted(String permission, String tag) {
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showPermissionDenied(String permission, boolean isPermanentlyDenied, String tag) {
        if(isPermanentlyDenied) {
            String permRationale = "";

            if(tag.equals(PERM_REQ_WRITE_EXT_STORAGE)) {
                permRationale = getString(R.string.perm_rationale_write_ext_storage);
            } else if(tag.equals(PERM_REQ_READ_EXT_STORAGE)) {
                permRationale = getString(R.string.perm_rationale_read_ext_storage);
            } else if(tag.equals(PERM_REQ_CAMERA)) {
                permRationale = getString(R.string.perm_rationale_camera);
            } else if(tag.equals(PERM_REQ_ACCESS_FINE_LOCATION)) {
                permRationale = getString(R.string.perm_rationale_location);
            } else if(tag.equals(PERM_REQ_READ_PHONE_STATE)) {
                permRationale = getString(R.string.perm_rationale_read_phone_state);
            }

            showAlertDialog(null
                    , permRationale
                    , getString(R.string.title_positive_go_setting_btn_label)
                    , getString(R.string.title_negative_btn_label)
                    , (dialog, which) -> {
                        dialog.dismiss();
                        startPermsSetting();
                    }
                    , null
                    , null);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showPermissionRationale(final PermissionToken token, final String permRationale) {
        showAlertDialog(null, permRationale
                , getString(R.string.title_positive_btn_label)
                , getString(R.string.title_negative_btn_label)
                , (dialog, which) -> {
                    dialog.dismiss();
                    token.continuePermissionRequest();
                }, (dialog, which) -> {
                    dialog.dismiss();
                    token.cancelPermissionRequest();
                }, (dialog) -> {
                    token.cancelPermissionRequest();
                });
    }
}
