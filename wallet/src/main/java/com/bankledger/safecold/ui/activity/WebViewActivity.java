package com.bankledger.safecold.ui.activity;

import com.bankledger.safecold.R;

/**
 * Created by zm on 2018/9/19.
 */

public class WebViewActivity extends ToolbarBaseActivity {


    @Override
    protected int setContentLayout() {
        return R.layout.activity_webview;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.user_protocol);
        setDefaultNavigation();
    }

}
