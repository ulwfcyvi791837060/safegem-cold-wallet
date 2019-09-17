package com.bankledger.safecold.ui.activity.eth;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.entity.ETHToken;

import org.web3j.utils.Convert;

import java.math.BigDecimal;

/**
 * @author bankledger
 * @time 2018/8/30 15:58
 */
public class ETHTransactionFeeActivity extends ToolbarBaseActivity {

    private EditText ethGasPrice;
    private EditText ethGasLimit;
    private TextView tvDefault;
    private TextView tvMaxFee;
    private ETHToken mEthToken;


    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            setMaxFee();
        }

        @Override
        public void afterTextChanged(Editable s) {
            String content = s.toString();
            if (content.startsWith("0")) {
                s.delete(0, 1);
            } else if (content.startsWith(".")) {
                s.delete(0, 1);
            }
        }
    };

    @Override
    protected int setContentLayout() {
        return R.layout.activity_eth_transaction_fee;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.transaction_fee);
        setDefaultNavigation();

        tvMaxFee = findViewById(R.id.tv_max_fee);

        ethGasPrice = findViewById(R.id.eth_gas_price);
        ethGasPrice.addTextChangedListener(textWatcher);
        ethGasLimit = findViewById(R.id.eth_gas_limit);
        ethGasLimit.addTextChangedListener(textWatcher);
        tvDefault = findViewById(R.id.tv_default);

        tvDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDefaultFee();
            }
        });
        findViewById(R.id.bt_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkParams()) {
                    Intent data = new Intent();
                    String price = ethGasPrice.getText().toString();
                    String limit = ethGasLimit.getText().toString();
                    data.putExtra(Constants.INTENT_KEY1, Long.valueOf(price));
                    data.putExtra(Constants.INTENT_KEY2, Long.valueOf(limit));
                    setResult(Constants.RESULT_SUCCESS, data);
                    finish();
                }
            }
        });
    }

    @Override
    public void initData() {
        mEthToken = (ETHToken) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        long price = getIntent().getLongExtra(Constants.INTENT_KEY2, Constants.ETH_DEFAULT_GAS_PRICE);
        long limit = getIntent().getLongExtra(Constants.INTENT_KEY3, Constants.ETH_DEFAULT_GAS_LIMIT);
        setFee(price, limit);
    }

    private void setMaxFee() {
        String price = ethGasPrice.getText().toString();
        String limit = ethGasLimit.getText().toString();
        String maxFee;
        if (TextUtils.isEmpty(price) || TextUtils.isEmpty(limit)) {
            maxFee = "0";
        } else {
            BigDecimal wei = Convert.toWei(price, Convert.Unit.GWEI);
            maxFee = Convert.fromWei(BigDecimalUtils.mul(wei.toPlainString(), limit), Convert.Unit.ETHER).toPlainString();
        }
        tvMaxFee.setText(String.format(getString(R.string.max_fee), maxFee, mEthToken.isEtc() ? SafeColdSettings.ETC : SafeColdSettings.ETH));
    }

    private void setDefaultFee() {
        setFee(Constants.ETH_DEFAULT_GAS_PRICE, Constants.ETH_DEFAULT_GAS_LIMIT);
    }

    private void setFee(long price, long limit) {
        ethGasPrice.setText(String.valueOf(price));
        ethGasPrice.setSelection(ethGasPrice.length());
        ethGasLimit.setText(String.valueOf(limit));
        ethGasLimit.setSelection(ethGasLimit.length());
    }

    private boolean checkParams() {
        if (TextUtils.isEmpty(ethGasPrice.getText())) {
            ToastUtil.showToast(R.string.eth_gas_price_hint);
            return false;
        }

        if (TextUtils.isEmpty(ethGasLimit.getText())) {
            ToastUtil.showToast(R.string.eth_gas_limit_hint);
            return false;
        }

        Long limit = Long.valueOf(ethGasLimit.getText().toString());

        if (limit < 21000) {
            ToastUtil.showToast(R.string.eth_gas_limit_less_than_hint);
            return false;
        }

        BigDecimal wei = Convert.toWei(ethGasPrice.getText().toString(), Convert.Unit.GWEI);
        BigDecimal txFee = Convert.fromWei(BigDecimalUtils.mul(wei.toPlainString(), ethGasLimit.getText().toString()), Convert.Unit.ETHER);
        if(BigDecimalUtils.greaterThan(txFee.toPlainString(), "0.2")){
            ToastUtil.showToast(getString(R.string.eth_gas_greater_than_hint, "0.2"));
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ethGasLimit.removeTextChangedListener(textWatcher);
        ethGasPrice.removeTextChangedListener(textWatcher);
    }
}
