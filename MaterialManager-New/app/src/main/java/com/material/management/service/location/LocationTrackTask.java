package com.material.management.service.location;

import android.Manifest;
import android.content.Context;
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
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.material.management.monitor.GroceryNearbyMonitorRunnable;
import com.material.management.utils.Utility;

/**
 * Created by yomi on 2017/11/8.
 */

public class LocationTrackTask extends LocationCallback implements SensorEventListener, LocationListener {
    private static final String HANDLER_THREAD_NAME = "handle_thread_name";
    private static final int WORST_ACCEPT_ACCURICY = 100;

    private Context mCtx;
    private SensorManager mSensorManager = null;
    private LocationManager mLocationManager = null;
    private Handler mHandler = null;
    private HandlerThread mHandlerThread;
    private Runnable mReasonTimeoutRunnable;
    private SettingsClient mSettingsClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationRequest mLocationRequest;
    private int mAccelReadings;
    private int mAccelSignificantReadings;
    private long mAccelTimestamp;
    private long mGPSTimestamp;


    public LocationTrackTask(Context ctx) {
        this.mCtx = ctx;
    }

    public void startLocationTrack() {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
            mHandlerThread.start();
        }
        mHandler = new Handler(mHandlerThread.getLooper());
        mLocationManager = (mLocationManager == null) ? (LocationManager) mCtx.getSystemService(Context.LOCATION_SERVICE) : mLocationManager;
        mReasonTimeoutRunnable = () -> {
            stopGPS();
            Utility.release();
            sleepAndRestart();
        };
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mCtx);
        mSettingsClient = LocationServices.getSettingsClient(mCtx);
        createLocationRequest();
        buildLocationSettingsRequest();

        stopGPS();
        startAccelerometer();
    }

    public void stopLocationTrack() {
        Utility.release();
        mHandler.removeCallbacksAndMessages(null);
        mLocationManager.removeUpdates(this);
        mFusedLocationProviderClient.removeLocationUpdates(this);
        mSensorManager.unregisterListener(this);
    }

    // ACCELEROMETER METHODS
    private void startAccelerometer() {
        mAccelReadings = 0;
        mAccelSignificantReadings = 0;
        mAccelTimestamp = System.currentTimeMillis();
        // should probably store handles to these earlier, when service is created
        mSensorManager = (SensorManager) mCtx.getSystemService(Context.SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        Log.d("randy", "onSensorChanged");
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

        mSensorManager.unregisterListener(this);

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


    private void onReceiveLocation(Location location) {
        /*
        /* So always store network/cell location, but only the first since accuracy will be quite low
         */
        Log.d("randy", "onLocationChanged");
        /*
         * What's our accuracy cutoff?
         * Keep polling if our accuracy is worse than 30 meter
         * This should be configurable
         */
        if (location.getAccuracy() > WORST_ACCEPT_ACCURICY) {
            return;
        }
        mHandler.post(new GroceryNearbyMonitorRunnable(Utility.getIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND)));
        sleepAndRestart();
        stopGPS();
        Utility.release();
    }

    // ========= LocationCallback callback =========

    @Override
    public void onLocationResult(LocationResult locationResult) {
        Location location = locationResult.getLastLocation();
        onReceiveLocation(location);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(10000);
        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    // ========= LocationListener callback =========

    @Override
    public void onLocationChanged(Location location) {
        onReceiveLocation(location);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mCtx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            return;
        }

        // If it's a provider we care about, and we're listening, listen!
        if (provider == LocationManager.GPS_PROVIDER) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        }
        if (provider == LocationManager.NETWORK_PROVIDER) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, this);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (mGPSTimestamp == 0) {
            // Not currently interested
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mCtx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            return;
        }

        // If it's a provider we care about, and we're listening, listen!
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        }
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, this);
        }
    }

    // GPS METHODS
    @SuppressWarnings("MissingPermission")
    public void startGPS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mCtx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

                // Begin by checking if the device has the necessary location settings.
                mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                        .addOnSuccessListener(locationSettingsResponse -> {
                            //noinspection MissingPermission
                            Log.d("randy", "Location settings success");
                            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, LocationTrackTask.this, mHandlerThread.getLooper());
                        })
                        .addOnFailureListener(e -> {
                            Log.d("randy", "Location settings failure");
                            e.printStackTrace();
                        });
            }
        } else {
            int iProviders = 0;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mCtx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
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
        }

        mHandler.postDelayed(mReasonTimeoutRunnable, 30000);
        mGPSTimestamp = System.currentTimeMillis();
    }

    public void stopGPS() {
        mFusedLocationProviderClient.removeLocationUpdates(this);
        mHandler.removeCallbacks(mReasonTimeoutRunnable);
        mLocationManager.removeUpdates(this);
    }

    // OTHER
    public void sleepAndRestart() {
        // Check desired state
        mHandler.postDelayed(() -> startLocationTrack(), 60000);
    }
}
