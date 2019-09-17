package com.bankledger.safecold.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bankledger.protobuf.bean.CoinBalance;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.fragment.SynchronousBalanceDialogFragment;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.StringUtil;
import com.bankledger.safecold.utils.SyncBalanceUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BTC系列详情
 * @author bankledger
 * @time 2018/7/30 15:26
 */
public class CurrencyDetailActivity extends ToolbarBaseActivity implements View.OnClickListener {

    private Currency mCurrency;
    private SafeAsset safeAsset;
    private TextView tvAmount;
    private TextView tvUnit;
    private RecyclerView rvAddress;
    private CommonAdapter<HDAddress> mAdapter;
    private View btSend;

    private Map<HDAddress, Long> hdAddressBalanceMap = new HashMap<>();
    private SynchronousBalanceDialogFragment synDialog;


    @Override
    protected int setContentLayout() {
        return R.layout.activity_currency_detail;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();

        tvAmount = findViewById(R.id.tv_amount);
        tvUnit = findViewById(R.id.tv_unit);
        findViewById(R.id.tv_refresh_balance).setOnClickListener(this);

        rvAddress = findViewById(R.id.rv_address);
        rvAddress.setLayoutManager(new LinearLayoutManager(this));
        rvAddress.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<HDAddress>(R.layout.listitem_currency_address_detail) {
            @Override
            protected void convert(ViewHolder viewHolder, final HDAddress item, int position) {
                TextView tvPosition = viewHolder.findViewById(R.id.tv_position);
                tvPosition.setText(Integer.toString(++position));
                TextView tvAlias = viewHolder.findViewById(R.id.tv_alias);
                if (TextUtils.isEmpty(item.getAlias())) {
                    tvAlias.setText(R.string.no_alias);
                } else {
                    tvAlias.setText(item.getAlias());
                }

                TextView tvAmount = viewHolder.findViewById(R.id.tv_amount);
                if (safeAsset != null) {
                    String balance = BigDecimalUtils.formatShowAmount(hdAddressBalanceMap.get(item), safeAsset.assetDecimals);
                    tvAmount.setText(StringUtil.subZeroAndDot(balance));
                } else {
                    String balance = BigDecimalUtils.formatSatoshi2Btc(Long.toString(hdAddressBalanceMap.get(item)));
                    tvAmount.setText(balance);
                }

                TextView tvAddress = viewHolder.findViewById(R.id.tv_address);
                tvAddress.setText(item.getAddress());

                viewHolder.findViewById(R.id.bt_refresh_balance).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refreshBalance(item);
                    }
                });
            }

            @Override
            protected void onItemClick(View view, HDAddress item, int position) {
                log.info("-----item address = {}, click = {}", item.getAddress(), Utils.bytesToHexString(item.getPubKey()));
            }
        };
        rvAddress.setAdapter(mAdapter);

        btSend = findViewById(R.id.bt_send);
        btSend.setOnClickListener(this);
        findViewById(R.id.bt_receive).setOnClickListener(this);
    }

    @Override
    public void initData() {
        super.initData();
        EventBus.getDefault().register(this);
        mCurrency = (Currency) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        if (getIntent().hasExtra(Constants.INTENT_KEY2)) {
            safeAsset = (SafeAsset) getIntent().getSerializableExtra(Constants.INTENT_KEY2);
        }
        if (safeAsset != null) {
            setTitle(safeAsset.assetName);
            tvUnit.setText(safeAsset.assetUnit);
        } else {
            setTitle(CurrencyNameUtil.getCurrencyName(mCurrency));
            tvUnit.setText(CurrencyNameUtil.getCurrencyName(mCurrency));
        }
        getBalance();
        getCurrencyAddress();
    }

    private void getCurrencyAddress() {
        new CommonAsyncTask.Builder<Currency, Void, List<HDAddress>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(CurrencyDetailActivity.this);
                    }
                })
                .setIDoInBackground(new IDoInBackground<Currency, Void, List<HDAddress>>() {
                    @Override
                    public List<HDAddress> doInBackground(IPublishProgress<Void> publishProgress, Currency... currency) {
                        return AbstractDb.hdAddressProvider.getHDAddressListWithAvailable(currency[0], HDAddress.AvailableState.STATE_AVAILABLE);
                    }
                })
                .setIPostExecute(new IPostExecute<List<HDAddress>>() {
                    @Override
                    public void onPostExecute(List<HDAddress> hdAddresses) {
                        checkAddressBalance(hdAddresses);
                    }
                })
                .start(mCurrency);
    }

    private void checkAddressBalance(final List<HDAddress> hdAddresses) {
        HDAddress[] arrHDAddress = new HDAddress[hdAddresses.size()];
        hdAddresses.toArray(arrHDAddress);
        new CommonAsyncTask.Builder<HDAddress, Void, Map<HDAddress, Long>>()
                .setIDoInBackground(new IDoInBackground<HDAddress, Void, Map<HDAddress, Long>>() {
                    @Override
                    public Map<HDAddress, Long> doInBackground(IPublishProgress<Void> publishProgress, HDAddress... addresses) {
                        Map<HDAddress, Long> addressMap = new HashMap<>(addresses.length);
                        for (HDAddress hdAddress : addresses) {
                            if (safeAsset != null) {
                                addressMap.put(hdAddress, OutProvider.getInstance().getBalanceWithAddressSafeAsset(hdAddress.getAddress(), safeAsset.assetId));
                            } else {
                                if (mCurrency.isUsdt()) {
                                    addressMap.put(hdAddress, BigDecimalUtils.unitToSatoshiL(EosUsdtBalanceProvider.getInstance().getUsdtBalance()));
                                } else {
                                    addressMap.put(hdAddress, OutProvider.getInstance().getBalanceWithAddress(hdAddress.getAddress()));
                                }
                            }
                        }
                        return addressMap;
                    }
                })
                .setIPostExecute(new IPostExecute<Map<HDAddress, Long>>() {
                    @Override
                    public void onPostExecute(Map<HDAddress, Long> addressMap) {
                        CommonUtils.dismissProgressDialog();
                        hdAddressBalanceMap.clear();
                        hdAddressBalanceMap.putAll(addressMap);
                        mAdapter.replaceAll(hdAddresses);
                    }
                })
                .start(arrHDAddress);
    }


    private void refreshBalance(HDAddress hdAddress) {
        synDialog = SynchronousBalanceDialogFragment.newInstance(hdAddress);
        synDialog.show(getSupportFragmentManager(), "syn");
    }

    private void getBalance() {
        if (safeAsset != null) {
            new CommonAsyncTask.Builder<String, Void, Long>()
                    .setIDoInBackground(new IDoInBackground<String, Void, Long>() {
                        @Override
                        public Long doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                            return OutProvider.getInstance().getBalanceWithCoinSafeAsset(SafeColdSettings.SAFE, strings[0]);
                        }
                    })
                    .setIPostExecute(new IPostExecute<Long>() {
                        @Override
                        public void onPostExecute(Long amountLong) {
                            String balance = BigDecimalUtils.formatShowAmount(amountLong, safeAsset.assetDecimals);
                            tvAmount.setText(StringUtil.subZeroAndDot(balance));
                        }
                    })
                    .start(safeAsset.assetId);
        } else {
            new CommonAsyncTask.Builder<String, Void, Long>()
                    .setIDoInBackground(new IDoInBackground<String, Void, Long>() {
                        @Override
                        public Long doInBackground(IPublishProgress<Void> publishProgress, String... strings) {
                            if (mCurrency.isUsdt()) {
                                String balance = EosUsdtBalanceProvider.getInstance().getUsdtBalance();
                                return BigDecimalUtils.unitToSatoshiL(balance);
                            } else {
                                return OutProvider.getInstance().getBalanceWithCoin(strings[0]);
                            }
                        }
                    })
                    .setIPostExecute(new IPostExecute<Long>() {
                        @Override
                        public void onPostExecute(Long amountLong) {
                            tvAmount.setText(BigDecimalUtils.formatSatoshi2Btc(Long.toString(amountLong)));
                        }
                    })
                    .start(mCurrency.coin);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_refresh_balance://更新余额
                verifyPermissions(Manifest.permission.CAMERA, new BaseActivity.PermissionCallBack() {
                    @Override
                    public void onGranted() {
                        Bundle args = new Bundle();
                        args.putString(Constants.INTENT_KEY1, getString(R.string.scan_sync_balance));
                        go2ActivityForResult(ScanActivity.class, Constants.REQUEST_CODE1, args);
                    }
                    @Override
                    public void onDenied() {

                    }
                });
                break;
            case R.id.bt_send://发送
                Bundle args3 = new Bundle();
                args3.putSerializable(Constants.INTENT_KEY1, mCurrency);
                args3.putString(Constants.INTENT_KEY2, "");
                args3.putString(Constants.INTENT_KEY3, "");
                if (safeAsset != null) {
                    args3.putSerializable(Constants.INTENT_KEY4, safeAsset);
                }
                go2Activity(CurrencySendActivity.class, args3);
                break;
            case R.id.bt_receive://接收
                Bundle args4 = new Bundle();
                if (safeAsset != null) {
                    args4.putSerializable(Constants.INTENT_KEY1, ConvertViewBean.safeAssetConvert(safeAsset));
                } else {
                    args4.putSerializable(Constants.INTENT_KEY1, ConvertViewBean.currencyConvert(mCurrency));
                }
                go2Activity(CurrencyReceiveActivity.class, args4);
                break;
            default:
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEventMessage(EventMessage msg) {
        if (msg.eventType == EventMessage.TYPE_BALANCE_CHANGED) {
            getBalance();
            getCurrencyAddress();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE1 && resultCode == Constants.RESULT_SUCCESS) {
            if (synDialog != null)
                synDialog.dismiss();

            new QrProtocolUtil()
                    .setIBalanceDecode(new QrProtocolUtil.IBalanceDecode() {
                        @Override
                        public void onBalanceDecode(List<CoinBalance> coinBalanceList) {
                            SyncBalanceUtil.replaceOuts(coinBalanceList, new SyncBalanceUtil.OnSyncListener() {
                                @Override
                                public void onSync() {
                                    getBalance();
                                    getCurrencyAddress();
                                }
                            });
                        }
                    })
                    .setIDecodeFail(new QrProtocolUtil.IDecodeFail() {
                        @Override
                        public void onQrDecodeFail(String errMsg) {
                            ToastUtil.showToast(errMsg);
                        }

                        @Override
                        public void onProtocolUpgrade(boolean isSelf) {
                            DialogUtil.showProtocolUpdateDilog(CurrencyDetailActivity.this, isSelf);
                        }
                    })
                    .decode(data.getStringExtra(Constants.INTENT_KEY1));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
