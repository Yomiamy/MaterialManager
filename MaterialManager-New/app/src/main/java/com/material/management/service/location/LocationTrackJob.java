package com.material.management.service.location;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;

/**
 * Created by yomi on 2017/11/8.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LocationTrackJob extends JobService {

    private LocationTrackTask mLocTrackTask = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle bundle = params.getExtras();
        int isTrackOn = (bundle != null) ? bundle.getInt("is_location_track_on", 1) : 1;

        if(mLocTrackTask != null) {
            mLocTrackTask.stopLocationTrack();
        }

        if (isTrackOn == 0) {
            return false;
        }

        mLocTrackTask = new LocationTrackTask(this);
        mLocTrackTask.startLocationTrack();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
