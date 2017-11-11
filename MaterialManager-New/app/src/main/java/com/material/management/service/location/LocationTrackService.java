package com.material.management.service.location;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LocationTrackService extends Service {

    private LocationTrackTask mLocTrackTask = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isTrackOn = (intent != null) ? intent.getBooleanExtra("is_location_track_on", true) : true;

        if(mLocTrackTask != null) {
            mLocTrackTask.stopLocationTrack();
        }

        if (!isTrackOn) {
            stopSelf();
            return START_NOT_STICKY;
        }

        mLocTrackTask = new LocationTrackTask(this);
        mLocTrackTask.startLocationTrack();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {}
}
