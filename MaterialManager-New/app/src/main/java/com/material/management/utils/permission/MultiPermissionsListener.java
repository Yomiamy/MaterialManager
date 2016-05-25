package com.material.management.utils.permission;

import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.material.management.MMActivity;
import com.material.management.MainActivity;

import java.util.List;

public class MultiPermissionsListener implements MultiplePermissionsListener {
    private MMActivity mActivity;
    private String mTag;
    private String mPermRationale;

    public MultiPermissionsListener(MMActivity activity, String permRationale, String tag) {
        this.mActivity = activity;
        this.mTag = tag;
        this.mPermRationale = permRationale;
    }

    @Override
    public void onPermissionsChecked(MultiplePermissionsReport report) {
        for (PermissionGrantedResponse response : report.getGrantedPermissionResponses()) {
            mActivity.showPermissionGranted(response.getPermissionName(), mTag);
        }

        for (PermissionDeniedResponse response : report.getDeniedPermissionResponses()) {
            mActivity.showPermissionDenied(response.getPermissionName(), response.isPermanentlyDenied(), mTag);
        }
    }

    @Override
    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
        mActivity.showPermissionRationale(token, mPermRationale);
    }
}
