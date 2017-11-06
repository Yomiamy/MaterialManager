package com.material.management.service.location;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import com.material.management.R;
import com.material.management.monitor.GroceryNearbyMonitorRunnable;
import com.material.management.utils.LogUtility;
import com.material.management.utils.Utility;

public class LocationTrackService extends Service implements SensorEventListener, LocationListener {
    private static final String HANDLER_THREAD_NAME = "handle_thread_name";
    private static final int WORST_ACCEPT_ACCURICY = 100;

    private SensorManager mSensorManager = null;
    private LocationManager mLocationManager = null;
    private Location mCurBestLocation = null;
    private Handler mHandler = null;
    private HandlerThread mHandlerThread;
    private int mAccelReadings;
    private int mAccelSignificantReadings;
    private long mAccelTimestamp;
    private long mGPSTimestamp;
    private boolean mIsTrackOn = false;
    private Runnable mReasonTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            stopGPS();
            Utility.release();
            sleepAndRestart();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);

            mHandlerThread.start();
        }
        mHandler = new Handler(mHandlerThread.getLooper());

        mLocationManager = (mLocationManager == null) ? (LocationManager) getSystemService(Context.LOCATION_SERVICE) : mLocationManager;
        /* Default we track the location */
        mIsTrackOn = (intent != null) ? intent.getBooleanExtra("is_location_track_on", true) : true;

        if (!mIsTrackOn) {
            stopSelf();

            return START_NOT_STICKY;
        }
//        startForeground(NOTIFICATION_ID, null);
        stopGPS();
        startAccelerometer();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Utility.release();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    // ACCELEROMETER METHODS
    private void startAccelerometer() {
        mAccelReadings = 0;
        mAccelSignificantReadings = 0;
        mAccelTimestamp = System.currentTimeMillis();
        // should probably store handles to these earlier, when service is created
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    private void stopAccelerometer() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        mAccelReadings++;
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];
        double accel = Math.abs(Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)));
        // Was 0.6. Lowered to 0.3 (plus gravity) to account for smooth motion from Portland Streetcar
        if (accel > 10.1) {
            mAccelSignificantReadings++;
        }

        /*
        /*  Get readings for 1 second
        /*  Maybe we should sample for longer given that I've lowered the threshold
        */
        if ((System.currentTimeMillis() - mAccelTimestamp) < 2000) return;

        stopAccelerometer();

        /*
        /*  Appeared to be movingState 30% of the time?
        /*  If the bar is this low, why not report motion at the first significant reading and be done with it?
        */
        if (((1.0 * mAccelSignificantReadings) / mAccelReadings) > 0.30) {
            // Start GPS
            Utility.release();
            Utility.acquire();
            startGPS();
        } else {
            sleepAndRestart();
            Utility.release();
            stopGPS();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // GPS METHODS
    public void startGPS() {
        int iProviders = 0;
        mCurBestLocation = null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            // Make sure at least one provider is available
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
                iProviders++;
            }
            if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, this);
                iProviders++;
            }
        }

        if (iProviders == 0) {
            stopGPS();
            Utility.release();
            sleepAndRestart();
            return;
        }

        mHandler.postDelayed(mReasonTimeoutRunnable, 30000);
        mGPSTimestamp = System.currentTimeMillis();
    }

    @Override
    public void onLocationChanged(Location location) {
        /*
        /* So always store network/cell location, but only the first since accuracy will be quite low
         */
        if (mCurBestLocation == null || location.getAccuracy() <= mCurBestLocation.getAccuracy()) {
            mCurBestLocation = location;

            /*
             * What's our accuracy cutoff?
             * Keep polling if our accuracy is worse than 30 meter
             * This should be configurable
             */
            if (location.getAccuracy() > WORST_ACCEPT_ACCURICY) {
                return;
            }
            mHandler.post(new GroceryNearbyMonitorRunnable(Utility.getIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND)));
        }

        sleepAndRestart();
        stopGPS();
        Utility.release();
    }

    public void stopGPS() {
        mHandler.removeCallbacks(mReasonTimeoutRunnable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            return;
        }

        mLocationManager.removeUpdates(this);
    }

    // OTHER
    public void sleepAndRestart() {
        // Check desired state
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(LocationTrackService.this, LocationTrackService.class);
                startService(intent);
            }
        }, 60000);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (mGPSTimestamp == 0) {
            // Not currently interested
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            return;
        }

        // If it's a provider we care about, and we're listening, listen!
        if (provider == LocationManager.GPS_PROVIDER || provider == LocationManager.NETWORK_PROVIDER) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (mGPSTimestamp == 0) {
            // Not currently interested
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            return;
        }


        // If it's a provider we care about, and we're listening, listen!
        if (provider == LocationManager.GPS_PROVIDER && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, this);
        } else if (provider == LocationManager.NETWORK_PROVIDER && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        }
    }
}
