package com.bankledger.safecold.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.bankledger.protobuf.bean.Address;
import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.protobuf.bean.TransMulAddress;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bankledger
 * @time 2018/7/26 15:09
 */
public class MonitorAddressActivity extends ToolbarBaseActivity {

    private RecyclerView rvCurrency;
    private CommonAdapter<ConvertViewBean> mAdapter;
    private List<ConvertViewBean> currencyList = new ArrayList<>();
    private List<Boolean> currencyCheckedList;

    private MenuItem menuCheckAll;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_monitor_address;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.monitoring_account);
        setDefaultNavigation();

        rvCurrency = findViewById(R.id.rv_currency);

        rvCurrency = findViewById(R.id.rv_currency);
        rvCurrency.setLayoutManager(new LinearLayoutManager(this));
        rvCurrency.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<ConvertViewBean>(R.layout.listitem_select_currnecy) {
            @Override
            protected void convert(ViewHolder viewHolder, ConvertViewBean item, final int position) {
                CommonTextWidget ctwCurrency = viewHolder.findViewById(R.id.ctw_currency);
                ctwCurrency.setBackgroundColor(getColor(R.color.white));
                ctwCurrency.setLeftImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), item.type));
                ctwCurrency.setLeftText(item.getName());
                ctwCurrency.setRightView(R.layout.public_checkbox);
                CheckBox cbCurrency = ctwCurrency.findViewById(R.id.cb_currency);
                cbCurrency.setChecked(currencyCheckedList.get(position));
                cbCurrency.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        currencyCheckedList.set(position, isChecked);
                        menuCheckAll.setTitle(isAllChecked() ? R.string.all_dont_check : R.string.check_all);
                    }
                });
            }

            @Override
            protected void onItemClick(View view, ConvertViewBean item, int position) {

            }
        };
        rvCurrency.setAdapter(mAdapter);

        findViewById(R.id.bt_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasChecked()) {
                    ToastUtil.showToast(R.string.select_export_currency);
                } else {
                    getQrContent();
                }
            }
        });
    }

    @Override
    public void initData() {
        super.initData();
        List<Currency> currencys = HDAddressManager.getInstance().getCurrencyList();
        for (Currency item : currencys) {
            currencyList.add(ConvertViewBean.currencyConvert(item));
        }
        currencyCheckedList = new ArrayList<>(currencyList.size());
        addETHToken();
    }

    private void addETHToken() {
        new CommonAsyncTask.Builder<Void, Void, List<ETHToken>>()
                .setIDoInBackground(new IDoInBackground<Void, Void, List<ETHToken>>() {
                    @Override
                    public List<ETHToken> doInBackground(IPublishProgress<Void> publishProgress, Void... aVoid) {
                        List<ETHToken> ethTokens = new ArrayList<>(2);
                        ethTokens.add(ETHTokenProvider.getInstance().queryETC());
                        ethTokens.add(ETHTokenProvider.getInstance().queryETH());
                        return ethTokens;
                    }
                })
                .setIPostExecute(new IPostExecute<List<ETHToken>>() {
                    @Override
                    public void onPostExecute(List<ETHToken> ethToken) {
                        for (ETHToken item : ethToken) {
                            currencyList.add(ConvertViewBean.ethTokenConvert(item));
                        }
                        EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                        if(eosAccount != null){
                            currencyList.add(ConvertViewBean.eosTokenConvert(EosUsdtBalance.getZeroEosBalance()));
                        }
                        Collections.sort(currencyList);
                        for (int i = 0; i < currencyList.size(); i++) {
                            currencyCheckedList.add(i, false);
                        }
                        mAdapter.replaceAll(currencyList);
                    }
                })
                .start();
    }

    private boolean hasChecked() {
        for (Boolean isChecked : currencyCheckedList) {
            if (isChecked) return true;
        }
        return false;
    }

    private boolean isAllChecked() {
        for (Boolean isChecked : currencyCheckedList) {
            if (!isChecked) return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.check_all_menu, menu);
        menuCheckAll = menu.findItem(R.id.menu_check_all);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_check_all) {
            setAllChecked(!isAllChecked());
        }
        return super.onOptionsItemSelected(item);
    }

    private void setAllChecked(boolean check) {
        for (int i = 0; i < currencyCheckedList.size(); i++) {
            currencyCheckedList.set(i, check);
        }
        menuCheckAll.setTitle(check ? R.string.all_dont_check : R.string.check_all);
        mAdapter.notifyView();
    }

    public void getQrContent() {
        ArrayList<ConvertViewBean> selectCurrencyList = new ArrayList<>();
        for (int i = 0; i < currencyCheckedList.size(); i++) {
            if (currencyCheckedList.get(i)) {
                selectCurrencyList.add(currencyList.get(i));
            }
        }
        ConvertViewBean[] selectCurrencyArr = new ConvertViewBean[selectCurrencyList.size()];
        selectCurrencyList.toArray(selectCurrencyArr);
        new CommonAsyncTask.Builder<ConvertViewBean, Void, String>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(MonitorAddressActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<ConvertViewBean, Void, String>() {
                    @Override
                    public String doInBackground(IPublishProgress<Void> publishProgress, ConvertViewBean... convertViewBeans) {
                        List<Address> addressList = new ArrayList<>();
                        HDAddressProvider hdAddressProvider = HDAddressProvider.getInstance();
                        for (ConvertViewBean item : convertViewBeans) {
                            if (item.isCurrency()) {
                                List<HDAddress> cAddressList = hdAddressProvider.getHDAddressListWithAvailable(item.currency, HDAddress.AvailableState.STATE_AVAILABLE);
                                for (HDAddress hdAddressItem : cAddressList) {
                                    if (hdAddressItem.isUsdt()) {
                                        addressList.add(new Address(hdAddressItem.getCoin(), hdAddressItem.getAddress(), 6, hdAddressItem.getAddress()));
                                    } else {
                                        addressList.add(new Address(hdAddressItem.getCoin(), hdAddressItem.getAddress(), 0, hdAddressItem.getAddress()));
                                    }
                                }
                            } else if (item.isEosBalance()) {
                                EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                                if (eosAccount != null) {
                                    addressList.add(new Address(item.eosBalance.tokenName, eosAccount.getAccountName(), 4, eosAccount.getAccountName()));
                                }
                            } else {
                                int isToken = item.ethToken.isEth() ? 1 : item.ethToken.isErc20() ? 2 : 3;
                                addressList.add(new Address(item.ethToken.name, item.ethToken.ethAddress, isToken, item.ethToken.contractsAddress));
                            }

                        }
                        return ProtoUtils.encodeMonitor(new TransMulAddress(WalletInfoManager.getWalletNumber(), addressList));
                    }
                })
                .setIPostExecute(new IPostExecute<String>() {
                    @Override
                    public void onPostExecute(String content) {
                        CommonUtils.dismissProgressDialog();
                        Bundle args = new Bundle();
                        args.putInt(Constants.INTENT_KEY1, QrCodePageActivity.START_TYPE_MONITOR);
                        args.putString(Constants.INTENT_KEY2, content);
                        go2Activity(QrCodePageActivity.class, args);
                    }
                })
                .start(selectCurrencyArr);
    }

}
