package com.bankledger.safecold.ui.widget;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/10/22 11:04
 */
public class WipeSpaceTextWatcher implements TextWatcher {

    private final EditText et;

    public WipeSpaceTextWatcher(EditText et) {
        this.et = et;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().contains(" ")) {
            et.setText(s.toString().replace(" ", ""));
            et.setSelection(et.length());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
