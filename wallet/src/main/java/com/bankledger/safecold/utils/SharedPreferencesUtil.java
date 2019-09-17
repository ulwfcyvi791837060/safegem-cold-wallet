package com.bankledger.safecold.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.bankledger.safecold.Constants;

public class SharedPreferencesUtil {

    private SharedPreferences preferences;

    private static SharedPreferencesUtil spUtils;

    public SharedPreferencesUtil(Context context, String fileName) {
        preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public SharedPreferences getSharedPreferences() {
        return preferences;
    }


    public synchronized static SharedPreferencesUtil getDefaultPreferences(Context context) {
        if (spUtils == null) {
            spUtils = new SharedPreferencesUtil(context, "default");
        }
        return spUtils;
    }

    /**
     * *************** get ******************
     */

    public String get(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    public boolean get(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public float get(String key, float defValue) {
        return preferences.getFloat(key, defValue);
    }

    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    public long get(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    /**
     * *************** put ******************
     */
    public void put(String key, String value) {
        if (value == null) {
            preferences.edit().remove(key).apply();
        } else {
            preferences.edit().putString(key, value).apply();
        }
    }

    public void put(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    public void put(String key, float value) {
        preferences.edit().putFloat(key, value).apply();
    }

    public void put(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }


    public void clearSP() {
        boolean fingerprintSwitch = preferences.getBoolean(Constants.KEY_SP_FINGERPRINT_SWITCH, false);
        String gesturePassword = preferences.getString(Constants.KEY_SP_GESTURE_PASSWORD, null);
        boolean gestureSwitch = preferences.getBoolean(Constants.KEY_SP_GESTURE_SWITCH, false);
        preferences.edit().clear().apply();
        put(Constants.KEY_SP_FINGERPRINT_SWITCH, fingerprintSwitch);
        if (gesturePassword == null) {
            put(Constants.KEY_SP_GESTURE_SWITCH, false);
        } else {
            put(Constants.KEY_SP_GESTURE_PASSWORD, gesturePassword);
            put(Constants.KEY_SP_GESTURE_SWITCH, gestureSwitch);
        }
    }
}