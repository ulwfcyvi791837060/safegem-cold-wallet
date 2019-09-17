package com.bankledger.safecold.ui.activity;

import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.ui.widget.EditLengthInputFilter;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;

/**
 * @author bankledger
 * @time 2018/8/28 10:50
 */
public class SetWalletNameActivity extends ToolbarBaseActivity {

    private EditText etWalletName;
    private boolean clicked = false;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_set_wallet_name;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.set_wallet_name);

        etWalletName = findViewById(R.id.et_wallet_name);
        etWalletName.setFilters(new InputFilter[]{new EditLengthInputFilter(Constants.ADDRESS_ALIS_LENGTH_MAX)});
        findViewById(R.id.bt_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clicked) return;
                if (TextUtils.isEmpty(etWalletName.getText().toString().trim())) {
                    ToastUtil.showToast(getString(R.string.input_wallet_name));
                } else {
                    saveWalletName();
                }
            }
        });
    }

    private void saveWalletName() {
        clicked = true;
        new CommonAsyncTask.Builder<String, Void, Void>()
                .setIDoInBackground(new IDoInBackground<String, Void, Void>() {
                    @Override
                    public Void doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        WalletInfoManager.saveWalletName(strings[0]);
                        return null;
                    }
                })
                .setIPostExecute(new IPostExecute<Void>() {
                    @Override
                    public void onPostExecute(Void aVoid) {
                        go2Activity(MainWalletActivity.class);
                        finish();
                        clicked = false;
                    }
                })
                .start(etWalletName.getText().toString());
    }

    @Override
    public void initData() {
        super.initData();
    }
}
