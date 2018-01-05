package com.material.management.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.dropbox.client2.DropboxAPI;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.material.management.MaterialManagerApplication;
import com.material.management.Observer;
import com.material.management.R;
import com.material.management.data.BackupRestoreInfo;
import com.material.management.data.Material;
import com.material.management.provider.MaterialProvider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import static com.dropbox.client2.DropboxAPI.UploadRequest;

public class FileUtility {
    /* /sdcard */
    private static File sSdRoot = Environment.getExternalStorageDirectory();
    private static Context sContext = Utility.getContext();
    public static final String MATERIAL_PHOTO_PATH = sSdRoot.getPath()
            + File.separator + MaterialManagerApplication.PHOTO_DIR_NAME;
    private static final String TEMP_PHOTO_FILE_NAME = "tmp_captured.jpg";
    public static final File TEMP_PHOTO_FILE;

    static {
        File extPhotoDir = new File(MATERIAL_PHOTO_PATH);
        if (!isPicStorageExist())
            extPhotoDir.mkdir();
        TEMP_PHOTO_FILE = new File(MATERIAL_PHOTO_PATH, TEMP_PHOTO_FILE_NAME);
    }

    public static boolean isPicStorageExist() {
        return new File(MATERIAL_PHOTO_PATH).exists();
    }

    public static String saveMaterialPhoto(String photoName, Bitmap photo) {
        photoName = photoName.trim();
        photoName = (photoName == null || photoName.isEmpty()) ? Integer
                .toString((int) (Math.random() * Integer.MAX_VALUE))
                : photoName;
        photoName += ".jpg";
        File filePhoto = new File(MATERIAL_PHOTO_PATH, photoName);
        FileOutputStream out = null;
        photo = (photo == null) ? BitmapFactory.decodeResource(Utility.getContext().getResources(), R.drawable.ic_no_image_available) : photo;

        try {
            out = new FileOutputStream(filePhoto);
            photo.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return filePhoto.getAbsolutePath();
    }

    public static String exportPhoto(DropboxAPI<?> api, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_photo_backup));

        msg.append(exportPhotoToDropBox(api, observ));

        return msg.toString();
    }

    public static String importPhoto(DropboxAPI<?> api, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_photo_restore));

        msg.append(importPhotoFromDropBox(api, observ));

        return msg.toString();
    }

    public static String exportPhoto(GoogleApiClient apiClient, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_photo_backup));

        msg.append(exportPhotoToDrive(apiClient, observ));

        return msg.toString();
    }

    public static String importPhoto(GoogleApiClient apiClient, Observer observ) {
        StringBuilder msg = new StringBuilder(sContext.getString(R.string.title_photo_restore));

        msg.append(importPhotoFromDrive(apiClient, observ));

        return msg.toString();
    }

    public static void clearAppDriveData(GoogleApiClient apiClient, Observer observ) {
        if (apiClient == null || !apiClient.isConnected()) {
            return;
        }

        DriveFolder appFolder = Drive.DriveApi.getAppFolder(apiClient);
        /* Clear all app data for avoiding the duplicate when uploading files.*/
        DriveApi.MetadataBufferResult mbr = appFolder.listChildren(apiClient).await();
        MetadataBuffer mb = mbr.getMetadataBuffer();

        for (int i = 0, len = mb.getCount(); i < len; i++) {
            Metadata childMetaData = mb.get(i);
            DriveFile childFile = childMetaData.getDriveId().asDriveFile();
            Status status = childFile.delete(apiClient).await();

            if (observ != null) {
                BackupRestoreInfo pi = new BackupRestoreInfo();
                pi.setMsg(sContext.getString(R.string.title_progress_adjust_drive_space_successfully,  ((i + 1) * 100) / len));
                pi.setProgress(((i + 1) * 100) / len);
                observ.update(pi);
            }
        }
    }

    private static String exportPhotoToDrive(GoogleApiClient apiClient, Observer observ) {
        if (apiClient == null || !apiClient.isConnected()) {
            return sContext.getString(R.string.title_googledrive_backup_fail);
        }

        File photoLocalDir = new File(sSdRoot, MaterialManagerApplication.PHOTO_DIR_NAME);
        File[] photoFileAry = photoLocalDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".jpg");
            }
        });
        DriveFolder appFolder = Drive.DriveApi.getAppFolder(apiClient);

        for (int i = 0, total = photoFileAry.length; i < total; i++) {
            File photoFile = photoFileAry[i];
            /* Create new drive content for writing the db file. */
            DriveApi.DriveContentsResult dcr = Drive.DriveApi.newDriveContents(apiClient).await();

            if (dcr == null || !dcr.getStatus().isSuccess()) {
                return sContext.getString(R.string.title_googledrive_backup_fail);
            }

            DriveContents dc = dcr.getDriveContents();
            OutputStream os = dc.getOutputStream();
            byte[] buf = new byte[4096];
            BufferedInputStream bis = null;

            try {
                bis = new BufferedInputStream(new FileInputStream(photoFile), buf.length);
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
                    .setTitle(photoFile.getName())
                    .setMimeType("image/jpeg")
                    .build();
            DriveFolder.DriveFileResult dfr = appFolder.createFile(apiClient, meta, dc).await();

            if (dfr == null || !dfr.getStatus().isSuccess()) {
                return sContext.getString(R.string.title_googledrive_backup_fail);
            }

            if (observ != null) {
                BackupRestoreInfo pi = new BackupRestoreInfo();
                pi.setMsg(sContext.getString(R.string.title_progress_backup_photo_successfully, photoFile.getName(), ((i + 1) * 100) / total));
                pi.setProgress(((i + 1) * 100) / total);
                observ.update(pi);
            }
        }

        return sContext.getString(R.string.title_googledrive_backup_successfully);
    }

    private static String importPhotoFromDrive(GoogleApiClient apiClient, Observer observ) {
        if (apiClient == null || !apiClient.isConnected()) {
            return sContext.getString(R.string.title_googledrive_restore_fail);
        }

        byte[] buf = new byte[4096];
        DriveFolder appFolder = Drive.DriveApi.getAppFolder(apiClient);
        /* Clear all app data for avoiding the duplicate when uploading files.*/
        DriveApi.MetadataBufferResult mbr = appFolder.listChildren(apiClient).await();
        MetadataBuffer mb = mbr.getMetadataBuffer();

        for (int i = 0, total = mb.getCount(); i < total ; i++) {
            Metadata childMetaData = mb.get(i);

            if (childMetaData.getTitle().equals(MaterialProvider.DB_NAME)) {
                /* skip the db file. */
                continue;
            }

            String fileName = childMetaData.getTitle();
            DriveFile childFile = childMetaData.getDriveId().asDriveFile();
            DriveApi.DriveContentsResult photoDcr = childFile.open(apiClient, DriveFile.MODE_READ_ONLY, null).await();
            DriveContents photoDc = photoDcr.getDriveContents();
            InputStream is = null;
            BufferedOutputStream bos = null;

            try {
                is = photoDc.getInputStream();
                bos = new BufferedOutputStream(new FileOutputStream(new File(sSdRoot + "/" + MaterialManagerApplication.PHOTO_DIR_NAME + "/" + fileName)));
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
                pi.setMsg(sContext.getString(R.string.title_progress_restore_photo_successfully, fileName, ((i + 1) * 100) / total));
                pi.setProgress(((i + 1) * 100) / total);
                observ.update(pi);
            }
        }

        return sContext.getString(R.string.title_dropbox_restore_successfully);
    }

    private static String exportPhotoToDropBox(DropboxAPI<?> api, Observer observ) {
        if (api == null || !api.getSession().isLinked()) {
            return sContext.getString(R.string.title_dropbox_backup_fail);

        }

        File photoLocalDir = new File(sSdRoot, MaterialManagerApplication.PHOTO_DIR_NAME);
        FileInputStream fis = null;
        UploadRequest request = null;
        BackupRestoreInfo pi = null;

        try {
            if (!photoLocalDir.isDirectory())
                return sContext.getString(R.string.title_dropbox_backup_fail);

            File[] photoFileAry = photoLocalDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(".jpg");
                }
            });

            /* upload the photo files */
            String path;
            int total = photoFileAry.length;
            pi = new BackupRestoreInfo();
            for (int i = 0; i < total; i++) {
                File photoFile = photoFileAry[i];
                path = MaterialManagerApplication.PHOTO_DROPBOX_PATH + photoFile.getName();
                fis = new FileInputStream(photoFile);
                request = api.putFileOverwriteRequest(path, fis, photoFile.length(), null);

                if (request != null) {
                    request.upload();
                }
                fis.close();

                pi.setMsg(sContext.getString(R.string.title_progress_backup_photo_successfully, photoFile.getName(), ((i + 1) * 100) / total));
                pi.setProgress(((i + 1) * 100) / total);
                observ.update(pi);
            }
            return sContext.getString(R.string.title_dropbox_backup_successfully);
        } catch (Exception e) {
            LogUtility.printStackTrace(e);

            return sContext.getString(R.string.title_dropbox_backup_fail);
        } finally {
            if (request != null) {
                request.abort();
            }

            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                LogUtility.printStackTrace(e);
            }
        }
    }

    public static String importPhotoFromDropBox(DropboxAPI<?> api, Observer observ) {
        if (api == null || !api.getSession().isLinked()) {
            return sContext.getString(R.string.title_dropbox_restore_fail);

        }

        DropboxAPI.Entry dirEntry = null;
        FileOutputStream fos = null;

        try {
            dirEntry = api.metadata(MaterialManagerApplication.PHOTO_DROPBOX_PATH, 1000, null, true, null);

            if (!dirEntry.isDir || dirEntry.contents == null) {
                // It's not a directory, or there's nothing in it
                return sContext.getString(R.string.title_dropbox_restore_fail);

            }

            /* Retrieve the photo file */
            List<DropboxAPI.Entry> contList = dirEntry.contents;
            int total = contList.size();
            BackupRestoreInfo pi = new BackupRestoreInfo();
            for (int i = 0; i < total; i++) {
                DropboxAPI.Entry ent = contList.get(i);
                String fileName = ent.fileName();

                if (!ent.isDir && fileName.endsWith(".jpg")) {
                    fos = new FileOutputStream(sSdRoot + "/" + MaterialManagerApplication.PHOTO_DIR_NAME + "/" + ent.fileName());

                    api.getFile(ent.path, null, fos, null);
                    fos.close();

                    pi.setMsg(sContext.getString(R.string.title_progress_restore_photo_successfully, fileName, ((i + 1) * 100) / total));
                    pi.setProgress(((i + 1) * 100) / total);
                    observ.update(pi);
                }
            }

            return sContext.getString(R.string.title_dropbox_restore_successfully);
        } catch (Exception e) {
            e.printStackTrace();
            return sContext.getString(R.string.title_dropbox_restore_fail);
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
