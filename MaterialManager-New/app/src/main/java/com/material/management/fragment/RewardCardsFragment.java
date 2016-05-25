package com.material.management.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.material.management.MMActivity;
import com.material.management.RewardLoginActivity;
import com.material.management.MMFragment;
import com.material.management.Observer;
import com.material.management.R;

import android.os.Handler;
import com.material.management.component.FlipAnimation;
import com.material.management.data.BundleInfo;
import com.material.management.data.RewardData;
import com.material.management.data.RewardInfo;
import com.material.management.utils.BarCodeUtility;
import com.material.management.utils.DBUtility;
import com.material.management.utils.Utility;
import com.picasso.Picasso;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

public class RewardCardsFragment extends MMFragment implements Observer {
    private static final int MESSAGE_RELOAD_REWARD = 0;
    private static final int MESSAGE_EDIT_REWARD_INFO = MESSAGE_RELOAD_REWARD + 1;
    private static final int MESSAGE_PREVIEW_REWARD_CARD_PHOTO = MESSAGE_RELOAD_REWARD + 2;
    public static final String ACTION_BAR_BTN_ACTION_NEW = "reward_card_new";

    private View mLayout;
    private RelativeLayout mRlEmptyView;
    private RelativeLayout mRlRewardPreview;
    private RelativeLayout mRlRewardFaceLayout;
    private RecyclerView mRvRewardList;
    private ImageView mIvRewardFrontPreview;
    private ImageView mIvRewardBackPreview;
    private ImageView mIvRewardPreviewDismiss;
    private ImageView mIvChangePreviewFace;

    private FlipAnimation mFlipAnimation;
    private RewardLoadTask mRewardLoadTask = null;
    private RewardListAdapter mRewardListAdapter = null;
    private String mTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mLayout = inflater.inflate(R.layout.fragment_reward_cards, container, false);

        initView();
        init();
        initListener();

        return mLayout;
    }

    private void initListener() {
        mIvChangePreviewFace.setOnClickListener(this);
        mIvRewardPreviewDismiss.setOnClickListener(this);
        /* To consume the click event, so that the child won't display popup menu. */
        mRlRewardPreview.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int id = v.getId();

        switch (id) {
            case R.id.iv_change_reward_preview_face: {
                flipCard();
            }
            break;

            case R.id.iv_reward_preview_dismiss: {
                displayRewardPreviewStatus(mRlRewardPreview.getWidth() / 2, mRlRewardPreview.getHeight() / 2, null);
            }
            break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        sendScreenAnalytics(getString(R.string.ga_app_view_reward_cards_fragment));
        reloadRewardList();
    }

    @Override
    public void onDestroyView() {
        mRewardListAdapter.clear();
        super.onDestroyView();
    }

    private void initView() {
        mRlEmptyView = (RelativeLayout) mLayout.findViewById(R.id.rl_empty_data);
        mRlRewardPreview = (RelativeLayout) mLayout.findViewById(R.id.rl_reward_preview_content);
        mRlRewardFaceLayout = (RelativeLayout) mLayout.findViewById(R.id.rl_reward_face_layout);
        mRvRewardList = (RecyclerView) mLayout.findViewById(R.id.rv_reward_list);
        mIvRewardFrontPreview = (ImageView) mLayout.findViewById(R.id.iv_reward_front_face);
        mIvRewardBackPreview = (ImageView) mLayout.findViewById(R.id.iv_reward_back_face);
        mIvChangePreviewFace = (ImageView) mLayout.findViewById(R.id.iv_change_reward_preview_face);
        mIvRewardPreviewDismiss = (ImageView) mLayout.findViewById(R.id.iv_reward_preview_dismiss);
    }

    private void init() {
        CharSequence titleCharSeq = mOwnerActivity.getActionBar().getTitle();

        if (titleCharSeq != null && !TextUtils.isEmpty(titleCharSeq)) {
            mTitle = titleCharSeq.toString();
        }

        mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_RELOAD_REWARD: {
                        reloadRewardList();
                    }
                    break;

                    case MESSAGE_EDIT_REWARD_INFO: {
                        RewardInfo rewardInfo = (RewardInfo) msg.obj;

                        editRewardInfo(rewardInfo);
                    }
                    break;

                    case MESSAGE_PREVIEW_REWARD_CARD_PHOTO: {
                        RewardInfo rewardInfo = (RewardInfo) msg.obj;

                        mFlipAnimation = new FlipAnimation(mIvRewardFrontPreview, mIvRewardBackPreview);
                        displayRewardPreviewStatus(mRlRewardPreview.getWidth() / 2, mRlRewardPreview.getHeight() / 2, rewardInfo);
                    }
                    break;
                }
                return false;
            }
        });

        mRewardListAdapter = new RewardListAdapter(mOwnerActivity, mInflater, mHandler);

        mRvRewardList.setLayoutManager(new LinearLayoutManager(mOwnerActivity));
        mRvRewardList.setAdapter(mRewardListAdapter);
        mRvRewardList.setItemAnimator(new DefaultItemAnimator());

        update(null);
    }

    private void reloadRewardList() {
        if (mRewardLoadTask != null && !mRewardLoadTask.isCancelled()) {
            mRewardLoadTask.cancel(true);
        }

        RewardLoadTask mRewardLoadTask = new RewardLoadTask(mRewardListAdapter);

        mRewardLoadTask.execute();
    }

    private void editRewardInfo(RewardInfo rewardInfo) {
        Intent intent = new Intent(mOwnerActivity, RewardLoginActivity.class);

        intent.putExtra(BundleInfo.BUNDLE_KEY_REWARD_TYPE, RewardData.REWARD_TYPE_CARD);
        intent.putExtra("title", mTitle);
        intent.putExtra("reward_info", rewardInfo);
        startActivity(intent);
    }

    private void displayRewardPreviewStatus(int cx, int cy, RewardInfo rewardInfo) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !mOwnerActivity.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            mOwnerActivity.requestPermissions(MMActivity.PERM_REQ_READ_EXT_STORAGE, getString(R.string.perm_rationale_read_ext_storage), Manifest.permission.READ_EXTERNAL_STORAGE);
            return;
        }

        int prevVisibility = mRlRewardPreview.getVisibility();

        if (prevVisibility == View.GONE) {
            // get the final radius for the clipping circle
            int finalRadius = Math.max(mRlRewardPreview.getWidth(), mRlRewardPreview.getHeight());
            // create the animator for this view (the start radius is zero)
//            Animator anim = ViewAnimationUtils.createCircularReveal(mRlRewardPreview, cx, cy, 0, finalRadius);
            SupportAnimator animator = ViewAnimationUtils.createCircularReveal(mRlRewardPreview, cx, cy, 0, finalRadius);

            Picasso.with(mOwnerActivity)
                    .load(new File(rewardInfo.getFrontPhotoPath()))
                    .fit()
                    .into(mIvRewardFrontPreview);

            Picasso.with(mOwnerActivity)
                    .load(new File(rewardInfo.getBackPhotoPath()))
                    .fit()
                    .into(mIvRewardBackPreview);

            mIvRewardFrontPreview.setVisibility(View.VISIBLE);
            mIvRewardBackPreview.setVisibility(View.GONE);
            mRlRewardPreview.setVisibility(View.VISIBLE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(1000);
            animator.start();
        } else if (prevVisibility == View.VISIBLE) {
            // get the initial radius for the clipping circle
            int initialRadius = mRlRewardPreview.getWidth();
            // create the animation (the final radius is zero)
            SupportAnimator animator = ViewAnimationUtils.createCircularReveal(mRlRewardPreview, cx, cy, initialRadius, 0);

            // make the view invisible when the animation is done
            animator.addListener(new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationStart() {}

                @Override
                public void onAnimationEnd() {
                    mIvRewardBackPreview.setImageDrawable(null);
                    mIvRewardFrontPreview.setImageDrawable(null);
                    mRlRewardPreview.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel() {}

                @Override
                public void onAnimationRepeat() {}
            });

            // start the animation
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(1000);
            animator.start();
        }
    }

    private void flipCard() {
        if(mIvRewardFrontPreview.getVisibility() == View.GONE || mFlipAnimation.isEqualToView(mIvRewardFrontPreview)) {
            mFlipAnimation.reverse();
        }
        mRlRewardFaceLayout.startAnimation(mFlipAnimation);
    }


    @Override
    public void update(Object data) {
        if (mOwnerActivity == null) {
            return;
        }

        if (data != null) {
            if (data.equals(ACTION_BAR_BTN_ACTION_NEW)) {
                editRewardInfo(null);
            }
        } else {
            mOwnerActivity.setMenuItemVisibility(R.id.action_search, false);
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

    private class RewardLoadTask extends AsyncTask<Void, Void, Void> {
        private RewardListAdapter mAdapter = null;

        public RewardLoadTask(RewardListAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<RewardInfo> rewardInfoList = DBUtility.selectRewardCard();

            mAdapter.clear();
            mAdapter.addAll(rewardInfoList);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (mAdapter.getItemCount() > 0) {
                mRlEmptyView.setVisibility(View.GONE);
                mRvRewardList.setVisibility(View.VISIBLE);
                mAdapter.notifyDataSetChanged();
            } else {
                mRlEmptyView.setVisibility(View.VISIBLE);
                mRvRewardList.setVisibility(View.GONE);
            }
        }
    }


    private static class RewardListAdapter extends RecyclerView.Adapter<RewardListAdapter.ViewHolder> {
        private Context mCtx = null;
        private LayoutInflater mInflater = null;
        private ArrayList<RewardInfo> mRewardInfoList = null;
        private HashMap<RewardInfo, ImageView> mImgViewMap;
        private Handler mHandler;
        private LruCache<RewardInfo, Bitmap> mBarcodeBitmapCache = new LruCache<RewardInfo, Bitmap>(20) {
            /**
             * Called for entries that have been evicted or removed. This method is
             * invoked when a value is evicted to make space, removed by a call to
             * {@link #remove}, or replaced by a call to {@link #put}. The default
             * implementation does nothing.
             *
             * <p>The method is called without synchronization: other threads may
             * access the cache while this method is executing.
             *
             * @param evicted true if the entry is being removed to make space, false
             *     if the removal was caused by a {@link #put} or {@link #remove}.
             * @param newValue the new value for {@code key}, if it exists. If non-null,
             *     this removal was caused by a {@link #put}. Otherwise it was caused by
             *     an eviction or a {@link #remove}.
             */
            protected void entryRemoved(boolean evicted, final RewardInfo key, final Bitmap oldValue, Bitmap newValue) {
                if (evicted) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mImgViewMap.get(key).setImageDrawable(null);
                            Utility.releaseBitmaps(oldValue);
                        }
                    });
                }
            }
        };

        public RewardListAdapter(Context ctx, LayoutInflater inflater, Handler handler) {
            mCtx = ctx;
            mRewardInfoList = new ArrayList<RewardInfo>();
            mImgViewMap = new HashMap<RewardInfo, ImageView>();
            mInflater = inflater;
            mHandler = handler;
        }

        @Override
        public RewardListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemLayout = mInflater.inflate(R.layout.view_reward_row, null);
            ViewHolder viewHolder = new ViewHolder(mCtx, mHandler, itemLayout);

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RewardListAdapter.ViewHolder holder, int position) {
            try {
                RewardInfo rewardInfo = mRewardInfoList.get(position);
                RewardInfo.RewardCardType cardType = RewardInfo.RewardCardType.valueOf(rewardInfo.getCardType());

                holder.setRewardInfo(rewardInfo);
                holder.mVRewardCardLyout.setVisibility(View.GONE);
                holder.mVCouponLayout.setVisibility(View.GONE);
                holder.mVGiftCardLayout.setVisibility(View.GONE);
                /* TODO: reward photo icon  */
                switch (cardType) {
                    case REWARD_CARD: {
                        holder.mVRewardCardLyout.setVisibility(View.VISIBLE);
                        /* reward title, barcode text */
                        holder.mTvRewardCardTitle.setText(rewardInfo.getName());
                        holder.mTvRewardBarCode.setText(rewardInfo.getBarCode());

                        /* barcode bitmap */
                        Bitmap barcodeBitmap = mBarcodeBitmapCache.get(rewardInfo);

                        if (barcodeBitmap == null) {
                            String barcode = rewardInfo.getBarCode();
                            String barcodeFormat = rewardInfo.getBarCodeFormat();

                            if (barcode != null && barcodeFormat != null && !barcode.isEmpty() && !barcodeFormat.isEmpty()) {
                                barcodeBitmap = BarCodeUtility.encodeAsBitmap(rewardInfo.getBarCode(), BarcodeFormat.valueOf(rewardInfo.getBarCodeFormat()), 600, 300);

                                if (barcodeBitmap != null) {
                                    mBarcodeBitmapCache.put(rewardInfo, barcodeBitmap);
                                }
                            }
                        }
                        holder.mIvRewardBarCode.setImageBitmap(barcodeBitmap);
                        mImgViewMap.put(rewardInfo, holder.mIvRewardBarCode);

                    }
                    break;

                    case COUPON_CARD: {
                        holder.mVCouponLayout.setVisibility(View.VISIBLE);
                    }
                    break;

                    case GIFT_CARD: {
                        holder.mVGiftCardLayout.setVisibility(View.VISIBLE);
                    }
                    break;
                }
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }

        public void clear() {
            mBarcodeBitmapCache.evictAll();
            mRewardInfoList.clear();
        }

        public void addAll(List<RewardInfo> list) {
            mRewardInfoList.addAll(list);
        }

        @Override
        public int getItemCount() {
            return mRewardInfoList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
            private Context mCtx;
            private RewardInfo mRewardInfo;
            private Handler mHandler;

            public View mVRewardCardLyout;
            public View mVCouponLayout;
            public View mVGiftCardLayout;

            /* =================== Reward card ==================== */
            public ImageView mIvRewardMenuBtn;
            public ImageView mIvRewardPhotoIcon;
            public ImageView mIvRewardBarCode;
            public TextView mTvRewardBarCode;
            public TextView mTvRewardCardTitle;

            /* =================== Coupon card ==================== */
            public ImageView mIvCouponAlert;
            public ImageView mIvCouponPhoto;
            public ImageView mIvCouponMenuBtn;
            public TextView mTvCouponTitle;
            public TextView mTvCouponValue;
            public TextView mTvCouponExpiry;
            public TextView mTvCouponComment;


            /* =================== Gift card ==================== */
            public ImageView mIvGiftAlert;
            public ImageView mIvGiftPhoto;
            public ImageView mIvGiftBarCode;
            public ImageView mIvGiftMenuBtn;
            public TextView mTvGiftTitle;
            public TextView mTvGiftBarCode;
            public TextView mTvGiftCardBalance;
            public TextView mTvGiftCardExpiry;
            public TextView mTvGiftCardPin;

            public ViewHolder(Context ctx, Handler handler, View itemView) {
                super(itemView);

                this.mCtx = ctx;
                this.mHandler = handler;

                initView();
                initListener();
            }

            public void setRewardInfo(RewardInfo rewardInfo) {
                this.mRewardInfo = rewardInfo;
            }

            private void initView() {
                mVRewardCardLyout = itemView.findViewById(R.id.rewardCardLayout);
                mVCouponLayout = itemView.findViewById(R.id.couponLayout);
                mVGiftCardLayout = itemView.findViewById(R.id.giftCardLayout);

            /* =================== Reward card ==================== */
                mIvRewardMenuBtn = (ImageView) itemView.findViewById(R.id.menuButton);
                mIvRewardPhotoIcon = (ImageView) itemView.findViewById(R.id.rewardPhotoIcon);
                mIvRewardBarCode = (ImageView) itemView.findViewById(R.id.barcodeImageView);
                mTvRewardBarCode = (TextView) itemView.findViewById(R.id.barcodeValueTextView);
                mTvRewardCardTitle = (TextView) itemView.findViewById(R.id.nameTextView);

            /* =================== Coupon card ==================== */
                mIvCouponAlert = (ImageView) itemView.findViewById(R.id.alertSetIcon);
                mIvCouponPhoto = (ImageView) itemView.findViewById(R.id.couponPhotoIcon);
                mIvCouponMenuBtn = (ImageView) itemView.findViewById(R.id.couponMenuButton);
                mTvCouponTitle = (TextView) itemView.findViewById(R.id.couponNameTextView);
                mTvCouponValue = (TextView) itemView.findViewById(R.id.couponValueTextView);
                mTvCouponExpiry = (TextView) itemView.findViewById(R.id.couponExpiryTextView);
                mTvCouponComment = (TextView) itemView.findViewById(R.id.couponNoteTextView);

            /* =================== Gift card ==================== */
                mIvGiftAlert = (ImageView) itemView.findViewById(R.id.giftCardAlertSetIcon);
                mIvGiftPhoto = (ImageView) itemView.findViewById(R.id.giftPhotoIcon);
                mIvGiftBarCode = (ImageView) itemView.findViewById(R.id.giftCardBarcodeImageView);
                mIvGiftMenuBtn = (ImageView) itemView.findViewById(R.id.giftCardMenuButton);
                mTvGiftTitle = (TextView) itemView.findViewById(R.id.giftCardNameTextView);
                mTvGiftBarCode = (TextView) itemView.findViewById(R.id.giftCardBarcodeValueTextView);
                mTvGiftCardBalance = (TextView) itemView.findViewById(R.id.giftCardBalanceTextView);
                mTvGiftCardExpiry = (TextView) itemView.findViewById(R.id.giftCardExpiryTextView);
                mTvGiftCardPin = (TextView) itemView.findViewById(R.id.giftCardPINTextView);
            }

            private void initListener() {
                mIvRewardPhotoIcon.setOnClickListener(this);
                mIvCouponPhoto.setOnClickListener(this);
                mIvGiftPhoto.setOnClickListener(this);

                mIvRewardMenuBtn.setOnClickListener(this);
                mIvCouponMenuBtn.setOnClickListener(this);
                mIvGiftMenuBtn.setOnClickListener(this);
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();

                switch (id) {
                    case R.id.menuButton:
                    case R.id.couponMenuButton:
                    case R.id.giftCardMenuButton: {
                        PopupMenu popup = new PopupMenu(mCtx, v);

                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                int id = item.getItemId();

                                switch (id) {
                                    case R.id.menu_edit: {
                                        Message msg = Message.obtain();
                                        msg.what = MESSAGE_EDIT_REWARD_INFO;
                                        msg.obj = mRewardInfo;

                                        mHandler.sendMessage(msg);
                                    }
                                    break;

                                    case R.id.menu_del: {
                                        DBUtility.deleteRewardCard(mRewardInfo);
                                        mHandler.sendEmptyMessage(MESSAGE_RELOAD_REWARD);
                                    }
                                    break;
                                }
                                return false;
                            }
                        });

                        popup.getMenuInflater().inflate(R.menu.fragment_reward_list_popup_menu, popup.getMenu());
                        popup.show();
                    }
                    break;

                    case R.id.rewardPhotoIcon:
                    case R.id.couponPhotoIcon:
                    case R.id.giftPhotoIcon: {
                        /* TODO: Maybe need to be refined because of duplicate code with R.id.menu_edit event .*/
                        Message msg = Message.obtain();
                        msg.what = MESSAGE_PREVIEW_REWARD_CARD_PHOTO;
                        msg.obj = mRewardInfo;

                        mHandler.sendMessage(msg);
                    }
                    break;
                }
            }
        }
    }
}
