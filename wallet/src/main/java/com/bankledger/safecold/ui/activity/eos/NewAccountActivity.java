package com.bankledger.safecold.ui.activity.eos;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.protobuf.bean.TransEos;
import com.bankledger.protobuf.bean.TransRetEos;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.MainWalletActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.ui.widget.EosDigitsInputFilter;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.utils.Utils;
import com.bankledger.utils.AesUtil;
import com.google.zxing.WriterException;

import io.eblock.eos4j.ecc.EccTool;


/**
 * Created by zm on 2018/11/15.
 */

public class NewAccountActivity extends ToolbarBaseActivity implements View.OnClickListener {

    private TextView tvCommonTips;
    private TextView tvAcountTitle;
    private AppCompatEditText editAccount;
    private TextView tvAcountTips;

    private TextView tvOwnerTitle;
    private AppCompatEditText editOwner;
    private ImageView ivScanOwner;

    private TextView tvActiveTitle;
    private AppCompatEditText editActive;
    private ImageView ivScanActive;

    private TextView tvScanTips;
    private Button btCreate;

    private boolean isNewAccount = true;


    @Override
    protected int setContentLayout() {
        return R.layout.activity_eos_new_account;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();

        tvCommonTips = findViewById(R.id.tv_common_tips);

        tvAcountTitle = findViewById(R.id.tv_account_title);
        editAccount = findViewById(R.id.edit_account);
        tvAcountTips = findViewById(R.id.tv_account_tips);

        tvOwnerTitle = findViewById(R.id.tv_owner_title);
        editOwner = findViewById(R.id.edit_owner);
        ivScanOwner = findViewById(R.id.iv_scan_owner);

        tvActiveTitle = findViewById(R.id.tv_active_title);
        editActive = findViewById(R.id.edit_active);
        ivScanActive = findViewById(R.id.iv_scan_active);

        tvScanTips = findViewById(R.id.tv_scan_tips);
        btCreate = findViewById(R.id.bt_create);
    }

    @Override
    public void initData() {
        super.initData();

        findViewById(R.id.iv_scan_owner).setOnClickListener(this);
        findViewById(R.id.iv_scan_active).setOnClickListener(this);
        btCreate.setOnClickListener(this);

        setTitle(SafeColdSettings.EOS);
        isNewAccount = getIntent().getBooleanExtra(Constants.INTENT_KEY1, isNewAccount);

        if (isNewAccount) {
            EosAccount eosAccount = EosAccountProvider.getInstance().queryCreateEosAccount();
            tvCommonTips.setText(R.string.eos_create_account_tips);

            tvAcountTitle.setText(R.string.eos_reg_account_name);
            editAccount.setHint(R.string.eos_account_name_tips);
            tvAcountTips.setText(R.string.eos_account_name_rule);

            tvOwnerTitle.setText(R.string.eos_owner_pubkey);
            editOwner.setText(eosAccount.getOwnerPubKey());
            editOwner.setEnabled(false);
            ivScanOwner.setVisibility(View.GONE);

            tvActiveTitle.setText(R.string.eos_active_pubkey);
            editActive.setText(eosAccount.getActivePubKey());
            editActive.setEnabled(false);
            ivScanActive.setVisibility(View.GONE);

            tvScanTips.setText(R.string.eos_create_scan_tips);
            btCreate.setText(R.string.eos_create_account);
        } else {
            tvCommonTips.setText(R.string.eos_import_account_tips);

            tvAcountTitle.setText(R.string.eos_import_account_name);
            editAccount.setHint(R.string.eos_account_name_tips);
            tvAcountTips.setText(R.string.eos_import_account_name_rule);

            tvOwnerTitle.setText(R.string.eos_owner_prikey);
            editOwner.setHint(R.string.eos_owner_prikey_tips);

            tvActiveTitle.setText(R.string.eos_active_prikey);
            editActive.setHint(R.string.eos_active_prikey_tips);

            tvScanTips.setText(R.string.eos_import_scan_tips);
            btCreate.setText(R.string.eos_import_account);
        }

        editAccount.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12), new EosDigitsInputFilter()});
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_scan_owner:
                verifyPermissions(Manifest.permission.CAMERA, new PermissionCallBack() {
                    @Override
                    public void onGranted() {
                        go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE1);
                    }

                    @Override
                    public void onDenied() {

                    }
                });
                break;
            case R.id.iv_scan_active:
                verifyPermissions(Manifest.permission.CAMERA, new PermissionCallBack() {
                    @Override
                    public void onGranted() {
                        go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE2);
                    }

                    @Override
                    public void onDenied() {

                    }
                });
                break;
            case R.id.bt_create:
                if (TextUtils.isEmpty(editAccount.getText())) {
                    ToastUtil.showToast(R.string.eos_account_name_tips);
                    return;
                }
                if (editAccount.getText().length() != 12) {
                    ToastUtil.showToast(R.string.eos_account_name_rule_tips);
                    return;
                }
                if (TextUtils.isEmpty(editOwner.getText())) {
                    return;
                }
                if (TextUtils.isEmpty(editActive.getText())) {
                    return;
                }
                TransEos te = new TransEos();
                te.walletSeqNumber = WalletInfoManager.getWalletNumber();
                te.account = editAccount.getText().toString();
                if (isNewAccount) {
                    te.opType = 1;
                    te.owner = editOwner.getText().toString();
                    te.active = editActive.getText().toString();
                } else {
                    te.opType = 2;
                    try {
                        te.owner = EccTool.privateToPublic(editOwner.getText().toString());
                        te.active = EccTool.privateToPublic(editActive.getText().toString());
                    } catch (Exception e) {
                        return;
                    }
                }
                te.deviceName = WalletInfoManager.getBluetoothAddress();

                if (te.opType == 1) {
                    createAccount(te);
                } else {
                    importAccount(te);
                }
                break;
        }
    }

    private void createAccount(final TransEos te) {
        new CommonAsyncTask.Builder<Void, Void, String>()
                .setIDoInBackground(new IDoInBackground<Void, Void, String>() {
                    @Override
                    public String doInBackground(IPublishProgress<Void> publishProgress, Void... aVoid) {
                        //替换原来的创建数据
                        EosAccount account = new EosAccount();
                        account.setOwnerPubKey(te.owner);
                        account.setActivePubKey(te.active);
                        account.setAccountName(te.account);
                        account.setOwnerPrivKey("");
                        account.setActivePrivKey("");
                        account.setOpType(EosAccount.OpType.TYPE_CREATE);
                        account.setState(EosAccount.AvailableState.STATE_DISABLED);
                        EosAccountProvider.getInstance().addOrUpdateEosAccount(account);
                        return ProtoUtils.encodeEos(te);
                    }
                })
                .setIPostExecute(new IPostExecute<String>() {
                    @Override
                    public void onPostExecute(String text) {
                        try {
                            Bitmap bitmap = QRCodeEncoderUtils.encodeAsBitmap(NewAccountActivity.this, text);
                            DialogUtil.showImageDialog(NewAccountActivity.this, bitmap, getString(R.string.eos_qrcode_tips), new DialogUtil.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, String content) {
                                    verifyPermissions(Manifest.permission.CAMERA, new PermissionCallBack() {
                                        @Override
                                        public void onGranted() {
                                            go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE3);
                                        }

                                        @Override
                                        public void onDenied() {

                                        }
                                    });
                                }
                            }, new DialogUtil.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, String content) {
                                    finish();
                                }
                            });
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .start();
    }

    private void importAccount(final TransEos te) {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, final String content) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        importAccount(te);
                    } else {
                        new CommonAsyncTask.Builder<Void, Void, String>()
                                .setIDoInBackground(new IDoInBackground<Void, Void, String>() {
                                    @Override
                                    public String doInBackground(IPublishProgress<Void> publishProgress, Void... aVoid) {
                                        EosAccount account = new EosAccount();
                                        account.setOwnerPubKey(te.owner);
                                        account.setActivePubKey(te.active);
                                        account.setAccountName(te.account);
                                        EosAccount eosAccount = EosAccountProvider.getInstance().queryCreateEosAccount();
                                        //两个公钥相同情况，替换原来的创建数据
                                        if (account.getOwnerPubKey().equals(eosAccount.getOwnerPubKey()) && account.getActivePubKey().equals(eosAccount.getActivePubKey())) {
                                            account.setOwnerPrivKey("");
                                            account.setActivePrivKey("");
                                            account.setOpType(EosAccount.OpType.TYPE_CREATE);
                                            account.setState(EosAccount.AvailableState.STATE_DISABLED);
                                            EosAccountProvider.getInstance().addOrUpdateEosAccount(account);
                                        } else { //导入账户
                                            try {
                                                String ownerPrivKey = editOwner.getText().toString();
                                                String activePrivKey = editActive.getText().toString();
                                                byte[] ownerBytes = AesUtil.encrypt(Utils.addZeroForNum(content, 16), Utils.addZeroForNum(content, 16), ownerPrivKey.getBytes());
                                                byte[] activeBytes = AesUtil.encrypt(Utils.addZeroForNum(content, 16), Utils.addZeroForNum(content, 16), activePrivKey.getBytes());
                                                account.setOwnerPrivKey(Utils.bytesToHexString(ownerBytes));
                                                account.setActivePrivKey(Utils.bytesToHexString(activeBytes));
                                                account.setOpType(EosAccount.OpType.TYPE_IMPORT);
                                                account.setState(EosAccount.AvailableState.STATE_DISABLED);
                                                EosAccountProvider.getInstance().addOrUpdateEosAccount(account);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        return ProtoUtils.encodeEos(te);
                                    }
                                })
                                .setIPostExecute(new IPostExecute<String>() {
                                    @Override
                                    public void onPostExecute(String text) {
                                        try {
                                            Bitmap bitmap = QRCodeEncoderUtils.encodeAsBitmap(NewAccountActivity.this, text);
                                            DialogUtil.showImageDialog(NewAccountActivity.this, bitmap, getString(R.string.eos_qrcode_tips), new DialogUtil.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which, String content) {
                                                    verifyPermissions(Manifest.permission.CAMERA, new PermissionCallBack() {
                                                        @Override
                                                        public void onGranted() {
                                                            go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE3);
                                                        }

                                                        @Override
                                                        public void onDenied() {

                                                        }
                                                    });
                                                }
                                            }, new DialogUtil.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which, String content) {
                                                    finish();
                                                }
                                            });
                                        } catch (WriterException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                                .start();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE1 && resultCode == Constants.RESULT_SUCCESS) {
            editOwner.setText(data.getStringExtra(Constants.INTENT_KEY1));
        } else if (requestCode == Constants.REQUEST_CODE2 && resultCode == Constants.RESULT_SUCCESS) {
            editActive.setText(data.getStringExtra(Constants.INTENT_KEY1));
        } else if (requestCode == Constants.REQUEST_CODE3 && resultCode == Constants.RESULT_SUCCESS) {
            new QrProtocolUtil()
                    .setRetEos(new QrProtocolUtil.IRetEos() {
                        @Override
                        public void onRetEos(TransRetEos retEos) {
                            updateEosAccount(retEos);
                        }
                    })
                    .setIDecodeFail(new QrProtocolUtil.IDecodeFail() {
                        @Override
                        public void onQrDecodeFail(String errMsg) {
                            ToastUtil.showToast(errMsg);
                        }

                        @Override
                        public void onProtocolUpgrade(boolean isSelf) {
                            DialogUtil.showProtocolUpdateDilog(NewAccountActivity.this, isSelf);
                        }
                    })
                    .decode(data.getStringExtra(Constants.INTENT_KEY1));
        }
    }

    private void updateEosAccount(final TransRetEos retEos) {
        new CommonAsyncTask.Builder<Void, Void, EosAccount>()
                .setIDoInBackground(new IDoInBackground<Void, Void, EosAccount>() {
                    @Override
                    public EosAccount doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        if (EosAccountProvider.getInstance().isExistPubkey(retEos.owner, retEos.active)) {
                            EosAccount eosAccount = new EosAccount();
                            eosAccount.setState(EosAccount.AvailableState.STATE_AVAILABLE);
                            eosAccount.setAccountName(retEos.account);
                            eosAccount.setOwnerPubKey(retEos.owner);
                            eosAccount.setActivePubKey(retEos.active);
                            return eosAccount;
                        } else {
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<EosAccount>() {
                    @Override
                    public void onPostExecute(EosAccount eosAccount) {
                        if(eosAccount != null){
                            EosAccountProvider.getInstance().updateEosAccountAvailable(eosAccount);
                            ToastUtil.showToast(R.string.eos_account_updated);
                            Intent intent = new Intent(NewAccountActivity.this, MainWalletActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }
                    }
                })
                .start();
    }
}
