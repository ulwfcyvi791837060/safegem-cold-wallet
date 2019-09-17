package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPreExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.db.AbstractDb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bankledger
 * @time 2018/10/11 21:37
 */
public class SelectAddressActivity extends ToolbarBaseActivity {

    private Currency mCurrency;
    private SafeAsset safeAsset;
    private RecyclerView rvList;
    private CommonAdapter<HDAddress> mAdapter;
    private Map<HDAddress, Long> hdAddressBalanceMap = new HashMap<>();

    @Override
    protected int setContentLayout() {
        return R.layout.recyclerview_list;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        rvList = findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<HDAddress>(R.layout.listitem_select_address) {
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
                if(safeAsset != null){
                    String balance = BigDecimalUtils.formatShowAmount(hdAddressBalanceMap.get(item), safeAsset.assetDecimals);
                    tvAmount.setText(balance);
                } else {
                    String balance = BigDecimalUtils.formatSatoshi2Btc(Long.toString(hdAddressBalanceMap.get(item)));
                    tvAmount.setText(balance);
                }

                TextView tvUnit = viewHolder.findViewById(R.id.tv_unit);
                tvUnit.setText(mCurrency.coin);

                TextView tvAddress = viewHolder.findViewById(R.id.tv_address);
                tvAddress.setText(item.getAddress());

            }

            @Override
            protected void onItemClick(View view, HDAddress item, int position) {
                Intent data = new Intent();
                data.putExtra(Constants.INTENT_KEY1, item);
                setResult(Constants.RESULT_SUCCESS, data);
                finish();
            }
        };
        rvList.setAdapter(mAdapter);
    }

    @Override
    public void initData() {
        super.initData();
        mCurrency = (Currency) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        if (getIntent().hasExtra(Constants.INTENT_KEY1)) {
            safeAsset = (SafeAsset) getIntent().getSerializableExtra(Constants.INTENT_KEY2);
        }
        if (safeAsset != null) {
            setTitle(safeAsset.assetName);
        } else {
            setTitle(mCurrency.coin);
        }

        getCurrencyAddress();
    }

    private void getCurrencyAddress() {
        new CommonAsyncTask.Builder<Currency, Void, List<HDAddress>>()
                .setIPreExecute(new IPreExecute() {
                    @Override
                    public void onPreExecute() {
                        CommonUtils.showProgressDialog(SelectAddressActivity.this);
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
                                if (hdAddress.isUsdt()) {
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
}
