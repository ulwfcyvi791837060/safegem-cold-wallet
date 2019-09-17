package com.bankledger.safecold.ui.activity;

import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;

/**
 * @author bankledger
 * @time 2018/8/17 10:31
 */
public class QrCodeContentActivity extends ToolbarBaseActivity {
    private TextView tvContent;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_qr_code_content;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.scan_result);
        tvContent = findViewById(R.id.tv_content);
    }

    @Override
    public void initData() {
        super.initData();
        String content = getIntent().getStringExtra(Constants.INTENT_KEY1);
        tvContent.setText(content);
    }
}
