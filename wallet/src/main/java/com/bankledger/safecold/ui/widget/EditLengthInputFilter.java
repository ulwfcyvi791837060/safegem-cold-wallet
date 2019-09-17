package com.bankledger.safecold.ui.widget;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * $desc 限制输入长度 中文算两个 英文算一个
 *
 * @author bankledger
 * @time 2018/8/31 10:18
 */
public class EditLengthInputFilter implements InputFilter {

    private final int maxLength;

    public EditLengthInputFilter(int maxLength) {
        this.maxLength = maxLength;
    }


    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        int dindex = 0;
        int count = 0;

        while (count <= maxLength && dindex < dest.length()) {
            char c = dest.charAt(dindex++);
            if (c < 128) {
                count = count + 1;
            } else {
                count = count + 2;
            }
        }

        if (count > maxLength) {
            return dest.subSequence(0, dindex - 1);
        }

        int sindex = 0;
        while (count <= maxLength && sindex < source.length()) {
            char c = source.charAt(sindex++);
            if (c < 128) {
                count = count + 1;
            } else {
                count = count + 2;
            }
        }

        if (count > maxLength) {
            sindex--;
        }

        return source.subSequence(0, sindex);
    }
}
