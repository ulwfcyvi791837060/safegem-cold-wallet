package com.bankledger.safecold.utils;

import android.util.Log;
import android.widget.Toast;

import com.bankledger.safecold.SafeColdApplication;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/8/2 09:49
 */
public class ToastUtil {

    private static Toast singleToast;


    public static void showToast(String content) {
        showToast(content, Toast.LENGTH_SHORT);
    }

    public static void showToast(int contentResource) {
        showToast(SafeColdApplication.mContext.getString(contentResource), Toast.LENGTH_SHORT);
    }

    public static void showToast(int contentResource, int duration) {
        showToast(SafeColdApplication.mContext.getString(contentResource), duration);
    }

    public static void showToast(String content, int duration) {
        if (singleToast == null) {
            singleToast = Toast.makeText(SafeColdApplication.mContext, content, duration);
        } else {
            singleToast.setText(content);
        }
        singleToast.show();
    }

    public static void cancel() {
        if (singleToast != null)
            singleToast.cancel();
    }
}
