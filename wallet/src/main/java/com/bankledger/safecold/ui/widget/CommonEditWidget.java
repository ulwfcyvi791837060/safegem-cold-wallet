package com.bankledger.safecold.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.bankledger.safecold.R;

/**
 * 公用输入组件
 *
 * @author zm
 */
public class CommonEditWidget extends CommonWidget {

    private EditText common_edit;
    private TextInputLayout common_text_input;

    public CommonEditWidget(Context context) {
        super(context);
    }

    public CommonEditWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.CommonEditWidget);

        int input_type = mTypedArray.getInteger(R.styleable.CommonEditWidget_input_type, 1);

        switch (input_type) {
            case 1:
                setInputType(InputType.TYPE_CLASS_TEXT);
                break;
            case 2:
                setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case 3:
                setInputType(InputType.TYPE_CLASS_PHONE);
                break;
            case 4:
                setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                break;
            case 5:
                setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                break;
            case 6:
                setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                break;
        }

        mTypedArray.recycle();
    }

    @Override
    public void createView() {
        setMiddleView(R.layout.common_edit);
        common_text_input = (TextInputLayout) findViewById(R.id.common_text_input);
        common_edit = common_text_input.getEditText();
    }

    public void setText(CharSequence text) {
        common_edit.setText(text);
    }

    public void setHint(CharSequence text) {
        common_text_input.setHint(text);
    }

    public void setHint(int textResource) {
        common_text_input.setHint(getContext().getString(textResource));
    }

    public void setErrorEnabled(boolean enabled) {
        common_text_input.setErrorEnabled(enabled);
    }

    public void setError(CharSequence error) {
        common_text_input.setError(error);
    }

    public String getText() {
        return common_edit.getText().toString();
    }

    public void setInputType(int type) {
        common_edit.setInputType(type);
    }

    public EditText getEditText() {
        return common_edit;
    }

    public TextInputLayout getTextInputLayout() {
        return common_text_input;
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        if (textWatcher != null) {
            common_edit.addTextChangedListener(textWatcher);
        }
    }

}
