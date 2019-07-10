package com.bankledger.safecold.ui.activity.eos;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.protobuf.bean.SignTx;
import com.bankledger.protobuf.bean.TransSignParam;
import com.bankledger.protobuf.bean.TransSignTx;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.AsyncTaskResult;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.QrCodePageActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicException;
import com.bankledger.safecoldj.exception.PasswordException;
import com.bankledger.safecoldj.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;

import io.eblock.eos4j.api.vo.SignParam;

public class BuyRamActivity extends ToolbarBaseActivity implements CompoundButton.OnCheckedChangeListener {

    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;

    private LinearLayout editLayout;
    private RadioButton rBtn1, rBtn2, rBtn3, rBtn4;
    private EditText editRam;

    private EosBalance eosBalance;
    private SignParam signParam;


    @Override
    protected int setContentLayout() {
        return R.layout.activity_buy_ram;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.eos_buy_ram);

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);

        rBtn1 = findViewById(R.id.rbtn_1);
        rBtn2 = findViewById(R.id.rbtn_2);
        rBtn3 = findViewById(R.id.rbtn_3);
        rBtn4 = findViewById(R.id.rbtn_4);

        rBtn1.setOnCheckedChangeListener(this);
        rBtn2.setOnCheckedChangeListener(this);
        rBtn3.setOnCheckedChangeListener(this);
        rBtn4.setOnCheckedChangeListener(this);

        editLayout = findViewById(R.id.edit_layout);
        editRam = findViewById(R.id.edit_ram);
    }

    @Override
    public void initData() {
        super.initData();
        eosBalance = (EosBalance) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        tvCurrencyName.setText(eosBalance.tokenName);
        ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(eosBalance.tokenName, ConvertViewBean.TYPE_EOS_COIN));

        editRam.setHint(R.string.eos_buy_ram_tips);

        findViewById(R.id.bt_sign_param).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE1);
            }
        });
        findViewById(R.id.btn_buy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CommonUtils.isRepeatClick()) {
                    if (checkParams()) {
                        send();
                    }
                }
            }
        });

    }

    private boolean checkParams() {
        if (signParam == null) {
            ToastUtil.showToast(getString(R.string.eos_sync_block_tips));
            return false;
        }
        if (rBtn4.isChecked() && TextUtils.isEmpty(editRam.getText())) {
            ToastUtil.showToast(R.string.eos_buy_ram_tips);
            return false;
        }
        if (rBtn4.isChecked() && !BigDecimalUtils.greaterThanEquals(editRam.getText().toString(), "3")) {
            ToastUtil.showToast(getString(R.string.eos_buy_ram_less_than,"3"));
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_SUCCESS) {
            if (requestCode == Constants.REQUEST_CODE1) { //签名参数
                final String result = data.getStringExtra(Constants.INTENT_KEY1);
                new QrProtocolUtil()
                        .setISignParamDecode(new QrProtocolUtil.ISignParam() {
                            @Override
                            public void onSignParamDecode(TransSignParam transSignParam) {
                                signParam = new SignParam();
                                signParam.setHeadBlockTime(new Date(transSignParam.headBlockTime));
                                signParam.setChainId(transSignParam.chainId);
                                signParam.setLastIrreversibleBlockNum(transSignParam.lastIrreversibleBlockNum);
                                signParam.setRefBlockPrefix(transSignParam.refBlockPrefix);
                                signParam.setExp(transSignParam.exp);
                                ToastUtil.showToast(R.string.eos_sync_succ);
                            }
                        })
                        .setIDecodeFail(new QrProtocolUtil.IDecodeFail() {
                            @Override
                            public void onQrDecodeFail(String errMsg) {
                                ToastUtil.showToast(R.string.eos_sync_code_err);
                            }

                            @Override
                            public void onProtocolUpgrade(boolean isSelf) {

                            }
                        })
                        .decode(result);
            }
        }
    }

    private void send() {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, final String content) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        send();
                    } else {
                        String buyRam;
                        if (rBtn1.isChecked()) {
                            buyRam = "5";
                        } else if (rBtn2.isChecked()) {
                            buyRam = "10";
                        } else if (rBtn3.isChecked()) {
                            buyRam = "20";
                        } else {
                            buyRam = editRam.getText().toString();
                        }
                        buyRam = BigDecimalUtils.mul(buyRam, "1024"); //转KB
                        new CommonAsyncTask.Builder<String, Void, AsyncTaskResult<String>>()
                                .setIPreExecute(new IPreExecute() {
                                    @Override
                                    public void onPreExecute() {
                                        CommonUtils.showProgressDialog(BuyRamActivity.this);
                                    }
                                })
                                .setIDoInBackground(new IDoInBackground<String, Void, AsyncTaskResult<String>>() {
                                    @Override
                                    public AsyncTaskResult<String> doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                        HDAccount hdAccount = HDAddressManager.getInstance().getHDAccount();
                                        try {
                                            EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                                            String buyRam = strings[0];
                                            String hexCode = hdAccount.buyRam(content, signParam, eosAccount.getAccountName(), eosAccount.getAccountName(), Long.parseLong(buyRam));
                                            return new AsyncTaskResult<>(ProtoUtils.encodeSignTx(
                                                    new TransSignTx(WalletInfoManager.getWalletNumber(),
                                                            new SignTx(SafeColdSettings.EOS, hexCode))));
                                        } catch (Exception e) {
                                            return new AsyncTaskResult<>(e);
                                        }
                                    }
                                })
                                .setIPostExecute(new IPostExecute<AsyncTaskResult<String>>() {
                                    @Override
                                    public void onPostExecute(AsyncTaskResult<String> result) {
                                        CommonUtils.dismissProgressDialog();
                                        if (result.isSuccess()) {
                                            //本地更新Eos状态
                                            EosUsdtBalanceProvider.getInstance().updateBalance(Utils.eosBalance2EosUsdtBalance(eosBalance));
                                            EventMessage message = new EventMessage(EventMessage.TYPE_BALANCE_CHANGED);
                                            EventBus.getDefault().post(message);

                                            Bundle args = new Bundle();
                                            args.putInt(Constants.INTENT_KEY1, QrCodePageActivity.START_TYPE_SEND_CURRENCY);
                                            args.putString(Constants.INTENT_KEY2, result.getResult());
                                            go2Activity(QrCodePageActivity.class, args);
                                            finish();
                                        } else {
                                            if (result.getException() instanceof PasswordException || result.getException() instanceof MnemonicException.MnemonicLengthException || result.getException() instanceof MnemonicException.MnemonicWordException) {
                                                ToastUtil.showToast(R.string.password_error);
                                            } else {
                                                ToastUtil.showToast(result.getException().getMessage());
                                            }
                                        }
                                    }
                                })
                                .start(buyRam);
                    }
                }
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (checked) {
            switch (compoundButton.getId()) {
                case R.id.rbtn_1:
                    rBtn2.setChecked(false);
                    rBtn3.setChecked(false);
                    rBtn4.setChecked(false);
                    editLayout.setVisibility(View.GONE);
                    break;
                case R.id.rbtn_2:
                    rBtn1.setChecked(false);
                    rBtn3.setChecked(false);
                    rBtn4.setChecked(false);
                    editLayout.setVisibility(View.GONE);
                    break;
                case R.id.rbtn_3:
                    rBtn1.setChecked(false);
                    rBtn2.setChecked(false);
                    rBtn4.setChecked(false);
                    editLayout.setVisibility(View.GONE);
                    break;
                case R.id.rbtn_4:
                    rBtn1.setChecked(false);
                    rBtn2.setChecked(false);
                    rBtn3.setChecked(false);
                    editLayout.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }
}
