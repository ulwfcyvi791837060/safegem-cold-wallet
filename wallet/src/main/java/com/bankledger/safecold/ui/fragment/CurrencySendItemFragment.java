package com.bankledger.safecold.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.bean.SendCurrencyItem;
import com.bankledger.safecold.db.ContactsAddressProvider;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.SelectContactsActivity;
import com.bankledger.safecold.ui.widget.CommonEditWidget;
import com.bankledger.safecold.ui.widget.DecimalDigitsInputFilter;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

/**
 * @author bankledger
 * @time 2018/8/16 14:50
 */
public class CurrencySendItemFragment extends BaseFragment {
    private TextView tvPayTo;
    private View ivRemoveSend;
    private CommonEditWidget cewPayeeAddress;
    private CommonEditWidget cewMoney;
    private Currency mCurrency;
    private SafeAsset safeAsset;
    private String address;
    private String amount;

    private SendCurrencyItem item = new SendCurrencyItem();

    public static CurrencySendItemFragment newInstance(Currency currency) {
        return newInstance(currency, null);
    }

    public static CurrencySendItemFragment newInstance(Currency currency, String address) {
        return newInstance(currency, address, null, null);
    }

    public static CurrencySendItemFragment newInstance(Currency currency, String address, String amount, SafeAsset safeAsset) {
        CurrencySendItemFragment fragment = new CurrencySendItemFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.INTENT_KEY1, currency);
        args.putString(Constants.INTENT_KEY2, address);
        args.putString(Constants.INTENT_KEY3, amount);
        if (safeAsset != null) {
            args.putSerializable(Constants.INTENT_KEY4, safeAsset);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public CurrencySendItemFragment() {
    }

    @Override
    public int setContentView() {
        return R.layout.fragment_currency_send_item;
    }

    @Override
    public void initView() {
        tvPayTo = findViewById(R.id.tv_pay_to);
        tvPayTo.setText(String.format(getString(R.string.pay_to), "-"));
        findViewById(R.id.iv_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrency == null) {
                    ToastUtil.showToast(R.string.hint_select_currency);
                } else {
                    go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE1);
                }
            }
        });
        findViewById(R.id.iv_select_payee).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrency == null) {
                    ToastUtil.showToast(R.string.hint_select_currency);
                } else {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, ConvertViewBean.currencyConvert(mCurrency));
                    if (safeAsset != null) {
                        args.putSerializable(Constants.INTENT_KEY2, safeAsset);
                    }
                    go2ActivityForResult(SelectContactsActivity.class, Constants.REQUEST_CODE2, args);
                }
            }
        });
        ivRemoveSend = findViewById(R.id.iv_remove_send);
        ivRemoveSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof CurrencySendIntf) {
                    ((CurrencySendIntf) getActivity()).removeSendItem(CurrencySendItemFragment.this);
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
        mCurrency = (Currency) getArguments().getSerializable(Constants.INTENT_KEY1);
        address = getArguments().getString(Constants.INTENT_KEY2);
        amount = getArguments().getString(Constants.INTENT_KEY3);
        if (getArguments().containsKey(Constants.INTENT_KEY4)) {
            safeAsset = (SafeAsset) getArguments().getSerializable(Constants.INTENT_KEY4);
        }
        cewPayeeAddress.setText(address);
        cewMoney.setText(amount);
        if (safeAsset != null) {
            cewMoney.getEditText().setFilters(new InputFilter[]{new DecimalDigitsInputFilter(10, (int) safeAsset.assetDecimals)});
        } else {
            cewMoney.getEditText().setFilters(new InputFilter[]{new DecimalDigitsInputFilter(10, 8)});
        }
    }

    private void checkAliasWithAddress() {
        if (cewPayeeAddress == null) {
            return;
        }
        String address = cewPayeeAddress.getText();
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
                .start(mCurrency == null ? null : mCurrency.coin, address);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_SUCCESS) {
            if (requestCode == Constants.REQUEST_CODE1) {
                final String result = data.getStringExtra(Constants.INTENT_KEY1);
                new QrProtocolUtil()
                        .setICoinAddressDecode(new QrProtocolUtil.ICoinAddressDecode() {
                            @Override
                            public void onCoinAddressDecode(UriDecode uriDecode) {
                                String fullName = uriDecode.scheme;
                                if (mCurrency.fullName.equals(fullName)) {
                                    cewPayeeAddress.setText(uriDecode.path);
                                    if (uriDecode.params != null && !TextUtils.isEmpty(uriDecode.params.get("amount"))) {
                                        cewMoney.setText(uriDecode.params.get("amount"));
                                    }
                                } else {
                                    cewPayeeAddress.setText(fullName + ":" + uriDecode.path);
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
            } else if (requestCode == Constants.REQUEST_CODE2) {
                ContactsAddress contactsAddress = (ContactsAddress) data.getSerializableExtra(Constants.INTENT_KEY1);
                cewPayeeAddress.setText(contactsAddress.getAddress());
            }
        }
    }

    public void setCurrency(Currency currency, SafeAsset safeAsset) {
        this.safeAsset = safeAsset;
        checkAliasWithAddress();
        if (mCurrency == null) {
            this.mCurrency = currency;
        } else if (!mCurrency.coin.equalsIgnoreCase(currency.coin)) {
            this.mCurrency = currency;
            tvPayTo.setText(String.format(getString(R.string.pay_to), "-"));
            cewPayeeAddress.setText(null);
            cewMoney.setText(null);
        }
    }


    public void setRemoveSendVisibility(int visibility) {
        ivRemoveSend.setVisibility(visibility);
    }

    public SendCurrencyItem getSendCurrencyItem() {
        item.address = cewPayeeAddress.getText();
        item.amount = cewMoney.getText();
        return item;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public interface CurrencySendIntf {
        void setCurrency(Currency currency);

        void removeSendItem(CurrencySendItemFragment fragment);
    }

}
