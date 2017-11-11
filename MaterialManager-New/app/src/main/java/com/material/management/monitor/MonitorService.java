package com.material.management.monitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import com.material.management.service.location.LocationTrackJob;
import com.material.management.service.location.LocationTrackService;
import com.material.management.utils.LogUtility;
import com.material.management.utils.Utility;

import java.util.Calendar;

public class MonitorService extends BroadcastReceiver {
    private static final String DEBUG = "MonitorService";

    public enum MonitorType {
        MONITOR_TYPE_EXPIRE_NOITFICATION(0), MONITOR_TYPE_LOCATION_TRACK(1), MONITOR_TYPE_ALL(2);

        private int mVal;

        MonitorType(int val) {
            mVal = val;
        }

        public int value() {
            return mVal;
        }
    }

    private static PendingIntent sExpiredMonitorRepeatIntent;
    private static PendingIntent sLocationTrackRepeatIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        /* Default  is to triggler the all functionality include notification, location tracking.*/
        int monitorTypeIndex = intent.getIntExtra("monitor_type", MonitorType.MONITOR_TYPE_ALL.value());
        String action = intent.getAction();

        if (action != null && (action.equals(Intent.ACTION_PACKAGE_ADDED) || action.equals(Intent.ACTION_PACKAGE_REPLACED))) {
            String packageName = intent.getDataString();

            if (!packageName.equals(Utility.getContext().getPackageName())) {
                return;
            }
        }

        if (monitorTypeIndex < 0) {
            return;
        } else {
            MonitorType monitorType = MonitorType.values()[monitorTypeIndex];

            switch (monitorType) {
                case MONITOR_TYPE_EXPIRE_NOITFICATION: {
                    doExpireCheck(context, intent);
                }
                break;

                case MONITOR_TYPE_LOCATION_TRACK: {
                    doGroceryListNearbyCheck(context, intent);
                }
                break;

                case MONITOR_TYPE_ALL: {
                    doExpireCheck(context, intent);
                    doGroceryListNearbyCheck(context, intent);
                }
                break;
            }
        }
    }

    private void doGroceryListNearbyCheck(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ComponentName componentName = new ComponentName(context, LocationTrackJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(1, componentName);
            JobScheduler tm = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            PersistableBundle bundle = new PersistableBundle();

            tm.cancel(1);
            bundle.putInt("is_location_track_on", 1);
            builder.setExtras(bundle);
            builder.setPersisted(false);
            builder.setMinimumLatency(0);
            tm.schedule(builder.build());
        } else {
            Intent locationTrackIntent = new Intent(context, LocationTrackService.class);
            locationTrackIntent.putExtra("is_location_track_on", true);
            context.startService(locationTrackIntent);
        }
    }

    private void doExpireCheck(Context context, Intent intent) {
     /*
           * 0:vibrate
           * 1:sound
           * 0 is default
           *
           * default is notificated per 1 hr.
            * */
        int notifType = Utility.getIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND);
        int notifFreq = Utility.getIntValueForKey(Utility.NOTIF_FREQUENCY);
        boolean isImmeditlyTrigger = false;

        if (intent != null) {
            notifType = intent.getIntExtra("notif_type", notifType);
            notifFreq = intent.getIntExtra("notif_freq", notifFreq);
            isImmeditlyTrigger = intent.getBooleanExtra("immeditly_triggered", isImmeditlyTrigger);
        }

        if (isImmeditlyTrigger) {
            Thread monitorThread = new Thread(new ExpireMonitorRunnable(notifType), ExpireMonitorRunnable.MONITOR_THREAD_NAME);

            monitorThread.setPriority(Thread.MIN_PRIORITY);
            monitorThread.start();
        }

        Intent i = new Intent(context, MonitorService.class);
        Calendar cal = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        /* cancel all pending to ensure only one pending working*/
        if (sExpiredMonitorRepeatIntent != null)
            am.cancel(sExpiredMonitorRepeatIntent);

        i.putExtra("monitor_type", MonitorType.MONITOR_TYPE_EXPIRE_NOITFICATION.value());
        i.putExtra("immeditly_triggered", true);
        i.putExtra("notif_type", notifType);
        i.putExtra("notif_freq", notifFreq);

        sExpiredMonitorRepeatIntent = PendingIntent.getBroadcast(Utility.getContext(), MonitorType.MONITOR_TYPE_EXPIRE_NOITFICATION.value(), i, PendingIntent.FLAG_CANCEL_CURRENT);

        cal.add(Calendar.HOUR, notifFreq);
//        cal.add(Calendar.SECOND, 10);
        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sExpiredMonitorRepeatIntent);
    }
}
