package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.view.View;

import com.bankledger.safecold.R;
import com.bankledger.safecold.utils.CommonUtils;

/**
 * @author bankledger
 * @time 2018/8/15 09:37
 */
public class SelectLanguageActivity extends ToolbarBaseActivity {

    private final String LANGUAGE_US = "SetUS"; //美国
    private final String LANGUAGE_CH = "SetCH"; //中国
    private final String LANGUAGE_TW = "SetTW"; //台湾
    private final String LANGUAGE_JP = "SetJP"; //日本
    private final String LANGUAGE_KR = "SetKR"; //韩国
    private final String LANGUAGE_MY = "SetMY"; //马来语
    private final String LANGUAGE_FR = "SetFR"; //法语
    private final String LANGUAGE_DE = "SetDE"; //德语
    private final String LANGUAGE_ES = "SetES"; //西班牙语
    private final String LANGUAGE_BR = "SetBR"; //葡萄牙语
    private final String LANGUAGE_RU = "SetRU"; //俄罗斯语
    private final String LANGUAGE_EG = "SetEG"; //阿拉伯语

    @Override
    protected int setContentLayout() {
        return R.layout.activity_select_language;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.language);
        findViewById(R.id.tv_chinese).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguage(LANGUAGE_CH);
            }
        });

        findViewById(R.id.tv_chinese_tw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguage(LANGUAGE_TW);
            }
        });

        findViewById(R.id.tv_english).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguage(LANGUAGE_US);
            }
        });
    }

    private void setLanguage(String language) {
        Intent intent = new Intent("action.change.language");
        intent.putExtra("changeLanguage", language);
        intent.setPackage("com.android.settings");
        sendBroadcast(intent);

        CommonUtils.showProgressDialog(this);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                CommonUtils.dismissProgressDialog();
                go2Activity(AddWalletActivity.class);
                finish();
            }
        },1500);
    }

}
