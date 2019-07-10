package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.ui.widget.CommonEditWidget;
import com.bankledger.safecold.ui.widget.DecimalDigitsInputFilter;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.WriterException;

import java.util.HashMap;
import java.util.Map;

/**
 * BTC系列接收
 *
 * @author bankledger
 * @time 2018/7/30 19:47
 */
public class CurrencyReceiveActivity extends ToolbarBaseActivity {
    private ConvertViewBean convertView;
    private CommonEditWidget cewAmount;
    private TextView tvAddress;
    private ImageView imgAddress;
    private ImageView ivReceiveCode;
    private String selectAddress;
    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;
    private TextView tvContractAddress;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_currency_receive;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.wallet_receive);
        tvContractAddress = findViewById(R.id.tv_contract_address);
        tvAddress = findViewById(R.id.tv_address);
        imgAddress = findViewById(R.id.img_address);

        findViewById(R.id.ll_currency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2ActivityForResult(SelectCurrencyActivity.class, Constants.REQUEST_CODE1);
            }
        });
        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);

        findViewById(R.id.ll_address).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (convertView != null && convertView.isCurrency()) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, convertView.currency);
                    go2ActivityForResult(SelectAddressActivity.class, Constants.REQUEST_CODE2, args);
                } else if(convertView != null && convertView.isSafeAsset()) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, HDAddressManager.getInstance().getCurrencyCoin(SafeColdSettings.SAFE));
                    args.putSerializable(Constants.INTENT_KEY2, convertView.safeAsset);
                    go2ActivityForResult(SelectAddressActivity.class, Constants.REQUEST_CODE2, args);
                }
            }
        });

        cewAmount = findViewById(R.id.cew_amount);
        cewAmount.setHint(R.string.receive_amount);
        cewAmount.getEditText().setFilters(new InputFilter[]{new DecimalDigitsInputFilter(10, 8)});
        cewAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().contains(".") && s.length() > 10) {
                    cewAmount.setText(s.subSequence(0, 10));
                    cewAmount.getEditText().setSelection(10);
                }

                int indexD = s.toString().indexOf(".");
                if (indexD >= 0 && s.length() - indexD > 9) {
                    cewAmount.setText(s.subSequence(0, indexD + 9));
                    cewAmount.getEditText().setSelection(cewAmount.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                buildReceive();
            }
        });

        ivReceiveCode = findViewById(R.id.iv_receive_code);
    }

    @Override
    public void initData() {
        super.initData();
        convertView = (ConvertViewBean) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        buildReceive();
    }


    private void buildReceive() {
        if (convertView != null) {
            tvCurrencyName.setText(convertView.getName());
            ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(convertView.getCoin(), convertView.type));
            if (convertView.isCurrency()) {
                imgAddress.setVisibility(View.VISIBLE);
                tvContractAddress.setVisibility(View.GONE);
            } else if (convertView.isETHToken()) {
                imgAddress.setVisibility(View.GONE);
                if(convertView.ethToken.isErc20()){
                    tvContractAddress.setText(getString(R.string.contract_address, convertView.ethToken.contractsAddress));
                    tvContractAddress.setVisibility(View.VISIBLE);
                } else {
                    tvContractAddress.setVisibility(View.GONE);
                }
            } else if (convertView.isEosBalance()) {
                imgAddress.setVisibility(View.GONE);
                tvContractAddress.setVisibility(View.GONE);
            } else if (convertView.isSafeAsset()){
                imgAddress.setVisibility(View.VISIBLE);
                tvContractAddress.setVisibility(View.GONE);
            }
        } else {
            tvCurrencyName.setText(R.string.select_currency);
            imgAddress.setVisibility(View.GONE);
            tvContractAddress.setVisibility(View.GONE);
            return;
        }
        String coin = "";
        Map<String, String> params = new HashMap<>();
        params.put("amount", cewAmount.getText());
        if (convertView.isCurrency()) {
            selectAddress = TextUtils.isEmpty(selectAddress) ? convertView.getAddress() : selectAddress;
            coin = convertView.currency.fullName;
        } else if (convertView.isETHToken()) {
            selectAddress = convertView.getAddress();
            if (convertView.ethToken.isEtc()) {
                coin = SafeColdSettings.ETC_FULL_NAME;
            } else {
                coin = SafeColdSettings.ETH_FULL_NAME;
            }
            if (convertView.ethToken.isErc20()) {
                params.put("token", convertView.ethToken.contractsAddress);
            }
        } else if (convertView.isEosBalance()) {
            selectAddress = EosAccountProvider.getInstance().queryAvailableEosAccount().getAccountName();
            coin = SafeColdSettings.EOS_FULL_NAME;
        } else if (convertView.isSafeAsset()) {
            selectAddress = TextUtils.isEmpty(selectAddress) ? convertView.getAddress() : selectAddress;
            coin = SafeColdSettings.SAFE_FULL_NAME;
            params.put("token", convertView.safeAsset.assetName);
        }
        tvAddress.setText(selectAddress);

        UriDecode uri = new UriDecode(coin, selectAddress, params);
        new CommonAsyncTask.Builder<String, Void, Bitmap>()
                .setIDoInBackground(new IDoInBackground<String, Void, Bitmap>() {
                    @Override
                    public Bitmap doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        try {
                            return QRCodeEncoderUtils.encodeAsBitmap(CurrencyReceiveActivity.this, strings[0]);
                        } catch (WriterException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<Bitmap>() {
                    @Override
                    public void onPostExecute(Bitmap bitmap) {
                        ivReceiveCode.setImageBitmap(bitmap);
                    }
                }).startOnSingleThread(QRCodeUtil.encodeUri(uri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_SUCCESS) {
            if (requestCode == Constants.REQUEST_CODE1) {
                convertView = (ConvertViewBean) data.getSerializableExtra(Constants.INTENT_KEY1);
                selectAddress = null;
                buildReceive();
            } else if (requestCode == Constants.REQUEST_CODE2) {
                HDAddress hdAddress = (HDAddress) data.getSerializableExtra(Constants.INTENT_KEY1);
                selectAddress = hdAddress.getAddress();
                buildReceive();
            }
        }
    }
}
