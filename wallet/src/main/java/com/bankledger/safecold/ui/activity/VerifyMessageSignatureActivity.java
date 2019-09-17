package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.core.HDAddressManager;

/**
 * @author bankledger
 * @time 2018/8/21 18:49
 */
public class VerifyMessageSignatureActivity extends ToolbarBaseActivity implements View.OnClickListener {
    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;


    private Currency mCurrency;
    private EditText etAddress;
    private EditText etOriginalMessage;
    private EditText etSignedMessage;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_verify_message_signature;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.verify_message_sign);
        setDefaultNavigation();

        findViewById(R.id.ll_currency).setOnClickListener(this);

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);

        etAddress = findViewById(R.id.et_address);
        findViewById(R.id.iv_scan_address).setOnClickListener(this);

        etOriginalMessage = findViewById(R.id.et_original_message);
        findViewById(R.id.iv_scan_original_message).setOnClickListener(this);

        etSignedMessage = findViewById(R.id.et_signed_message);
        findViewById(R.id.iv_scan_signed_message).setOnClickListener(this);

        findViewById(R.id.bt_verify).setOnClickListener(this);
    }

    @Override
    public void initData() {
        super.initData();
        refreshSelectCurrency();
    }


    private void refreshSelectCurrency() {
        if (mCurrency != null) {
            tvCurrencyName.setText(CurrencyNameUtil.getCurrencyName(mCurrency));
            ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(mCurrency.coin, ConvertViewBean.TYPE_CURRENCY));
        } else {
            tvCurrencyName.setText(R.string.select_currency);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_currency://更换币种
                Bundle args = new Bundle();
                args.putInt(Constants.INTENT_KEY1, SelectCurrencyActivity.TYPE_MESSAGE_SIGN);
                go2ActivityForResult(SelectCurrencyActivity.class, Constants.REQUEST_CODE1, args);
                break;

            case R.id.iv_scan_address://扫描签名地址
                go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE2);
                break;

            case R.id.iv_scan_original_message://扫描原消息
                go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE3);
                break;

            case R.id.iv_scan_signed_message://扫描已签名消息
                go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE4);
                break;

            case R.id.bt_verify://验证
                if (CommonUtils.isRepeatClick() && checkParams())
                    verifyMessage();
                break;
            default:
                break;
        }
    }

    private boolean checkParams() {
        if (mCurrency == null) {
            ToastUtil.showToast(R.string.hint_select_currency);
            return false;
        } else if (TextUtils.isEmpty(etAddress.getText().toString())) {
            ToastUtil.showToast(R.string.input_sign_address);
            return false;
        } else if (TextUtils.isEmpty(etOriginalMessage.getText().toString())) {
            ToastUtil.showToast(R.string.input_original_message);
            return false;
        } else if (TextUtils.isEmpty(etSignedMessage.getText().toString())) {
            ToastUtil.showToast(R.string.input_signed_message);
            return false;
        }
        return true;
    }

    /**
     * 验证消息签名
     */
    private void verifyMessage() {
        new CommonAsyncTask.Builder<String, Void, Boolean>()
                .setIDoInBackground(new IDoInBackground<String, Void, Boolean>() {
                    @Override
                    public Boolean doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        return HDAddressManager.getInstance().getHDAccount().verifyMessage(mCurrency, strings[0], strings[1], strings[2]);
                    }
                })
                .setIPostExecute(new IPostExecute<Boolean>() {
                    @Override
                    public void onPostExecute(Boolean aBoolean) {
                        DialogUtil.showTextDialog(VerifyMessageSignatureActivity.this, R.string.tip, aBoolean ? R.string.verify_success : R.string.verify_fail, null);
                    }
                })
                .start(etAddress.getText().toString(), etOriginalMessage.getText().toString(), etSignedMessage.getText().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_SUCCESS) {
            if (requestCode == Constants.REQUEST_CODE1) {
                Currency tempCurrency = (Currency) data.getSerializableExtra(Constants.INTENT_KEY1);
                if (mCurrency == null || !tempCurrency.coin.equalsIgnoreCase(mCurrency.coin)) {
                    mCurrency = tempCurrency;
                    refreshSelectCurrency();
                }
            } else {
                String result = data.getStringExtra(Constants.INTENT_KEY1);
                if (requestCode == Constants.REQUEST_CODE2) {
                    etAddress.setText(result);
                } else if (requestCode == Constants.REQUEST_CODE3) {
                    etOriginalMessage.setText(result);
                } else if (requestCode == Constants.REQUEST_CODE4) {
                    etSignedMessage.setText(result);
                }
            }
        }
    }
}
