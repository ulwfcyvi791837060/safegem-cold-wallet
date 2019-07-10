package com.bankledger.safecold.ui.activity.eos;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.protobuf.bean.SignTx;
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
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.ui.activity.QrCodePageActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicException;
import com.bankledger.safecoldj.exception.PasswordException;
import com.bankledger.safecoldj.utils.GsonUtils;
import com.bankledger.safecoldj.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import io.eblock.eos4j.api.vo.SignParam;

/**
 * Created by zm on 2018/11/13.
 */
public class EosSendAffirmActivity extends ToolbarBaseActivity {

    private TextView tvPayTo;
    private TextView tvPayAmount;
    private TextView tvMemo;

    private EosBalance eosBalance;
    private SignParam signParam;
    private String account;
    private String amount;
    private String memo;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_eos_send_affirm;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.affirm_pay);
        setDefaultNavigation();

        tvPayTo = findViewById(R.id.tv_pay_to);
        tvPayAmount = findViewById(R.id.tv_pay_amount);
        tvMemo = findViewById(R.id.tv_memo);
        findViewById(R.id.bt_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
        eosBalance = (EosBalance) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        String param =  getIntent().getStringExtra(Constants.INTENT_KEY2);
        signParam = GsonUtils.getObjFromJSON(param, SignParam.class);
        account = getIntent().getStringExtra(Constants.INTENT_KEY3);
        amount = getIntent().getStringExtra(Constants.INTENT_KEY4);
        memo = getIntent().getStringExtra(Constants.INTENT_KEY5);

        tvPayTo.setText(getString(R.string.pay_to, account));

        amount = formatAmount(amount);
        tvPayAmount.setText(amount + " " + eosBalance.tokenName);
        tvMemo.setText(memo);


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
                        new CommonAsyncTask.Builder<String, Void, AsyncTaskResult<String>>()
                                .setIPreExecute(new IPreExecute() {
                                    @Override
                                    public void onPreExecute() {
                                        CommonUtils.showProgressDialog(EosSendAffirmActivity.this);
                                    }
                                })
                                .setIDoInBackground(new IDoInBackground<String, Void, AsyncTaskResult<String>>() {
                                    @Override
                                    public AsyncTaskResult<String> doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                        HDAccount hdAccount = HDAddressManager.getInstance().getHDAccount();
                                        try {
                                            EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                                            String hexCode = hdAccount.signRawTx(content, signParam, eosAccount.getAccountName(), strings[0], strings[1], strings[2]);
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
                                            String newBalance = BigDecimalUtils.sub(eosBalance.balance, amount);
                                            eosBalance.balance = newBalance;
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
                                .start(account, tvPayAmount.getText().toString(), memo);
                    }
                }
            }
        });
    }

    // 格式化金额
    public String formatAmount(String amount){
        return new BigDecimal(amount).setScale(4, BigDecimal.ROUND_DOWN).toString();
    }

}
