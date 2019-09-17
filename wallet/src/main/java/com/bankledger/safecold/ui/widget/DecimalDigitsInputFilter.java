package com.bankledger.safecold.ui.widget;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

/**
 * @author bankledger
 * @time 2018/8/31 10:18
 */
public class DecimalDigitsInputFilter implements InputFilter {

    private int integerDigits = Integer.MAX_VALUE;
    private final int decimalDigits;

    public DecimalDigitsInputFilter(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    public DecimalDigitsInputFilter(int integerDigits, int decimalDigits) {
        this.integerDigits = integerDigits;
        this.decimalDigits = decimalDigits;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        int dotPos = -1;
        int len = dest.length();
        for (int i = 0; i < len; i++) {
            char c = dest.charAt(i);
            if (c == '.' || c == ',') {
                dotPos = i;
                break;
            }
        }
        if (dotPos >= 0) {
            if (source.equals(".") || source.equals(",")) {
                return "";
            }
            if (integerDigits > 0 && dotPos >= integerDigits && dend <= dotPos) {
                return "";
            }
            if (dend <= dotPos) {
                return null;
            }
            if (len - dotPos > decimalDigits) {
                return "";
            }
        } else {
            if (len >= integerDigits && !(source.equals(".") || source.equals(","))) {
                return "";
            }
        }
        return null;
    }

}
