package com.bankledger.safecold.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公用方法
 *
 * @author zhangmiao
 */

public class CommonUtils {

    private static long lastClickTime;
    private static LockProgressDialog dialog;
    private static final int MIN_CLICK_DELAY_TIME = 500;

    public static boolean isRepeatClick() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            flag = true;
            lastClickTime = curClickTime;
        }
        return flag;
    }

    public static void showProgressDialog(Context mContext, final String showStr, final boolean cancelable) {
        if (mContext != null) {
            if (dialog == null) {
                dialog = new LockProgressDialog(mContext);
                dialog.setTitle(showStr);
                dialog.setCancelable(cancelable);
                dialog.setCanceledOnTouchOutside(cancelable);
                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                            if (cancelable) {
                                dialog.dismiss();
                                dialog = null;
                            }
                        }
                        return false;
                    }
                });
                dialog.show();
            } else {
                if (!dialog.isShowing()) {
                    dialog.show();
                }
            }
        }
    }

    public static void showProgressDialog(Context mContext) {
        showProgressDialog(mContext, null);
    }

    public static void showProgressDialog(Context mContext, final String showStr) {
        showProgressDialog(mContext, showStr, false);
    }

    public static void dismissProgressDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    public static InputFilter forbidEmoji() {
        final Pattern emoji = Pattern.compile("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]", Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        InputFilter emijoyFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Matcher emojiMatcher = emoji.matcher(source);
                if (emojiMatcher.find()) {
                    return "";
                }
                return null;
            }
        };
        return emijoyFilter;
    }
}
