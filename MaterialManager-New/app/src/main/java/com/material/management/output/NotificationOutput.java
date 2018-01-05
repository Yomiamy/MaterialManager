package com.material.management.output;

import com.material.management.MainActivity;
import com.material.management.R;
import com.material.management.data.BundleInfo;
import com.material.management.utils.Utility;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;

public class NotificationOutput {
    public  static final int NOTIF_CAT_COMMON = 0;
    public  static final int NOTIF_CAT_WITH_GROCERY_LIST_ACTIONS = 1;
    public static final String GENERIC_CHANNEL_ID = "com.material.management";
    public static final String PROGRESS_CHANNEL_ID = "com.material.management-progress";

    private static final int PROGRESS_NOTIFICATION_ID = 0;
    private static final Uri SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private static final long[] VIBRATE = {0, 100, 200, 300};
    private static NotificationOutput sNotOutput = null;

    private Context mContext;
    private Resources mRes;
    private NotificationManager mNotMgr = null;
    private Notification.Builder mProgressNotifBuilder = null;
    private NotificationChannel mGenericNotifChannel;

    private NotificationOutput(Context context) {
        mNotMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mContext = context;
        mRes = context.getResources();
        mProgressNotifBuilder = new Notification.Builder(mContext);

        mProgressNotifBuilder.setOnlyAlertOnce(true);
        if(VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotifChannel();
        }
    }

    public static void initInstance(Context context) {
        if (sNotOutput == null)
            sNotOutput = new NotificationOutput(context);
    }

    public static NotificationOutput getInstance() {
        return sNotOutput;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void createNotifChannel() {
        mGenericNotifChannel = new NotificationChannel(GENERIC_CHANNEL_ID, GENERIC_CHANNEL_ID, mNotMgr.IMPORTANCE_HIGH);
        mGenericNotifChannel.enableLights(true);
        mGenericNotifChannel.setLightColor(Color.RED);
        mGenericNotifChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mGenericNotifChannel.setImportance(NotificationManager.IMPORTANCE_LOW);
        mNotMgr.createNotificationChannel(mGenericNotifChannel);

        NotificationChannel progressNotifChannel = new NotificationChannel(PROGRESS_CHANNEL_ID, PROGRESS_CHANNEL_ID, mNotMgr.IMPORTANCE_DEFAULT);
        progressNotifChannel.enableLights(true);
        progressNotifChannel.setLightColor(Color.RED);
        progressNotifChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotMgr.createNotificationChannel(progressNotifChannel);
        mProgressNotifBuilder.setChannelId(PROGRESS_CHANNEL_ID);
    }

    public void outProgress(String msg, int progress, int max) {
        mProgressNotifBuilder.setSmallIcon(R.drawable.ic_launcher);
        mProgressNotifBuilder.setContentTitle(mRes.getString(R.string.app_name));
        mProgressNotifBuilder.setContentText(msg);
        mProgressNotifBuilder.setProgress(max, progress, false);

        if (VERSION.SDK_INT >= 16) {
            mNotMgr.notify(PROGRESS_NOTIFICATION_ID, mProgressNotifBuilder.build());
        } else {
            mNotMgr.notify(PROGRESS_NOTIFICATION_ID, mProgressNotifBuilder.getNotification());
        }
    }

    /* output may be refined ,this is a temporary implementation */
    public void outNotif(int notifCat, int objId, String msg, int notifType, Bundle bundle) {
        Intent actionIntent = new Intent(mContext, MainActivity.class);
        actionIntent.putExtras(bundle);

        outNotif(notifCat, objId, actionIntent, msg, notifType);
    }

    private void outNotif(int notifCat, int objId, Intent actionIntent, String msg, int notifType) {
        // Clear exist task and create the new one.
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        // Start initialize the PendingIntent for notificaiton.
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, objId, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notifBuilder = new Notification.Builder(mContext);

        if (notifCat == NOTIF_CAT_WITH_GROCERY_LIST_ACTIONS && VERSION.SDK_INT >= 16) {
            Bundle bundle = actionIntent.getExtras();

            if(bundle != null) {
                String geoUriStr = bundle.getString(BundleInfo.BUNDLE_KEY_GROCERY_STORE_GEO_URI_STR);

                if(!TextUtils.isEmpty(geoUriStr)) {
                    Intent geoActIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUriStr));
                    PendingIntent geoPendingIntent = PendingIntent.getActivity(mContext, objId, geoActIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    notifBuilder.addAction(R.drawable.ic_action_navigate_green, mRes.getString(R.string.title_notif_action_nav_to_store), geoPendingIntent);
                    notifBuilder.addAction(R.drawable.ic_action_search_green, mRes.getString(R.string.title_notif_action_go_grocery_list), contentIntent);
                }
            }
        }

        /*
         * 0: Vibrate 1: Sound
         */
        notifBuilder.setContentTitle(mRes.getString(R.string.app_name))
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(mRes, R.drawable.ic_launcher))
                .setContentIntent(contentIntent)
                .setTicker(msg)
                .setDefaults(notifType == 0 ? Notification.DEFAULT_VIBRATE : Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
        Ringtone r = null;
        Vibrator v = null;

        /* Set heads-up settings */
        if(VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            r = (notifType == 1) ? RingtoneManager.getRingtone(Utility.getContext(), SOUND) : null;
            v = (notifType == 0) ? (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE) : null;

            notifBuilder.setChannelId(GENERIC_CHANNEL_ID);
        }

        /* Set heads-up settings */
        if (VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notifBuilder.setPriority(Notification.PRIORITY_HIGH);
        }

        /* use wake lock to wake up devie */
        Utility.acquire();

        if (VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notifBuilder.setPriority(Notification.PRIORITY_HIGH);
            mNotMgr.notify(objId, notifBuilder.build());
        } else {
            mNotMgr.notify(objId, notifBuilder.getNotification());
        }

        if (r != null) {
            r.play();
        }

        if (v != null) {
            v.vibrate(VibrationEffect.createWaveform(VIBRATE, -1));
        }

        /* release wake lock to wake up devie */
        Utility.release();
    }
}
