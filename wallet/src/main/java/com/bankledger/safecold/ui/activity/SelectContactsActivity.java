package com.bankledger.safecold.ui.activity;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.ContactsAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bankledger
 * @time 2018/8/16 16:56
 */
public class SelectContactsActivity extends ToolbarBaseActivity {
    private RecyclerView rvList;
    private CommonAdapter<ContactsAddress> mAdapter;
    private View includeNotData;
    private ConvertViewBean convertView;
    private SafeAsset safeAsset;

    @Override
    protected int setContentLayout() {
        return R.layout.recyclerview_list;
    }

    @Override
    public void initView() {
        super.initView();
        setDefaultNavigation();
        setTitle(R.string.select_contacts);

        includeNotData = findViewById(R.id.include_not_data);

        rvList = findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
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
                } else if (item.isETHAddress() || item.isTokenAddress()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(SafeColdSettings.ETH, ConvertViewBean.TYPE_ETH_TOKEN));
                } else if (item.isETCAddress()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(SafeColdSettings.ETC, ConvertViewBean.TYPE_CURRENCY));
                } else if (item.isEosAddress()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(SafeColdSettings.EOS, ConvertViewBean.TYPE_EOS_COIN));
                } else if (item.isSafeAsset()) {
                    ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), ConvertViewBean.TYPE_SAFE_ASSET));
                }
                TextView tvCurrencyName = viewHolder.findViewById(R.id.tv_currency_name);
                tvCurrencyName.setText(item.getCoin());
                TextView tvAlias = viewHolder.findViewById(R.id.tv_address_count);
                tvAlias.setText(item.getAlias());
                TextView tvSelectAddress = viewHolder.findViewById(R.id.tv_select_address);
                String hexAddress = item.getAddress();
                tvSelectAddress.setText(hexAddress);

            }

            @Override
            protected void onItemClick(View view, ContactsAddress item, int position) {
                Intent intent = new Intent();
                intent.putExtra(Constants.INTENT_KEY1, item);
                setResult(Constants.RESULT_SUCCESS, intent);
                finish();
            }
        };
        rvList.setAdapter(mAdapter);
    }

    @Override
    public void initData() {
        super.initData();
        convertView = (ConvertViewBean) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        if (getIntent().hasExtra(Constants.INTENT_KEY2)) {
            safeAsset = (SafeAsset) getIntent().getSerializableExtra(Constants.INTENT_KEY2);
        }
        getContactAddress();
    }

    public void getContactAddress() {
        new CommonAsyncTask.Builder<Void, Void, List<ContactsAddress>>()
                .setIDoInBackground(new IDoInBackground<Void, Void, List<ContactsAddress>>() {
                    @Override
                    public List<ContactsAddress> doInBackground(IPublishProgress<Void> publishProgress, Void... params) {
                        List<ContactsAddress> list = new ArrayList();
                        if (safeAsset != null) {
                            list.addAll(ContactsAddressProvider.getInstance().getSafeAssetAddressWithCoin(safeAsset.assetName));
                        } else {
                            if (convertView.isCurrency()) {
                                list.addAll(ContactsAddressProvider.getInstance().getCurrencyAddressWithCoin(convertView.currency.coin));
                            } else if (convertView.isETHToken()) {
                                list.addAll(ContactsAddressProvider.getInstance().getAddressWithContractAddress(convertView.ethToken.contractsAddress));
                            } else if (convertView.isEosBalance()) {
                                list.addAll(ContactsAddressProvider.getInstance().getEosAddress());
                            }
                        }
                        return list;
                    }
                })
                .setIPostExecute(new IPostExecute<List<ContactsAddress>>() {
                    @Override
                    public void onPostExecute(List<ContactsAddress> hdAddresses) {
                        mAdapter.replaceAll(hdAddresses);
                        includeNotData.setVisibility(hdAddresses.size() == 0 ? View.VISIBLE : View.GONE);
                        rvList.setVisibility(hdAddresses.size() == 0 ? View.GONE : View.VISIBLE);
                    }
                })
                .start();
    }
}
