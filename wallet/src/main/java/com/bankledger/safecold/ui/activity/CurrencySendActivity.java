package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.AsyncTaskResult;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.bean.SendCurrencyItem;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.ui.activity.eos.ChoiceAccountActivity;
import com.bankledger.safecold.ui.activity.eos.EosSendActivity;
import com.bankledger.safecold.ui.activity.eth.ETHTokenSendActivity;
import com.bankledger.safecold.ui.fragment.CurrencySendItemFragment;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.core.Tx;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.exception.NotSufficientFundsException;
import com.bankledger.safecoldj.exception.TxBuilderException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * BTC系列发送
 *
 * @author bankledger
 * @time 2018/7/30 16:51
 */
public class CurrencySendActivity extends ToolbarBaseActivity implements CurrencySendItemFragment.CurrencySendIntf {

    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;
    private Currency mCurrency;
    private SafeAsset safeAsset;
    private String normalAddress;
    private String normalAmount;
    private HDAccount hdAccount;
    private ScrollView svContent;

    private FragmentManager fragmentManager;

    private Map<String, CurrencySendItemFragment> fragmentMap = new HashMap<>(SafeColdSettings.MAX_ONCE_SEND_COUNT);
    private ArrayList<SendCurrencyItem> sendCurrencyItems = new ArrayList<>(SafeColdSettings.MAX_ONCE_SEND_COUNT);

    private int tagPosition = 0;

    private long transactionFee;
    private Menu menu;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        go2Activity(CurrencySendActivity.class, getIntent().getExtras());
        finish();
    }

    @Override
    protected int setContentLayout() {
        return R.layout.activity_currency_send;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.wallet_transfer);

        findViewById(R.id.ll_currency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2ActivityForResult(SelectCurrencyActivity.class, Constants.REQUEST_CODE2);
            }
        });

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);

        svContent = findViewById(R.id.sv_content);

        findViewById(R.id.bt_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkParams();
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
        hdAccount = HDAddressManager.getInstance().getHDAccount();
        mCurrency = (Currency) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        normalAddress = getIntent().getStringExtra(Constants.INTENT_KEY2);
        normalAmount = getIntent().getStringExtra(Constants.INTENT_KEY3);
        if (getIntent().hasExtra(Constants.INTENT_KEY4)) {
            safeAsset = (SafeAsset) getIntent().getSerializableExtra(Constants.INTENT_KEY4);
        }
        if (mCurrency != null) {
            transactionFee = mCurrency.normalFee;
        }
        fragmentManager = getSupportFragmentManager();
        addSendItem();
        refreshSelectCurrency();
    }

    private void addSendItem() {
        if (fragmentMap.size() >= 5) {
            ToastUtil.showToast(String.format(getString(R.string.max_once_send_count), SafeColdSettings.MAX_ONCE_SEND_COUNT));
            return;
        }
        CurrencySendItemFragment fragment;
        if (fragmentMap.size() == 0) {
            fragment = CurrencySendItemFragment.newInstance(mCurrency, normalAddress, normalAmount, safeAsset);
        } else {
            fragment = CurrencySendItemFragment.newInstance(mCurrency);
        }

        String tag = getFragmentTag();
        fragmentMap.put(tag, fragment);
        fragmentManager.beginTransaction().add(R.id.ll_send_item, fragment, tag).commit();
        svContent.postDelayed(new Runnable() {
            @Override
            public void run() {
                svContent.fullScroll(ScrollView.FOCUS_DOWN);
                setRemoveSendVisibility();
            }
        }, 200);
    }

    private String getFragmentTag() {
        return "currencySendItemFragment_" + (tagPosition++);
    }


    private void refreshSelectCurrency() {
        if (safeAsset != null) {
            tvCurrencyName.setText(safeAsset.assetName);
            ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(mCurrency.coin, ConvertViewBean.TYPE_SAFE_ASSET));
        } else if (mCurrency != null) {
            tvCurrencyName.setText(CurrencyNameUtil.getCurrencyName(mCurrency));
            ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(mCurrency.coin, ConvertViewBean.TYPE_CURRENCY));
        } else {
            tvCurrencyName.setText(R.string.select_currency);
        }
        if (mCurrency != null) {
            if (mCurrency.isUsdt() || safeAsset != null) {
                while (fragmentMap.size() > 1) {
                    removeSendItem(fragmentMap.values().iterator().next());
                }
            }
        }
        for (CurrencySendItemFragment fragment : fragmentMap.values()) {
            fragment.setCurrency(mCurrency, safeAsset);
        }
        resetMenu();
    }

    private void checkParams() {
        if (mCurrency == null) {
            ToastUtil.showToast(R.string.hint_select_currency);
            return;
        }

        if (fragmentMap.size() == 0) {
            ToastUtil.showToast(R.string.please_add_receiver);
            return;
        }

        sendCurrencyItems.clear();
        for (CurrencySendItemFragment fragment : fragmentMap.values()) {
            SendCurrencyItem item = fragment.getSendCurrencyItem();
            if (TextUtils.isEmpty(item.address)) {
                ToastUtil.showToast(R.string.select_payee_address);
                return;
            }
            if (TextUtils.isEmpty(item.amount) || ".".equalsIgnoreCase(item.amount)) {
                ToastUtil.showToast(R.string.input_money);
                return;
            }
            if (safeAsset != null) {
                if (!BigDecimalUtils.greaterThan(item.amount, "0")) {
                    ToastUtil.showToast(getString(R.string.input_money_limit, "0"));
                    return;
                }
            } else {
                String minNondustOutput = BigDecimalUtils.formatSatoshi2Btc(String.valueOf(Tx.getMinNondustOutput(mCurrency)));
                if (BigDecimalUtils.greaterThan(minNondustOutput, item.amount)) {
                    ToastUtil.showToast(String.format(getString(R.string.send_limit), mCurrency.coin, minNondustOutput));
                    return;
                }
            }
            sendCurrencyItems.add(item);
        }

        checkAddressIsSelf();
    }

    //检查发送地址中是否存在自己的地址（不允许自己给自己转账）
    private void checkAddressIsSelf() {
        SendCurrencyItem[] arrParams = new SendCurrencyItem[sendCurrencyItems.size()];
        sendCurrencyItems.toArray(arrParams);
        new CommonAsyncTask.Builder<SendCurrencyItem, Void, AsyncTaskResult<SendCurrencyItem>>()
                .setIDoInBackground(new IDoInBackground<SendCurrencyItem, Void, AsyncTaskResult<SendCurrencyItem>>() {
                    @Override
                    public AsyncTaskResult<SendCurrencyItem> doInBackground(IPublishProgress<Void> publishProgress, SendCurrencyItem... sendCurrencyItems) {
                        for (SendCurrencyItem item : sendCurrencyItems) {
                            if (HDAddressProvider.getInstance().checkAddressExist(mCurrency.coin, item.address)) {
                                return new AsyncTaskResult<>(false, item);
                            }
                        }
                        return new AsyncTaskResult<>();
                    }
                })
                .setIPostExecute(new IPostExecute<AsyncTaskResult<SendCurrencyItem>>() {
                    @Override
                    public void onPostExecute(AsyncTaskResult<SendCurrencyItem> result) {
                        if (result.isSuccess()) {
                            buildTx();
                        } else {
                            DialogUtil.showTextDialog(CurrencySendActivity.this, getString(R.string.tip),
                                    String.format(getString(R.string.cant_send_self), result.getResult().address), false, null);
                        }
                    }
                })
                .start(arrParams);
    }

    private void buildTx() {
        SendCurrencyItem[] arrParams = new SendCurrencyItem[sendCurrencyItems.size()];
        sendCurrencyItems.toArray(arrParams);
        new CommonAsyncTask.Builder<SendCurrencyItem, Void, AsyncTaskResult<Void>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(CurrencySendActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<SendCurrencyItem, Void, AsyncTaskResult<Void>>() {
                    @Override
                    public AsyncTaskResult<Void> doInBackground(IPublishProgress<Void> publishProgress, SendCurrencyItem... sendCurrencyItems) {
                        String[] toAddresses = new String[sendCurrencyItems.length];
                        Long[] amounts = new Long[sendCurrencyItems.length];

                        for (int i = 0; i < sendCurrencyItems.length; i++) {
                            toAddresses[i] = sendCurrencyItems[i].address;
                            if (mCurrency != null && safeAsset != null) {
                                amounts[i] = BigDecimalUtils.formatAssetAmount(sendCurrencyItems[i].amount, safeAsset.assetDecimals);
                            } else if (mCurrency != null && mCurrency.isUsdt()) {
                                amounts[i] = Tx.getMinNondustOutput(mCurrency);
                            } else {
                                amounts[i] = BigDecimalUtils.unitToSatoshiL(sendCurrencyItems[i].amount);
                            }
                        }

                        try {
                            if (mCurrency != null && safeAsset != null) {
                                hdAccount.newSafeAssetTx(safeAsset, toAddresses[0], amounts[0], mCurrency, transactionFee);
                            } else if (mCurrency != null && mCurrency.isUsdt()) {
                                if (BigDecimalUtils.greaterThan(sendCurrencyItems[0].amount, EosUsdtBalanceProvider.getInstance().getUsdtBalance())) {
                                    return new AsyncTaskResult<>(new NotSufficientFundsException("usdt not sufficient funds"));
                                }
                                Currency btcCurrent = HDAddressManager.getInstance().getCurrencyMap().get(SafeColdSettings.BTC);
                                hdAccount.newTx(toAddresses, amounts, btcCurrent, transactionFee, true);
                            } else {
                                hdAccount.newTx(toAddresses, amounts, mCurrency, transactionFee, false);
                            }
                            return new AsyncTaskResult<>();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return new AsyncTaskResult<>(e);
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<AsyncTaskResult<Void>>() {
                    @Override
                    public void onPostExecute(AsyncTaskResult<Void> voidAsyncTaskResult) {
                        CommonUtils.dismissProgressDialog();
                        if (voidAsyncTaskResult.isSuccess()) {
                            Bundle args = new Bundle();
                            args.putSerializable(Constants.INTENT_KEY1, mCurrency);
                            args.putSerializable(Constants.INTENT_KEY2, sendCurrencyItems);
                            args.putLong(Constants.INTENT_KEY3, transactionFee);
                            if (safeAsset != null) {
                                args.putSerializable(Constants.INTENT_KEY4, safeAsset);
                            }
                            go2Activity(CurrencySendAffirmActivity.class, args);
                        } else {
                            handleException(voidAsyncTaskResult.getException());
                        }
                    }
                })
                .start(arrParams);
    }

    private void handleException(Exception exception) {
        if (exception instanceof AddressFormatException) {
            ToastUtil.showToast(R.string.address_err);
        } else if (exception instanceof TxBuilderException) {
            TxBuilderException.TxBuilderErrorType errorType = ((TxBuilderException) exception).type;
            if (errorType == TxBuilderException.TxBuilderErrorType.TxDustOut) {
                ToastUtil.showToast(R.string.amount_is_too_small);
            } else if (errorType == TxBuilderException.TxBuilderErrorType.TxNotEnoughMoney) {
                if (mCurrency.isUsdt()) {
                    ToastUtil.showToast(String.format(getString(R.string.not_sufficient_funds), SafeColdSettings.BTC));
                } else {
                    ToastUtil.showToast(String.format(getString(R.string.not_sufficient_funds), mCurrency.coin));
                }
            } else if (errorType == TxBuilderException.TxBuilderErrorType.TxMaxSize) {
                ToastUtil.showToast(R.string.transaction_size_failed);
            } else if (errorType == TxBuilderException.TxBuilderErrorType.TxCannotCalculate) {
                ToastUtil.showToast(String.format(getString(R.string.not_sufficient_funds2), mCurrency.coin));
            } else {
                ToastUtil.showToast(R.string.unknown_abnormal);
            }
        } else if (exception instanceof NotSufficientFundsException) {
            ToastUtil.showToast(String.format(getString(R.string.not_sufficient_funds), mCurrency.coin));
        } else {
            ToastUtil.showToast(String.format(getString(R.string.not_sufficient_funds), mCurrency.coin));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_SUCCESS) {
            if (requestCode == Constants.REQUEST_CODE2) {
                ConvertViewBean convertView = (ConvertViewBean) data.getSerializableExtra(Constants.INTENT_KEY1);
                if (convertView.isCurrency()) {
                    safeAsset = null;
                    if (mCurrency == null || !convertView.currency.coin.equalsIgnoreCase(mCurrency.coin)) {
                        mCurrency = convertView.currency;
                        transactionFee = mCurrency.normalFee;
                    }
                    refreshSelectCurrency();
                } else if (convertView.isETHToken()) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, convertView.ethToken);
                    go2Activity(ETHTokenSendActivity.class, args);
                    finish();
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
                    safeAsset = convertView.safeAsset;

                    mCurrency = HDAddressManager.getInstance().getCurrencyCoin(SafeColdSettings.SAFE);
                    transactionFee = mCurrency.normalFee;
                    refreshSelectCurrency();
                }
            } else if (requestCode == Constants.REQUEST_CODE3) {
                transactionFee = data.getLongExtra(Constants.INTENT_KEY1, mCurrency.normalFee);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_currency_menu, menu);
        this.menu = menu;
        resetMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            addSendItem();
        } else if (item.getItemId() == R.id.menu_select_fee) {
            if (mCurrency == null) {
                ToastUtil.showToast(R.string.hint_select_currency);
                return super.onOptionsItemSelected(item);
            }
            Bundle args = new Bundle();
            args.putSerializable(Constants.INTENT_KEY1, mCurrency);
            args.putLong(Constants.INTENT_KEY2, transactionFee);
            go2ActivityForResult(SelectTransactionFeeActivity.class, Constants.REQUEST_CODE3, args);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setCurrency(Currency currency) {
        this.mCurrency = currency;
        refreshSelectCurrency();
    }

    @Override
    public void removeSendItem(CurrencySendItemFragment fragment) {
        if (fragmentMap.size() <= 1) {
            return;
        }
        fragmentMap.remove(fragment.getTag());
        fragmentManager.beginTransaction().remove(fragment).commit();
        setRemoveSendVisibility();
    }

    private void setRemoveSendVisibility() {
        int visibility = fragmentMap.size() == 1 ? View.GONE : View.VISIBLE;
        for (CurrencySendItemFragment f : fragmentMap.values()) {
            f.setRemoveSendVisibility(visibility);
        }
    }

    private void resetMenu() {
        if (menu != null && mCurrency != null) {
            if (mCurrency.isUsdt() || safeAsset != null) {
                menu.findItem(R.id.menu_add).setVisible(false);
            } else {
                menu.findItem(R.id.menu_add).setVisible(true);
            }
        }
    }
}
