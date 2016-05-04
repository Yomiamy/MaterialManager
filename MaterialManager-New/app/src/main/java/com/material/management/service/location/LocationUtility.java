package com.material.management.service.location;


import android.content.Context;

import android.location.Location;

import android.os.Bundle;

import com.material.management.broadcast.BroadCastEvent;

import org.greenrobot.eventbus.EventBus;


public class LocationUtility extends BestLocationListener {
    private static Context sContext;
    private static LocationUtility sInstance = null;

    private Location mLastKnowLocation = null;
    private BestLocationProvider mBestLocationProvider = null;

    private LocationUtility(){
    }

    public static void init(Context context) {
        sContext = context;
    }

    public static LocationUtility getsInstance() {
        if(sInstance == null) {
            sInstance = new LocationUtility();
            sInstance.mBestLocationProvider = new BestLocationProvider(sContext, true, true, 2000, 2000, 2, 0);
        }
        return sInstance;
    }

    public boolean isLocationEnabled() {
        /*
        * Another provider type LocationManager.NETWORK_PROVIDER, maybe we can switch different
        * provider type for different purpose. e.g: power saving, exactly location and so on.
        * */
        return sInstance.mBestLocationProvider.isLocationEnabled();
    }

    public Location getLocation() {
        mLastKnowLocation = mBestLocationProvider.getLastKnowLocation();

        mBestLocationProvider.startLocationUpdatesWithListener(sInstance);

        return mLastKnowLocation;
    }

    @Override
    public void onLocationUpdate(Location location, BestLocationProvider.LocationType type, boolean isFresh) {
        mLastKnowLocation = location;
        BroadCastEvent broadCastEvent = new BroadCastEvent(BroadCastEvent.BROADCAST_EVENT_TYPE__LOC_UPDATE, mLastKnowLocation);
        /*
        * If we get a new location, then we stop update.
        * Either the location is from GPS or Network.
        * */
        mBestLocationProvider.stopLocationUpdates();
        EventBus.getDefault().post(broadCastEvent);
    }

    @Override
    public void onLocationUpdateTimeoutExceeded(BestLocationProvider.LocationType type) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
