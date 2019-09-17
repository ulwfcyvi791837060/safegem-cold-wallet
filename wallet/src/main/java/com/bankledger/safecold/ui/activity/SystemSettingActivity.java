package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.view.View;

import com.bankledger.safecold.R;
import com.bankledger.safecold.ui.widget.CommonTextWidget;

/**
 * @author bankledger
 * @time 2018/8/17 10:58
 */
public class SystemSettingActivity extends ToolbarBaseActivity implements View.OnClickListener {
    private CommonTextWidget ctwViewSet;
    private CommonTextWidget ctwFingerprintSet;
    private CommonTextWidget ctwLanguageSet;
    private CommonTextWidget ctwDateSet;
    private CommonTextWidget ctwVolSet;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_system_setting;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.system_setting);

        ctwViewSet = findViewById(R.id.ctw_view_set);
        ctwViewSet.setLeftText(R.string.view_set);
        ctwViewSet.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwViewSet.setOnClickListener(this);

        ctwFingerprintSet = findViewById(R.id.ctw_fingerprint_set);
        ctwFingerprintSet.setLeftText(R.string.fingerprint_set);
        ctwFingerprintSet.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwFingerprintSet.setOnClickListener(this);

        ctwLanguageSet = findViewById(R.id.ctw_language_set);
        ctwLanguageSet.setLeftText(R.string.language_set);
        ctwLanguageSet.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwLanguageSet.setOnClickListener(this);

        ctwDateSet = findViewById(R.id.ctw_date_set);
        ctwDateSet.setLeftText(R.string.date_set);
        ctwDateSet.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwDateSet.setOnClickListener(this);

        ctwVolSet = findViewById(R.id.ctw_vol_set);
        ctwVolSet.setLeftText(R.string.vol_set);
        ctwVolSet.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwVolSet.setOnClickListener(this);
    }

    @Override
    public void initData() {
        super.initData();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ctw_view_set:
                break;

            case R.id.ctw_fingerprint_set:
                toSystemSetActivity( "com.android.settings.Settings$FingerprintEnrollSuggestionActivity");
                break;

            case R.id.ctw_language_set:
                break;

            case R.id.ctw_date_set:
                toSystemSetActivity("com.android.settings.Settings$DateTimeSettingsActivity");
                break;

            case R.id.ctw_vol_set:
                break;

            default:
                break;
        }
    }

    private void toSystemSetActivity(String className) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", className);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
