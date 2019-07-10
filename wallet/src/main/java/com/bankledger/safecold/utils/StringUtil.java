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

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.text.TextUtils;

import com.bankledger.safecold.SafeColdApplication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public static boolean checkBackupFileOfCold(String fileName) {
        Pattern pattern = Pattern.compile("[^-]{6,6}_[^-]{6,6}.bak");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    public static String subZeroAndDot(String amount) {
        if (TextUtils.isEmpty(amount)) {
            return "0";
        }
        if (amount.indexOf(".") > 0) {
            amount = amount.replaceAll("0+?$", "");//去掉多余的0
            amount = amount.replaceAll("[.]$", "");//如最后一位是.则去掉
        }
        return amount;
    }

    public static boolean passwordContain(String str) {
        boolean isDigit = false;
        boolean isLower = false;
        boolean isUpper = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                isDigit = true;
                if (isLower && isUpper) break;
            } else if (Character.isLowerCase(c)) {
                isLower = true;
                if (isDigit && isUpper) break;
            } else if (Character.isUpperCase(c)) {
                isUpper = true;
                if (isDigit && isLower) break;
            }
        }
        return isDigit && isLower && isUpper;
    }

    //byte数组转16进制字符串
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static boolean isChinese(String s) {
        String chinese = "[\u4E00-\u9FA5]";
        return s.matches(chinese);
    }
}
