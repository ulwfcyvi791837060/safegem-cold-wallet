package com.bankledger.safecold.ui.activity.eos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.protobuf.bean.TransSignParam;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.db.ContactsAddressProvider;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.CurrencySendActivity;
import com.bankledger.safecold.ui.activity.SelectContactsActivity;
import com.bankledger.safecold.ui.activity.SelectCurrencyActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.ui.activity.eth.ETHTokenSendActivity;
import com.bankledger.safecold.ui.widget.CommonEditWidget;
import com.bankledger.safecold.ui.widget.DecimalDigitsInputFilter;
import com.bankledger.safecold.ui.widget.EosDigitsInputFilter;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.utils.GsonUtils;
import com.bankledger.safecoldj.utils.Utils;

import java.util.Date;

import io.eblock.eos4j.api.vo.SignParam;


/**
 * Created by zm on 2018/11/13.
 */
public class EosSendActivity extends ToolbarBaseActivity implements View.OnClickListener {

    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;

    private TextView tvPayTo;

    private CommonEditWidget cewAccount;
    private CommonEditWidget cewMoney;
    private CommonEditWidget cewMemo;

    private EosBalance eosBalance;
    private SignParam signParam;


    @Override
    protected int setContentLayout() {
        return R.layout.activity_eos_send;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.wallet_transfer);

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);

        tvPayTo = findViewById(R.id.tv_pay_to);
        tvPayTo.setText(String.format(getString(R.string.pay_to), "-"));

        cewAccount = findViewById(R.id.cew_account);
        cewAccount.setHint(R.string.eos_account);
        cewAccount.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(12), new EosDigitsInputFilter()});
        cewAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkAliasWithAddress();
            }

            @Override
            public void afterTextChanged(Editable s) {


            }
        });

        cewMoney = findViewById(R.id.cew_money);
        cewMoney.setHint(R.string.money);
        cewMoney.getEditText().setTextSize(14);
        cewMoney.getEditText().setFilters(new InputFilter[]{new DecimalDigitsInputFilter(10, 4)});
        cewMoney.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().contains(".") && s.length() > 10) {
                    cewMoney.setText(s.subSequence(0, 10));
                    cewMoney.getEditText().setSelection(10);
                }

                int indexD = s.toString().indexOf(".");
                if (indexD >= 0 && s.length() - indexD > 9) {
                    cewMoney.setText(s.subSequence(0, indexD + 9));
                    cewMoney.getEditText().setSelection(cewMoney.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        cewMemo = findViewById(R.id.cew_memo);
        cewMemo.setHint(R.string.remark);
        cewMemo.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

        findViewById(R.id.ll_currency).setOnClickListener(this);
        findViewById(R.id.bt_sign_param).setOnClickListener(this);
        findViewById(R.id.iv_scan).setOnClickListener(this);
        findViewById(R.id.iv_select_payee).setOnClickListener(this);
        findViewById(R.id.bt_next).setOnClickListener(this);
    }

    @Override
    public void initData() {
        super.initData();
        eosBalance = (EosBalance) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        if (eosBalance != null) {
            tvCurrencyName.setText(eosBalance.tokenName);
            ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(eosBalance.tokenName, ConvertViewBean.TYPE_EOS_COIN));

            if(getIntent().hasExtra(Constants.INTENT_KEY2)){
                String account = getIntent().getStringExtra(Constants.INTENT_KEY2);
                cewAccount.setText(account);
            }

            if(getIntent().hasExtra(Constants.INTENT_KEY3)){
                String amount = getIntent().getStringExtra(Constants.INTENT_KEY3);
                cewMoney.setText(amount);
            }

            if(getIntent().hasExtra(Constants.INTENT_KEY4)){
                TransSignParam transSignParam = (TransSignParam)getIntent().getSerializableExtra(Constants.INTENT_KEY4);
                signParam = new SignParam();
                signParam.setHeadBlockTime(new Date(transSignParam.headBlockTime));
                signParam.setChainId(transSignParam.chainId);
                signParam.setLastIrreversibleBlockNum(transSignParam.lastIrreversibleBlockNum);
                signParam.setRefBlockPrefix(transSignParam.refBlockPrefix);
                signParam.setExp(transSignParam.exp);
                ToastUtil.showToast(R.string.eos_sync_succ);
            }
        } else {
            tvCurrencyName.setText(R.string.select_currency);
        }
    }

    private boolean checkParams() {
        if (signParam == null) {
            ToastUtil.showToast(getString(R.string.eos_sync_block_tips));
            return false;
        }
        if (eosBalance == null) {
            ToastUtil.showToast(R.string.hint_select_currency);
            return false;
        }
        if (TextUtils.isEmpty(cewAccount.getText())) {
            ToastUtil.showToast(R.string.eos_select_account);
            return false;
        }
        if (TextUtils.isEmpty(cewMoney.getText()) || ".".equalsIgnoreCase(cewMoney.getText())) {
            ToastUtil.showToast(R.string.input_money);
            return false;
        }
        if (!BigDecimalUtils.greaterThan(cewMoney.getText(), "0.001")) {
            ToastUtil.showToast(getString(R.string.input_money_limit,"0.001"));
            return false;
        }
        if (BigDecimalUtils.greaterThan(cewMoney.getText(), eosBalance.balance)) {
            ToastUtil.showToast(R.string.not_sufficient_funds_title);
            return false;
        }
        EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
        if (eosAccount.getAccountName().equals(cewAccount.getText())) {
            ToastUtil.showToast(R.string.eos_cant_send_self);
            return false;
        }
        return true;
    }

    private void checkAliasWithAddress() {
        if (cewAccount == null) {
            return;
        }
        String address = cewAccount.getText();
        if (TextUtils.isEmpty(address)) {
            return;
        }
        new CommonAsyncTask.Builder<String, Void, String>()
                .setIDoInBackground(new IDoInBackground<String, Void, String>() {
                    @Override
                    public String doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        return ContactsAddressProvider.getInstance().getAliasWithAddress(strings[0], strings[1]);
                    }
                })
                .setIPostExecute(new IPostExecute<String>() {
                    @Override
                    public void onPostExecute(String s) {
                        if (!TextUtils.isEmpty(s)) {
                            tvPayTo.setText(String.format(getString(R.string.pay_to), s));
                        } else {
                            tvPayTo.setText(String.format(getString(R.string.pay_to), "-"));
                        }
                    }
                })
                .start(SafeColdSettings.EOS, address);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_SUCCESS) {
            if (requestCode == Constants.REQUEST_CODE1) {//选择币种
                ConvertViewBean convertView = (ConvertViewBean) data.getSerializableExtra(Constants.INTENT_KEY1);
                if (convertView.isCurrency()) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, convertView.currency);
                    go2Activity(CurrencySendActivity.class, args);
                    finish();
                } else if (convertView.isETHToken()) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, convertView.ethToken);
                    go2Activity(ETHTokenSendActivity.class, args);
                    finish();
                } else if (convertView.isEosBalance()) {
                    if ((eosBalance == null) || (!convertView.eosBalance.tokenName.equals(eosBalance.tokenName))) {
                        eosBalance = convertView.eosBalance;
                        EosUsdtBalance eosUsdtBalance = EosUsdtBalanceProvider.getInstance().getEosBalance(eosBalance.tokenName);
                        eosBalance.balance = eosUsdtBalance.balance;
                        cewAccount.setText("");
                        cewMoney.setText("");
                        cewMemo.setText("");
                    }
                } else if (convertView.isSafeAsset()) {
                    Bundle args = new Bundle();
                    Currency mCurrency = HDAddressManager.getInstance().getCurrencyCoin(SafeColdSettings.SAFE);
                    args.putSerializable(Constants.INTENT_KEY1, mCurrency);
                    args.putSerializable(Constants.INTENT_KEY4, convertView.safeAsset);
                    go2Activity(CurrencySendActivity.class, args);
                    finish();
                }
            } else if (requestCode == Constants.REQUEST_CODE2) { //签名参数
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
                                ToastUtil.showToast(errMsg);
                            }

                            @Override
                            public void onProtocolUpgrade(boolean isSelf) {

                            }
                        })
                        .decode(result);
            } else if (requestCode == Constants.REQUEST_CODE3) {//扫描账户
                final String result = data.getStringExtra(Constants.INTENT_KEY1);
                new QrProtocolUtil()
                        .setICoinAddressDecode(new QrProtocolUtil.ICoinAddressDecode() {
                            @Override
                            public void onCoinAddressDecode(UriDecode uriDecode) {
                                String fullName = uriDecode.scheme;
                                String tokenName = uriDecode.params.get("token");
                                if (Utils.fullNameIsEos(fullName) || eosBalance.tokenName.equalsIgnoreCase(tokenName)) {
                                    cewAccount.setText(uriDecode.path);
                                    if (uriDecode.params != null && !TextUtils.isEmpty(uriDecode.params.get("amount"))) {
                                        cewMoney.setText(uriDecode.params.get("amount"));
                                    }
                                } else {
                                    cewAccount.setText(fullName + ":" + uriDecode.path);
                                }
                            }
                        })
                        .setIDecodeFail(new QrProtocolUtil.IDecodeFail() {
                            @Override
                            public void onQrDecodeFail(String errMsg) {
                                if(result.length() > 12 || !isEosLetterDigit(result)){
                                    ToastUtil.showToast(R.string.eos_account_name_invalid);
                                } else {
                                    cewAccount.setText(result);
                                }
                            }
                            @Override
                            public void onProtocolUpgrade(boolean isSelf) {

                            }
                        })
                        .decode(result);
            } else if (requestCode == Constants.REQUEST_CODE4) {//选择联系人
                ContactsAddress contactsAddress = (ContactsAddress) data.getSerializableExtra(Constants.INTENT_KEY1);
                cewAccount.setText(contactsAddress.getAddress());
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_currency:
                go2ActivityForResult(SelectCurrencyActivity.class, Constants.REQUEST_CODE1);
                break;
            case R.id.bt_sign_param:
                go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE2);
                break;
            case R.id.iv_scan:
                if (eosBalance == null) {
                    ToastUtil.showToast(R.string.hint_select_currency);
                } else {
                    go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE3);
                }
                break;
            case R.id.iv_select_payee:
                if (eosBalance == null) {
                    ToastUtil.showToast(R.string.hint_select_currency);
                } else {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, ConvertViewBean.eosTokenConvert(eosBalance));
                    go2ActivityForResult(SelectContactsActivity.class, Constants.REQUEST_CODE4, args);
                }
                break;
            case R.id.bt_next:
                if (CommonUtils.isRepeatClick()) {
                    if (checkParams()) {
                        Bundle args = new Bundle();
                        args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                        args.putString(Constants.INTENT_KEY2, GsonUtils.toString(signParam));
                        args.putString(Constants.INTENT_KEY3, cewAccount.getText().toString());
                        args.putString(Constants.INTENT_KEY4, cewMoney.getText());
                        args.putString(Constants.INTENT_KEY5, cewMemo.getText());
                        go2Activity(EosSendAffirmActivity.class, args);
                    }
                }
                break;
        }
    }

    public static boolean isEosLetterDigit(String str) {
        String regex = "^[a-z1-5]+$";
        return str.matches(regex);
    }


}
