package com.material.management.fragment;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.material.management.IStatusUpdate;
import com.material.management.MMActivity;
import com.material.management.MMFragment;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.broadcast.BroadCastEvent;
import com.material.management.data.BackupRestoreInfo;
import com.material.management.dialog.LightProgressDialog;
import com.material.management.monitor.MonitorService;
import com.material.management.service.DropboxCloudService;
import com.material.management.service.GoogleDriveCloudService;
import com.material.management.service.IBackupRestore;
import com.material.management.utils.LogUtility;
import com.material.management.utils.Utility;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SettingsFragment extends MMFragment implements Observer, RadioGroup.OnCheckedChangeListener, AdapterView.OnItemSelectedListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int CLOUD_SERVICE_TYPE_DROPBOX = 0;
    private static final int CLOUD_SERVICE_TYPE_GOOGLE_DRIVER = 1;

    public static final int REQUEST_CODE_RESOLVE_CONNECTION = 0;
    public static final int REQUEST_CODE_ACCPICK = 1;

    private View mLayout;
    private TextView mTvDbRestore;
    private TextView mTvDbBackup;
    private RadioGroup mRgNotifVibrateOrSound;
    private Spinner mSpinNotifFrequency;
    private Spinner mSpinFontSizeChange;
    private ImageView mIvBtnDropbox;
    private ImageView mIvDropboxEnableStatus;
    private ImageView mIvBtnGoogleDrive;
    private ImageView mIvGoogleDriveEnableStatus;

    private LightProgressDialog mProgressDialog = null;
    private IBackupRestore mDropboxService;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Log.d(MaterialManagerApplication.TAG, "onServiceConnected");
            mDropboxService = IBackupRestore.Stub.asInterface((IBinder) service);
        }

        public void onServiceDisconnected(ComponentName className) {
            // Log.d(MaterialManagerApplication.TAG, "onServiceDisconnected");
            mDropboxService = null;
        }
    };
    private IStatusUpdate.Stub mStatusUpdate = new IStatusUpdate.Stub() {

        @Override
        public void updateProgress(final String msg, final int progress) throws RemoteException {
            if (mProgressDialog != null) {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mProgressDialog.setMessage(msg);
                        mProgressDialog.setProgress(progress);
                    }
                });
            }
        }

        @Override
        public void finishBackupOrRestore(final String msg) throws RemoteException {
            mHandler.post(() -> {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.clearDefaultAccountAndReconnect();
                    mGoogleApiClient.disconnect();
                    mIvGoogleDriveEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
                }

                try {
                    if (mDropboxService != null && mDropboxService.isLinked()) {
                        mDropboxService.disConnect();
                        mIvDropboxEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
                    }
                } catch (RemoteException e) {
                    LogUtility.printStackTrace(e);
                }

                showToast(msg);
                if (mProgressDialog != null) {
                    // TODO: Add a workaround to avoid the NullPointerException
                    mProgressDialog.setShowState(false);
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            });
        }
    };
    private GoogleApiClient mGoogleApiClient;
    private String[] mFontSizeScaleTitles;
    private String[] mFontSizeScaleLeves;
    private int mIsNotifVibrateOrSound;
    private int mNotifFreq;
    private int mCurReqCloudService = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayout = inflater.inflate(R.layout.fragment_setting_layout, container, false);

        update(null);
        initView();
        init();

        return mLayout;
    }

    @Override
    public void onResume() {
        try {
            mIvDropboxEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
            mIvGoogleDriveEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
            if (mCurReqCloudService == CLOUD_SERVICE_TYPE_DROPBOX && mDropboxService != null && mDropboxService.isLinked()) {
                mIvDropboxEnableStatus.setImageResource(R.drawable.ic_cloud_service_selected);
            } else if (mCurReqCloudService == CLOUD_SERVICE_TYPE_GOOGLE_DRIVER && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                mIvGoogleDriveEnableStatus.setImageResource(R.drawable.ic_cloud_service_selected);
            }
        } catch (RemoteException e) {
            LogUtility.printStackTrace(e);
            mIvDropboxEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
            mIvGoogleDriveEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
        }
        sendScreenAnalytics(getString(R.string.ga_app_view_settings_fragment));
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        try {
            if (mDropboxService != null && mDropboxService.isLinked()) {
                mDropboxService.disConnect();
            }
        } catch (RemoteException e) {
            LogUtility.printStackTrace(e);
        }

        EventBus.getDefault().unregister(this);
        mOwnerActivity.unbindService(mConnection);
        super.onDestroyView();
    }

    private void initView() {
        mRgNotifVibrateOrSound = (RadioGroup) mLayout.findViewById(R.id.rg_notif_types);
        mSpinNotifFrequency = (Spinner) mLayout.findViewById(R.id.spin_notification_frequency);
        mSpinFontSizeChange = (Spinner) mLayout.findViewById(R.id.spin_font_size_scale_factor);
        RadioButton rbNotifVibrate = (RadioButton) mLayout.findViewById(R.id.rb_notif_type_vibrate);
        RadioButton rbNotifSound = (RadioButton) mLayout.findViewById(R.id.rb_notif_type_sound);
        mIvBtnDropbox = (ImageView) mLayout.findViewById(R.id.iv_dropbox_enable);
        mIvDropboxEnableStatus = (ImageView) mLayout.findViewById(R.id.iv_dropbox_enable_status);
        mIvBtnGoogleDrive = (ImageView) mLayout.findViewById(R.id.iv_googledriver_enable);
        mIvGoogleDriveEnableStatus = (ImageView) mLayout.findViewById(R.id.iv_googledriver_enable_status);
        mTvDbBackup = (TextView) mLayout.findViewById(R.id.tv_database_backup);
        mTvDbRestore = (TextView) mLayout.findViewById(R.id.tv_database_restore);
        /* default is vibrate */
        mIsNotifVibrateOrSound = Utility.getIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND);
        /* default frequency is 1 hour */
        mNotifFreq = Utility.getIntValueForKey(Utility.NOTIF_FREQUENCY);
        String notifFreqStr = (mNotifFreq == mResources.getInteger(R.integer.max_notif_freq)) ? mResources.getString(R.string.title_notif_silent_mode) : Integer.toString(mNotifFreq);
        ArrayAdapter<String> notifSpinAdapter = new SpinnerSettingsAdapter<String>(Utility.getContext(), R.layout.view_spinner_item_layout,
                getResources().getStringArray(R.array.default_notification_frequency));
        ArrayAdapter<String> fontSizeSpinAdapter = new SpinnerSettingsAdapter<String>(Utility.getContext(), R.layout.view_spinner_item_layout,
                getResources().getStringArray(R.array.font_size_scale_level_title));
        notifSpinAdapter.setDropDownViewResource(R.layout.view_spinner_item_layout);
        fontSizeSpinAdapter.setDropDownViewResource(R.layout.view_spinner_item_layout);
        mFontSizeScaleTitles = getResources().getStringArray(R.array.font_size_scale_level_title);
        mFontSizeScaleLeves = getResources().getStringArray(R.array.font_size_scale_level);
        String defaultScaleFact = Utility.getStringValueForKey(Utility.FONT_SIZE_SCALE_FACTOR);

        if (mIsNotifVibrateOrSound == 0) {
            rbNotifVibrate.setChecked(true);
        } else {
            rbNotifSound.setChecked(true);
        }

        mSpinFontSizeChange.setAdapter(fontSizeSpinAdapter);
        mSpinFontSizeChange.setOnItemSelectedListener(this);
        for (int i = 0, len = mFontSizeScaleTitles.length; i < len; i++) {
            if (defaultScaleFact.equals(mFontSizeScaleLeves[i])) {
                mSpinFontSizeChange.setSelection(i);
            }
        }
        mSpinNotifFrequency.setAdapter(notifSpinAdapter);
        mSpinNotifFrequency.setOnItemSelectedListener(this);
        mSpinNotifFrequency.setSelection(notifSpinAdapter.getPosition(notifFreqStr));
        mRgNotifVibrateOrSound.setOnCheckedChangeListener(this);
        mIvBtnDropbox.setOnClickListener(this);
        mIvBtnGoogleDrive.setOnClickListener(this);
        mTvDbBackup.setOnClickListener(this);
        mTvDbRestore.setOnClickListener(this);
    }

    private void init() {
        mCurReqCloudService = CLOUD_SERVICE_TYPE_DROPBOX;
        EventBus.getDefault().register(this);
        mOwnerActivity.bindService(new Intent(mOwnerActivity, DropboxCloudService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    /* Radio Button */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_notif_type_vibrate:
                mIsNotifVibrateOrSound = 0;
                break;
            case R.id.rb_notif_type_sound:
                mIsNotifVibrateOrSound = 1;
                break;
        }


        Utility.setIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND, mIsNotifVibrateOrSound);
        Utility.setIntValueForKey(Utility.NOTIF_FREQUENCY, mNotifFreq);
        Intent intent = new Intent();

        intent.setClass(Utility.getContext(), MonitorService.class);
        intent.putExtra("monitor_type", MonitorService.MonitorType.MONITOR_TYPE_EXPIRE_NOITFICATION.value());
        intent.putExtra("notif_type", mIsNotifVibrateOrSound);
        /* if immeditly_triggered== false , it will delay one ExpireMonitorRunnable checking */
        intent.putExtra("immeditly_triggered", false);
        Utility.getContext().sendBroadcast(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapter, View selectedView, int pos, long id) {
        int vId = adapter.getId();

        if (vId == R.id.spin_notification_frequency) {
            TextView tvFreq = (TextView) selectedView;
            String freqStr = null;

            /* To avoid the trim memory issue. */
            if (tvFreq != null) {
                freqStr = tvFreq.getText().toString().trim();
            } else {
                freqStr = Integer.toString(Utility.getIntValueForKey(Utility.NOTIF_FREQUENCY));
            }

            mNotifFreq = (freqStr.endsWith(mResources.getString(R.string.title_notif_silent_mode))) ? mResources.getInteger(R.integer.max_notif_freq) : Integer.parseInt(freqStr);
            Intent intent = new Intent();
            Utility.setIntValueForKey(Utility.NOTIF_IS_VIBRATE_SOUND, mIsNotifVibrateOrSound);
            Utility.setIntValueForKey(Utility.NOTIF_FREQUENCY, mNotifFreq);

            intent.setClass(Utility.getContext(), MonitorService.class);
            intent.putExtra("monitor_type", MonitorService.MonitorType.MONITOR_TYPE_EXPIRE_NOITFICATION.value());
            intent.putExtra("notif_freq", mNotifFreq);
            /* if immeditly_triggered== false , it will delay one ExpireMonitorRunnable checking */
            intent.putExtra("immeditly_triggered", false);
            Utility.getContext().sendBroadcast(intent);
        } else if (vId == R.id.spin_font_size_scale_factor) {
            Utility.setStringValueForKey(Utility.FONT_SIZE_SCALE_FACTOR, mFontSizeScaleLeves[pos]);
            mOwnerActivity.updateLayoutConfig();
            changeLayoutConfig(mLayout);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void onClick(View view) {
        int id = view.getId();

        if ((id == R.id.tv_database_backup) || (id == R.id.tv_database_restore)) {
            if (mDropboxService != null && !mOwnerActivity.isFinishing()) {
                mProgressDialog = LightProgressDialog.getInstance(mOwnerActivity);

                mProgressDialog.setMessage(getString(R.string.title_progress_startup));
                mProgressDialog.show();

                try {
                    if (id == R.id.tv_database_backup) {
                        if (mCurReqCloudService == CLOUD_SERVICE_TYPE_DROPBOX) {
                            mDropboxService.startBackup(mStatusUpdate);
                        } else if (mCurReqCloudService == CLOUD_SERVICE_TYPE_GOOGLE_DRIVER) {
                            GoogleDriveCloudService driveCloudService = new GoogleDriveCloudService(mGoogleApiClient);

                            driveCloudService.startBakupRestoreTask(GoogleDriveCloudService.Mode.BACKUP_MODE);
                        }
                    } else if (id == R.id.tv_database_restore) {
                        if (mCurReqCloudService == CLOUD_SERVICE_TYPE_DROPBOX) {
                            mDropboxService.startRestore(mStatusUpdate);
                        } else if (mCurReqCloudService == CLOUD_SERVICE_TYPE_GOOGLE_DRIVER) {
                            GoogleDriveCloudService driveCloudService = new GoogleDriveCloudService(mGoogleApiClient);

                            driveCloudService.startBakupRestoreTask(GoogleDriveCloudService.Mode.RESTORE_MODE);
                        }
                    }
                } catch (RemoteException e) {
                    LogUtility.printStackTrace(e);
                }
            }
        } else if (id == R.id.iv_dropbox_enable || id == R.id.iv_googledriver_enable) {
            try {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !mOwnerActivity.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    mOwnerActivity.requestPermissions(MMActivity.PERM_REQ_WRITE_EXT_STORAGE, getString(R.string.perm_rationale_write_ext_storage), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return;
                }

                /* TODO: Need to refactor the button status for duplicate codes. */
                if (id == R.id.iv_dropbox_enable) {
                    mCurReqCloudService = CLOUD_SERVICE_TYPE_DROPBOX;

                    if (mDropboxService == null) {
                        return;
                    }

                    if (mDropboxService.isLinked()) {
                        mDropboxService.disConnect();
                        mIvDropboxEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
                    } else {
                        mDropboxService.connect();

                        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                            mGoogleApiClient.clearDefaultAccountAndReconnect();
                            mGoogleApiClient.disconnect();
                            mIvGoogleDriveEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
                        }
                    }
                } else if (id == R.id.iv_googledriver_enable) {
                    mCurReqCloudService = CLOUD_SERVICE_TYPE_GOOGLE_DRIVER;

                    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.clearDefaultAccountAndReconnect();
                        mGoogleApiClient.disconnect();
                        mIvGoogleDriveEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
                    } else {
                        if (mDropboxService != null && mDropboxService.isLinked()) {
                            mDropboxService.disConnect();
                            mIvDropboxEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
                        }

                        showProgressDialog(null, mResources.getString(R.string.title_progress_waiting_connect));

                        startActivityForResult(AccountPicker.newChooseAccountIntent(
                                null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, getString(R.string.title_account_pick_description), null, null, null), REQUEST_CODE_ACCPICK);
                    }
                }
            } catch (RemoteException e) {
                LogUtility.printStackTrace(e);
                mIvDropboxEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
                mIvGoogleDriveEnableStatus.setImageResource(R.drawable.ic_cloud_service_unselected);
            }
        }
    }

    @Override
    public void update(Object data) {
        if (mOwnerActivity == null) {
            return;
        }

        mOwnerActivity.setMenuItemVisibility(R.id.action_search, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_action_add, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_action_cancel, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_action_new, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_sort_by_date, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_sort_by_name, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_sort_by_place, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_grid_1x1, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_grid_2x1, false);
        mOwnerActivity.setMenuItemVisibility(R.id.menu_clear_expired_items, false);

    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BroadCastEvent event) {
        if (event.getEventType() == BroadCastEvent.BROADCAST_EVENT_TYPE_RESOLVE_CONNECTION_REQUEST
                || event.getEventType() == BroadCastEvent.BROADCAST_EVENT_TYPE_RESOLVE_CANCEL_CONNECTION_REQUEST) {
            if (event.getEventType() == BroadCastEvent.BROADCAST_EVENT_TYPE_RESOLVE_CANCEL_CONNECTION_REQUEST) {
                closeProgressDialog();
            } else {
                mGoogleApiClient.connect();
            }
        } else if (event.getEventType() == BroadCastEvent.BROADCAST_EVENT_TYPE_BACKUP_RESTORE_PROGRESS_UPDATE) {
            try {
                BackupRestoreInfo bri = (BackupRestoreInfo) event.getData();
                mStatusUpdate.updateProgress(bri.getMsg(), bri.getProgress());
            } catch (RemoteException e) {
                LogUtility.printStackTrace(e);
            }
        } else if (event.getEventType() == BroadCastEvent.BROADCAST_EVENT_TYPE_BACKUP_RESTORE_FINISHED) {
            try {
                String finishMsg = (String) event.getData();

                mStatusUpdate.finishBackupOrRestore(finishMsg);
            } catch (RemoteException e) {
                LogUtility.printStackTrace(e);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ACCPICK: {
                if (data != null) {
                    String account = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    if (TextUtils.isEmpty(account)) {
                        return;
                    }

                    mGoogleApiClient = new GoogleApiClient.Builder(mOwnerActivity)
                            .addApi(Drive.API)
                            .addScope(Drive.SCOPE_FILE)
                            .addScope(Drive.SCOPE_APPFOLDER)
                            .setAccountName(account)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .build();

                    mGoogleApiClient.connect();
                } else {
                    closeProgressDialog();
                }
            }
            break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LogUtility.printLogD("randy", "onConnected");
        mHandler.post(() -> {
            closeProgressDialog();
            mIvGoogleDriveEnableStatus.setImageResource(R.drawable.ic_cloud_service_selected);
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        LogUtility.printLogD("randy", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        LogUtility.printLogD("randy", "onConnectionFailed");
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(mOwnerActivity, REQUEST_CODE_RESOLVE_CONNECTION);
            } catch (IntentSender.SendIntentException e) {
                LogUtility.printStackTrace(e);
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), mOwnerActivity, 0).show();
        }
    }

    private class SpinnerSettingsAdapter<String> extends ArrayAdapter<String> {

        public SpinnerSettingsAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            changeLayoutConfig(v);

            return v;
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = super.getDropDownView(position, convertView, parent);

            ((TextView) v).setGravity(Gravity.CENTER);
            changeLayoutConfig(v);

            return v;
        }
    }
}
