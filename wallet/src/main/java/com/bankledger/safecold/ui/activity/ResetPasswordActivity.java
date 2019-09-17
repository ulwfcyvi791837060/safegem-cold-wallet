package com.bankledger.safecold.ui.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.text.InputFilter;
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
import com.bankledger.safecold.ui.widget.CommonEditWidget;
import com.bankledger.safecold.ui.widget.DigitsInputFilter;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.StringUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.exception.PasswordException;

/**
 * @author bankledger
 * @time 2018/8/1 20:00
 */
public class ResetPasswordActivity extends ToolbarBaseActivity {
    private CommonEditWidget cewOldPassword;
    private CommonEditWidget cewNewPassword;
    private CommonEditWidget cewSubmitPassword;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_reset_password;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.change_password);
        setDefaultNavigation();

        cewOldPassword = findViewById(R.id.cew_old_password);
        cewOldPassword.setHint(R.string.old_password);
        cewOldPassword.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.PASSWORD_LENGTH_MAX), new DigitsInputFilter()});
        cewNewPassword = findViewById(R.id.cew_new_password);
        cewNewPassword.setHint(R.string.new_password);
        cewNewPassword.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.PASSWORD_LENGTH_MAX), new DigitsInputFilter()});
        cewSubmitPassword = findViewById(R.id.cew_submit_password);
        cewSubmitPassword.setHint(R.string.submit_password);
        cewSubmitPassword.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.PASSWORD_LENGTH_MAX), new DigitsInputFilter()});

        findViewById(R.id.bt_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkParams())
                    resetPassword(cewOldPassword.getText(), cewNewPassword.getText());
            }
        });

        findViewById(R.id.tv_forget_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtil.showTextDialogWithCancelButton(ResetPasswordActivity.this, R.string.forget_password, R.string.hint_forget_password, new DialogUtil.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String content) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            DialogUtil.showTextDialogWithCancelButton(ResetPasswordActivity.this, R.string.warn, R.string.confirm_continue, new DialogUtil.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, String content) {
                                    if (which == Dialog.BUTTON_POSITIVE) {
                                        SafeColdApplication.clearData();
                                        go2Activity(AddWalletActivity.class);
                                        finish();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
    }

    private boolean checkParams() {
        if (TextUtils.isEmpty(cewOldPassword.getText())) {
            ToastUtil.showToast(R.string.hint_old_password);
            return false;
        } else if (TextUtils.isEmpty(cewNewPassword.getText())) {
            ToastUtil.showToast(R.string.hint_new_password);
            return false;
        } else if (TextUtils.isEmpty(cewSubmitPassword.getText())) {
            ToastUtil.showToast(R.string.hint_submit_password);
            return false;
        } else if (!cewNewPassword.getText().equals(cewSubmitPassword.getText())) {
            ToastUtil.showToast(R.string.hint_new_password_error);
            return false;
        } else if (cewOldPassword.getText().equals(cewNewPassword.getText())) {
            ToastUtil.showToast(R.string.hint_password_error);
            return false;
        } else if (cewNewPassword.getText().length() < Constants.PASSWORD_LENGTH_MIN) {
            ToastUtil.showToast(R.string.input_password_length_min);
            return false;
        } else if (!StringUtil.passwordContain(cewNewPassword.getText())) {
            ToastUtil.showToast(R.string.input_password_contain);
            return false;
        }
        return true;
    }

    private void resetPassword(String oldPassword, String newPassword) {
        new CommonAsyncTask.Builder<String, Void, AsyncTaskResult<Long>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(ResetPasswordActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<String, Void, AsyncTaskResult<Long>>() {
                    @Override
                    public AsyncTaskResult<Long> doInBackground(IPublishProgress<Void> publishProgress, String... params) {
                        try {
                            long ret = HDAddressManager.getInstance().getHDAccount().resetPassword(params[0], params[1]);
                            return new AsyncTaskResult<>(ret);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return new AsyncTaskResult<>(e);
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<AsyncTaskResult<Long>>() {
                    @Override
                    public void onPostExecute(AsyncTaskResult<Long> asyncTaskResult) {
                        CommonUtils.dismissProgressDialog();
                        if (asyncTaskResult.isSuccess()) {
                            if (asyncTaskResult.getResult() == 0) {
                                ToastUtil.showToast(R.string.reset_success);
                                finish();
                            } else {
                                ToastUtil.showToast(String.format(getString(R.string.reset_password_error), String.valueOf(asyncTaskResult.getResult())));
                            }
                        } else if (asyncTaskResult.getException() instanceof PasswordException) {
                            ToastUtil.showToast(R.string.old_password_error);
                        }
                    }
                })
                .start(oldPassword, newPassword);
    }
}
