package com.material.management.utils;

import android.view.View;

import com.material.management.BuildConfig;

public class TestInstructUtility {

    /**
     *  Used for Espresso testing to identify the view by description.
     *
     * */
    public static void setViewDesp(View view, String description) {
        if(BuildConfig.DEBUG) {
            view.setContentDescription(description);
        }
    }
}
