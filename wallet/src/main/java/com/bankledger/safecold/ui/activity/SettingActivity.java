package com.bankledger.safecold.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.RingManager;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.core.HDAddressManager;

/**
 * @author bankledger
 * @time 2018/7/23 11:21
 */
public class SettingActivity extends ToolbarBaseActivity implements View.OnClickListener {

    private CommonTextWidget ctwChangePassword;
    private CommonTextWidget ctwSafeSetting;
    private CommonTextWidget ctwSystemSetting;
    private CommonTextWidget ctwRestoreFactory;
    private CommonTextWidget ctwPrivacyPolicy;
    private CommonTextWidget ctwUserAgreement;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_setting;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.setting);
        setDefaultNavigation();

        ctwChangePassword = findViewById(R.id.ctw_change_password);
        ctwChangePassword.setLeftText(R.string.change_password);
        ctwChangePassword.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwChangePassword.setOnClickListener(this);

        ctwSafeSetting = findViewById(R.id.ctw_safe_setting);
        ctwSafeSetting.setLeftText(R.string.security_setting);
        ctwSafeSetting.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwSafeSetting.setOnClickListener(this);

        ctwSystemSetting = findViewById(R.id.ctw_system_setting);
        ctwSystemSetting.setLeftText(R.string.system_setting);
        ctwSystemSetting.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwSystemSetting.setOnClickListener(this);

        ctwRestoreFactory = findViewById(R.id.ctw_restore_factory);
        ctwRestoreFactory.setLeftText(R.string.restore_factory);
        ctwRestoreFactory.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwRestoreFactory.setOnClickListener(this);

        ctwPrivacyPolicy = findViewById(R.id.ctw_privacy_policy);
        ctwPrivacyPolicy.setLeftText(R.string.privacy_policy);
        ctwPrivacyPolicy.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwPrivacyPolicy.setOnClickListener(this);

        ctwUserAgreement = findViewById(R.id.ctw_user_agreement);
        ctwUserAgreement.setLeftText(R.string.user_agreement);
        ctwUserAgreement.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
        ctwUserAgreement.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ctw_change_password://修改密码
                go2Activity(ResetPasswordActivity.class);
                break;

            case R.id.ctw_safe_setting://安全设置
                go2Activity(SecuritySettingActivity.class);
                break;

            case R.id.ctw_system_setting://系统设置
                toSystemSetActivity("com.android.settings.Settings");
                break;

            case R.id.ctw_restore_factory://恢复出厂设置
                DialogUtil.showTextDialogWithCancelButton(this, R.string.restore_factory, R.string.restore_factory_warn, new DialogUtil.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String content) {
                        if (which == AlertDialog.BUTTON_POSITIVE) {
                            inputWalletPassword();
                        }
                    }
                });
                break;

            case R.id.ctw_privacy_policy://隐私政策
                break;

            case R.id.ctw_user_agreement://用户协议
                go2Activity(WebViewActivity.class);
                break;
            default:
                break;
        }
    }

    public void inputWalletPassword() {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE)
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        inputWalletPassword();
                    } else {
                        checkPassword(content);
                    }
            }
        });
    }

    private void checkPassword(final String password) {
        new CommonAsyncTask.Builder<String, Void, Boolean>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(SettingActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<String, Void, Boolean>() {
                    @Override
                    public Boolean doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        return HDAddressManager.getInstance().getHDAccount().checkWithPassword(strings[0]);
                    }
                })
                .setIPostExecute(new IPostExecute<Boolean>() {
                    @Override
                    public void onPostExecute(Boolean result) {
                        CommonUtils.dismissProgressDialog();
                        if (result) {
                            RingManager.getInstance().playCrystal();
                            SafeColdApplication.clearData();
                            restoreFactory();
                        } else {
                            ToastUtil.showToast(R.string.password_error);
                        }
                    }
                })
                .start(password);
    }

    private void restoreFactory() {
        Intent intent = new Intent("android.intent.action.FACTORY_RESET");
        intent.setPackage("android");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
        intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", false);
        sendBroadcast(intent);
    }

    private void toSystemSetActivity(String className) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", className);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
