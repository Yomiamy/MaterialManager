package com.material.management.service;


import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.broadcast.BroadCastEvent;
import com.material.management.data.BackupRestoreInfo;
import com.material.management.output.NotificationOutput;
import com.material.management.utils.DBUtility;
import com.material.management.utils.FileUtility;
import com.material.management.utils.Utility;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executors;

public class GoogleDriveCloudService {

    public enum Mode {
        BACKUP_MODE, RESTORE_MODE
    }

    private GoogleApiClient mGoogleApiClient;
    private NotificationOutput mNotifOutput;

    public GoogleDriveCloudService(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;
        this.mNotifOutput = NotificationOutput.getInstance();
    }

    public void startBakupRestoreTask(Mode mode) {
        new BackupRestoreTask().executeOnExecutor(Executors.newCachedThreadPool(), mode);
    }

    private class BackupRestoreTask extends AsyncTask<Mode, Void, String> implements Observer {

        @Override
        protected String doInBackground(Mode... params) {
            Mode mode = params[0];
            StringBuilder msg = new StringBuilder("");

            switch (mode) {
                case BACKUP_MODE: {
                    FileUtility.clearAppDriveData(mGoogleApiClient, this);
                    msg.append(DBUtility.exportDB(mGoogleApiClient, this));
                    msg.append(FileUtility.exportPhoto(mGoogleApiClient, this));
                }
                break;

                case RESTORE_MODE: {
                    msg.append(DBUtility.importDB(mGoogleApiClient, this));
                    msg.append(FileUtility.importPhoto(mGoogleApiClient, this));
                }
                break;
            }


            return msg.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            BroadCastEvent bakRestoreFinshedEvent = new BroadCastEvent(BroadCastEvent.BROADCAST_EVENT_TYPE_BACKUP_RESTORE_FINISHED, s);

            EventBus.getDefault().post(bakRestoreFinshedEvent);
            mNotifOutput.outProgress(
                    Utility.getContext().getString(
                            R.string.title_progress_notif_backup_restore_successfully), 0, 0);
        }

        @Override
        public void update(Object data) {
            if (!(data instanceof BackupRestoreInfo)) {
                return;
            }

            BackupRestoreInfo bri = (BackupRestoreInfo) data;
            BroadCastEvent progressUpdateEvent = new BroadCastEvent(BroadCastEvent.BROADCAST_EVENT_TYPE_BACKUP_RESTORE_PROGRESS_UPDATE, data);

            EventBus.getDefault().post(progressUpdateEvent);
            mNotifOutput.outProgress(bri.getMsg(), bri.getProgress(), 100);
        }
    }
}
