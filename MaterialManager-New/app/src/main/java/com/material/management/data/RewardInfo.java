package com.material.management.data;


import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;

public class RewardInfo implements Parcelable {
    /* For global search usage. */
    protected GlobalSearchData.ItemType mItemType = GlobalSearchData.ItemType.REWARD_CARD;

    public enum RewardCardType {
        REWARD_CARD("REWARD_CARD"), COUPON_CARD("COUPON_CARD"), GIFT_CARD("GIFT_CARD");

        private String mType;

        RewardCardType(String type) {
            mType = type;
        }

        public String type() {
            return mType;
        }

    }

    ;

    private String mName = "";
    private String mCardType = "";
    private String mBarCode = "";
    private String mBarCodeFormat = "";
    private String mFrontPhotoPath = "";
    private Bitmap mFrontRewardPhoto;
    private String mBackPhotoPath = "";
    private Bitmap mBackRewardPhoto;
    private String mCouponValue = "";
    private Calendar mValidDateFrom = Calendar.getInstance();
    private Calendar mExpiry = Calendar.getInstance();
    private int mNotificationDays = 0;
    private String mComment = "";

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getCardType() {
        return mCardType;
    }

    public void setCardType(String mCardType) {
        this.mCardType = mCardType;
    }

    public String getBarCode() {
        return mBarCode;
    }

    public void setBarCode(String mBarCode) {
        this.mBarCode = mBarCode;
    }

    public String getBarCodeFormat() {
        return mBarCodeFormat;
    }

    public void setBarCodeFormat(String mBarCodeFormat) {
        this.mBarCodeFormat = mBarCodeFormat;
    }

    public Bitmap getFrontRewardPhoto() {
        return mFrontRewardPhoto;
    }

    public void setFrontRewardPhoto(Bitmap mRewardPhoto) {
        this.mFrontRewardPhoto = mRewardPhoto;
    }

    public String getFrontPhotoPath() {
        return mFrontPhotoPath;
    }

    public void setFrontPhotoPath(String mPhotoPath) {
        this.mFrontPhotoPath = mPhotoPath;
    }

    public String getBackPhotoPath() {
        return mBackPhotoPath;
    }

    public void setBackPhotoPath(String mBackPhotoPath) {
        this.mBackPhotoPath = mBackPhotoPath;
    }

    public Bitmap getBackRewardPhoto() {
        return mBackRewardPhoto;
    }

    public void setBackRewardPhoto(Bitmap mBackRewardPhoto) {
        this.mBackRewardPhoto = mBackRewardPhoto;
    }

    public String getCouponValue() {
        return mCouponValue;
    }

    public void setCouponValue(String mCouponValue) {
        this.mCouponValue = mCouponValue;
    }

    public Calendar getValidDateFrom() {
        return mValidDateFrom;
    }

    public void setValidDateFrom(Calendar mValidDateFrom) {
        this.mValidDateFrom = mValidDateFrom;
    }

    public Calendar getExpiry() {
        return mExpiry;
    }

    public void setExpiry(Calendar mExpiry) {
        this.mExpiry = mExpiry;
    }

    public int getNotificationDays() {
        return mNotificationDays;
    }

    public void setNotificationDays(int mNotificationDays) {
        this.mNotificationDays = mNotificationDays;
    }

    public String getComment() {
        return mComment;
    }

    public void setComment(String mComment) {
        this.mComment = mComment;
    }

    public RewardInfo() {
    }

    RewardInfo(Parcel p) {
        mName = p.readString();
        mCardType = p.readString();
        mBarCode = p.readString();
        mBarCodeFormat = p.readString();
        mFrontPhotoPath = p.readString();
        mFrontRewardPhoto = p.readParcelable(Bitmap.class.getClassLoader());
        mBackPhotoPath = p.readString();
        mBackRewardPhoto = p.readParcelable(Bitmap.class.getClassLoader());
        mCouponValue = p.readString();
        mValidDateFrom = (Calendar) p.readSerializable();
        mExpiry = (Calendar) p.readSerializable();
        mNotificationDays = p.readInt();
        mComment = p.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mCardType);
        dest.writeString(mBarCode);
        dest.writeString(mBarCodeFormat);
        dest.writeString(mFrontPhotoPath);
        dest.writeParcelable(mFrontRewardPhoto, flags);
        dest.writeString(mBackPhotoPath);
        dest.writeParcelable(mBackRewardPhoto, flags);
        dest.writeString(mCouponValue);
        dest.writeSerializable(mValidDateFrom);
        dest.writeSerializable(mExpiry);
        dest.writeInt(mNotificationDays);
        dest.writeString(mComment);
    }

    public static final Parcelable.Creator<RewardInfo> CREATOR = new Parcelable.Creator<RewardInfo>() {
        public RewardInfo createFromParcel(Parcel p) {
            return new RewardInfo(p);
        }

        @Override
        public RewardInfo[] newArray(int size) {
            return new RewardInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
