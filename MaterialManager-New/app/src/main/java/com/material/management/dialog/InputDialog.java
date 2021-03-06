package com.material.management.dialog;

import com.material.management.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

public class InputDialog extends AlertDialog.Builder {
    private String mTitle;
    private String mMessage;
    private Context mContext;
    private boolean mIsShown;

    private EditText mEtInputText;
    private DialogInterface.OnClickListener mListener;
    private AlertDialog mDialog;

    public InputDialog(Context context, String title, String message, DialogInterface.OnClickListener listener) {
        super(context, R.style.AlertDialogTheme);
        this.mTitle = title;
        this.mMessage = message;
        this.mContext = context;
        this.mListener = listener;
        initView();
    }

    public InputDialog(Context context, String title, String message, String defaultText, DialogInterface.OnClickListener listener) {
        this(context, title, message, listener);
        this.mEtInputText.setText(defaultText);
    }

    private void initView() {
        mEtInputText = new EditText(mContext);

        this.setTitle(mTitle);
        this.setMessage(mMessage);
        this.setView(mEtInputText);
        this.setPositiveButton(mContext.getString(R.string.title_positive_btn_label), mListener);
        this.setNegativeButton(mContext.getString(R.string.title_negative_btn_label), mListener);
    }

    public String getInputString() {
        return mEtInputText.getText().toString();
    }

    @Override
    public AlertDialog show() {
        mIsShown = true;
        mDialog = super.show();
        Window window = mDialog.getWindow();

        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        return mDialog;
    }

    public boolean isDialogShowing() {
        return mIsShown;
    }

    public void setShowState(boolean isShowing) {
        mIsShown = isShowing;

        if(mDialog != null && !isShowing) {
            mDialog.dismiss();
        }
    }
}
