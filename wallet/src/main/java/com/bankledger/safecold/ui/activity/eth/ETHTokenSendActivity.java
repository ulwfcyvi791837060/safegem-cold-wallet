package com.bankledger.safecold.ui.activity.eth;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.db.ContactsAddressProvider;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.CurrencySendActivity;
import com.bankledger.safecold.ui.activity.SelectContactsActivity;
import com.bankledger.safecold.ui.activity.SelectCurrencyActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.ui.activity.eos.ChoiceAccountActivity;
import com.bankledger.safecold.ui.activity.eos.EosSendActivity;
import com.bankledger.safecold.ui.widget.CommonEditWidget;
import com.bankledger.safecold.ui.widget.DecimalDigitsInputFilter;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.utils.Utils;

import org.w3c.dom.Text;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;

/**
 * @author bankledger
 * @time 2018/7/30 16:51
 */
public class ETHTokenSendActivity extends ToolbarBaseActivity {
    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;

    private TextView tvPayTo;
    private CommonEditWidget cewPayeeAddress;
    private CommonEditWidget cewMoney;

    private ETHToken mETHToken;
    private String normalAddress;
    private String normalAmount;

    private long currentPrice;
    private long currentLimit;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_ethtoken_send;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.wallet_transfer);


        findViewById(R.id.ll_currency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2ActivityForResult(SelectCurrencyActivity.class, Constants.REQUEST_CODE1);
            }
        });

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);

        tvPayTo = findViewById(R.id.tv_pay_to);
        tvPayTo.setText(String.format(getString(R.string.pay_to), "-"));
        findViewById(R.id.iv_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mETHToken == null) {
                    ToastUtil.showToast(R.string.hint_select_currency);
                } else {
                    go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE2);
                }
            }
        });
        findViewById(R.id.iv_select_payee).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mETHToken == null) {
                    ToastUtil.showToast(R.string.hint_select_currency);
                } else {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, ConvertViewBean.ethTokenConvert(mETHToken));
                    go2ActivityForResult(SelectContactsActivity.class, Constants.REQUEST_CODE3, args);
                }
            }
        });

        cewPayeeAddress = findViewById(R.id.cew_payee_address);
        cewPayeeAddress.setHint(R.string.payee_address);
        cewPayeeAddress.getEditText().addTextChangedListener(payeeTextWatcher);
        cewPayeeAddress.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        cewPayeeAddress.getEditText().setTextSize(14);
        cewMoney = findViewById(R.id.cew_money);
        cewMoney.setHint(R.string.money);
        cewMoney.getEditText().setTextSize(14);
        cewMoney.getEditText().setFilters(new InputFilter[]{new DecimalDigitsInputFilter(10, 8)});
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
        findViewById(R.id.bt_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isRepeatClick()) {
                    if (mETHToken == null) {
                        ToastUtil.showToast(R.string.hint_select_currency);
                    } else {
                        refreshETHToken();
                    }
                }
            }
        });
    }

    private TextWatcher payeeTextWatcher = new TextWatcher() {
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
    };

    @Override
    public void initData() {
        super.initData();

        mETHToken = (ETHToken) getIntent().getSerializableExtra(Constants.INTENT_KEY1);

        currentPrice = Constants.ETH_DEFAULT_GAS_PRICE;
        currentLimit = Constants.ETH_DEFAULT_GAS_LIMIT;

        normalAddress = getIntent().getStringExtra(Constants.INTENT_KEY2);
        cewPayeeAddress.setText(normalAddress);

        normalAmount = getIntent().getStringExtra(Constants.INTENT_KEY3);
        cewMoney.setText(normalAmount);

        refreshSelectETHToken();
    }


    private void checkAliasWithAddress() {
        String coin = mETHToken == null ? null : mETHToken.isErc20() ? mETHToken.symbol : mETHToken.name;
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
                .start(coin, cewPayeeAddress.getText());
    }

    private void refreshSelectETHToken() {
        checkAliasWithAddress();
        if (mETHToken != null) {
            tvCurrencyName.setText(CurrencyNameUtil.getEthTokenName(mETHToken));
            ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(mETHToken.name, ConvertViewBean.TYPE_ETH_TOKEN));
        } else {
            tvCurrencyName.setText(R.string.select_currency);
        }
    }

    //生成转账二维码后回到该页面再次转账时需要刷新最新的ETHToken，这里在点击下一步前进行刷新
    private void refreshETHToken() {
        new CommonAsyncTask.Builder<ETHToken, Void, ETHToken>()
                .setIDoInBackground(new IDoInBackground<ETHToken, Void, ETHToken>() {
                    @Override
                    public ETHToken doInBackground(IPublishProgress<Void> publishProgress, ETHToken... ethTokens) {
                        if (ethTokens[0].isEth()) {
                            return ETHTokenProvider.getInstance().queryETH();
                        } else if (ethTokens[0].isEtc()) {
                            return ETHTokenProvider.getInstance().queryETC();
                        } else {
                            return ETHTokenProvider.getInstance().queryETHTokenWithContractAddress(ethTokens[0].contractsAddress);
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<ETHToken>() {
                    @Override
                    public void onPostExecute(ETHToken ethToken) {
                        if (ethToken != null) {
                            mETHToken = ethToken;
                            if (checkParams()) {
                                Bundle args = new Bundle();
                                args.putSerializable(Constants.INTENT_KEY1, mETHToken);
                                args.putString(Constants.INTENT_KEY2, cewPayeeAddress.getText());
                                args.putString(Constants.INTENT_KEY3, cewMoney.getText());
                                args.putLong(Constants.INTENT_KEY4, currentPrice);
                                args.putLong(Constants.INTENT_KEY5, currentLimit);
                                go2Activity(ETHTokenSendAffirmActivity.class, args);
                            }
                        } else {
                            finish();
                        }
                    }
                })
                .start(mETHToken);
    }

    private boolean checkParams() {
        if (mETHToken == null) {
            ToastUtil.showToast(R.string.hint_select_currency);
            return false;
        }
        if (TextUtils.isEmpty(cewPayeeAddress.getText().replace(" ", ""))) {
            ToastUtil.showToast(R.string.select_payee_address);
            return false;
        }
        if (TextUtils.isEmpty(cewMoney.getText()) || ".".equalsIgnoreCase(cewMoney.getText())) {
            ToastUtil.showToast(R.string.input_money);
            return false;
        }
        if (!BigDecimalUtils.greaterThan(cewMoney.getText(), "0")) {
            ToastUtil.showToast(getString(R.string.input_money_limit, "0"));
            return false;
        }

        try {
            Numeric.toBigInt(cewPayeeAddress.getText());
        } catch (Exception e) {
            ToastUtil.showToast(R.string.address_err);
            return false;
        }

        String amount = new BigDecimal(cewMoney.getText()).multiply(BigDecimal.TEN.pow(mETHToken.isErc20() ? mETHToken.decimals : 18)).toPlainString();
        if (BigDecimalUtils.greaterThan(amount, mETHToken.balance)) {
            String hintMsg = String.format(getString(R.string.not_sufficient_funds), mETHToken.name);
            DialogUtil.showTextDialog(this, getString(R.string.not_sufficient_funds_title), hintMsg, false, new DialogUtil.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, String content) {
                    dialog.dismiss();
                }
            });
            return false;
        }

        return true;
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
                    if (mETHToken == null || !convertView.ethToken.contractsAddress.equals(mETHToken.contractsAddress)) {
                        mETHToken = convertView.ethToken;
                        cewPayeeAddress.setText("");
                        cewMoney.setText("");
                        refreshSelectETHToken();
                    }
                } else if (convertView.isEosBalance()) {
                    EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                    if (eosAccount != null) {
                        Bundle args = new Bundle();
                        args.putSerializable(Constants.INTENT_KEY1, convertView.eosBalance);
                        go2Activity(EosSendActivity.class, args);
                        finish();
                    } else {
                        go2Activity(ChoiceAccountActivity.class);
                    }
                } else if (convertView.isSafeAsset()) {
                    Bundle args = new Bundle();
                    Currency mCurrency = HDAddressManager.getInstance().getCurrencyCoin(SafeColdSettings.SAFE);
                    args.putSerializable(Constants.INTENT_KEY1, mCurrency);
                    args.putSerializable(Constants.INTENT_KEY4, convertView.safeAsset);
                    go2Activity(CurrencySendActivity.class, args);
                    finish();
                }
            } else if (requestCode == Constants.REQUEST_CODE2) {//扫描返回
                final String result = data.getStringExtra(Constants.INTENT_KEY1);
                new QrProtocolUtil()
                        .setICoinAddressDecode(new QrProtocolUtil.ICoinAddressDecode() {
                            @Override
                            public void onCoinAddressDecode(UriDecode uriDecode) {
                                String fullName = uriDecode.scheme;
                                String tokenName = uriDecode.params.get("token");
                                if(mETHToken != null){
                                    if(mETHToken.isEth() && Utils.fullNameIsEth(fullName)){
                                        cewPayeeAddress.setText(uriDecode.path);
                                        if (uriDecode.params != null && !TextUtils.isEmpty(uriDecode.params.get("amount"))) {
                                            cewMoney.setText(uriDecode.params.get("amount"));
                                        }
                                    } else if(mETHToken.isEtc() && Utils.fullNameIsEtc(fullName)){
                                        cewPayeeAddress.setText(uriDecode.path);
                                        if (uriDecode.params != null && !TextUtils.isEmpty(uriDecode.params.get("amount"))) {
                                            cewMoney.setText(uriDecode.params.get("amount"));
                                        }
                                    } else if(mETHToken.isErc20() && mETHToken.symbol.equalsIgnoreCase(tokenName)){
                                        cewPayeeAddress.setText(uriDecode.path);
                                        if (uriDecode.params != null && !TextUtils.isEmpty(uriDecode.params.get("amount"))) {
                                            cewMoney.setText(uriDecode.params.get("amount"));
                                        }
                                    } else {
                                        cewPayeeAddress.setText(fullName + ":" + uriDecode.path);
                                    }
                                } else {
                                    cewPayeeAddress.setText(uriDecode.path);
                                }
                            }
                        })
                        .setIDecodeFail(new QrProtocolUtil.IDecodeFail() {
                            @Override
                            public void onQrDecodeFail(String errMsg) {
                                cewPayeeAddress.setText(result);
                            }

                            @Override
                            public void onProtocolUpgrade(boolean isSelf) {

                            }
                        })
                        .decode(result);
            } else if (requestCode == Constants.REQUEST_CODE3) {//选择联系人
                ContactsAddress contactsAddress = (ContactsAddress) data.getSerializableExtra(Constants.INTENT_KEY1);
                cewPayeeAddress.setText(contactsAddress.getAddress());
            } else if (requestCode == Constants.REQUEST_CODE4) {//选择交易费
                currentPrice = data.getLongExtra(Constants.INTENT_KEY1, Constants.ETH_DEFAULT_GAS_PRICE);
                currentLimit = data.getLongExtra(Constants.INTENT_KEY2, Constants.ETH_DEFAULT_GAS_LIMIT);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_eth_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_select_fee) {
            if(mETHToken == null){
                ToastUtil.showToast(R.string.hint_select_currency);
                return super.onOptionsItemSelected(item);
            }
            Bundle args = new Bundle();
            args.putSerializable(Constants.INTENT_KEY1, mETHToken);
            args.putLong(Constants.INTENT_KEY2, currentPrice);
            args.putLong(Constants.INTENT_KEY3, currentLimit);
            go2ActivityForResult(ETHTransactionFeeActivity.class, Constants.REQUEST_CODE4, args);
        }
        return super.onOptionsItemSelected(item);
    }
}
