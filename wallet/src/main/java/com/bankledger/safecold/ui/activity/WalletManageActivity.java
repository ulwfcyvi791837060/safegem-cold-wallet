package com.bankledger.safecold.ui.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.AsyncTaskResult;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.exception.PasswordException;

import org.spongycastle.util.encoders.Hex;

/**
 * @author bankledger
 * @time 2018/7/26 10:13
 */
public class WalletManageActivity extends ToolbarBaseActivity implements View.OnClickListener {
    private CommonTextWidget ctwChangePassword;
    private CommonTextWidget ctwMyPublicKey;
    private CommonTextWidget ctwBackupsSeed;
    private CommonTextWidget ctwWalletInfo;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_wallet_manage;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.wallet_manage);
        setDefaultNavigation();

        ctwChangePassword = findViewById(R.id.ctw_change_password);
        ctwChangePassword.setLeftText(R.string.change_password);
        ctwChangePassword.setLeftImageResource(R.mipmap.change_password);
        ctwChangePassword.setOnClickListener(this);

        ctwMyPublicKey = findViewById(R.id.ctw_my_public_key);
        ctwMyPublicKey.setLeftText(R.string.my_public_key);
        ctwMyPublicKey.setLeftImageResource(R.mipmap.public_key);
        ctwMyPublicKey.setOnClickListener(this);

        ctwBackupsSeed = findViewById(R.id.ctw_backups_seed);
        ctwBackupsSeed.setLeftText(R.string.backups_seed);
        ctwBackupsSeed.setLeftImageResource(R.mipmap.backups_seed);
        ctwBackupsSeed.setOnClickListener(this);

        ctwWalletInfo = findViewById(R.id.ctw_wallet_info);
        ctwWalletInfo.setLeftText(R.string.wallet_info);
        ctwWalletInfo.setLeftImageResource(R.mipmap.wallet_info);
        ctwWalletInfo.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ctw_change_password://修改密码
                go2Activity(ResetPasswordActivity.class);
                break;

            case R.id.ctw_my_public_key://我的公钥
                setWalletPassword(1);
                break;

            case R.id.ctw_backups_seed://备份种子密码
                setWalletPassword(2);
                break;

            case R.id.ctw_wallet_info://钱包信息
                go2Activity(WalletInfoActivity.class);
                break;
            default:
                break;
        }
    }

    public void setWalletPassword(final int type) {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE)
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        setWalletPassword(type);
                    } else {
                        if (type == 1) {
                            checkPassword(content);
                        } else if (type == 2) {
                            getMnemonicSeed(content);
                        }
                    }
            }
        });
    }

    private void getMnemonicSeed(String password) {
        new CommonAsyncTask.Builder<String, Void, AsyncTaskResult<byte[]>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(WalletManageActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<String, Void, AsyncTaskResult<byte[]>>() {
                    @Override
                    public AsyncTaskResult<byte[]> doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        try {
                            return new AsyncTaskResult<>(HDAddressManager.getInstance().getHDAccount().decryptMnemonicSeed(strings[0]));
                        } catch (PasswordException e) {
                            e.printStackTrace();
                            return new AsyncTaskResult<>(e);
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<AsyncTaskResult<byte[]>>() {
                    @Override
                    public void onPostExecute(AsyncTaskResult<byte[]> result) {
                        CommonUtils.dismissProgressDialog();
                        if (result.isSuccess()) {
                            Bundle args = new Bundle();
                            args.putByteArray(Constants.INTENT_KEY1, result.getResult());
                            go2Activity(SeedBackupActivity.class, args);
                        } else {
                            ToastUtil.showToast(R.string.password_error);
                        }
                    }
                })
                .start(password);
    }

    private void checkPassword(String password) {
        new CommonAsyncTask.Builder<String, Void, Boolean>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(WalletManageActivity.this);
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
                            go2Activity(MyPublicKeyActivity.class);
                        } else {
                            ToastUtil.showToast(R.string.password_error);
                        }
                    }
                })
                .start(password);
    }


}
