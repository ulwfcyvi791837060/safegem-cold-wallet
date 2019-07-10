package com.bankledger.safecold.utils;

import android.util.Log;

/**
 * @Class: LogUtils
 * @Description: 日志处理类
 * @author zm
 */
public class LogUtils {

    private static boolean DEBUG = true;
    private static final String tag = "LogUtils";

    public static void setDebug(boolean debug){
        DEBUG = debug;
    }


    public static void i(String message, Object... object) {
        if(DEBUG) {
            Log.i(tag, String.format(message, object));
        }
    }

    public static void i(String tag, String message, Object... object) {
        if(DEBUG) {
            Log.i(tag, String.format(message, object));
        }
    }

    public static void v(String message, Object... object) {
        if(DEBUG) {
            Log.v(tag, String.format(message, object));
        }
    }


    public static void v(String tag, String message, Object... object) {
        if(DEBUG) {
            Log.v(tag, String.format(message, object));
        }
    }

    public static void d(String message, Object... object) {
        if(DEBUG) {
            Log.d(tag, String.format(message, object));
        }
    }

    public static void d(String tag, String message, Object... object) {
        if(DEBUG) {
            Log.d(tag, String.format(message, object));
        }
    }

    public static void w(String message, Object... object) {
        if(DEBUG) {
            Log.w(tag, String.format(message, object));
        }
    }

    public static void w(String tag, String message, Object... object) {
        if(DEBUG) {
            Log.w(tag, String.format(message, object));
        }
    }

    public static void e(String message, Object... object) {
        if(DEBUG) {
            Log.e(tag, String.format(message, object));
        }
    }

    public static void e(String tag, String message, Object... object) {
        if(DEBUG) {
            Log.e(tag, String.format(message, object));
        }
    }

}
