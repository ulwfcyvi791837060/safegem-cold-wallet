package com.bankledger.safecold.ui.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.AsyncTaskResult;
import com.bankledger.safecold.ui.activity.AppUpgradeActivity;
import com.bankledger.safecold.ui.activity.HelpActivity;
import com.bankledger.safecold.ui.activity.MyPublicKeyActivity;
import com.bankledger.safecold.ui.activity.SeedBackupActivity;
import com.bankledger.safecold.ui.activity.SignMessageActivity;
import com.bankledger.safecold.ui.activity.MonitorAddressActivity;
import com.bankledger.safecold.ui.activity.SettingActivity;
import com.bankledger.safecold.ui.activity.VerifyMessageSignatureActivity;
import com.bankledger.safecold.ui.activity.WalletInfoActivity;
import com.bankledger.safecold.ui.animation.ExpandAnimation;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.exception.PasswordException;

/**
 * Created by zm on 2018/6/22.
 */

public class MineFragment extends BaseFragment implements View.OnClickListener {

    private TextView tvWalletName;
    private CommonTextWidget ctwMessageSign;
    private LinearLayout llSubMessageSign;
    private ImageView rightImageView;


    @Override
    public int setContentView() {
        return R.layout.fragment_mine;
    }

    @Override
    public void initView() {
        tvWalletName = findViewById(R.id.tv_wallet_name);

        CommonTextWidget ctwWalletInfo = findViewById(R.id.ctw_wallet_info);
        ctwWalletInfo.setLeftText(R.string.wallet_info);
        ctwWalletInfo.setLeftImageResource(R.mipmap.wallet_info);
        ctwWalletInfo.setOnClickListener(this);

        CommonTextWidget ctwMonitoringAccount = findViewById(R.id.ctw_monitoring_account);
        ctwMonitoringAccount.setLeftText(R.string.monitoring_account);
        ctwMonitoringAccount.setLeftImageResource(R.mipmap.my_monitoring_account);
        ctwMonitoringAccount.setOnClickListener(this);

        CommonTextWidget ctwPublicKeyBook = findViewById(R.id.ctw_public_key_book);
        ctwPublicKeyBook.setLeftText(R.string.public_key_book);
        ctwPublicKeyBook.setLeftImageResource(R.mipmap.my_public_key);
        ctwPublicKeyBook.setOnClickListener(this);

        CommonTextWidget ctwManySignDeal = findViewById(R.id.ctw_many_sign_deal);
        ctwManySignDeal.setLeftText(R.string.many_sign_deal);
        ctwManySignDeal.setLeftImageResource(R.mipmap.my_many_sign_deal);
        ctwManySignDeal.setOnClickListener(this);

        ctwMessageSign = findViewById(R.id.ctw_message_sign);
        ctwMessageSign.setLeftText(R.string.message_sign);
        ctwMessageSign.setRightImageResource(R.drawable.ic_expand_more_gray_24dp);
        ctwMessageSign.setLeftImageResource(R.mipmap.my_message_sign);
        ctwMessageSign.setOnClickListener(this);

        rightImageView = ctwMessageSign.getRightImageView();
        llSubMessageSign = findViewById(R.id.ll_sub_message_sign);

        //默认关闭消息前面展开
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ctwMessageSign.performClick();
            }
        }, 200);

        findViewById(R.id.tv_sign_message).setOnClickListener(this);
        findViewById(R.id.tv_verify_message_sign).setOnClickListener(this);

        CommonTextWidget ctwSetting = findViewById(R.id.ctw_setting);
        ctwSetting.setLeftText(R.string.setting);
        ctwSetting.setLeftImageResource(R.mipmap.my_setting);
        ctwSetting.setOnClickListener(this);

        CommonTextWidget ctwMyPublicKey = findViewById(R.id.ctw_my_public_key);
        ctwMyPublicKey.setLeftText(R.string.my_public_key);
        ctwMyPublicKey.setLeftImageResource(R.mipmap.public_key);
        ctwMyPublicKey.setOnClickListener(this);

        CommonTextWidget ctwBackupsSeed = findViewById(R.id.ctw_backups_seed);
        ctwBackupsSeed.setLeftText(R.string.backups_seed);
        ctwBackupsSeed.setLeftImageResource(R.mipmap.backups_seed);
        ctwBackupsSeed.setOnClickListener(this);

        CommonTextWidget ctwSafeUpgrade = findViewById(R.id.ctw_safe_upgrade);
        ctwSafeUpgrade.setLeftText(R.string.safe_upgrade);
        ctwSafeUpgrade.setLeftImageResource(R.mipmap.my_safe_upgrade);
        ctwSafeUpgrade.setOnClickListener(this);

        CommonTextWidget ctwHelp = findViewById(R.id.ctw_help);
        ctwHelp.setLeftText(R.string.help);
        ctwHelp.setLeftImageResource(R.mipmap.my_help);
        ctwHelp.setOnClickListener(this);
    }

    @Override
    public void initData() {
        tvWalletName.setText(WalletInfoManager.getWalletName());
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.ctw_wallet_info://钱包信息
                go2Activity(WalletInfoActivity.class);
                break;

            case R.id.ctw_monitoring_account://监控账户
                go2Activity(MonitorAddressActivity.class);
                break;

            case R.id.ctw_public_key_book://公钥簿
                break;

            case R.id.ctw_many_sign_deal://多签名交易
                break;

            case R.id.ctw_message_sign://消息签名
                animMessageSign();
                break;

            case R.id.tv_sign_message://签名消息
                go2Activity(SignMessageActivity.class);
                break;

            case R.id.tv_verify_message_sign://验证消息签名
                go2Activity(VerifyMessageSignatureActivity.class);
                break;

            case R.id.ctw_setting://设置
                go2Activity(SettingActivity.class);
                break;

            case R.id.ctw_my_public_key://我的公钥
                inputWalletPassword(2);
                break;

            case R.id.ctw_backups_seed://备份种子密码
                inputWalletPassword(0);
                break;

            case R.id.ctw_safe_upgrade://安全升级
                inputWalletPassword(1);
                break;

            case R.id.ctw_help://帮助
                go2Activity(HelpActivity.class);
                break;
            default:
                break;
        }
    }

    public void inputWalletPassword(final int type) {
        DialogUtil.showEditPasswordDialog(getActivity(), R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE)
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        inputWalletPassword(type);
                    } else {
                        if (type == 0) {
                            getMnemonicSeed(content);
                        } else if (type == 1) {
                            checkPassword(content);
                        } else if (type == 2) {
                            checkPublicKey(content);
                        }
                    }
            }
        });
    }

    private void checkPublicKey(String password) {
        new CommonAsyncTask.Builder<String, Void, String>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(getActivity());
                    }
                })
                .setIDoInBackground(new IDoInBackground<String, Void, String>() {
                    @Override
                    public String doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        return HDAddressManager.getInstance().getHDAccount().getAccountPubKey(strings[0]);
                    }
                })
                .setIPostExecute(new IPostExecute<String>() {
                    @Override
                    public void onPostExecute(String s) {
                        CommonUtils.dismissProgressDialog();
                        if (s == null) {
                            ToastUtil.showToast(R.string.password_error);
                        } else {
                            Bundle args = new Bundle();
                            args.putString(Constants.INTENT_KEY1, s);
                            go2Activity(MyPublicKeyActivity.class, args);
                        }
                    }
                })
                .start(password);
    }

    private void checkPassword(final String password) {
        new CommonAsyncTask.Builder<String, Void, Boolean>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(getActivity());
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
                            go2Activity(AppUpgradeActivity.class);
                        } else {
                            ToastUtil.showToast(R.string.password_error);
                        }
                    }
                })
                .start(password);
    }

    private void getMnemonicSeed(String password) {
        new CommonAsyncTask.Builder<String, Void, AsyncTaskResult<byte[]>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(getActivity());
                    }
                })
                .setIDoInBackground(new IDoInBackground<String, Void, AsyncTaskResult<byte[]>>() {
                    @Override
                    public AsyncTaskResult<byte[]> doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        Boolean result = HDAddressManager.getInstance().getHDAccount().checkWithPassword(strings[0]);
                        if(result){
                            try {
                                return new AsyncTaskResult<>(HDAddressManager.getInstance().getHDAccount().decryptMnemonicSeed(strings[0]));
                            } catch (Exception e) {
                                return new AsyncTaskResult<>(e);
                            }
                        } else {
                            return new AsyncTaskResult<>(false, null);
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

    /**
     * 消息签名展开收拢
     */
    private void animMessageSign() {
        ExpandAnimation animation = new ExpandAnimation(rightImageView, llSubMessageSign, 100);
        llSubMessageSign.startAnimation(animation);
    }

}
