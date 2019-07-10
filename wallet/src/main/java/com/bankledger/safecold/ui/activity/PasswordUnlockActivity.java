package com.bankledger.safecold.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CrashHandler;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.EncryptionChipManagerV2;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.db.AbstractDb;

import java.io.File;
import java.util.List;

/**
 * @author bankledger
 * @time 2018/8/14 19:10
 */
public class PasswordUnlockActivity extends BaseActivity {

    private TextView tvTitle;
    private EditText etPassword;
    private int clickCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_unlock);
    }

    @Override
    public void initView() {
        super.initView();

        tvTitle = findViewById(R.id.tv_title);
        tvTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (++clickCount >= 8) {
                    showCrashLogDialog();
                    clickCount = 0;
                }
            }
        });

        etPassword = findViewById(R.id.et_password);
        findViewById(R.id.bt_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPassword(etPassword.getText().toString());
            }
        });

        findViewById(R.id.tv_forget_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtil.showTextDialogWithCancelButton(PasswordUnlockActivity.this, R.string.forget_password, R.string.hint_forget_password, new DialogUtil.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String content) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            DialogUtil.showTextDialogWithCancelButton(PasswordUnlockActivity.this, R.string.warn, R.string.confirm_continue, new DialogUtil.OnClickListener() {
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
        tvTitle.setText(R.string.password_unlock);
    }

    private void checkPassword(String password) {
        new CommonAsyncTask.Builder<String, Void, HDAccount>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(PasswordUnlockActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<String, Void, HDAccount>() {
                    @Override
                    public HDAccount doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        HDAccount hdAccount = HDAddressManager.getInstance().getHDAccount(strings[0]);
                        if (hdAccount.checkWithPassword(strings[0])) {
                            if (SafeColdSettings.SEED_SAVE_TO_CHIP) {
                                EncryptionChipManagerV2.getInstance().fromOldSeedToNew(strings[0]);
                            }
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
                            if (WalletInfoManager.hasWalletName()) {
                                go2Activity(MainWalletActivity.class);
                            } else {
                                go2Activity(SetWalletNameActivity.class);
                            }
                            finish();
                        } else {
                            ToastUtil.showToast(R.string.password_error);
                        }
                    }
                })
                .start(password);
    }

    private void showCrashLogDialog() {
        List<File> logFileList = CrashHandler.getInstance().getCrashLogFileList();
        if (logFileList == null || logFileList.size() == 0) {
            ToastUtil.showToast("haven't crash log");
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.recyclerview_list, null);
        RecyclerView rvList = view.findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
        CommonAdapter<File> mAdapter = new CommonAdapter<File>(R.layout.listitem_crash_log) {
            @Override
            protected void convert(ViewHolder viewHolder, File item, int position) {
                TextView tv = viewHolder.findViewById(R.id.tv_file_name);
                tv.setText((position + 1) + "、 " + item.getName());
            }

            @Override
            protected void onItemClick(View view, File item, int position) {
                Bundle args = new Bundle();
                args.putSerializable(Constants.INTENT_KEY1, item);
                go2Activity(CrashLogActivity.class, args);
            }
        };
        rvList.setAdapter(mAdapter);
        mAdapter.addAll(logFileList);
        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.close, null)
                .show();
    }
}
