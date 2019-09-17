package com.bankledger.safecold.ui.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.db.ContactsAddressProvider;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.ui.activity.ContactsAddressDetailActivity;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.utils.GsonUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * $desc
 * 联系人地址
 *
 * @author bankledger
 * @time 2018/8/7 11:35
 */
public class ContactsAddressFragment extends BaseFragment {
    private RecyclerView rvList;
    private CommonAdapter<ContactsAddress> mAdapter;
    private ArrayList<ConvertViewBean> allConvertViewList = new ArrayList<>();
    private View includeNotData;

    @Override
    public int setContentView() {
        return R.layout.recyclerview_list;
    }

    @Override
    public void initView() {
        includeNotData = findViewById(R.id.include_not_data);

        rvList = findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvList.addItemDecoration(new RecyclerViewDivider(getContext(), LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<ContactsAddress>(R.layout.listitem_address_book) {

            @Override
            public int getItemViewType(int position) {
                if (position == 0) {
                    return 0;
                } else {
                    boolean coinEquals = data.get(position).getCoin().equals(data.get(position - 1).getCoin());
                    boolean contractAddressEquals = data.get(position).getContractAddress() != null && !data.get(position).getContractAddress().equals(data.get(position - 1).getContractAddress());
                    if (!coinEquals || contractAddressEquals) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                if (viewType == 0) {
                    return super.onCreateViewHolder(parent, viewType);
                } else {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_address_book_line, parent, false);
                    return new ViewHolder(view);
                }
            }

            @Override
            protected void convert(ViewHolder viewHolder, ContactsAddress item, int position) {
                ImageView ivCurrencyIcon = viewHolder.findViewById(R.id.iv_currency_icon);
                if (item.isCurrencyAddress()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), ConvertViewBean.TYPE_CURRENCY));
                } else if (item.isETHAddress() || item.isETCAddress() || item.isTokenAddress()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), ConvertViewBean.TYPE_ETH_TOKEN));
                } else if (item.isEosAddress()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), ConvertViewBean.TYPE_EOS_COIN));
                } else if (item.isEosTokenAddress()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), ConvertViewBean.TYPE_EOS_COIN));
                } else if (item.isSafeAsset()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), ConvertViewBean.TYPE_SAFE_ASSET));
                }
                TextView tvCurrencyName = viewHolder.findViewById(R.id.tv_currency_name);
                tvCurrencyName.setText(CurrencyNameUtil.getContactsCoinName(item));
                TextView tvAlias = viewHolder.findViewById(R.id.tv_address_count);
                tvAlias.setText(item.getAlias());
                TextView tvSelectAddress = viewHolder.findViewById(R.id.tv_select_address);
                String hexAddress = item.getAddress();
                tvSelectAddress.setText(hexAddress);
            }

            @Override
            protected void onItemClick(View view, ContactsAddress item, int position) {
                Bundle args = new Bundle();
                args.putSerializable(Constants.INTENT_KEY1, item);
                go2Activity(ContactsAddressDetailActivity.class, args);
            }
        };
        rvList.setAdapter(mAdapter);
    }

    @Override
    public void initData() {
        EventBus.getDefault().register(this);
        checkCurrency();
    }

    private void checkCurrency() {
        allConvertViewList.clear();
        for (Currency currency : HDAddressManager.getInstance().getCurrencyList()) {
            allConvertViewList.add(ConvertViewBean.currencyConvert(currency));
        }
        addSafeAsset();
    }

    private void addSafeAsset() {
        new CommonAsyncTask.Builder<Void, Void, List<SafeAsset>>()
                .setIDoInBackground(new IDoInBackground<Void, Void, List<SafeAsset>>() {
                    @Override
                    public List<SafeAsset> doInBackground(IPublishProgress<Void> publishProgress, Void... aVoid) {
                        List<String> assetList = OutProvider.getInstance().getSafeAsset();
                        List<SafeAsset> safeAssetList = new ArrayList<>();
                        for (String item : assetList) {
                            SafeAsset safeAsset = GsonUtils.getObjFromJSON(item, SafeAsset.class);
                            safeAssetList.add(safeAsset);
                        }
                        return safeAssetList;
                    }
                })
                .setIPostExecute(new IPostExecute<List<SafeAsset>>() {
                    @Override
                    public void onPostExecute(List<SafeAsset> safeAssets) {
                        for (int i = 0; i < safeAssets.size(); i++) {
                            allConvertViewList.add(ConvertViewBean.safeAssetConvert(safeAssets.get(i)));
                        }
                        addETHToken();
                    }
                })
                .start();
    }

    private void addETHToken() {
        new CommonAsyncTask.Builder<Void, Void, List<ETHToken>>()
                .setIDoInBackground(new IDoInBackground<Void, Void, List<ETHToken>>() {
                    @Override
                    public List<ETHToken> doInBackground(IPublishProgress<Void> publishProgress, Void... aVoid) {
                        return ETHTokenProvider.getInstance().queryAll();
                    }
                })
                .setIPostExecute(new IPostExecute<List<ETHToken>>() {
                    @Override
                    public void onPostExecute(List<ETHToken> ethTokens) {
                        for (ETHToken item : ethTokens) {
                            allConvertViewList.add(ConvertViewBean.ethTokenConvert(item));
                        }
                        allConvertViewList.add(ConvertViewBean.eosTokenConvert(EosUsdtBalance.getZeroEosBalance()));
                        Collections.sort(allConvertViewList);
                        getContactAddress();
                    }
                })
                .start();
    }

    public void getContactAddress() {
        new CommonAsyncTask.Builder<Void, Void, List<ContactsAddress>>()
                .setIDoInBackground(new IDoInBackground<Void, Void, List<ContactsAddress>>() {
                    @Override
                    public List<ContactsAddress> doInBackground(IPublishProgress<Void> publishProgress, Void... params) {
                        List<ContactsAddress> list = new ArrayList();
                        for (ConvertViewBean item : allConvertViewList) {
                            if (item.isCurrency()) {
                                list.addAll(ContactsAddressProvider.getInstance().getCurrencyAddressWithCoin(item.currency.coin));
                            } else if (item.isETHToken()) {
                                list.addAll(ContactsAddressProvider.getInstance().getAddressWithContractAddress(item.ethToken.contractsAddress));
                            } else if (item.isEosBalance()) {
                                list.addAll(ContactsAddressProvider.getInstance().getEosAddress());
                                list.addAll(ContactsAddressProvider.getInstance().getEosTokenAddress());
                            } else if (item.isSafeAsset()) {
                                list.addAll(ContactsAddressProvider.getInstance().getSafeAssetAddressWithCoin(item.safeAsset.assetName));
                            }
                        }
                        return list;
                    }
                })
                .setIPostExecute(new IPostExecute<List<ContactsAddress>>() {
                    @Override
                    public void onPostExecute(List<ContactsAddress> contactsAddress) {
                        mAdapter.replaceAll(contactsAddress);
                        includeNotData.setVisibility(contactsAddress.size() == 0 ? View.VISIBLE : View.GONE);
                        rvList.setVisibility(contactsAddress.size() == 0 ? View.GONE : View.VISIBLE);
                    }
                })
                .start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEventMessage(EventMessage msg) {
        if (msg.eventType == EventMessage.TYPE_CONTACT_ADDRESS_CHANGED || msg.eventType == EventMessage.TYPE_ACTIVE) {
            checkCurrency();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
