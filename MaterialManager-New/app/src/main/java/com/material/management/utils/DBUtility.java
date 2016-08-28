package com.material.management.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.DropboxAPI.UploadRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.material.management.MaterialManagerApplication;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.data.GroceryItem;
import com.material.management.data.GroceryListData;
import com.material.management.data.Material;
import com.material.management.data.BackupRestoreInfo;
import com.material.management.data.RewardInfo;
import com.material.management.provider.MaterialProvider;

public class DBUtility {
    private static ContentResolver sResolver = Utility.getContext().getContentResolver();
    private static Options sOptions = new Options();
    private static Context sContext = Utility.getContext();
    private static String sStringCharSet = "UTF-8";

    static {
        sOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        sOptions.inPurgeable = true;
        sOptions.inInputShareable = true;
    }

    public synchronized static void insertMaterialInfo(Material materialInfo) {
        ContentValues value = new ContentValues();
        String picPath = materialInfo.getMaterialPicPath();

        /* If we have the pic path information, then we reuse it. */
        if (picPath == null || picPath.isEmpty()) {
            picPath = FileUtility.saveMaterialPhoto(materialInfo.getName(), materialInfo.getMaterialPic());
        }

        try {
            value.put("name", new String(materialInfo.getName().getBytes(), sStringCharSet));
            value.put("barcode", new String(materialInfo.getBarcode().getBytes(), sStringCharSet));
            value.put("barcode_format", new String(materialInfo.getBarcodeFormat().getBytes(), sStringCharSet));
            value.put("material_type", new String(materialInfo.getMaterialType().getBytes(), sStringCharSet));
            value.put("is_as_photo_type", materialInfo.getIsAsPhotoType());
            value.put("material_place", new String(materialInfo.getMaterialPlace().getBytes(), sStringCharSet));
            value.put("purchase_date", Utility.transDateToString(materialInfo.getPurchaceDate().getTime()));
            value.put("valid_date", Utility.transDateToString(materialInfo.getValidDate().getTime()));
            value.put("is_valid_date_setup", materialInfo.getIsValidDateSetup());
            value.put("notification_days", materialInfo.getNotificationDays());
            value.put("photo_path", new String(picPath.getBytes(), sStringCharSet));
            value.put("comment", new String(materialInfo.getComment().getBytes(), sStringCharSet));

            sResolver.insert(MaterialProvider.URI_MATERIAL, value);

            value.remove("is_as_photo_type");
            value.remove("is_valid_date_setup");
            value.remove("purchase_date");
            value.remove("valid_date");
            value.remove("notification_days");
            updateMaterialHistory(value);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private synchronized static void insertMaterialHistory(ContentValues value) {
        sResolver.insert(MaterialProvider.URI_MATERIAL_HISTORY, value);
    }

    /* As a photo type */
    public synchronized static void updateMaterialIsAsPhotoType(Material materialInfo) {
        ContentValues value = new ContentValues();

        try {
            String name = new String(materialInfo.getName().getBytes(), sStringCharSet);
            String barcode = new String(materialInfo.getBarcode().getBytes(), sStringCharSet);
            String barcodeFormat = new String(materialInfo.getBarcodeFormat().getBytes(), sStringCharSet);
            String materialType = new String(materialInfo.getMaterialType().getBytes(), sStringCharSet);
            String materialPlace = new String(materialInfo.getMaterialPlace().getBytes(), sStringCharSet);
            String purchaceDate = Utility.transDateToString(materialInfo.getPurchaceDate().getTime());
            String validDate = Utility.transDateToString(materialInfo.getValidDate().getTime());
            String comment = new String(materialInfo.getComment().getBytes(), sStringCharSet);

            /* Reset all "as_photo_type" */
            value.put("is_as_photo_type", 0);
            sResolver.update(MaterialProvider.URI_MATERIAL, value, "material_type='" + materialType + "'", null);

            value = new ContentValues();
            value.put("is_as_photo_type", materialInfo.getIsAsPhotoType());
            sResolver.update(MaterialProvider.URI_MATERIAL, value, "name=? and barcode=? "
                    + "and barcode_format=? and material_type=? "
                    + "and material_place=? and purchase_date=? "
                    + "and valid_date=? and comment=?", new String[]{name, barcode, barcodeFormat, materialType, materialPlace, purchaceDate, validDate, comment});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* update SD card photo path */
    public synchronized static void updateSdPhotoPath(Material materialInfo, String newPhotoPath) {
        try {
            String name = new String(materialInfo.getName().getBytes(), sStringCharSet);
            String barcode = new String(materialInfo.getBarcode().getBytes(), sStringCharSet);
            String barcodeFormat = new String(materialInfo.getBarcodeFormat().getBytes(), sStringCharSet);
            String materialType = new String(materialInfo.getMaterialType().getBytes(), sStringCharSet);
            String materialPlace = new String(materialInfo.getMaterialPlace().getBytes(), sStringCharSet);
            String purchaceDate = Utility.transDateToString(materialInfo.getPurchaceDate().getTime());
            String validDate = Utility.transDateToString(materialInfo.getValidDate().getTime());
            String comment = new String(materialInfo.getComment().getBytes(), sStringCharSet);

            /* update photo path */
            ContentValues value = new ContentValues();

            value.put("name", name);
            value.put("barcode", barcode);
            value.put("barcode_format", barcodeFormat);
            value.put("material_type", materialType);
            value.put("photo_path", newPhotoPath);
            value.put("material_place", materialPlace);
            value.put("comment", comment);

            sResolver.update(MaterialProvider.URI_MATERIAL, value,
                    "name=? and barcode=? "
                            + "and barcode_format=? and material_type=? "
                            + "and material_place=? and purchase_date=? "
                            + "and valid_date=? and comment=?", new String[]{name, barcode, barcodeFormat, materialType, materialPlace, purchaceDate, validDate, comment});
            updateMaterialHistory(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static void updateMateriaInfo(Material newMaterialInfo, Material oldMaterialInfo) {
        try {
            String oldName = new String(oldMaterialInfo.getName().getBytes(), sStringCharSet);
            String oldType = new String(oldMaterialInfo.getMaterialType().getBytes(), sStringCharSet);
            int isAsPhotoType = oldMaterialInfo.getIsAsPhotoType();
            String oldMaterialPlace = new String(oldMaterialInfo.getMaterialPlace().getBytes(), sStringCharSet);
            int oldIsValidDateSetup = oldMaterialInfo.getIsValidDateSetup();
            int oldNotificationDays = oldMaterialInfo.getNotificationDays();
            String oldPicPath = Utility.getPathFromUri(Utility.getImageUri(oldMaterialInfo.getMaterialPic()));
            String oldBarcode = new String(oldMaterialInfo.getBarcode().getBytes(), sStringCharSet);
            String oldBarcodeFormat = new String(oldMaterialInfo.getBarcodeFormat().getBytes(), sStringCharSet);
            String oldComment = new String(oldMaterialInfo.getComment().getBytes(), sStringCharSet);

            ContentValues value = new ContentValues();
            value.put("name", new String(newMaterialInfo.getName().getBytes(), sStringCharSet));
            value.put("material_type", new String(newMaterialInfo.getMaterialType().getBytes(), sStringCharSet));
            value.put("material_place", new String(newMaterialInfo.getMaterialPlace().getBytes(), sStringCharSet));
            value.put("is_valid_date_setup", newMaterialInfo.getIsValidDateSetup());
            value.put("notification_days", newMaterialInfo.getNotificationDays());
            value.put("photo_path", Utility.getPathFromUri(Utility.getImageUri(newMaterialInfo.getMaterialPic())));
            value.put("barcode", new String(newMaterialInfo.getBarcode().getBytes(), sStringCharSet));
            value.put("barcode_format", new String(newMaterialInfo.getBarcodeFormat().getBytes(), sStringCharSet));
            value.put("comment", new String(newMaterialInfo.getComment().getBytes(), sStringCharSet));

            sResolver.update(MaterialProvider.URI_MATERIAL, value,
                    "name=? and barcode=? "
                            + "and is_as_photo_type=" + isAsPhotoType
                            + "and barcode_format=? and material_type=? "
                            + "and notification_days=" + oldNotificationDays + " and is_valid_date_setup=" + oldIsValidDateSetup + " and photo_path=? "
                            + "and material_place=? and comment=?", new String[]{oldName, oldBarcode, oldBarcodeFormat, oldType, oldPicPath, oldMaterialPlace, oldComment});
            updateMaterialHistory(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized static void updateMaterialHistory(Material materialInfo) {
        try {
            ContentValues value = new ContentValues();
            String name = new String(materialInfo.getName().getBytes(), sStringCharSet);
            String materialType = new String(materialInfo.getMaterialType().getBytes(), sStringCharSet);
            String barcode = new String(materialInfo.getBarcode().getBytes(), sStringCharSet);
            String barcodeFormat = new String(materialInfo.getBarcodeFormat().getBytes(), sStringCharSet);
            String picPath = Utility.getPathFromUri(Utility.getImageUri(materialInfo.getMaterialPic()));
            String materialPlace = new String(materialInfo.getMaterialPlace().getBytes(), sStringCharSet);
            String comment = new String(materialInfo.getComment().getBytes(), sStringCharSet);

            /* Need barcode information as index */
            if (barcode == null || barcodeFormat == null || barcode.isEmpty() || barcodeFormat.isEmpty())
                return;

            value.put("name", name);
            value.put("material_type", materialType);
            value.put("barcode", barcode);
            value.put("barcode_format", barcodeFormat);
            value.put("photo_path", picPath);
            value.put("material_place", materialPlace);
            value.put("comment", comment);

            int affectRows = sResolver.update(MaterialProvider.URI_MATERIAL_HISTORY, value, "barcode=? and barcode_format=?", new String[]{barcode, barcodeFormat});
            if (affectRows <= 0) {
                insertMaterialHistory(value);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private synchronized static void updateMaterialHistory(ContentValues value) {
        String barcode = (String) value.get("barcode");
        String barcodeFormat = (String) value.get("barcode_format");

        /* Need barcode information as index */
        if (barcode == null || barcodeFormat == null || barcode.isEmpty() || barcodeFormat.isEmpty())
            return;

        int affectRows = sResolver.update(MaterialProvider.URI_MATERIAL_HISTORY, value, "barcode=? and barcode_format=?",
                new String[]{barcode, barcodeFormat});

        if (affectRows <= 0) {
            insertMaterialHistory(value);
        }
    }

    public synchronized static void deleteMaterialInfoByType(String materialType) {
        try {
            materialType = new String(materialType.getBytes(), sStringCharSet);

            sResolver.delete(MaterialProvider.URI_MATERIAL, "material_type=?", new String[]{materialType});
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }

    public synchronized static void deleteMaterialInfo(Material materialInfo) {
        try {
            /* FIXME: doesn't include bitmap path may not be */
            String name = new String(materialInfo.getName().getBytes(), sStringCharSet);
            String barcode = new String(materialInfo.getBarcode().getBytes(), sStringCharSet);
            String barcodeFormat = new String(materialInfo.getBarcodeFormat().getBytes(), sStringCharSet);
            String materialType = new String(materialInfo.getMaterialType().getBytes(), sStringCharSet);
            int isAsPhotoType = materialInfo.getIsAsPhotoType();
            int isValidDateSetup = materialInfo.getIsValidDateSetup();
            String materialPlace = new String(materialInfo.getMaterialPlace().getBytes(), sStringCharSet);
            String purchaceDate = Utility.transDateToString(materialInfo.getPurchaceDate().getTime());
            String validDate = Utility.transDateToString(materialInfo.getValidDate().getTime());
            String comment = new String(materialInfo.getComment().getBytes(), sStringCharSet);

            sResolver.delete(MaterialProvider.URI_MATERIAL, "(name is null or name=?) and (barcode is null or barcode=?) "
                            + "and (barcode_format is null or barcode_format=?) and (material_type is null or material_type=?) "
                            + "and is_as_photo_type=" + isAsPhotoType + " and is_valid_date_setup=" + isValidDateSetup + " and (material_place is null or material_place=?) "
                            + "and (purchase_date is null or purchase_date=?) and (valid_date is null or valid_date=?) "
                            + "and (comment is null or comment=?)"
                    , new String[]{name, barcode, barcodeFormat, materialType, materialPlace, purchaceDate, validDate, comment});
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }

    public synchronized static Material selectMaterialHistory(String barcodeFormat, String barcode) {
        Cursor c = null;
        Material materialInfo;
        c = sResolver.query(MaterialProvider.URI_MATERIAL_HISTORY, null, "barcode=? and barcode_format=?",
                new String[]{barcode, barcodeFormat}, null);

        try {
            if (c.moveToNext()) {
                materialInfo = new Material();

                materialInfo.setName(c.getString(1));
                materialInfo.setMaterialType(c.getString(4));
                materialInfo.setMaterialPlace(c.getString(5));
                materialInfo.setMaterialPic(BitmapFactory.decodeFile(c.getString(6), sOptions));
                materialInfo.setComment(c.getString(7));

                return materialInfo;
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        /* No history result */
        return null;
    }

    public synchronized static ArrayList<Material> selectMaterialInfos() {
        ArrayList<Material> materialInfos = new ArrayList<Material>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_MATERIAL, null, null, null, null);

            while (c.moveToNext()) {
                Material material = new Material();
                Calendar purchaceDate = Calendar.getInstance();
                Calendar valudDate = Calendar.getInstance();

                purchaceDate.setTime(Utility.transStringToDate(c.getString(5)));
                valudDate.setTime(Utility.transStringToDate(c.getString(6)));

                material.setName(c.getString(1));
                material.setMaterialType(c.getString(2));
                material.setIsAsPhotoType(c.getInt(3));
                material.setMaterialPlace(c.getString(4));
                material.setPurchaceDate(purchaceDate);
                material.setValidDate(valudDate);
                material.setNotificationDays(c.getInt(7));
//              material.setMaterialPic(BitmapFactory.decodeFile(c.getString(8), sOptions));
                material.setMaterialPicPath(c.getString(8));
                material.setComment(c.getString(9));
                material.setBarcode(c.getString(10));
                material.setBarcodeFormat(c.getString(11));
                material.setIsValidDateSetup(c.getInt(12));

                materialInfos.add(material);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return materialInfos;
    }

    /*
     * ==================================================================================================================
     */
    public synchronized static void insertMaterialTypeInfo(String... materialTypes) {
        ArrayList<String> oldMaterialTypes = selectMaterialTypeInfo();
        ArrayList<ContentValues> typeContentValues = new ArrayList<>();

        try {
            for (String newMaterialType : materialTypes) {
                if (oldMaterialTypes.contains(newMaterialType))
                    continue;

                ContentValues value = new ContentValues();

                value.put("name", new String(newMaterialType.getBytes(), sStringCharSet));
                typeContentValues.add(value);
            }

            if (typeContentValues.size() > 0) {
                sResolver.bulkInsert(MaterialProvider.URI_MATERIAL_TYPE, typeContentValues.toArray(new ContentValues[0]));
            }
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }

    public synchronized static void delMaterialTypeInfo(String materialType) {
        ContentValues value = new ContentValues();

        try {
            String type = new String(materialType.getBytes(), sStringCharSet);
            value.put("name", type);
            sResolver.delete(MaterialProvider.URI_MATERIAL_TYPE, "name='" + type + "'", null);
            /* Also delete all material informaion belong to the specific type */
            deleteMaterialInfoByType(materialType);
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }

    public synchronized static ArrayList<String> selectMaterialTypeInfo() {
        ArrayList<String> materialTypeInfos = new ArrayList<String>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_MATERIAL_TYPE, null, null, null, null);
            String type;

            while (c.moveToNext()) {
                type = c.getString(1);

                if (!materialTypeInfos.contains(type))
                    materialTypeInfos.add(type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return materialTypeInfos;

    }

    /* ============================================ GroceryItem && GroceryList  ============================================== */

    public synchronized static void updateGroceryListInfo(GroceryListData oldGroceryListInfo, GroceryListData newGroceryListInfo) {
        try {
            ContentValues value = new ContentValues();
            String groceryListName = new String(newGroceryListInfo.getGroceryListName().getBytes(), sStringCharSet);
            String storeName = new String(newGroceryListInfo.getStoreName().getBytes(), sStringCharSet);
            String address = new String(newGroceryListInfo.getAddress().getBytes(), sStringCharSet);
            String lat = new String(newGroceryListInfo.getLat().getBytes(), sStringCharSet);
            String lon = new String(newGroceryListInfo.getLong().getBytes(), sStringCharSet);
            String phone = new String(newGroceryListInfo.getPhone().getBytes(), sStringCharSet);
            String serviceTime = new String(newGroceryListInfo.getServiceTime().getBytes(), sStringCharSet);
            String receiptNum = new String(newGroceryListInfo.getReceiptNum().getBytes(), sStringCharSet);

            value.put("grocery_list_name", groceryListName);
            value.put("alert_nearby", newGroceryListInfo.getIsAlertWhenNearBy());
            value.put("store_name", storeName);
            value.put("address", address);
            value.put("latitude", lat);
            value.put("longitude", lon);
            value.put("phone", phone);
            value.put("service_time", serviceTime);
            value.put("receipt_num", receiptNum);

            int row = sResolver.update(MaterialProvider.URI_GROCERY_LIST, value, "id=?", new String[]{Integer.toString(oldGroceryListInfo.getId())});
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    public synchronized static void insertGroceryListInfo(GroceryListData groceryListInfo) {
        ContentValues value = new ContentValues();
        try {
            value.put("grocery_list_name", new String(groceryListInfo.getGroceryListName().getBytes(), sStringCharSet));
            value.put("alert_nearby", groceryListInfo.getIsAlertWhenNearBy());
            value.put("store_name", new String(groceryListInfo.getStoreName().getBytes(), sStringCharSet));
            value.put("address", new String(groceryListInfo.getAddress().getBytes(), sStringCharSet));
            value.put("latitude", new String(groceryListInfo.getLat().getBytes(), sStringCharSet));
            value.put("longitude", new String(groceryListInfo.getLong().getBytes(), sStringCharSet));
            value.put("phone", new String(groceryListInfo.getPhone().getBytes(), sStringCharSet));
            value.put("service_time", new String(groceryListInfo.getServiceTime().getBytes(), sStringCharSet));
            value.put("receipt_num", new String(groceryListInfo.getReceiptNum().getBytes(), sStringCharSet));

            sResolver.insert(MaterialProvider.URI_GROCERY_LIST, value);
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }

    public synchronized static void insertGroceryListHistoryInfo(GroceryListData groceryListInfo) {
        ContentValues value = new ContentValues();
        try {
            value.put("grocery_list_id", Integer.valueOf(groceryListInfo.getId()));
            value.put("grocery_list_name", new String(groceryListInfo.getGroceryListName().getBytes(), sStringCharSet));
            value.put("alert_nearby", groceryListInfo.getIsAlertWhenNearBy());
            value.put("store_name", new String(groceryListInfo.getStoreName().getBytes(), sStringCharSet));
            value.put("address", new String(groceryListInfo.getAddress().getBytes(), sStringCharSet));
            value.put("phone", new String(groceryListInfo.getPhone().getBytes(), sStringCharSet));
            value.put("service_time", new String(groceryListInfo.getServiceTime().getBytes(), sStringCharSet));
            value.put("check_out_time", Utility.transDateToString("yyyy-MM-dd HH:mm:ss", groceryListInfo.getCheckOutTime()));

            sResolver.insert(MaterialProvider.URI_GROCERY_LIST_HISTORY, value);
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }

    public synchronized static ArrayList<GroceryListData> selectGroceryListHistoryInfosByDates(Date startDate, Date endDate) {
        ArrayList<GroceryListData> groceryListInfos = new ArrayList<GroceryListData>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_GROCERY_LIST_HISTORY, null, "check_out_time >= Datetime(\'" + Utility.transDateToString("yyyy-MM-dd HH:mm:ss", startDate) + "\') and check_out_time <= Datetime (\'" + Utility.transDateToString("yyyy-MM-dd HH:mm:ss", endDate) + "\')", null, null);

            while (c.moveToNext()) {
                GroceryListData groceryListInfo = new GroceryListData();

                groceryListInfo.setId(c.getInt(1));
                groceryListInfo.setGroceryListName(c.getString(2));
                groceryListInfo.setIsAlertWhenNearBy(c.getInt(3));
                groceryListInfo.setStoreName(c.getString(4));
                groceryListInfo.setAddress(c.getString(5));
                groceryListInfo.setPhone(c.getString(6));
                groceryListInfo.setServiceTime(c.getString(7));
                groceryListInfo.setCheckOutTime(Utility.transStringToDate("yyyy-MM-dd HH:mm:ss", c.getString(8)));

                groceryListInfos.add(groceryListInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return groceryListInfos;
    }

    public synchronized static GroceryListData selectGroceryListHistoryInfosById(int id) {
        ArrayList<GroceryListData> groceryListInfos = new ArrayList<GroceryListData>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_GROCERY_LIST_HISTORY, null, "grocery_list_id=?", new String[]{Integer.toString(id)}, null, null);

            while (c.moveToNext()) {
                GroceryListData groceryListInfo = new GroceryListData();

                groceryListInfo.setId(c.getInt(1));
                groceryListInfo.setGroceryListName(c.getString(2));
                groceryListInfo.setIsAlertWhenNearBy(c.getInt(3));
                groceryListInfo.setStoreName(c.getString(4));
                groceryListInfo.setAddress(c.getString(5));
                groceryListInfo.setPhone(c.getString(6));
                groceryListInfo.setServiceTime(c.getString(7));
                groceryListInfo.setCheckOutTime(Utility.transStringToDate("yyyy-MM-dd HH:mm:ss", c.getString(8)));

                groceryListInfos.add(groceryListInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return (groceryListInfos.size() > 0) ? groceryListInfos.get(0) : null;
    }

    public synchronized static ArrayList<GroceryListData> selectGroceryListInfos() {
        ArrayList<GroceryListData> groceryListInfos = new ArrayList<GroceryListData>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_GROCERY_LIST, null, null, null, null);

            while (c.moveToNext()) {
                GroceryListData groceryListInfo = new GroceryListData();

                groceryListInfo.setId(c.getInt(0));
                groceryListInfo.setGroceryListName(c.getString(1));
                groceryListInfo.setIsAlertWhenNearBy(c.getInt(2));
                groceryListInfo.setStoreName(c.getString(3));
                groceryListInfo.setAddress(c.getString(4));
                groceryListInfo.setLat(c.getString(5));
                groceryListInfo.setLong(c.getString(6));
                groceryListInfo.setPhone(c.getString(7));
                groceryListInfo.setServiceTime(c.getString(8));
                groceryListInfo.setReceiptNum(c.getString(9));

                groceryListInfos.add(groceryListInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return groceryListInfos;
    }

    public synchronized static GroceryListData selectGroceryListInfoByReceiptNum(String receiptNum) {
        ArrayList<GroceryListData> groceryListInfos = new ArrayList<>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_GROCERY_LIST, null, "receipt_num=?", new String[]{receiptNum}, null);

            while (c.moveToNext()) {
                GroceryListData groceryListInfo = new GroceryListData();

                groceryListInfo.setId(c.getInt(0));
                groceryListInfo.setGroceryListName(c.getString(1));
                groceryListInfo.setIsAlertWhenNearBy(c.getInt(2));
                groceryListInfo.setStoreName(c.getString(3));
                groceryListInfo.setAddress(c.getString(4));
                groceryListInfo.setLat(c.getString(5));
                groceryListInfo.setLong(c.getString(6));
                groceryListInfo.setPhone(c.getString(7));
                groceryListInfo.setServiceTime(c.getString(8));
                groceryListInfo.setReceiptNum(c.getString(9));

                groceryListInfos.add(groceryListInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return (groceryListInfos.size() > 0) ? groceryListInfos.get(0) : null;
    }


    public synchronized static GroceryListData selectGroceryListInfosById(int id) {
        ArrayList<GroceryListData> groceryListInfos = new ArrayList<GroceryListData>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_GROCERY_LIST, null, "id=?", new String[]{Integer.toString(id)}, null);

            while (c.moveToNext()) {
                GroceryListData groceryListInfo = new GroceryListData();

                groceryListInfo.setId(c.getInt(0));
                groceryListInfo.setGroceryListName(c.getString(1));
                groceryListInfo.setIsAlertWhenNearBy(c.getInt(2));
                groceryListInfo.setStoreName(c.getString(3));
                groceryListInfo.setAddress(c.getString(4));
                groceryListInfo.setLat(c.getString(5));
                groceryListInfo.setLong(c.getString(6));
                groceryListInfo.setPhone(c.getString(7));
                groceryListInfo.setServiceTime(c.getString(8));
                groceryListInfo.setReceiptNum(c.getString(9));

                groceryListInfos.add(groceryListInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return (groceryListInfos.size() > 0) ? groceryListInfos.get(0) : null;
    }

    public synchronized static void insertGroceryItemInfo(GroceryItem groceryItemInfo) {
        ContentValues value = new ContentValues();
        try {
            String picPath = FileUtility.saveMaterialPhoto(groceryItemInfo.getName(), groceryItemInfo.getGroceryPic());

            value.put("name", new String(groceryItemInfo.getName().getBytes(), sStringCharSet));
            value.put("grocery_type", new String(groceryItemInfo.getGroceryType().getBytes(), sStringCharSet));
            value.put("grocery_list_id", Integer.valueOf(groceryItemInfo.getGroceryListId()));
            value.put("photo_path", new String(picPath.getBytes(), sStringCharSet));
            value.put("barcode", new String(groceryItemInfo.getBarcode().getBytes(), sStringCharSet));
            value.put("barcode_format", new String(groceryItemInfo.getBarcodeFormat().getBytes(), sStringCharSet));
            value.put("purchase_date", Utility.transDateToString(groceryItemInfo.getPurchaceDate().getTime()));
            value.put("valid_date", Utility.transDateToString(groceryItemInfo.getValidDate().getTime()));
            value.put("size", new String(groceryItemInfo.getSize().getBytes(), sStringCharSet));
            value.put("size_unit", new String(groceryItemInfo.getSizeUnit().getBytes(), sStringCharSet));
            value.put("qty", new String(groceryItemInfo.getQty().getBytes(), sStringCharSet));
            value.put("price", new String(groceryItemInfo.getPrice().getBytes(), sStringCharSet));
            value.put("comment", new String(groceryItemInfo.getComment().getBytes(), sStringCharSet));

            sResolver.insert(MaterialProvider.URI_GROCERY_ITEMS, value);
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }

    public synchronized static ArrayList<GroceryItem> selectGroceryItemsById(int groceryListId) {
        ArrayList<GroceryItem> groceryItems = new ArrayList<GroceryItem>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_GROCERY_ITEMS, null, "grocery_list_id=?", new String[]{Integer.toString(groceryListId)}, null);

            while (c.moveToNext()) {
                GroceryItem groceryItem = new GroceryItem();
                Calendar purchaceDate = Calendar.getInstance();
                Calendar validDate = Calendar.getInstance();

                purchaceDate.setTime(Utility.transStringToDate(c.getString(7)));
                validDate.setTime(Utility.transStringToDate(c.getString(8)));

                groceryItem.setName(c.getString(1));
                groceryItem.setGroceryType(c.getString(2));
                groceryItem.setGroceryListId(c.getInt(3));
                groceryItem.setGroceryPicPath(c.getString(4));
                groceryItem.setBarcode(c.getString(5));
                groceryItem.setBarcodeFormat(c.getString(6));
                groceryItem.setPurchaceDate(purchaceDate);
                groceryItem.setValidDate(validDate);
                groceryItem.setSize(c.getString(9));
                groceryItem.setSizeUnit(c.getString(10));
                groceryItem.setQty(c.getString(11));
                groceryItem.setPrice(c.getString(12));
                groceryItem.setComment(c.getString(13));

                groceryItems.add(groceryItem);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return groceryItems;
    }

    public synchronized static ArrayList<GroceryItem> selectGroceryItems() {
        ArrayList<GroceryItem> groceryItems = new ArrayList<GroceryItem>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_GROCERY_ITEMS, null, null, null, null);

            while (c.moveToNext()) {
                GroceryItem groceryItem = new GroceryItem();
                Calendar purchaceDate = Calendar.getInstance();
                Calendar validDate = Calendar.getInstance();

                purchaceDate.setTime(Utility.transStringToDate(c.getString(7)));
                validDate.setTime(Utility.transStringToDate(c.getString(8)));

                groceryItem.setName(c.getString(1));
                groceryItem.setGroceryType(c.getString(2));
                groceryItem.setGroceryListId(c.getInt(3));
                groceryItem.setGroceryPicPath(c.getString(4));
                groceryItem.setBarcode(c.getString(5));
                groceryItem.setBarcodeFormat(c.getString(6));
                groceryItem.setPurchaceDate(purchaceDate);
                groceryItem.setValidDate(validDate);
                groceryItem.setSize(c.getString(9));
                groceryItem.setSizeUnit(c.getString(10));
                groceryItem.setQty(c.getString(11));
                groceryItem.setPrice(c.getString(12));
                groceryItem.setComment(c.getString(13));

                groceryItems.add(groceryItem);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return groceryItems;
    }

    public synchronized static void deleteGroceryItem(GroceryItem groceryItem) {
        try {
            String name = new String(groceryItem.getName().getBytes(), sStringCharSet);
            String groceryListId = new String(Integer.toString(groceryItem.getGroceryListId()).getBytes(), sStringCharSet);
            String barcodeFormat = new String(groceryItem.getBarcodeFormat().getBytes(), sStringCharSet);
            String barcode = new String(groceryItem.getBarcode().getBytes(), sStringCharSet);
            String photoPath = new String(groceryItem.getGroceryPicPath().getBytes(), sStringCharSet);

            sResolver.delete(MaterialProvider.URI_GROCERY_ITEMS, "name=? and grocery_list_id =? and barcode_format=? and barcode=? and photo_path=?", new String[]{name, groceryListId, barcodeFormat, barcode, photoPath});
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }

    public synchronized static void deleteGreceryItemsByListId(int groceryListId) {
        sResolver.delete(MaterialProvider.URI_GROCERY_ITEMS, "grocery_list_id =?", new String[]{Integer.toString(groceryListId)});
    }

    public synchronized static void deleteGroceryList(int groceryListId) {
        sResolver.delete(MaterialProvider.URI_GROCERY_LIST, "id =?", new String[]{Integer.toString(groceryListId)});
    }


    /* ============================================ Reward utility ==================================== */

    public synchronized static ArrayList<RewardInfo> selectRewardCard() {
        ArrayList<RewardInfo> rewardInfos = new ArrayList<RewardInfo>();
        Cursor c = null;

        try {
            c = sResolver.query(MaterialProvider.URI_REWARD_CARD, null, null, null, null);

            while (c.moveToNext()) {
                RewardInfo rewardInfo = new RewardInfo();
                Calendar validFrom = Calendar.getInstance();
                Calendar expiry = Calendar.getInstance();

                validFrom.setTime(Utility.transStringToDate(c.getString(7)));
                expiry.setTime(Utility.transStringToDate(c.getString(8)));

                rewardInfo.setName(c.getString(1));
                rewardInfo.setCardType(c.getString(2));
                rewardInfo.setBarCode(c.getString(3));
                rewardInfo.setBarCodeFormat(c.getString(4));
                rewardInfo.setFrontPhotoPath(c.getString(5));
                rewardInfo.setCouponValue(c.getString(6));
                rewardInfo.setValidDateFrom(validFrom);
                rewardInfo.setExpiry(expiry);
                rewardInfo.setNotificationDays(c.getInt(9));
                rewardInfo.setComment(c.getString(10));
                rewardInfo.setBackPhotoPath(c.getString(11));

                rewardInfos.add(rewardInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return rewardInfos;
    }

    public synchronized static void insertRewardCard(RewardInfo rewardInfo) {
        ContentValues value = new ContentValues();
        String picPath = rewardInfo.getFrontPhotoPath();
        String picBackPath = rewardInfo.getBackPhotoPath();

        if (picPath == null || picPath.isEmpty()) {
            picPath = FileUtility.saveMaterialPhoto(rewardInfo.getName(), rewardInfo.getFrontRewardPhoto());
        }

        if (picBackPath == null || picBackPath.isEmpty()) {
            picBackPath = FileUtility.saveMaterialPhoto(rewardInfo.getName() + "_back", rewardInfo.getBackRewardPhoto());
        }

        try {
            value.put("name", new String(rewardInfo.getName().getBytes(), sStringCharSet));
            value.put("card_type", new String(rewardInfo.getCardType().getBytes(), sStringCharSet));
            value.put("barcode", new String(rewardInfo.getBarCode().getBytes(), sStringCharSet));
            value.put("barcode_format", new String(rewardInfo.getBarCodeFormat().getBytes(), sStringCharSet));
            value.put("photo_path", new String(picPath.getBytes(), sStringCharSet));
            value.put("coupon_value", new String(rewardInfo.getCouponValue().getBytes(), sStringCharSet));
            value.put("valid_from", new String(Utility.transDateToString(rewardInfo.getValidDateFrom().getTime()).getBytes(), sStringCharSet));
            value.put("expiry", new String(Utility.transDateToString(rewardInfo.getExpiry().getTime()).getBytes(), sStringCharSet));
            value.put("notification_days", rewardInfo.getNotificationDays());
            value.put("comment", new String(rewardInfo.getComment().getBytes(), sStringCharSet));
            value.put("photo_back_path", new String(picBackPath.getBytes(), sStringCharSet));

            sResolver.insert(MaterialProvider.URI_REWARD_CARD, value);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void updateRewardCard(RewardInfo oldRewardInfo, RewardInfo newRewardInfo) {
        try {
            String oldName = new String(oldRewardInfo.getName().getBytes(), sStringCharSet);
            String oldCardType = new String(oldRewardInfo.getCardType().getBytes(), sStringCharSet);
            String oldBarcode = new String(oldRewardInfo.getBarCode().getBytes(), sStringCharSet);
            String oldBarcodeFormat = new String(oldRewardInfo.getBarCodeFormat().getBytes(), sStringCharSet);
            String oldPhotoPath = new String(oldRewardInfo.getFrontPhotoPath().getBytes(), sStringCharSet);
            String oldBackPhotoPath = new String(oldRewardInfo.getBackPhotoPath().getBytes(), sStringCharSet);
            String oldCouponValue = new String(oldRewardInfo.getCouponValue().getBytes(), sStringCharSet);
            String oldValidFrom = new String(Utility.transDateToString(oldRewardInfo.getValidDateFrom().getTime()).getBytes(), sStringCharSet);
            String oldExpiry = new String(Utility.transDateToString(oldRewardInfo.getExpiry().getTime()).getBytes(), sStringCharSet);
            int oldNotificationDays = oldRewardInfo.getNotificationDays();
            String oldComment = new String(oldRewardInfo.getComment().getBytes(), sStringCharSet);

            ContentValues value = new ContentValues();
            String newPhotoPath = FileUtility.saveMaterialPhoto(newRewardInfo.getName(), newRewardInfo.getFrontRewardPhoto());
            value.put("name", new String(newRewardInfo.getName().getBytes(), sStringCharSet));
            value.put("card_type", new String(newRewardInfo.getCardType().getBytes(), sStringCharSet));
            value.put("barcode", new String(newRewardInfo.getBarCode().getBytes(), sStringCharSet));
            value.put("barcode_format", new String(newRewardInfo.getBarCodeFormat().getBytes(), sStringCharSet));
            value.put("photo_path", new String(newPhotoPath.getBytes(), sStringCharSet));
            value.put("coupon_value", new String(newRewardInfo.getCouponValue().getBytes(), sStringCharSet));
            value.put("valid_from", new String(Utility.transDateToString(newRewardInfo.getValidDateFrom().getTime()).getBytes(), sStringCharSet));
            value.put("expiry", new String(Utility.transDateToString(newRewardInfo.getExpiry().getTime()).getBytes(), sStringCharSet));
            value.put("notification_days", newRewardInfo.getNotificationDays());
            value.put("comment", new String(newRewardInfo.getComment().getBytes(), sStringCharSet));
            value.put("photo_back_path", new String(newRewardInfo.getBackPhotoPath().getBytes(), sStringCharSet));

            sResolver.update(MaterialProvider.URI_REWARD_CARD, value,
                    "name=? "
                            + "and card_type=? "
                            + "and barcode=? "
                            + "and barcode_format=? "
                            + "and photo_path=? "
                            + "and coupon_value=? "
                            + "and valid_from=? "
                            + "and expiry=? "
                            + "and notification_days=" + oldNotificationDays + " "
                            + "and comment=?"
                            + "and photo_back_path=?"
                    , new String[]{oldName, oldCardType, oldBarcode, oldBarcodeFormat, oldPhotoPath, oldCouponValue, oldValidFrom, oldExpiry, oldComment, oldBackPhotoPath});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static void deleteRewardCard(RewardInfo rewardInfo) {
        try {
            /* FIXME: doesn't include bitmap path may not be */
            String oldName = new String(rewardInfo.getName().getBytes(), sStringCharSet);
            String oldCardType = new String(rewardInfo.getCardType().getBytes(), sStringCharSet);
            String oldBarcode = new String(rewardInfo.getBarCode().getBytes(), sStringCharSet);
            String oldBarcodeFormat = new String(rewardInfo.getBarCodeFormat().getBytes(), sStringCharSet);
            String oldPhotoPath = new String(rewardInfo.getFrontPhotoPath().getBytes(), sStringCharSet);
            String oldBackPhotoPath = new String(rewardInfo.getBackPhotoPath().getBytes(), sStringCharSet);
            String oldCouponValue = new String(rewardInfo.getCouponValue().getBytes(), sStringCharSet);
            String oldValidFrom = new String(Utility.transDateToString(rewardInfo.getValidDateFrom().getTime()).getBytes(), sStringCharSet);
            String oldExpiry = new String(Utility.transDateToString(rewardInfo.getExpiry().getTime()).getBytes(), sStringCharSet);
            int oldNotificationDays = rewardInfo.getNotificationDays();
            String oldComment = new String(rewardInfo.getComment().getBytes(), sStringCharSet);

            sResolver.delete(MaterialProvider.URI_REWARD_CARD, "(name is null or name=?) "
                            + "and (card_type is null or card_type=?) "
                            + "and (barcode is null or barcode=?) "
                            + "and (barcode_format is null or barcode_format=?) "
                            + "and (photo_path is null or photo_path=?) "
                            + "and (coupon_value is null or coupon_value=?) "
                            + "and (valid_from is null or valid_from=?) "
                            + "and (expiry is null or expiry=?) "
                            + "and notification_days=" + oldNotificationDays + " "
                            + "and (comment is null or comment=?) "
                            + "and (photo_back_path is null or photo_back_path=?)"
                    , new String[]{oldName, oldCardType, oldBarcode, oldBarcodeFormat, oldPhotoPath, oldCouponValue, oldValidFrom, oldExpiry, oldComment, oldBackPhotoPath});
        } catch (UnsupportedEncodingException e) {
            LogUtility.printStackTrace(e);
        }
    }
    /* ============================================backup utility==================================== */

    /* need to refactor for naming */
    public synchronized static String importDB(DropboxAPI<?> api, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_db_restore));

        msg.append(importDBFromDropBox(api, observ));
        msg.append(importDBFromLocal(observ));

        return msg.toString();
    }

    public synchronized static String exportDB(DropboxAPI<?> api, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_db_backup));

        msg.append(exportDBToLocal(observ));
        msg.append(exportDBToDropBox(api, observ));

        return msg.toString();
    }

    /* need to refactor for naming */
    public synchronized static String importDB(GoogleApiClient apiClient, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_db_restore));

        msg.append(importDBFromDrive(apiClient, observ));
        msg.append(importDBFromLocal(observ));

        return msg.toString();
    }

    public synchronized static String exportDB(GoogleApiClient apiClient, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_db_backup));

        msg.append(exportDBToLocal(observ));
        msg.append(exportDBToDrive(apiClient, observ));

        return msg.toString();
    }

    private static String importDBFromDrive(GoogleApiClient apiClient, Observer observ) {
        if (apiClient == null || !apiClient.isConnected()) {
            return sContext.getString(R.string.title_googledrive_restore_fail);
        }


        byte[] buf = new byte[4096];
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, MaterialProvider.DB_NAME))
                .build();

        DriveApi.MetadataBufferResult mbr = Drive.DriveApi.query(apiClient, query).await();
        MetadataBuffer mb = mbr.getMetadataBuffer();

        if (mbr == null || !mbr.getStatus().isSuccess() || mb.getCount() <= 0) {
            return sContext.getString(R.string.title_googledrive_restore_fail);
        }

        DriveFile dbDriveFile = mb.get(0).getDriveId().asDriveFile();
        DriveApi.DriveContentsResult dbDcr = dbDriveFile.open(apiClient, DriveFile.MODE_READ_ONLY, null).await();
        DriveContents dbDc = dbDcr.getDriveContents();
        InputStream is = null;
        BufferedOutputStream bos = null;

        try {
            is = dbDc.getInputStream();
            bos = new BufferedOutputStream(new FileOutputStream(new File(Utility.getExternalStorageDir(), "/" + MaterialManagerApplication.DB_DIR_NAME + "/" + MaterialProvider.DB_NAME)));
            int c;

            while ((c = is.read(buf)) > 0) {
                bos.write(buf, 0, c);
                bos.flush();
            }
        } catch (Exception e) {
            LogUtility.printStackTrace(e);
            return sContext.getString(R.string.title_googledrive_restore_fail);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                LogUtility.printStackTrace(e);
            }
        }

        if (observ != null) {
            BackupRestoreInfo pi = new BackupRestoreInfo();
            pi.setMsg(sContext.getString(R.string.title_progress_googledrive_restore_successfully));
            pi.setProgress(100);
            observ.update(pi);
        }

        return sContext.getString(R.string.title_googledrive_restore_successfully);
    }

    private static String exportDBToDrive(final GoogleApiClient apiClient, Observer observ) {
        if (apiClient == null || !apiClient.isConnected()) {
            return sContext.getString(R.string.title_googledrive_backup_fail);
        }

        DriveFolder appFolder = Drive.DriveApi.getAppFolder(apiClient);

        /* Create new drive content for writing the db file. */
        DriveApi.DriveContentsResult dcr = Drive.DriveApi.newDriveContents(apiClient).await();

        if (dcr == null || !dcr.getStatus().isSuccess()) {
            return sContext.getString(R.string.title_googledrive_backup_fail);
        }

                /* Get the outputstream for writing the db file. */
        DriveContents dc = dcr.getDriveContents();
        OutputStream os = dc.getOutputStream();

        if (os == null) {
            return sContext.getString(R.string.title_googledrive_backup_fail);
        }

        byte[] buf = new byte[4096];
        BufferedInputStream bis = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(sContext.getDatabasePath(MaterialProvider.DB_NAME)), buf.length);
            int c;
            while ((c = bis.read(buf)) > 0) {
                os.write(buf, 0, c);
                os.flush();
            }
        } catch (Exception e) {
            LogUtility.printStackTrace(e);
            return sContext.getString(R.string.title_googledrive_backup_fail);
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                LogUtility.printStackTrace(e);
            }
        }

            /* Create drive file and upload it. */
        MetadataChangeSet meta = new MetadataChangeSet
                .Builder()
                .setTitle(MaterialProvider.DB_NAME)
                .setMimeType("application/x-sqlite3")
                .build();
        DriveFolder.DriveFileResult dfr = appFolder.createFile(apiClient, meta, dc).await();

        if (dfr == null || !dfr.getStatus().isSuccess()) {
            return sContext.getString(R.string.title_googledrive_backup_fail);
        }

        /* Update the notification's progress for backup progress completely.*/
        BackupRestoreInfo bri = new BackupRestoreInfo();

        bri.setMsg(sContext.getString(R.string.title_progress_googledrive_backup_successfully));
        bri.setProgress(100);
        observ.update(bri);

        return sContext.getString(R.string.title_googledrive_backup_successfully);
    }


    @SuppressWarnings("resource")
    private static String exportDBToLocal(Observer observ) {
        try {
            File sd = Utility.getExternalStorageDir();
            File data = Environment.getDataDirectory();
            BackupRestoreInfo pi;

            if (sd.canWrite()) {
                String currentDBPath = "/data/" + MaterialManagerApplication.DB_DIR_NAME + "/databases/"
                        + MaterialProvider.DB_NAME;
                String backupDBPath = "/" + MaterialManagerApplication.DB_DIR_NAME + "/" + MaterialProvider.DB_NAME;
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);
                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();

                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                if (observ != null) {
                    pi = new BackupRestoreInfo();
                    pi.setMsg(sContext.getString(R.string.title_progress_local_backup_successfully));
                    pi.setProgress(100);
                }

                return sContext.getString(R.string.title_local_backup_successfully);
            }

            return sContext.getString(R.string.title_local_backup_fail);
        } catch (Exception e) {
            LogUtility.printStackTrace(e);

            return sContext.getString(R.string.title_local_backup_fail);
        }
    }

    private static String exportDBToDropBox(DropboxAPI<?> api, Observer observ) {
        if (api == null || !api.getSession().isLinked()) {
            return sContext.getString(R.string.title_dropbox_backup_fail);

        }

        UploadRequest request = null;
        FileInputStream fis = null;
        BackupRestoreInfo pi;
        try {
            File file = new File(Utility.getExternalStorageDir(), "/" + MaterialManagerApplication.DB_DIR_NAME + "/"
                    + MaterialProvider.DB_NAME);
            fis = new FileInputStream(file);
            request = api.putFileOverwriteRequest(
                    MaterialManagerApplication.DB_DROPBOX_PATH + MaterialProvider.DB_NAME, fis, file.length(), null);

            if (request != null) {
                request.upload();
                pi = new BackupRestoreInfo();
                pi.setMsg(sContext.getString(R.string.title_progress_dropbox_backup_successfully));
                pi.setProgress(100);
                observ.update(pi);
                return sContext.getString(R.string.title_dropbox_backup_successfully);
            }
            return sContext.getString(R.string.title_dropbox_backup_fail);
        } catch (Exception e) {
            if (request != null)
                request.abort();

            LogUtility.printStackTrace(e);

            return sContext.getString(R.string.title_dropbox_backup_fail);
        } finally {
            if (request != null) {
                request.abort();
            }

            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                LogUtility.printStackTrace(e);
            }
        }
    }

    @SuppressWarnings("resource")
    private static String importDBFromLocal(Observer observ) {
        try {
            File sd = Utility.getExternalStorageDir();
            File data = Environment.getDataDirectory();
            BackupRestoreInfo pi;

            if (sd.canWrite()) {
                String currentDBPath = "/data/" + MaterialManagerApplication.DB_DIR_NAME + "/databases/"
                        + MaterialProvider.DB_NAME;
                String backupDBPath = "/" + MaterialManagerApplication.DB_DIR_NAME + "/" + MaterialProvider.DB_NAME; // From
                // SD
                // directory.
                File backupDB = new File(data, currentDBPath);
                File currentDB = new File(sd, backupDBPath);
                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();

                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                /* Update photo sdcard path in DataBase */
                ArrayList<Material> materialList = selectMaterialInfos();
                String newPhotoPath;

                for (Material material : materialList) {
                    newPhotoPath = FileUtility.MATERIAL_PHOTO_PATH + File.separator + material.getName().trim()
                            + ".jpg";
                    updateSdPhotoPath(material, newPhotoPath);
                }

                /* Re-backup to local */
                exportDBToLocal(null);

                if (observ != null) {
                    pi = new BackupRestoreInfo();
                    pi.setMsg(sContext.getString(R.string.title_progress_local_restore_successfully));
                    pi.setProgress(100);
                    observ.update(pi);
                }

                return sContext.getString(R.string.title_local_restore_successfully);
            }
            return sContext.getString(R.string.title_local_restore_fail);
        } catch (Exception e) {
            e.printStackTrace();
            return sContext.getString(R.string.title_local_restore_fail);
        }
    }

    private static String importDBFromDropBox(DropboxAPI<?> api, Observer observ) {
        try {
            if (api == null || !api.getSession().isLinked()) {
                return sContext.getString(R.string.title_dropbox_restore_fail);

            }

            Entry dirEntry = api.metadata(MaterialManagerApplication.DB_DROPBOX_PATH, 1000, null, true, null);
            Entry db = null;

            if (!dirEntry.isDir || dirEntry.contents == null) {
                // It's not a directory, or there's nothing in it
                return sContext.getString(R.string.title_dropbox_restore_fail);
            }

            /* Retrieve the db file */
            for (Entry ent : dirEntry.contents) {
                if (!ent.isDir && ent.fileName().equals(MaterialProvider.DB_NAME)) {
                    db = ent;
                    break;
                }
            }

            if (db == null) {
                // It's not a directory, or there's nothing in it
                return sContext.getString(R.string.title_dropbox_restore_fail);
            }

            String path = db.path;
            FileOutputStream fos = null;
            BackupRestoreInfo pi;

            try {
                fos = new FileOutputStream(Utility.getExternalStorageDir() + "/"
                        + MaterialManagerApplication.DB_DIR_NAME + "/" + MaterialProvider.DB_NAME);
                api.getFile(path, null, fos, null);

                if (observ != null) {
                    pi = new BackupRestoreInfo();
                    pi.setMsg(sContext.getString(R.string.title_progress_dropbox_restore_successfully));
                    pi.setProgress(100);
                    observ.update(pi);
                }

                return sContext.getString(R.string.title_dropbox_restore_successfully);
            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                if (fos != null)
                    fos.close();
            }

            return sContext.getString(R.string.title_dropbox_restore_fail);
        } catch (Exception e) {
            e.printStackTrace();
            return sContext.getString(R.string.title_dropbox_restore_fail);
        }
    }
}
