package com.bankledger.safecold.ui.widget;

import android.text.InputFilter;
import android.text.Spanned;

import com.bankledger.safecold.utils.StringUtil;

/**
 * $desc EOS名称过滤
 * @author bankledger
 * @time 2018/10/30 09:08
 */
public class EosDigitsInputFilter implements InputFilter {

    private String digits = "abcdefghijklmnopqrstuvwxyz12345";

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        for (int i = start; i < end; i++) {
            if (!digits.contains(String.valueOf(source.charAt(i)))) {
                return "";
            }
        }
        return null;
    }
}
