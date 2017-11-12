package com.material.management.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.material.management.BuildConfig;
import com.material.management.MainActivity;
import com.material.management.MaterialManagerApplication;
import com.material.management.R;
import com.material.management.data.DeviceInfo;
import com.material.management.service.location.LocationUtility;
import com.material.management.utils.openudid.OpenUDID_manager;

public class Utility {
    /* Share preference name. */
    private static final String SETTING = "setting_config";
    public static final String FONT_SIZE_SCALE_FACTOR = "font_size_scale_factor";
    public static final String INPUT_TEXT_HISTORY = "input_text_history";
    public static final String DB_UPGRADE_FLAG_1to2 = "db_upgrade_flag_1_to_2";
    public static final String DB_UPGRADE_FLAG_2to3 = "db_upgrade_flag_2_to_3";
    public static final String DB_UPGRADE_FLAG_3to4 = "db_upgrade_flag_3_to_4";
    public static final String DB_UPGRADE_FLAG_4to5 = "db_upgrade_flag_4_to_5";
    public static final String DB_UPGRADE_FLAG_5to6 = "db_upgrade_flag_5_to_6";
    public static final String CATEGORY_IS_INITIALIZED = "category_is_initialized";
    public static final String SHARE_IS_INITIALIZED = "share_is_initialized";
    public static final String SHARE_AUTO_COMPLETE_TEXT = "share_auto_complete_text";
    public static final String MATERIAL_TYPE_GRID_COLUMN_NUM = "grid_column_num";
    public static final String NOTIF_IS_VIBRATE_SOUND = "is_notif_vibrate_sound";
    public static final String NOTIF_FREQUENCY = "notif_freq";
    public static final String SHARE_PREF_KEY_MATERIAL_SORT_MODE = "share_pref_key_material_sort_mode";
    public static final String SHARE_PREF_KEY_CURRENCY_SYMBOL = "share_pref_key_currency_symbol";
    public static final String SHARE_PREF_KEY_COMPOSED_DATE_FORMAT_SYMBOL = "share_pref_key_composed_date_format_symbol";
    public static final String SYMBOL_COMPOSED_DATE_FORMAT = "::";

    private static Context sApplicationContext;
    private static Activity sActivity;
    private static SharedPreferences sSpSettings = null;
    private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static File sSdDir = Environment.getExternalStorageDirectory();
    private static Locale sLocale = Locale.getDefault();
    private static PowerManager.WakeLock sWakeLock;
    private static DisplayMetrics sDisplayMetrics = null;
    private static LocationUtility sLocUtility;
    private static DeviceInfo sDeviceInfo = new DeviceInfo();

    public static void setApplicationContext(Context context) {
        sApplicationContext = context;
        LocationUtility.init(sApplicationContext);
    }

    /* Must be initialize before using the Utility */
    public static void setMainActivity(Activity activity) {
        sActivity = activity;

//      LocationUtility.init(sActivity);
        BitmapUtility.init(sApplicationContext);
        sLocUtility = LocationUtility.getsInstance();
        sDeviceInfo.setDevice(Build.MANUFACTURER + " " + Build.MODEL);
        sDeviceInfo.setPlatformVersion("Android " + Build.VERSION.RELEASE);
        sDeviceInfo.setAppVersion(BuildConfig.VERSION_NAME);
        sDeviceInfo.setLanguage(Locale.getDefault().getLanguage());
        sDeviceInfo.setLocale(Locale.getDefault().getCountry());
    }

    /**
     * Used to detect whether or not the memory of application is cleared.
     * These static variables should be keeped when process alive.
     *
     * @return True indicate the memory is clear; Otherwise, is False.
     */
    public static boolean isApplicationInitialized() {
        return sActivity != null && sLocUtility != null && sDeviceInfo != null;
    }

    public static Context getContext() {
        return sApplicationContext;
    }

    public static File getExternalStorageDir() {
        return sSdDir;
    }

    public static String transDateToString(String pattern, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        return sdf.format(date);
    }

    public static String transDateToString(Date date) {
        return sSimpleDateFormat.format(date);
    }

    public static Date transStringToDate(String pattern, String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date transStringToDate(String date) {
        try {
            return sSimpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static SpannableString formatMatchedString(String text, String search) {
        if (text == null || text.isEmpty()) {
            return new SpannableString("");
        }

        SpannableString ss = new SpannableString(text);
        if (search == null || search.length() == 0)
            return ss;

        text = text.toLowerCase(sLocale);
        search = search.toLowerCase(sLocale);

        int beginIndex = text.indexOf(search);
        if (beginIndex >= 0) {

            ss.setSpan(new BackgroundColorSpan(Color.BLUE), beginIndex, beginIndex + search.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ss;
    }

    public static String convertTWDate(String AD, String beforeFormat, String afterFormat) {
        //轉年月格式
        if (AD == null) return "";
        SimpleDateFormat df4 = new SimpleDateFormat(beforeFormat);
        SimpleDateFormat df2 = new SimpleDateFormat(afterFormat);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(df4.parse(AD));
            if (cal.get(Calendar.YEAR) > 1492) cal.add(Calendar.YEAR, -1911);
            else cal.add(Calendar.YEAR, +1911);
            return df2.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T extends Number> String convertDecimalFormat(T valObj, String format) {
        DecimalFormat formatter = new DecimalFormat(format);

        return formatter.format(valObj);
    }

    public static Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = Images.Media.insertImage(sApplicationContext.getContentResolver(), inImage, "Title", null);

        return Uri.parse(path);
    }

    public static String getPathFromUri(Uri uri) {
        // just some safety built in
        if (uri == null) {
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = {Images.Media.DATA};
        Cursor cursor = sActivity.managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        // this is our fallback here
        return uri.getPath();
    }

    public static void showToast(String msg) {
        Toast.makeText(sApplicationContext, msg, Toast.LENGTH_LONG).show();
    }

    public static void changeHomeAsUp(Activity activity, int resId) {
        ImageView res = null;
        View decorView = activity.getWindow().getDecorView();

        if (Build.VERSION.SDK_INT < 21) {
            int upId = Resources.getSystem().getIdentifier("up", "id", "android");
            if (upId > 0) {
                res = (ImageView) decorView.findViewById(upId);

                res.setImageResource(resId);
            } else {

                if (android.os.Build.VERSION.SDK_INT <= 10) {
                    ViewGroup acbOverlay = (ViewGroup) ((ViewGroup) decorView).getChildAt(0);
                    ViewGroup abcFrame = (ViewGroup) acbOverlay.getChildAt(0);
                    ViewGroup actionBar = (ViewGroup) abcFrame.getChildAt(0);
                    ViewGroup abLL = (ViewGroup) actionBar.getChildAt(0);
                    ViewGroup abLL2 = (ViewGroup) abLL.getChildAt(1);
                    res = (ImageView) abLL2.getChildAt(0);
                } else if (android.os.Build.VERSION.SDK_INT > 10 && android.os.Build.VERSION.SDK_INT < 16) {
                    ViewGroup acbOverlay = (ViewGroup) ((ViewGroup) decorView).getChildAt(0);
                    ViewGroup abcFrame = (ViewGroup) acbOverlay.getChildAt(0);
                    ViewGroup actionBar = (ViewGroup) abcFrame.getChildAt(0);
                    ViewGroup abLL = (ViewGroup) actionBar.getChildAt(1);
                    res = (ImageView) abLL.getChildAt(0);
                } else {
                    ViewGroup acbOverlay = (ViewGroup) ((ViewGroup) decorView).getChildAt(0);
                    ViewGroup abcFrame = (ViewGroup) acbOverlay.getChildAt(1);
                    ViewGroup actionBar = (ViewGroup) abcFrame.getChildAt(0);
                    ViewGroup abLL = (ViewGroup) actionBar.getChildAt(0);
                    ViewGroup abF = (ViewGroup) abLL.getChildAt(0);
                    res = (ImageView) abF.getChildAt(0);
                }
                res.setImageResource(resId);
            }
        } else {
            activity.getActionBar().setHomeAsUpIndicator(resId);
        }
    }

    public static DeviceInfo getDeviceInfo() {
        return sDeviceInfo;
    }

    public static void releaseBitmaps(Bitmap... bitmaps) {
        if (bitmaps != null) {
            for (Bitmap bitmap : bitmaps) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
            forceGC(false);
        }
    }

    public static void forceGC(boolean isForce) {
        if (isForce) {
            System.runFinalization();
        }
        System.gc();
    }

    public static boolean isValidNumber(Class c, String numStr) {
        try {
            if (c == double.class || c == Double.class) {
                Double.parseDouble(numStr);
            } else if (c == int.class || c == Integer.class) {
                Integer.parseInt(numStr);
            } else if (c == float.class || c == Float.class) {
                Float.parseFloat(numStr);
            } else if (c == long.class || c == Long.class) {
                Long.parseLong(numStr);
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static boolean isLocationEnabled() {
        return sLocUtility.isLocationEnabled();
    }

    /* wake lock control */
    public synchronized static void acquire() {
        if (sWakeLock != null)
            sWakeLock.release();

        PowerManager pm = (PowerManager) sApplicationContext.getSystemService(Context.POWER_SERVICE);
        sWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, MaterialManagerApplication.TAG);
        sWakeLock.acquire();
    }

    public synchronized static void release() {
        if (sWakeLock != null)
            sWakeLock.release();
        sWakeLock = null;
    }

    public static String getUUID(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(ctx.TELEPHONY_SERVICE);

        String android_id = null;
        //To check if the initialization is over (it's asynchronous)
        if (OpenUDID_manager.isInitialized()) {
            android_id = OpenUDID_manager.getOpenUDID();
        } else {
            OpenUDID_manager.sync(ctx); //initialize the OpenUDID
            android_id = OpenUDID_manager.getOpenUDID(); //to retrieve your OpenUDID
        }

        String tmDevice = "" + tm.getDeviceId();
        String tmSerial = "" + tm.getSimSerialNumber();
        UUID deviceUuid = new UUID(android_id.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String uniqueId = deviceUuid.toString();

        return uniqueId;
    }

    public static DisplayMetrics getDisplayMetrics() {
        if (sDisplayMetrics == null) {
            Display display = ((WindowManager) sApplicationContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            sDisplayMetrics = metrics;
        }

        return sDisplayMetrics;
    }

    public static double getDist(double lat1, double lon1, double lat2,
                                 double lon2) {
        double realDistance = 0;
        Location locationA = new Location("A");
        locationA.setLatitude(lat1);
        locationA.setLongitude(lon1);
        Location locationB = new Location("B");
        locationB.setLatitude(lat2);
        locationB.setLongitude(lon2);
        realDistance = locationA.distanceTo(locationB);

        return realDistance;
    }

    public static void setBooleanValueForKey(String key, boolean value) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor PE = sSpSettings.edit();
        PE.putBoolean(key, value);
        PE.commit();
    }

    public static boolean getBooleanValueForKey(String key) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }
        return sSpSettings.getBoolean(key, false);
    }

    public static void setIntValueForKey(String key, int value) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor PE = sSpSettings.edit();
        PE.putInt(key, value);
        PE.commit();
    }

    public static int getIntValueForKey(String key) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }

        if (key.equals(MATERIAL_TYPE_GRID_COLUMN_NUM)) {
            return sSpSettings.getInt(key, 2);
        } else if (key.equals(NOTIF_FREQUENCY) || key.equals(NOTIF_FREQUENCY)) {
            return sSpSettings.getInt(key, 1);
        } else {
            return sSpSettings.getInt(key, 0);
        }
    }

    public static void setStringValueForKey(String key, String value) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }

        SharedPreferences.Editor PE = sSpSettings.edit();
        PE.putString(key, value);
        PE.commit();
    }

    public static String getStringValueForKey(String key) {
        if (sSpSettings == null) {
            sSpSettings = sApplicationContext.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        }

        if (key.equals(FONT_SIZE_SCALE_FACTOR)) {
            return sSpSettings.getString(key, "1.0");
        } else if (key.equals(SHARE_PREF_KEY_CURRENCY_SYMBOL)) {
            String currencySymbol = sSpSettings.getString(key, sActivity.getString(R.string.title_default_currency_symbol));

            return currencySymbol;
        } else if(key.equals(SHARE_PREF_KEY_COMPOSED_DATE_FORMAT_SYMBOL)) {
            String composedFormat = sSpSettings.getString(key, sApplicationContext.getResources().getStringArray(R.array.date_format_ary)[0]);

            return composedFormat;
        }
        return sSpSettings.getString(key, "");
    }

    public static boolean isNetworkConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static Uri getUriFromPath(String filePath) {
        long photoId;
        Uri photoUri = MediaStore.Images.Media.getContentUri("external");

        String[] projection = {MediaStore.Images.ImageColumns._ID};
        // TODO This will break if we have no matching item in the MediaStore.
        Cursor cursor = sApplicationContext.getContentResolver().query(photoUri, projection, MediaStore.Images.ImageColumns.DATA + " LIKE ?", new String[]{filePath}, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(projection[0]);
        photoId = cursor.getLong(columnIndex);

        cursor.close();
        return Uri.parse(photoUri.toString() + "/" + photoId);
    }
}
