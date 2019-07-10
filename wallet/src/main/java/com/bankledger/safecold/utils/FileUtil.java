/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bankledger.safecold.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecoldj.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FileUtil {

    public static final String WALLET_SDCARD_DIR = "SafeCold";
    public static final String WALLET_NAME_SDCARD_NAME = "wallet_name";
    public static final String BITHER_BACKUP_SDCARD_DIR = "SafeColdBackup";
    public static final String WALLET_LOG_SDCARD_DIR = WALLET_SDCARD_DIR + File.separator + "log";

    /*接收蓝牙发送文件*/
    public static final String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    public static final String fileName = "YinLianLauncher.apk";

    public static final String tempFileName = "YinLian.BSG";

    public static File getSDPath() {
        File sdDir = Environment.getExternalStorageDirectory();
        return sdDir;
    }

    public static File getBackupSdCardDir() {
        File backupDir = new File(getSDPath(), BITHER_BACKUP_SDCARD_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        return backupDir;
    }

    public static File getWalletNameSdCardFile() {
        File walletNameDir = new File(Environment.getExternalStorageDirectory(), WALLET_SDCARD_DIR);
        if (!walletNameDir.exists()) {
            walletNameDir.mkdirs();
        }
        File walletNameFile = new File(walletNameDir, WALLET_NAME_SDCARD_NAME);
        if (!walletNameFile.exists()) {
            try {
                walletNameFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return walletNameFile;
    }

    public static File getBackupFileOfCold() {
        File file = new File(getBackupSdCardDir(),
                DateTimeUtil.getNameForFile(System.currentTimeMillis())
                        + ".bak"
        );
        return file;
    }

    public static List<File> getBackupFileListOfCold() {
        File dir = getBackupSdCardDir();
        List<File> fileList = new ArrayList<File>();
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            files = orderByDateDesc(files);
            for (File file : files) {
                if (StringUtil.checkBackupFileOfCold(file.getName())) {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }


    public static File getDiskDir(String dirName, Boolean createNomedia) {

        File dir = getDiskCacheDir(SafeColdApplication.mContext, dirName);
        if (!dir.exists()) {
            dir.mkdirs();
            if (createNomedia) {
                try {
                    File noMediaFile = new File(dir, ".nomedia");
                    noMediaFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return dir;
    }


    public static File getExternalCacheDir(Context context) {
//        if (SdkUtils.hasFroyo()) {
//
//            return context.getCacheDir();
//        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName()
                + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath()
                + cacheDir);
    }


    public static File getDiskCacheDir(Context context, String uniqueName) {
        File extCacheDir = getExternalCacheDir(context);
        final String cachePath = (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState()) || !isExternalStorageRemovable())
                && extCacheDir != null ? extCacheDir.getPath() : context
                .getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    @TargetApi(9)
    public static boolean isExternalStorageRemovable() {
        if (SdkUtils.hasGingerbread()) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    @SuppressWarnings("resource")
    public static Object deserialize(File file) {
        Object object = new Object();
        FileInputStream fos = null;
        try {
            if (!file.exists()) {
                return null;
            }
            fos = new FileInputStream(file);
            ObjectInputStream ois;
            ois = new ObjectInputStream(fos);
            object = ois.readObject();

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return object;
    }

    public static void serializeObject(File file, Object object) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            oos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    public static File[] orderByDateDesc(File[] fs) {
        Arrays.sort(fs, new Comparator<File>() {
            public int compare(File f1, File f2) {
                long diff = f1.lastModified() - f2.lastModified();
                if (diff > 0) {
                    return -1;//-1 f1 before f2
                } else if (diff == 0) {
                    return 0;
                } else {
                    return 1;
                }
            }

            public boolean equals(Object obj) {
                return true;
            }

        });
        return fs;
    }

    public static void copyFile(File src, File tar) throws Exception {

        if (src.isFile()) {
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                InputStream is = new FileInputStream(src);
                bis = new BufferedInputStream(is);
                OutputStream op = new FileOutputStream(tar);
                bos = new BufferedOutputStream(op);
                byte[] bt = new byte[8192];
                int len = bis.read(bt);
                while (len != -1) {
                    bos.write(bt, 0, len);
                    len = bis.read(bt);
                }
                bis.close();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

            }

        } else if (src.isDirectory()) {
            File[] files = src.listFiles();
            tar.mkdir();
            for (int i = 0;
                 i < files.length;
                 i++) {
                copyFile(files[i].getAbsoluteFile(),
                        new File(tar.getAbsoluteFile() + File.separator
                                + files[i].getName())
                );
            }
        } else {
            throw new FileNotFoundException();
        }

    }

    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath);
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private static void delAllFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        String[] tempList = file.list();
        if (tempList == null) {
            return;
        }
        File temp = null;
        for (int i = 0;
             i < tempList.length;
             i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);
                delFolder(path + "/" + tempList[i]);
            }
        }
    }

    public static boolean fileExistAndDelete(File file) {
        return file.exists() && file.delete();

    }

    public static File convertUriToFile(Activity activity, Uri uri) {
        File file = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            @SuppressWarnings("deprecation")
            Cursor actualimagecursor = activity.managedQuery(uri, proj, null,
                    null, null);
            if (actualimagecursor != null) {
                int actual_image_column_index = actualimagecursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                actualimagecursor.moveToFirst();
                String img_path = actualimagecursor
                        .getString(actual_image_column_index);
                if (!Utils.isEmpty(img_path)) {
                    file = new File(img_path);
                }
            } else {

                file = new File(new URI(uri.toString()));
                if (file.exists()) {
                    return file;
                }

            }
        } catch (Exception e) {
        }
        return file;

    }

    public static int getOrientationOfFile(String fileName) {
        int orientation = 0;
        try {
            ExifInterface exif = new ExifInterface(fileName);
            String orientationString = exif
                    .getAttribute(ExifInterface.TAG_ORIENTATION);
            if (Utils.isNubmer(orientationString)) {
                int orc = Integer.valueOf(orientationString);
                switch (orc) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        orientation = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        orientation = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        orientation = 270;
                        break;
                    default:
                        break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orientation;

    }

    public static File createFile() {
        // 创建String对象保存文件名路径

        // 创建指定路径的文件
        File file = new File(filePath, tempFileName);
        // 如果文件不存在
        if (file.exists()) {
            // 创建新的空文件
            file.delete();
        }
        File fileParent = file.getParentFile();
        if (!fileParent.exists()) {
            fileParent.mkdirs();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static void deleteTempFile() {
        // 创建String对象保存文件名路径

        // 创建指定路径的文件
        File file = new File(filePath, tempFileName);
        // 如果文件不存在
        if (file.exists()) {
            // 创建新的空文件
            file.delete();
        }
    }

}
