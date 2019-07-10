package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.AsyncTaskResult;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.ui.widget.DigitsInputFilter;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.StringUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicCode;
import com.bankledger.safecoldj.exception.SaveChipException;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

/**
 * 创建密码
 * @author bankledger
 * @time 2018/8/14 19:10
 */
public class CreatePasswordActivity extends ToolbarBaseActivity {

    private EditText etPassword;
    private EditText etPassword2;
    private ArrayList<String> wordList;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_create_password;
    }

    @Override
    public void initView() {
        super.initView();
        etPassword = findViewById(R.id.et_password);
        etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.PASSWORD_LENGTH_MAX), new DigitsInputFilter()});
        etPassword2 = findViewById(R.id.et_password2);
        etPassword2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.PASSWORD_LENGTH_MAX), new DigitsInputFilter()});
        findViewById(R.id.bt_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkParams()) {
                    return;
                }

                restoreWallet(etPassword.getText().toString());
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
        setDefaultNavigation();
        setTitle(R.string.set_password);
        wordList = getIntent().getStringArrayListExtra(Constants.INTENT_KEY1);
    }

    private boolean checkParams() {
        if (SafeColdSettings.DEV_DEBUG) {
            return true;
        }
        if (TextUtils.isEmpty(etPassword.getText().toString())) {
            ToastUtil.showToast(R.string.hint_input_password);
            return false;
        } else if (TextUtils.isEmpty(etPassword2.getText().toString())) {
            ToastUtil.showToast(R.string.submit_input_password);
            return false;
        } else if (etPassword.getText().length() < Constants.PASSWORD_LENGTH_MIN) {
            ToastUtil.showToast(R.string.input_password_length_min);
            return false;
        } else if (!etPassword.getText().toString().equals(etPassword2.getText().toString())) {
            ToastUtil.showToast(R.string.input_password_err);
            return false;
        } else if (!StringUtil.passwordContain(etPassword.getText().toString())) {
            ToastUtil.showToast(R.string.input_password_contain);
            return false;
        }
        return true;
    }

    private void restoreWallet(String password) {
        new CommonAsyncTask.Builder<String, Void, AsyncTaskResult<HDAccount>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(CreatePasswordActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<String, Void, AsyncTaskResult<HDAccount>>() {
                    @Override
                    public AsyncTaskResult<HDAccount> doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        try {
                            byte[] mnemonicCodeSeed = MnemonicCode.instance().toEntropy(wordList);
                            HDAccount hdAccount = new HDAccount(mnemonicCodeSeed, strings[0], false);
                            return new AsyncTaskResult<>(hdAccount);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return new AsyncTaskResult<>(e);
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<AsyncTaskResult<HDAccount>>() {
                    @Override
                    public void onPostExecute(AsyncTaskResult<HDAccount> asyncTaskResult) {
                        CommonUtils.dismissProgressDialog();
                        if (asyncTaskResult.isSuccess()) {
                            SafeColdApplication.appSharedPreferenceUtil.put(Constants.KEY_SP_START_GUIDE, true);
                            EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_CREATE_HDACCOUNT, asyncTaskResult.getResult()));
                            Intent intent = new Intent(CreatePasswordActivity.this, WalletInfoManager.hasWalletName() ? MainWalletActivity.class : SetWalletNameActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            go2Activity(intent);
                            finish();
                        } else {
                            SafeColdApplication.clearData();
                            if (asyncTaskResult.getException() instanceof SaveChipException) {
                                long ret = ((SaveChipException) asyncTaskResult.getException()).ret;
                                ToastUtil.showToast(String.format(getString(R.string.create_wallet_error), String.valueOf(ret)));
                            } else {
                                ToastUtil.showToast(String.format(getString(R.string.reset_password_error), "-"));
                            }
                        }
                    }
                })
                .start(password);
    }

    @Override
    public void onBackPressed() {

    }
}
