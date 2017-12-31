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
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.text.TextUtils;

public class NotificationOutput {
    public  static final int NOTIF_CAT_COMMON = 0;
    public  static final int NOTIF_CAT_WITH_GROCERY_LIST_ACTIONS = 1;
    public static final String GENERIC_CHANNEL_ID = "com.material.management";
    public static final String PROGRESS_CHANNEL_ID = "com.material.management-progress";
    public static final String CHANNEL_NAME = "com.material.management_channel_name";

    private static final int PROGRESS_NOTIFICATION_ID = 0;
    private static final Uri SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private static final long[] VIBRATE = {0, 100, 200, 300};
    private static NotificationOutput sNotOutput = null;

    private Context mContext;
    private Resources mRes;
    private NotificationManager mNotMgr = null;
    private Notification.Builder mProgressNotif = null;

    private NotificationOutput(Context context) {
        mNotMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mContext = context;
        mRes = context.getResources();
        mProgressNotif = new Notification.Builder(mContext);

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
        NotificationChannel genericNotifChannel = new NotificationChannel(GENERIC_CHANNEL_ID, CHANNEL_NAME, mNotMgr.IMPORTANCE_HIGH);
        genericNotifChannel.enableLights(true);
        genericNotifChannel.setLightColor(Color.RED);
        genericNotifChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotMgr.createNotificationChannel(genericNotifChannel);

        NotificationChannel progressNotifChannel = new NotificationChannel(PROGRESS_CHANNEL_ID, CHANNEL_NAME, mNotMgr.IMPORTANCE_DEFAULT);
        progressNotifChannel.enableLights(true);
        progressNotifChannel.setLightColor(Color.RED);
        progressNotifChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotMgr.createNotificationChannel(progressNotifChannel);
    }

    public void outProgress(String msg, int progress, int max) {
        mProgressNotif.setSmallIcon(R.drawable.ic_launcher);
        mProgressNotif.setContentTitle(Utility.getContext().getString(R.string.app_name));
        mProgressNotif.setContentText(msg);
        mProgressNotif.setProgress(max, progress, false);

        if(VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mProgressNotif.setChannelId(PROGRESS_CHANNEL_ID);
        }

        if (VERSION.SDK_INT >= 16) {
            mNotMgr.notify(PROGRESS_NOTIFICATION_ID, mProgressNotif.build());
        } else {
            mNotMgr.notify(PROGRESS_NOTIFICATION_ID, mProgressNotif.getNotification());
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
        Ringtone r = null;
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

        notifBuilder.setContentTitle(mContext.getString(R.string.app_name))
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(contentIntent)
                .setTicker(msg)
                .setAutoCancel(true);

        if(VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifBuilder.setChannelId(GENERIC_CHANNEL_ID);
        }

        /*
         * 0: Vibrate 1: Sound
         */
        if (notifType == 0) {
            notifBuilder.setVibrate(VIBRATE);
        } else {
            r = RingtoneManager.getRingtone(Utility.getContext(), SOUND);
        }

        /* use wake lock to wake up devie */
        Utility.acquire();
        if (VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notifBuilder.setPriority(Notification.PRIORITY_HIGH);
            mNotMgr.notify(objId, notifBuilder.build());
        } else {
            mNotMgr.notify(objId, notifBuilder.getNotification());
        }
        if (r != null && notifType == 1) {
            r.play();
        }
        /* release wake lock to wake up devie */
        Utility.release();
    }
}
