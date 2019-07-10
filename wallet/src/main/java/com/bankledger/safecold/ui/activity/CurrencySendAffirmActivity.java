package com.bankledger.safecold.ui.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.bean.SendCurrencyItem;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.core.Tx;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.exception.TxBuilderException;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

import org.greenrobot.eventbus.EventBus;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;

/**
 * BTC系列发送确认
 *
 * @author bankledger
 * @time 2018/9/19 16:52
 */
public class CurrencySendAffirmActivity extends ToolbarBaseActivity {
    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;
    private LinearLayout llItem;
    private TextView tvTransactionFee;
    private HDAccount hdAccount;
    private Currency mCurrency;
    private SafeAsset safeAsset;
    private ArrayList<SendCurrencyItem> sendCurrencyItems = new ArrayList<>(SafeColdSettings.MAX_ONCE_SEND_COUNT);
    private long transactionFee;
    private TextView tvUnit;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_currency_send_affirm;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.affirm_pay);
        setDefaultNavigation();

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);
        llItem = findViewById(R.id.ll_item);
        tvTransactionFee = findViewById(R.id.tv_transaction_fee);
        findViewById(R.id.bt_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        tvUnit = findViewById(R.id.tv_unit);
    }

    @Override
    public void initData() {
        super.initData();
        hdAccount = HDAddressManager.getInstance().getHDAccount();
        mCurrency = (Currency) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        sendCurrencyItems = (ArrayList<SendCurrencyItem>) getIntent().getSerializableExtra(Constants.INTENT_KEY2);
        transactionFee = getIntent().getLongExtra(Constants.INTENT_KEY3, 0L);
        if (getIntent().hasExtra(Constants.INTENT_KEY4)) {
            safeAsset = (SafeAsset) getIntent().getSerializableExtra(Constants.INTENT_KEY4);
        }
        ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(mCurrency.coin, ConvertViewBean.TYPE_CURRENCY));
        if (safeAsset != null) {
            tvCurrencyName.setText(safeAsset.assetName);
        } else {
            tvCurrencyName.setText(CurrencyNameUtil.getCurrencyName(mCurrency));
        }
        tvTransactionFee.setText(BigDecimalUtils.formatSatoshi2Btc(Long.toString(transactionFee)));
        tvUnit.setText(String.format(getString(R.string.transaction_fee_unit), mCurrency.isUsdt() ? SafeColdSettings.BTC : mCurrency.coin));
        for (int i = 0; i < sendCurrencyItems.size(); i++) {
            addItem(sendCurrencyItems.get(i));
        }
    }

    private void addItem(SendCurrencyItem sendCurrencyItem) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_send_currency, llItem, false);
        TextView tvPayAddress = item.findViewById(R.id.tv_pay_address);
        TextView tvPayAmount = item.findViewById(R.id.tv_pay_amount);
        tvPayAddress.setText(sendCurrencyItem.address);
        tvPayAmount.setText(sendCurrencyItem.amount);
        llItem.addView(item);
    }

    private void send() {
        final SendCurrencyItem[] arrParams = new SendCurrencyItem[sendCurrencyItems.size()];
        sendCurrencyItems.toArray(arrParams);

        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, final String content) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        send();
                    } else {
                        new CommonAsyncTask.Builder<SendCurrencyItem, Void, AsyncTaskResult<String>>()
                                .setIPreExecute(new IPreExecute() {
                                    @Override
                                    public void onPreExecute() {
                                        CommonUtils.showProgressDialog(CurrencySendAffirmActivity.this);
                                    }
                                })
                                .setIDoInBackground(new IDoInBackground<SendCurrencyItem, Void, AsyncTaskResult<String>>() {
                                    @Override
                                    public AsyncTaskResult<String> doInBackground(IPublishProgress<Void> publishProgress, SendCurrencyItem... sendCurrencyItems) {
                                        String[] toAddresses = new String[sendCurrencyItems.length];
                                        Long[] amounts = new Long[sendCurrencyItems.length];

                                        for (int i = 0; i < sendCurrencyItems.length; i++) {
                                            toAddresses[i] = sendCurrencyItems[i].address;
                                            if (safeAsset != null) {
                                                amounts[i] = BigDecimalUtils.formatAssetAmount(sendCurrencyItems[i].amount, safeAsset.assetDecimals);
                                            } else {
                                                amounts[i] = BigDecimalUtils.unitToSatoshiL(sendCurrencyItems[i].amount);
                                            }
                                        }

                                        try {
                                            Tx tx;
                                            if (mCurrency != null && safeAsset != null) {
                                                tx = hdAccount.newSafeAssetTx(safeAsset, toAddresses[0], amounts[0], mCurrency, transactionFee, content);
                                            } else if (mCurrency != null && mCurrency.isUsdt()) {
                                                tx = hdAccount.newUsdtTx(toAddresses[0], amounts[0], transactionFee, content);
                                            } else {
                                                tx = hdAccount.newTx(toAddresses, amounts, content, mCurrency, transactionFee);
                                            }
                                            String hexCode = new String(Hex.encode(tx.bitcoinSerialize()));
                                            return new AsyncTaskResult<>(
                                                    ProtoUtils.encodeSignTx(
                                                            new TransSignTx(
                                                                    WalletInfoManager.getWalletNumber(), new SignTx(mCurrency.coin, hexCode))));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            return new AsyncTaskResult<>(e);
                                        }
                                    }
                                })
                                .setIPostExecute(new IPostExecute<AsyncTaskResult<String>>() {
                                    @Override
                                    public void onPostExecute(AsyncTaskResult<String> result) {
                                        CommonUtils.dismissProgressDialog();
                                        if (result.isSuccess()) {
                                            Bundle args = new Bundle();
                                            args.putInt(Constants.INTENT_KEY1, QrCodePageActivity.START_TYPE_SEND_CURRENCY);
                                            args.putString(Constants.INTENT_KEY2, result.getResult());
                                            go2Activity(QrCodePageActivity.class, args);
                                            EventMessage message = new EventMessage(EventMessage.TYPE_BALANCE_CHANGED);
                                            EventBus.getDefault().post(message);
                                            finish();
                                        } else {
                                            handleException(result.getException());
                                        }
                                    }
                                })
                                .start(arrParams);
                    }
                }
            }
        });
    }

    private void handleException(Exception exception) {
        if (exception instanceof AddressFormatException) {
            ToastUtil.showToast(R.string.address_err);
        } else if (exception instanceof TxBuilderException) {
            TxBuilderException.TxBuilderErrorType errorType = ((TxBuilderException) exception).type;
            if (errorType == TxBuilderException.TxBuilderErrorType.TxDustOut) {
                ToastUtil.showToast(R.string.amount_is_too_small);
            } else if (errorType == TxBuilderException.TxBuilderErrorType.TxNotEnoughMoney) {
                ToastUtil.showToast(String.format(getString(R.string.not_sufficient_funds), mCurrency.coin));
            } else if (errorType == TxBuilderException.TxBuilderErrorType.TxMaxSize) {
                ToastUtil.showToast(R.string.transaction_size_failed);
            } else {
                ToastUtil.showToast(R.string.unknown_abnormal);
            }
        } else {
            ToastUtil.showToast(R.string.password_error);
        }
    }
}
