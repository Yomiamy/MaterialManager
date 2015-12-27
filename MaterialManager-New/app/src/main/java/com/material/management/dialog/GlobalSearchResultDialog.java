package com.material.management.dialog;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.material.management.GroceryItemLoginActivity;
import com.material.management.MaterialModifyActivity;
import com.material.management.R;
import com.material.management.component.RoundedImageView;
import com.material.management.data.GlobalSearchData;
import com.material.management.data.GroceryItem;
import com.material.management.data.GroceryListData;
import com.material.management.data.Material;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.Utility;
import com.picasso.Picasso;

import java.io.File;
import java.util.Date;

public class GlobalSearchResultDialog extends AlertDialog.Builder implements View.OnClickListener {
    private View mLayout;
    private ImageView mIvTopBackground;
    private RoundedImageView mRivHead;
    private Button mBtnClose;
    private Button mBtnEdit;
    /* Material layout */
    private LinearLayout mLlMaterialInfoLayout;
    private TextView mTvMaterialName;
    private TextView mTvMaterialCategory;
    private TextView mTvExpireStatus;
    private TextView mTvMaterialBarcode;
    /* Grocery layout */
    private LinearLayout mLlGroceryInfoLayout;
    private TextView mTvGroceryName;
    private TextView mTvGroceryCategory;
    private TextView mTvGroceryShopListName;
    private TextView mTvGroceryShopDate;
    private TextView mTvGroceryCount;
    private TextView mTvGroceryShopCost;
    private TextView mTvGroceryBarcode;

    private Context mCtx;
    private GlobalSearchData mSearchData;
    private AlertDialog mDialog;

    public GlobalSearchResultDialog(Context context, GlobalSearchData searchData) {
        super(context);
        this.mCtx = context;
        this.mSearchData = searchData;

        initView();
        initListener();
        init();
    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayout = inflater.inflate(R.layout.view_search_info_dialog, null);
        mIvTopBackground = (ImageView) mLayout.findViewById(R.id.topBackground);
        mRivHead = (RoundedImageView) mLayout.findViewById(R.id.riv_head);
        mBtnClose = (Button) mLayout.findViewById(R.id.btn_close);
        mBtnEdit = (Button) mLayout.findViewById(R.id.group_dialog_edit);

        /* Material info layout. */
        mLlMaterialInfoLayout = (LinearLayout) mLayout.findViewById(R.id.ll_material_info_layout);
        mTvMaterialName = (TextView) mLayout.findViewById(R.id.tv_material_name);
        mTvMaterialCategory = (TextView) mLayout.findViewById(R.id.tv_material_category);
        mTvExpireStatus = (TextView) mLayout.findViewById(R.id.tv_expire_status);
        mTvMaterialBarcode = (TextView) mLayout.findViewById(R.id.tv_material_barcode);
        /* Grocery info layout. */
        mLlGroceryInfoLayout = (LinearLayout) mLayout.findViewById(R.id.ll_grocery_info_layout);
        mTvGroceryName = (TextView) mLayout.findViewById(R.id.tv_grocery_name);
        mTvGroceryCategory = (TextView) mLayout.findViewById(R.id.tv_grocery_category);
        mTvGroceryShopListName = (TextView) mLayout.findViewById(R.id.tv_grocery_shop_list_name);
        mTvGroceryShopDate = (TextView) mLayout.findViewById(R.id.tv_grocery_shop_date);
        mTvGroceryCount = (TextView) mLayout.findViewById(R.id.tv_grocery_shop_count);
        mTvGroceryShopCost = (TextView) mLayout.findViewById(R.id.tv_grocery_shop_cost);
        mTvGroceryBarcode = (TextView) mLayout.findViewById(R.id.tv_grocery_barcode);

        setView(mLayout);
    }

    private void initListener() {
        mBtnClose.setOnClickListener(this);
        mBtnEdit.setOnClickListener(this);
    }

    private void init() {
        Resources res = mCtx.getResources();
        TypedArray topBackgroundsAry = res.obtainTypedArray(R.array.top_backgrounds);
        int topBackgroundIndex = (int) (Math.random() * topBackgroundsAry.length());

        mIvTopBackground.setImageDrawable(topBackgroundsAry.getDrawable(topBackgroundIndex));

        switch (mSearchData.getItemType()) {
            case MATERIAL_ITEM: {
                mLlMaterialInfoLayout.setVisibility(View.VISIBLE);
                mLlGroceryInfoLayout.setVisibility(View.GONE);

                Material material = mSearchData.getMaterial();
                String barcode = material.getBarcode();
                String barcodeFormat = material.getBarcodeFormat();

                Picasso.with(mCtx).load(new File(material.getMaterialPicPath())).fit().into(mRivHead);
                mTvMaterialName.setText(material.getName());
                mTvMaterialCategory.setText(material.getMaterialType());
                mTvExpireStatus.setText(mSearchData.getItemRestExpDays());
                setBarcode(mTvMaterialBarcode, barcodeFormat, barcode);
            }
            break;

            case GROCERY_ITEM: {
                mLlMaterialInfoLayout.setVisibility(View.GONE);
                mLlGroceryInfoLayout.setVisibility(View.VISIBLE);

                GroceryItem groceryItem = mSearchData.getGroceryItem();
                String barcode = groceryItem.getBarcode();
                String barcodeFormat = groceryItem.getBarcodeFormat();
                GroceryListData groceryListData = DBUtility.selectGroceryListHistoryInfosById(groceryItem.getGroceryListId());
                groceryListData = (groceryListData == null) ? DBUtility.selectGroceryListInfosById(groceryItem.getGroceryListId()) : groceryListData;
                Date checkoutTime = groceryListData.getCheckOutTime();

                Picasso.with(mCtx).load(new File(groceryItem.getGroceryPicPath())).fit().into(mRivHead);
                mTvGroceryName.setText(groceryItem.getName());
                mTvGroceryCategory.setText(groceryItem.getGroceryType());
                mTvGroceryShopListName.setText(res.getString(R.string.global_search_shop_place, groceryListData.getGroceryListName()));
                mTvGroceryShopDate.setText((checkoutTime != null) ? res.getString(R.string.global_search_checkout_date, Utility.transDateToString("yyyy-MM-dd HH:mm:ss", checkoutTime)) : res.getString(R.string.global_search_not_checkout));
                mTvGroceryCount.setText(res.getString(R.string.global_search_qty, groceryItem.getQty()));
                mTvGroceryShopCost.setText(res.getString(R.string.global_search_price, groceryItem.getPrice()));
                setBarcode(mTvGroceryBarcode, barcodeFormat, barcode);
            }
            break;
        }
    }

    private void setBarcode(TextView tvBarcode, String barcodeFormat, String barcode) {
        try {
            Resources res = mCtx.getResources();

            if (barcode != null && barcodeFormat != null && !barcode.isEmpty() && !barcodeFormat.isEmpty()) {
                Bitmap barcodeBitmap = BarCodeUtility.encodeAsBitmap(barcode,
                        BarcodeFormat.valueOf(barcodeFormat), 600, 300);
                Drawable barcodeDrawable = new BitmapDrawable(res, barcodeBitmap);

                barcodeDrawable.setBounds(0, 0, barcodeDrawable.getIntrinsicWidth(),
                        barcodeDrawable.getIntrinsicHeight());
                tvBarcode.setCompoundDrawables(null, barcodeDrawable, null, null);
                tvBarcode.setText(barcode);
                tvBarcode.setVisibility(View.VISIBLE);
            } else {
                tvBarcode.setVisibility(View.GONE);
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btn_close: {
                close();
            }
            break;

            case R.id.group_dialog_edit: {
                GlobalSearchData.ItemType type = mSearchData.getItemType();
                Intent intent = new Intent();

                if (type == GlobalSearchData.ItemType.MATERIAL_ITEM) {
                    intent.setClass(mCtx, MaterialModifyActivity.class);
                    intent.putExtra("material_item", mSearchData.getMaterial());
                } else if (type == GlobalSearchData.ItemType.GROCERY_ITEM) {
                    intent.setClass(mCtx, GroceryItemLoginActivity.class);
                    intent.putExtra("grocery_item", mSearchData.getGroceryItem());
                }
                mCtx.startActivity(intent);
                close();
            }
            break;
        }

    }

    private void close() {
//        Drawable topBackgroundDrawable = mIvTopBackground.getDrawable();
        Drawable headDrawable = mRivHead.getDrawable();
        Drawable[] materialCompoundDrawables = mTvMaterialBarcode.getCompoundDrawables();
        Drawable[] groceryCompoundDrawables = mTvGroceryBarcode.getCompoundDrawables();
//        Bitmap topBackgroundBmp = topBackgroundDrawable != null && (topBackgroundDrawable instanceof BitmapDrawable) ? ((BitmapDrawable) topBackgroundDrawable).getBitmap() : null;
        Bitmap headBmp = headDrawable != null && (headDrawable instanceof BitmapDrawable) ? ((BitmapDrawable) headDrawable).getBitmap() : null;
        Bitmap materialBarcodeBmp = (materialCompoundDrawables != null && materialCompoundDrawables.length > 0) ? ((materialCompoundDrawables[1] instanceof BitmapDrawable) ? ((BitmapDrawable) materialCompoundDrawables[1]).getBitmap() : null) : null;
        Bitmap groceryBarcodeBmp = (groceryCompoundDrawables != null && groceryCompoundDrawables.length > 0) ? ((groceryCompoundDrawables[1] instanceof BitmapDrawable) ? ((BitmapDrawable) groceryCompoundDrawables[1]).getBitmap() : null) : null;

//        mIvTopBackground.setImageDrawable(null);
        mRivHead.setImageDrawable(null);
        mTvMaterialBarcode.setCompoundDrawables(null, null, null, null);
        mTvGroceryBarcode.setCompoundDrawables(null, null, null, null);
        Utility.releaseBitmaps(headBmp, materialBarcodeBmp, groceryBarcodeBmp);
        Utility.forceGC(true);
        mDialog.dismiss();
    }

    @Override
    public AlertDialog show() {
        mDialog = super.show();
        Window window = mDialog.getWindow();

        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        return mDialog;
    }


}
