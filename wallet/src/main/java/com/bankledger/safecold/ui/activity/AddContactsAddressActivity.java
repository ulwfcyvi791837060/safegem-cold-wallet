package com.bankledger.safecold.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.AsyncTaskResult;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.db.ContactsAddressProvider;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.widget.CommonEditWidget;
import com.bankledger.safecold.ui.widget.DigitsInputFilter;
import com.bankledger.safecold.ui.widget.EditLengthInputFilter;
import com.bankledger.safecold.ui.widget.EosDigitsInputFilter;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.utils.Utils;

import org.greenrobot.eventbus.EventBus;

/**
 * 添加联系人地址
 *
 * @author bankledger
 * @time 2018/8/8 14:56
 */
public class AddContactsAddressActivity extends ToolbarBaseActivity {

    private ImageView ivCurrencyIcon;
    private TextView tvCurrencyName;
    private CommonEditWidget cewAddress;
    private CommonEditWidget cewAlias;
    private ConvertViewBean selectConvertView;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_add_contact_address;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.add_address);

        ivCurrencyIcon = findViewById(R.id.iv_currency_icon);
        tvCurrencyName = findViewById(R.id.tv_currency_name);
        tvCurrencyName.setText(R.string.select_currency);
        findViewById(R.id.ll_currency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2ActivityForResult(SelectCurrencyActivity.class, Constants.REQUEST_CODE2);
            }
        });

        cewAddress = findViewById(R.id.cew_address);
        cewAddress.setHint(R.string.wallet_address);
        cewAddress.setRightImageResource(R.mipmap.scan_qr);
        cewAddress.getEditText().setTextSize(14);
        cewAddress.getRightImageView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPermissions(Manifest.permission.CAMERA, new BaseActivity.PermissionCallBack() {
                    @Override
                    public void onGranted() {
                        go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE1);
                    }

                    @Override
                    public void onDenied() {
                    }
                });
            }
        });
        cewAddress.setHint(R.string.wallet_address);
        cewAddress.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(120), new DigitsInputFilter()});

        cewAlias = findViewById(R.id.cew_alias);
        cewAlias.setHint(R.string.remark);
        cewAlias.getEditText().setFilters(new InputFilter[]{new EditLengthInputFilter(12)});
        cewAlias.getEditText().setTextSize(14);
    }

    private void checkAddressExist(final ContactsAddress contactsAddress) {
        new CommonAsyncTask.Builder<ContactsAddress, Void, AsyncTaskResult<String>>()
                .setIDoInBackground(new IDoInBackground<ContactsAddress, Void, AsyncTaskResult<String>>() {
                    @Override
                    public AsyncTaskResult<String> doInBackground(IPublishProgress<Void> publishProgress, ContactsAddress... addresses) {
                        if (ContactsAddressProvider.getInstance().checkContactsAddressExist(addresses[0])) {
                            return new AsyncTaskResult<>(getString(R.string.exist_address));
                        } else {
                            return new AsyncTaskResult<>(false, null);
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<AsyncTaskResult<String>>() {
                    @Override
                    public void onPostExecute(AsyncTaskResult<String> result) {
                        if (!result.isSuccess()) {
                            addContactAddress(contactsAddress);
                        } else {
                            DialogUtil.showTextDialog(AddContactsAddressActivity.this, getString(R.string.tip), result.getResult(), false, null);
                        }
                    }
                })
                .start(contactsAddress);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.complete_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_complete) {
            if (selectConvertView == null) {
                ToastUtil.showToast(R.string.select_address);
                return super.onOptionsItemSelected(item);
            }
            if (TextUtils.isEmpty(cewAddress.getText())) {
                ToastUtil.showToast(R.string.input_address);
                return super.onOptionsItemSelected(item);
            }

            ContactsAddress contactsAddress = null;
            String alias = cewAlias.getText().replace("\n", " ").replace("\r", " ");
            if (selectConvertView.isCurrency()) {
                contactsAddress = ContactsAddress.createCurrencyContactsAddress(
                        selectConvertView.currency.coin,
                        cewAddress.getText(),
                        alias
                );
            } else if (selectConvertView.isETHToken() && selectConvertView.ethToken.isEth()) {
                contactsAddress = ContactsAddress.createETHContactsAddress(
                        selectConvertView.ethToken.name,
                        cewAddress.getText(),
                        alias,
                        selectConvertView.ethToken.ethAddress);
            } else if (selectConvertView.isETHToken() && selectConvertView.ethToken.isEtc()) {
                contactsAddress = ContactsAddress.createETCContactsAddress(
                        selectConvertView.ethToken.name,
                        cewAddress.getText(),
                        alias,
                        selectConvertView.ethToken.ethAddress);
            } else if (selectConvertView.isETHToken() && selectConvertView.ethToken.isErc20()) {
                contactsAddress = ContactsAddress.createTokenContactsAddress(
                        selectConvertView.ethToken.symbol,
                        cewAddress.getText(),
                        alias,
                        selectConvertView.ethToken.contractsAddress);
            } else if (selectConvertView.isEosBalance()) {
                if (Utils.isEos(selectConvertView.getCoin())) {
                    contactsAddress = ContactsAddress.createEosContactsAccount(cewAddress.getText(),
                            alias);
                } else {
                    contactsAddress = ContactsAddress.createEosTokenContactsAccount(cewAddress.getText(), selectConvertView.getCoin(),
                            alias);
                }
            } else if (selectConvertView.isSafeAsset()) {
                contactsAddress = ContactsAddress.createSafeAssetContactsAddress(
                        selectConvertView.safeAsset.assetName,
                        cewAddress.getText(),
                        alias
                );
            }

            if (contactsAddress != null)
                checkAddressExist(contactsAddress);
        }
        return super.onOptionsItemSelected(item);
    }

    private void addContactAddress(ContactsAddress contactsAddress) {
        new CommonAsyncTask.Builder<ContactsAddress, Void, Void>()
                .setIDoInBackground(new IDoInBackground<ContactsAddress, Void, Void>() {
                    @Override
                    public Void doInBackground(IPublishProgress<Void> publishProgress, ContactsAddress... addresses) {
                        ContactsAddressProvider.getInstance().addContactsAddress(addresses[0]);
                        return null;
                    }
                })
                .setIPostExecute(new IPostExecute<Void>() {
                    @Override
                    public void onPostExecute(Void aVoid) {
                        EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_CONTACT_ADDRESS_CHANGED));
                        finish();
                    }
                })
                .start(contactsAddress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Constants.RESULT_SUCCESS) {
            if (requestCode == Constants.REQUEST_CODE1) {
                final String address = data.getStringExtra(Constants.INTENT_KEY1);
                new QrProtocolUtil()
                        .setICoinAddressDecode(new QrProtocolUtil.ICoinAddressDecode() {
                            @Override
                            public void onCoinAddressDecode(UriDecode uriDecode) {
                                String fullName = uriDecode.scheme;
                                if (Utils.fullNameIsBtc(fullName)) {
                                    cewAddress.setText(uriDecode.path);
                                    Currency currency = Utils.findCurrencyForFullName(fullName);
                                    if (currency != null) {
                                        if (uriDecode.params != null && uriDecode.params.get("token") != null) {
                                            String assetName = uriDecode.params.get("token");
                                            SafeAsset safeAsset = OutProvider.getInstance().getSafeAsset(assetName);
                                            if (safeAsset != null) {
                                                selectConvertView = ConvertViewBean.safeAssetConvert(safeAsset);
                                            }
                                        } else {
                                            selectConvertView = ConvertViewBean.currencyConvert(currency);
                                        }
                                        refreshCurrencyView();
                                    }
                                } else if (Utils.fullNameIsEth(fullName)) {
                                    cewAddress.setText(uriDecode.path);
                                    if (uriDecode.params != null && uriDecode.params.get("token") != null) {
                                        String token = uriDecode.params.get("token");
                                        selectConvertView = ConvertViewBean.ethTokenConvert(ETHTokenProvider.getInstance().queryETHTokenWithContractAddress(token));
                                    } else {
                                        selectConvertView = ConvertViewBean.ethTokenConvert(ETHTokenProvider.getInstance().queryETH());
                                    }
                                    refreshCurrencyView();
                                } else if (Utils.fullNameIsEtc(fullName)) {
                                    cewAddress.setText(uriDecode.path);
                                    selectConvertView = ConvertViewBean.ethTokenConvert(ETHTokenProvider.getInstance().queryETC());
                                    refreshCurrencyView();
                                } else if (Utils.fullNameIsEos(fullName)) {
                                    cewAddress.setText(uriDecode.path);
                                    selectConvertView = ConvertViewBean.eosTokenConvert(EosUsdtBalanceProvider.getInstance().getEosBalance());
                                    if (uriDecode.params != null && uriDecode.params.get("token") != null) {
                                        String token = uriDecode.params.get("token");
                                        selectConvertView = ConvertViewBean.eosTokenConvert(EosUsdtBalanceProvider.getInstance().getEosBalance(token));
                                    } else {
                                        selectConvertView = ConvertViewBean.eosTokenConvert(EosUsdtBalanceProvider.getInstance().getEosBalance());
                                    }
                                    refreshCurrencyView();
                                } else {
                                    cewAddress.setText(uriDecode.path);
                                }
                            }
                        })
                        .setIDecodeFail(new QrProtocolUtil.IDecodeFail() {
                            @Override
                            public void onQrDecodeFail(String errMsg) {
                                cewAddress.setText(address);
                            }

                            @Override
                            public void onProtocolUpgrade(boolean isSelf) {

                            }
                        })
                        .decode(address);
            } else if (requestCode == Constants.REQUEST_CODE2) {
                selectConvertView = (ConvertViewBean) data.getSerializableExtra(Constants.INTENT_KEY1);
                refreshCurrencyView();
                cewAddress.setText(null);
            }
        }
    }

    private void refreshCurrencyView() {
        if (selectConvertView == null) return;
        tvCurrencyName.setText(selectConvertView.getName());
        ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(selectConvertView.getCoin(), selectConvertView.type));
        if (selectConvertView.isEosBalance()) {
            cewAddress.setHint(R.string.eos_account);
            cewAddress.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(12), new EosDigitsInputFilter()});
        } else {
            cewAddress.setHint(R.string.wallet_address);
            cewAddress.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(120), new DigitsInputFilter()});
        }
    }
}
