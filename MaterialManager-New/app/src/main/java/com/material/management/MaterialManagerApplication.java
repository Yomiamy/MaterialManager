package com.material.management;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.karumi.dexter.Dexter;
import com.material.management.output.NotificationOutput;
import com.material.management.utils.FabricUtility;
import com.material.management.utils.LogUtility;
import com.material.management.utils.Utility;
import com.picasso.Picasso;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import io.fabric.sdk.android.Fabric;
import java.io.File;
import java.util.HashMap;

public class MaterialManagerApplication extends MultiDexApplication {

    // The following line should be changed to include the correct property id.
    private static final String PROPERTY_ID = "UA-55274552-2";
    public static int GENERAL_TRACKER = 0;

    public static final String TAG = "MaterialManager-Debug";
    public static final String PHOTO_DIR_NAME = "com.material";
    public static final String DB_DIR_NAME = "com.material.management";
    public static final String DB_DROPBOX_PATH = "/MaterialManager/" + DB_DIR_NAME + "/";
    public static final String PHOTO_DROPBOX_PATH = "/MaterialManager/" + PHOTO_DIR_NAME + "/";

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    private HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dexter.initialize(this);
        }

        FabricUtility.connectFabric(this);
        /* Iinit context for Utility */
        Utility.setApplicationContext(this.getApplicationContext());
        /* Init Notification Utility */
        NotificationOutput.initInstance(Utility.getContext());

        File dbDir = new File(Utility.getExternalStorageDir(), DB_DIR_NAME);
        File photoDir = new File(Utility.getExternalStorageDir(), PHOTO_DIR_NAME);

        if (!dbDir.exists()) {
            dbDir.mkdir();
        }

        if(!photoDir.exists()) {
            photoDir.mkdir();
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {
                Picasso.with(MaterialManagerApplication.this).clearCache();
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }


    public synchronized Tracker getTracker(TrackerName trackerId) {
        try {
            if (!mTrackers.containsKey(trackerId)) {

                GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
                Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(R.xml.app_tracker) : analytics.newTracker(PROPERTY_ID);
                mTrackers.put(trackerId, t);

            }
            return mTrackers.get(trackerId);
        } catch (Exception e) {
            LogUtility.printStackTrace(e);
        }
        return null;
    }

}
