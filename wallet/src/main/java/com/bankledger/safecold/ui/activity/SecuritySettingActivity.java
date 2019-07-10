package com.bankledger.safecold.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.SharedPreferencesUtil;

/**
 * 安全设置
 * @author bankledger
 * @time 2018/7/30 09:35
 */
public class SecuritySettingActivity extends ToolbarBaseActivity {
    private CommonTextWidget ctwFingerprintSetting;
    private CommonTextWidget ctwGestureSetting;
    private Switch sFingerprint;
    private boolean gestureSwitch;
    private String gesturePassword;
    private FingerprintManagerCompat fingerprintManager;
    private SharedPreferencesUtil sp;
    private boolean fingerprintSwitch;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_security_setting;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.security_setting);
        setDefaultNavigation();

        ctwFingerprintSetting = findViewById(R.id.ctw_fingerprint_setting);
        ctwFingerprintSetting.setLeftText(R.string.fingerprint_setting);
        sFingerprint = new Switch(this);
        ctwFingerprintSetting.setRightView(sFingerprint);

        sFingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!fingerprintManager.hasEnrolledFingerprints()) {
                        sFingerprint.setChecked(false);
                        DialogUtil.showTextDialog(SecuritySettingActivity.this, getString(R.string.tip), getString(R.string.no_fingerprint), true,
                                new DialogUtil.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which, String content) {
                                        if(which== AlertDialog.BUTTON_POSITIVE) {
                                            toSystemSetActivity("com.android.settings.Settings");
                                        }
                                    }
                                });
                    } else {
                        sp.put(Constants.KEY_SP_FINGERPRINT_SWITCH, isChecked);
                    }
                } else {
                    sp.put(Constants.KEY_SP_FINGERPRINT_SWITCH, isChecked);
                }
            }
        });

        ctwGestureSetting = findViewById(R.id.ctw_gesture_setting);
        ctwGestureSetting.setLeftText(R.string.gesture_setting);
        ctwGestureSetting.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwGestureSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2Activity(GestureLockActivity.class);
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
        sp = SafeColdApplication.appSharedPreferenceUtil;

        fingerprintManager = FingerprintManagerCompat.from(this);
        fingerprintSwitch = sp.get(Constants.KEY_SP_FINGERPRINT_SWITCH, false);
        sFingerprint.setChecked(fingerprintSwitch);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    private void refreshState() {

        gesturePassword = sp.get(Constants.KEY_SP_GESTURE_PASSWORD, "");
        gestureSwitch = sp.get(Constants.KEY_SP_GESTURE_SWITCH, false);
        if (TextUtils.isEmpty(gesturePassword)) {
            ctwGestureSetting.setRightHintText(R.string.not_set);
        } else if (gestureSwitch) {
            ctwGestureSetting.setRightHintText(R.string.is_enabled);
        } else {
            ctwGestureSetting.setRightHintText(R.string.not_enabled);
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
