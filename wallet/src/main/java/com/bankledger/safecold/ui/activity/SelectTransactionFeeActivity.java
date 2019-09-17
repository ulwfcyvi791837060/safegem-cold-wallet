package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.ui.widget.DecimalDigitsInputFilter;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.StringUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;

/**
 * @author bankledger
 * @time 2018/8/30 15:58
 */
public class SelectTransactionFeeActivity extends ToolbarBaseActivity {
    private EditText etFee;
    private TextView tvUnit;
    private TextView tvMinFee;
    private Currency mCurrency;
    private long transactionFee;
    private String lowFee;
    private String highFee;
    private String normalFee;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_select_transaction_fee;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.transaction_fee);
        setDefaultNavigation();

        etFee = findViewById(R.id.et_fee);
        etFee.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(12, BigDecimalUtils.BTC_SCALE)});
        tvUnit = findViewById(R.id.tv_unit);
        tvMinFee = findViewById(R.id.tv_min_fee);
        findViewById(R.id.tv_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etFee.setText(StringUtil.subZeroAndDot(normalFee));
                etFee.setSelection(etFee.length());
            }
        });
        findViewById(R.id.bt_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkParams()) {
                    Intent data = new Intent();
                    String value = StringUtil.subZeroAndDot(BigDecimalUtils.unitToSatoshi(etFee.getText().toString()));
                    data.putExtra(Constants.INTENT_KEY1, Long.valueOf(value));
                    setResult(Constants.RESULT_SUCCESS, data);
                    finish();
                }
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
        mCurrency = (Currency) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        if (mCurrency == null) {
            finish();
        }
        transactionFee = getIntent().getLongExtra(Constants.INTENT_KEY2, mCurrency.normalFee);

        lowFee = BigDecimalUtils.unitToBtc(Long.toString(mCurrency.lowFee));
        highFee = BigDecimalUtils.unitToBtc(Long.toString(mCurrency.highFee));
        normalFee = BigDecimalUtils.unitToBtc(Long.toString(mCurrency.normalFee));

        etFee.setText(StringUtil.subZeroAndDot(BigDecimalUtils.unitToBtc(Long.toString(transactionFee))));
        etFee.setSelection(etFee.length());
        if (mCurrency.isUsdt()) {
            tvUnit.setText(String.format(getString(R.string.transaction_fee_unit), SafeColdSettings.BTC));
            tvMinFee.setText(String.format(getString(R.string.min_transaction_fee), StringUtil.subZeroAndDot(lowFee),  SafeColdSettings.BTC));
        } else {
            tvUnit.setText(String.format(getString(R.string.transaction_fee_unit), mCurrency.coin));
            tvMinFee.setText(String.format(getString(R.string.min_transaction_fee), StringUtil.subZeroAndDot(lowFee), mCurrency.coin));
        }
    }

    private boolean checkParams() {
        if (TextUtils.isEmpty(etFee.getText()) || etFee.getText().toString().equals(".")) {
            ToastUtil.showToast(R.string.input_transaction_fee);
            return false;
        }

        if (BigDecimalUtils.greaterThan(etFee.getText().toString(), highFee)) {
            ToastUtil.showToast(String.format(getString(R.string.high_transaction_fee), mCurrency.coin, StringUtil.subZeroAndDot(highFee)));
            return false;
        }

        if (BigDecimalUtils.greaterThan(lowFee, etFee.getText().toString())) {
            ToastUtil.showToast(getString(R.string.low_transaction_fee));
            return false;
        }

        String value = StringUtil.subZeroAndDot(BigDecimalUtils.unitToSatoshi(etFee.getText().toString()));
        if (value.contains(".")) {
            ToastUtil.showToast(String.format(getString(R.string.decimal_places_limit), String.valueOf(BigDecimalUtils.BTC_SCALE)));
            return false;
        }
        return true;
    }
}
