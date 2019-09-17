package com.bankledger.safecold.ui.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.ui.activity.AddWalletActivity;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddressManager;

/**
 * @author bankledger
 * @time 2018/9/20 14:35
 */
public class VerifyPasswordFragment extends BaseFragment {

    private TextView tvTitle;
    private EditText etPassword;

    @Override
    public int setContentView() {
        return R.layout.activity_password_unlock;
    }

    @Override
    public void initView() {
        tvTitle = findViewById(R.id.tv_title);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tvTitle.getLayoutParams();
        params.height = params.height + (int) getStatusBarHeight();
        tvTitle.setLayoutParams(params);
        tvTitle.setPadding(0, (int) getStatusBarHeight(), 0, 0);

        etPassword = findViewById(R.id.et_password);
        findViewById(R.id.bt_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isRepeatClick()) {
                    checkPassword(etPassword.getText().toString());
                }
            }
        });

        findViewById(R.id.tv_forget_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtil.showTextDialogWithCancelButton(getContext(), R.string.forget_password, R.string.hint_forget_password, new DialogUtil.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String content) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            DialogUtil.showTextDialogWithCancelButton(getContext(), R.string.warn, R.string.confirm_continue, new DialogUtil.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, String content) {
                                    if (which == Dialog.BUTTON_POSITIVE) {
                                        SafeColdApplication.clearData();
                                        go2Activity(AddWalletActivity.class);
                                        getBaseActivity().finish();
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
        tvTitle.setText(R.string.password_unlock);
    }

    private void checkPassword(String password) {
        new CommonAsyncTask.Builder<String, Void, HDAccount>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(getContext());
                    }
                })
                .setIDoInBackground(new IDoInBackground<String, Void, HDAccount>() {
                    @Override
                    public HDAccount doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        HDAccount hdAccount = HDAddressManager.getInstance().getHDAccount();
                        if (hdAccount.checkWithPassword(strings[0])) {
                            return hdAccount;
                        } else {
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<HDAccount>() {
                    @Override
                    public void onPostExecute(HDAccount hdAccount) {
                        CommonUtils.dismissProgressDialog();
                        if (hdAccount != null) {
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        } else {
                            ToastUtil.showToast(R.string.password_error);
                        }
                    }
                })
                .start(password);
    }

    private double getStatusBarHeight() {
        double statusBarHeight = Math.ceil(25 * getResources().getDisplayMetrics().density);
        return statusBarHeight;
    }

}
