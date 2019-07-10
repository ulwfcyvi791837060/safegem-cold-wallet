package com.bankledger.safecold.utils;

import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import com.bankledger.safecold.R;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/8/22 20:49
 */
public class SpanUtil {
    public static void setMatchSpan(String matchStr, TextView... views) {
        if (TextUtils.isEmpty(matchStr)) return;
        int spanLen = matchStr.length();
        for (TextView view : views) {
            String str = view.getText().toString().toLowerCase();
            SpannableStringBuilder builder = new SpannableStringBuilder(view.getText());
            if (str.contains(matchStr.toLowerCase())) {
                //当matchStr结尾时string.slip切割出来的数组长度为1
                if (str.endsWith(matchStr.toLowerCase())) {
                    ForegroundColorSpan blueSpan = new ForegroundColorSpan(ContextCompat.getColor(view.getContext(), R.color.blue));
                    builder.setSpan(blueSpan, str.length() - matchStr.length(), str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    String[] arr = str.split(matchStr.toLowerCase());
                    int startIndex = 0;
                    for (int i = 0; i < arr.length - 1; i++) {
                        startIndex += arr[i].length();
                        ForegroundColorSpan blueSpan = new ForegroundColorSpan(ContextCompat.getColor(view.getContext(), R.color.blue));
                        builder.setSpan(blueSpan, startIndex, startIndex + spanLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        startIndex += spanLen;
                    }
                }
            }
            view.setText(builder);
        }
    }
}
