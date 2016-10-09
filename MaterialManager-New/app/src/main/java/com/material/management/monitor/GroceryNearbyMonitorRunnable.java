package com.material.management.monitor;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.material.management.R;
import com.material.management.data.BundleInfo;
import com.material.management.data.GroceryListData;
import com.material.management.output.NotificationOutput;
import com.material.management.service.location.LocationUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.LogUtility;
import com.material.management.utils.Utility;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class GroceryNearbyMonitorRunnable implements Runnable {
    public static final String TAG = "GroceryNearbyMonitorRunnable";
    private static NotificationOutput sNotificationOutput = NotificationOutput.getInstance();
    /*
    /* Maximum  radius for nearby the grocery shop in kilo-meter. Default is 1000 meters
     */
    public static final double MAX_NEARBY_RADIUS = 1000;

    private DecimalFormat mKmFormat = new DecimalFormat("#.##");
    private DecimalFormat mMeterFormat = new DecimalFormat("###");
    private Context mContext;
    private int mNotifType;

    public GroceryNearbyMonitorRunnable(int notifType) {
        mContext = Utility.getContext();
        mNotifType = notifType;
    }

    @Override
    public void run() {
        ArrayList<GroceryListData> groceryListDatas = DBUtility.selectGroceryListInfos();
        Location curLoc = LocationUtility.getsInstance().getLocation();

        if(curLoc == null) {
            return;
        }

        for (GroceryListData groceryListData : groceryListDatas) {
            String storeName = groceryListData.getStoreName();
            String lat = groceryListData.getLat();
            String lng = groceryListData.getLong();
            boolean isNeedCheckNearby = (groceryListData.getIsAlertWhenNearBy() == 1);

            if(!isNeedCheckNearby || lat == null || lng == null || !Utility.isValidNumber(Double.class, lat) || !Utility.isValidNumber(Double.class, lng)) {
                continue;
            }

            double dist = Utility.getDist(curLoc.getLatitude(), curLoc.getLongitude(), Double.parseDouble(groceryListData.getLat()), Double.parseDouble(groceryListData.getLong()));
            boolean isNearby = (dist >= MAX_NEARBY_RADIUS) ? false : true ;
            String distStr = (dist >= 1000) ? mContext.getString(R.string.format_dist_kilometer, mKmFormat.format(dist / 1000)): mContext.getString(R.string.format_dist_meter, mMeterFormat.format(dist));

            if(isNearby) {
                String address = groceryListData.getAddress();
                String uri = String.format("geo:%s,%s?q=%s,%s(%s)", lat, lng, lat, lng, address);
                Bundle bundle = new Bundle();

                bundle.putInt(BundleInfo.BUNDLE_KEY_BUNDLE_TYPE, BundleInfo.BundleType.BUNDLE_TYPE_GROCERY_LIST_NOTIFICATION.value());
                bundle.putString(BundleInfo.BUNDLE_KEY_GROCERY_STORE_GEO_URI_STR, uri);
                sNotificationOutput.outNotif(NotificationOutput.NOTIF_CAT_WITH_GROCERY_LIST_ACTIONS, groceryListData.hashCode(), mContext.getString(R.string.format_nearby_store, storeName, distStr), mNotifType, bundle);
            }
        }
    }
}
