package com.material.management.utils;

import android.content.Context;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by yomi on 16/2/13.
 */
public class FabricUtility {

    /**
     * Connect to fabric
     *
     * @param ctx
     */
    public static void connectFabric(Context ctx) {
        Fabric.with(ctx, new Crashlytics());
    }

    /**
     * Upload the customized string information to fabric
     *
     * @param key The information mapping key.
     * @param str The information value of mapping key.
     */
    public static void logString(String key, String str) {
        Crashlytics.setString(key, str);
    }

    /**
     * Used to upload the user name and identify to fabric for tracking
     *
     * @param userName The login user name, here is IM ID.
     * @param identify The unique identify login user, here is UUID.
     */
    public static void logUserInfo(String userName, String identify) {
        Crashlytics.setUserName(userName);
        Crashlytics.setUserIdentifier(identify);
    }
}
