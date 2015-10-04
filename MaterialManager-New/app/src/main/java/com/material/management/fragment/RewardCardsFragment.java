package com.material.management.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.datetimepicker.date.DatePickerDialog;
import com.material.management.RewardLoginActivity;
import com.material.management.MMFragment;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.data.BundleInfo;
import com.material.management.data.RewardData;

public class RewardCardsFragment extends MMFragment implements Observer, DatePickerDialog.OnDateSetListener {
    public static final String ACTION_BAR_BTN_ACTION_NEW = "reward_card_new";

    private View mLayout;

    private Object mData;
    private String mTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayout = inflater.inflate(R.layout.fragment_reward_cards, container, false);

        initView();
        initListener();
        init();


        return mLayout;
    }

    private void initView() {

    }

    private void initListener() {

    }

    private void init() {
        mTitle = mOwnerActivity.getActionBar().getTitle().toString();

        update(null);
    }


    @Override
    public void update(Object data) {
        mData = data;

        if (mOwnerActivity == null) {
            return;
        }

        if (data != null) {
            if (data.equals(ACTION_BAR_BTN_ACTION_NEW)) {
                Intent intent = new Intent(mOwnerActivity, RewardLoginActivity.class);

                intent.putExtra(BundleInfo.BUNDLE_KEY_REWARD_TYPE, RewardData.REWARD_TYPE_CARD);
                intent.putExtra("title", mTitle);
                startActivity(intent);
            }
        } else {
            mOwnerActivity.setMenuItemVisibility(R.id.action_search, true);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_action_add, false);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_action_cancel, false);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_action_new, true);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_sort_by_date, false);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_sort_by_name, false);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_sort_by_place, false);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_grid_1x1, false);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_grid_2x1, false);
            mOwnerActivity.setMenuItemVisibility(R.id.menu_clear_expired_items, false);
        }
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
    }
}
