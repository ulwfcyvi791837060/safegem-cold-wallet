package com.bankledger.safecold.ui.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
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
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.AsyncTaskResult;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QRCodeEncoderUtils;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.google.zxing.WriterException;

/**
 * @author bankledger
 * @time 2018/8/21 13:36
 */
public class SignMessageActivity extends ToolbarBaseActivity implements View.OnClickListener {
    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;
    private TextView tvAddress;
    private View ivQrAddress;
    private EditText etSignMessage;
    private View ivDown;
    private View llSignedMessage;
    private TextView tvSignedMessage;
    private View btSign;

    private Currency mCurrency;
    private HDAddress mHDAddress;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_sign_message;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.message_sign);
        setDefaultNavigation();

        findViewById(R.id.ll_currency).setOnClickListener(this);

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);

        findViewById(R.id.ll_address).setOnClickListener(this);
        tvAddress = findViewById(R.id.tv_address);
        ivQrAddress = findViewById(R.id.iv_qr_address);
        ivQrAddress.setOnClickListener(this);
        findViewById(R.id.iv_qr_message).setOnClickListener(this);

        etSignMessage = findViewById(R.id.et_sign_message);

        btSign = findViewById(R.id.bt_sign);
        btSign.setOnClickListener(this);

        ivDown = findViewById(R.id.iv_down);
        llSignedMessage = findViewById(R.id.ll_signed_message);
        tvSignedMessage = findViewById(R.id.tv_signed_message);
        findViewById(R.id.iv_qr_signed_message).setOnClickListener(this);
    }

    @Override
    public void initData() {
        super.initData();
        refreshSelectCurrency();
    }

    private void refreshSelectCurrency() {
        if (mCurrency != null) {
            tvCurrencyName.setText(CurrencyNameUtil.getCurrencyName(mCurrency));
            ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(mCurrency.coin,ConvertViewBean.TYPE_CURRENCY));
        } else {
            tvCurrencyName.setText(R.string.select_currency);
        }
    }

    private void refreshSelectHDAddress() {
        if (mHDAddress != null) {
            tvAddress.setText(mHDAddress.getAddress());
            ivQrAddress.setVisibility(View.VISIBLE);
        } else {
            tvAddress.setText(null);
            ivQrAddress.setVisibility(View.GONE);
        }
        refreshSignState(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_currency://更换币种
                mHDAddress = null;
                Bundle args1 = new Bundle();
                args1.putInt(Constants.INTENT_KEY1,SelectCurrencyActivity.TYPE_MESSAGE_SIGN);
                go2ActivityForResult(SelectCurrencyActivity.class, Constants.REQUEST_CODE1,args1);
                break;

            case R.id.ll_address://更换签名地址
                if (mCurrency == null) {
                    ToastUtil.showToast(R.string.hint_select_currency);
                } else {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, mCurrency);
                    args.putInt(Constants.INTENT_KEY2, MineCurrencyAddressDetailActivity.START_TYPE_SELECT);
                    go2ActivityForResult(MineCurrencyAddressDetailActivity.class, Constants.REQUEST_CODE2, args);
                }
                break;

            case R.id.iv_qr_address://地址二维码
                if (CommonUtils.isRepeatClick()) {
                    if (mCurrency != null && mHDAddress != null) {
                        showQrCode(mHDAddress.getAddress());
                    }
                }
                break;

            case R.id.iv_qr_message://原消息二维码
                if (CommonUtils.isRepeatClick()) {
                    if (!TextUtils.isEmpty(etSignMessage.getText().toString()))
                        showQrCode(etSignMessage.getText().toString());
                }
                break;

            case R.id.bt_sign://签名
                if (checkParams()) {
                    signMessage();
                }
                break;

            case R.id.iv_qr_signed_message://显示签名二维码
                if (CommonUtils.isRepeatClick()) {
                    showQrCode(tvSignedMessage.getText().toString());
                }
                break;
        }
    }

    private boolean checkParams() {
        if (mCurrency == null) {
            ToastUtil.showToast(R.string.select_sign_currency);
            return false;
        } else if (TextUtils.isEmpty(etSignMessage.getText())) {
            ToastUtil.showToast(R.string.input_sign_message);
            return false;
        }
        return true;
    }

    private void signMessage() {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, String content) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    new CommonAsyncTask.Builder<String, Void, AsyncTaskResult<String>>()
                            .setIPreExecute(new IPreExecute() {
                                @Override
                                public void onPreExecute() {
                                    CommonUtils.showProgressDialog(SignMessageActivity.this);
                                }
                            })
                            .setIDoInBackground(new IDoInBackground<String, Void, AsyncTaskResult<String>>() {
                                @Override
                                public AsyncTaskResult<String> doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                    try {
                                        String signedMessage = HDAddressManager.getInstance().getHDAccount().messageSign(mHDAddress, strings[0], strings[1]);
                                        log.info("address = {}, edMessage = {}", mHDAddress.getAddress(), signedMessage);
                                        return new AsyncTaskResult<>(signedMessage);
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
                                        refreshSignState(true);
                                        tvSignedMessage.setText(result.getResult());
                                    } else {
                                        ToastUtil.showToast(R.string.password_error);
                                    }
                                }
                            }).start(etSignMessage.getText().toString(), content);
                }
            }
        });
    }

    /**
     * 刷新页面的签名状态
     *
     * @param signed
     */
    private void refreshSignState(boolean signed) {
        if (signed) {
            btSign.setVisibility(View.GONE);
            ivDown.setVisibility(View.VISIBLE);
            llSignedMessage.setVisibility(View.VISIBLE);
        } else {
            btSign.setVisibility(View.VISIBLE);
            ivDown.setVisibility(View.GONE);
            llSignedMessage.setVisibility(View.GONE);
            tvSignedMessage.setText(null);
        }
        etSignMessage.setEnabled(!signed);
    }

    private void showQrCode(final String content) {
        new CommonAsyncTask.Builder<String, Void, Bitmap>()
                .setIDoInBackground(new IDoInBackground<String, Void, Bitmap>() {
                    @Override
                    public Bitmap doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        try {
                            return QRCodeEncoderUtils.encodeAsBitmapUTF_8(SignMessageActivity.this, strings[0]);
                        } catch (WriterException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<Bitmap>() {
                    @Override
                    public void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null)
                            DialogUtil.showImageDialog(SignMessageActivity.this, bitmap, content);
                    }
                })
                .start(content);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_SUCCESS) {
            if (requestCode == Constants.REQUEST_CODE1) {
                Currency tempBean = (Currency) data.getSerializableExtra(Constants.INTENT_KEY1);
                if (mCurrency == null || !tempBean.coin.equalsIgnoreCase(mCurrency.coin)) {
                    mCurrency = tempBean;
                    refreshSelectCurrency();
                    getHDAddressWithAddress(mCurrency.selectAddress);
                }
            } else if (requestCode == Constants.REQUEST_CODE2) {
                mHDAddress = (HDAddress) data.getSerializableExtra(Constants.INTENT_KEY1);
                refreshSelectHDAddress();
            }
        }
    }

    private void getHDAddressWithAddress(String address) {
        new CommonAsyncTask.Builder<String, Void, HDAddress>()
                .setIDoInBackground(new IDoInBackground<String, Void, HDAddress>() {
                    @Override
                    public HDAddress doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                        return HDAddressProvider.getInstance().getHDAddressWithAddress(mCurrency.coin,strings[0]);
                    }
                })
                .setIPostExecute(new IPostExecute<HDAddress>() {
                    @Override
                    public void onPostExecute(HDAddress hdAddress) {
                        if (hdAddress != null) {
                            mHDAddress = hdAddress;
                            refreshSelectHDAddress();
                        }
                    }
                })
                .start(address);
    }
}
