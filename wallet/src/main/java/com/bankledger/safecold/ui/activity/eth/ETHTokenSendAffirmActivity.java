package com.bankledger.safecold.ui.activity.eth;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.ui.activity.QrCodePageActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAccount;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

import org.greenrobot.eventbus.EventBus;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bankledger
 * @time 2018/9/19 16:52
 */
public class ETHTokenSendAffirmActivity extends ToolbarBaseActivity {
    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;
    private TextView tvPayAddress;
    private TextView tvPayAmount;
    private TextView tvGasPrice;
    private TextView tvGasLimit;

    private HDAccount hdAccount;
    private ETHToken mETHToken;
    private String payAddress;
    private String payAmount;
    private long gasPrice;
    private long gasLimit;


    @Override
    protected int setContentLayout() {
        return R.layout.activity_ethtoken_send_affirm;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.affirm_pay);
        setDefaultNavigation();

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);
        tvPayAddress = findViewById(R.id.tv_pay_address);
        tvPayAmount = findViewById(R.id.tv_pay_amount);
        tvGasPrice = findViewById(R.id.tv_gas_price);
        tvGasLimit = findViewById(R.id.tv_gas_limit);
        findViewById(R.id.bt_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
        hdAccount = HDAddressManager.getInstance().getHDAccount();
        mETHToken = (ETHToken) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        payAddress = getIntent().getStringExtra(Constants.INTENT_KEY2);
        payAmount = getIntent().getStringExtra(Constants.INTENT_KEY3);
        gasPrice = getIntent().getLongExtra(Constants.INTENT_KEY4, 0L);
        gasLimit = getIntent().getLongExtra(Constants.INTENT_KEY5, 0L);

        ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(mETHToken.name, ConvertViewBean.TYPE_ETH_TOKEN));
        tvCurrencyName.setText(CurrencyNameUtil.getEthTokenName(mETHToken));
        tvPayAddress.setText(payAddress);
        tvPayAmount.setText(payAmount);
        tvGasPrice.setText(Long.toString(gasPrice));
        tvGasLimit.setText(Long.toString(gasLimit));

    }

    private void send() {
        DialogUtil.showEditPasswordDialog(this, R.string.input_password, R.string.password, new DialogUtil.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, final String content) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    if (TextUtils.isEmpty(content)) {
                        ToastUtil.showToast(R.string.hint_password);
                        send();
                    } else {
                        new CommonAsyncTask.Builder<String, Void, AsyncTaskResult<String>>()
                                .setIPreExecute(new IPreExecute() {
                                    @Override
                                    public void onPreExecute() {
                                        CommonUtils.showProgressDialog(ETHTokenSendAffirmActivity.this);
                                    }
                                })
                                .setIDoInBackground(new IDoInBackground<String, Void, AsyncTaskResult<String>>() {
                                    @Override
                                    public AsyncTaskResult<String> doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                                        if (mETHToken.isErc20()) {
                                            try {
                                                String methodName = "transfer";
                                                BigInteger nonce = new BigInteger(String.valueOf(mETHToken.transactionCount));
                                                BigInteger gasPriceB = Convert.toWei(BigDecimal.valueOf(gasPrice), Convert.Unit.GWEI).toBigInteger();
                                                BigInteger gasLimitB = BigInteger.valueOf(gasLimit);
                                                String toAddress = strings[0];
                                                BigInteger amount = Convert.toWei(new BigDecimal(strings[1]), Convert.Unit.ETHER).toBigInteger();
                                                log.info("-------nonce = {}", nonce);
                                                log.info("-------toAddress = {}", toAddress);
                                                log.info("-------amount = {}", amount);
                                                log.info("-------gasPrice = {}", gasPrice);
                                                log.info("-------gasLimit = {}", gasLimit);
                                                List<Type> inputParameters = new ArrayList<>();
                                                List<TypeReference<?>> outputParameters = new ArrayList<>();
                                                Address tAddress = new Address(toAddress);
                                                Uint256 tokenValue = new Uint256(new BigDecimal(strings[1]).multiply(BigDecimal.TEN.pow(mETHToken.decimals)).toBigInteger());
                                                inputParameters.add(tAddress);
                                                inputParameters.add(tokenValue);
                                                TypeReference<Bool> typeReference = new TypeReference<Bool>() {
                                                };
                                                outputParameters.add(typeReference);
                                                Function function = new Function(methodName, inputParameters, outputParameters);
                                                String data = FunctionEncoder.encode(function);
                                                log.info("-------data = {}", data);
                                                String hexCode = hdAccount.signRawTx(nonce, gasPriceB, gasLimitB, mETHToken.contractsAddress, amount, data, content, mETHToken);
                                                return new AsyncTaskResult<>(ProtoUtils.encodeSignTx(
                                                        new TransSignTx(WalletInfoManager.getWalletNumber(),
                                                                new SignTx(SafeColdSettings.ETH, hexCode))));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                return new AsyncTaskResult<>(e);
                                            }
                                        } else {
                                            try {
                                                BigInteger nonce = new BigInteger(String.valueOf(mETHToken.transactionCount));
                                                BigInteger gasPriceB = Convert.toWei(BigDecimal.valueOf(gasPrice), Convert.Unit.GWEI).toBigInteger();
                                                BigInteger gasLimitB = BigInteger.valueOf(gasLimit);
                                                String toAddress = strings[0];
                                                BigInteger amount = Convert.toWei(new BigDecimal(strings[1]), Convert.Unit.ETHER).toBigInteger();
                                                log.info("-------nonce = {}", nonce);
                                                log.info("-------toAddress = {}", toAddress);
                                                log.info("-------amount = {}", amount);
                                                log.info("-------gasPrice = {}", gasPrice);
                                                log.info("-------gasLimit = {}", gasLimit);
                                                String hexCode = hdAccount.signRawTx(nonce, gasPriceB, gasLimitB, toAddress, amount, "", content, mETHToken);
                                                return new AsyncTaskResult<>(ProtoUtils.encodeSignTx(
                                                        new TransSignTx(WalletInfoManager.getWalletNumber(),
                                                                new SignTx(mETHToken.name, hexCode))));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                return new AsyncTaskResult<>(e);
                                            }
                                        }
                                    }
                                })
                                .setIPostExecute(new IPostExecute<AsyncTaskResult<String>>() {
                                    @Override
                                    public void onPostExecute(AsyncTaskResult<String> result) {
                                        CommonUtils.dismissProgressDialog();
                                        if (result.isSuccess()) {

                                            //本地更新ETH状态
                                            BigDecimal payAmountBd = Convert.toWei(new BigDecimal(payAmount), Convert.Unit.ETHER);
                                            String balance = BigDecimalUtils.sub(mETHToken.balance, payAmountBd.toPlainString());
                                            log.info("----balance = {}, payamount = {}", mETHToken.balance, payAmountBd.toPlainString());
                                            mETHToken.balance = balance;
                                            mETHToken.transactionCount++;
                                            ETHTokenProvider.getInstance().refreshBalance(mETHToken);
                                            EventMessage message = new EventMessage(EventMessage.TYPE_BALANCE_CHANGED);
                                            EventBus.getDefault().post(message);

                                            Bundle args = new Bundle();
                                            args.putInt(Constants.INTENT_KEY1, QrCodePageActivity.START_TYPE_SEND_CURRENCY);
                                            args.putString(Constants.INTENT_KEY2, result.getResult());
                                            go2Activity(QrCodePageActivity.class, args);
                                            finish();
                                        } else {
                                            ToastUtil.showToast(R.string.password_error);
                                        }
                                    }
                                })
                                .start(payAddress, payAmount);
                    }
                }
            }
        });
    }
}
