package com.bankledger.safecold.ui.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.ui.activity.MineCurrencyAddressDetailActivity;
import com.bankledger.safecold.ui.activity.eos.MineEosAddressDetailActivity;
import com.bankledger.safecold.ui.activity.eth.MineETHTokenAddressDetailActivity;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.entity.ETHToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author bankledger
 * @time 2018/8/7 11:35
 */
public class MineAddressFragment extends BaseFragment {
    private RecyclerView rvList;
    private CommonAdapter<ConvertViewBean> mAdapter;
    private List<ConvertViewBean> allConvertViewList = new ArrayList<>();

    private List<Integer> addressCountList = new ArrayList<>();

    @Override
    public int setContentView() {
        return R.layout.recyclerview_list;
    }

    @Override
    public void initView() {
        rvList = findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvList.addItemDecoration(new RecyclerViewDivider(getContext(), LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<ConvertViewBean>(R.layout.listitem_address_book) {
            @Override
            protected void convert(ViewHolder viewHolder, ConvertViewBean item, int position) {
                ImageView ivCurrencyIcon = viewHolder.findViewById(R.id.iv_currency_icon);
                TextView tvCurrencyName = viewHolder.findViewById(R.id.tv_currency_name);
                TextView tvAddressCount = viewHolder.findViewById(R.id.tv_address_count);
                if (item.isEosBalance()) {
                    tvAddressCount.setText(String.format(getString(R.string.account_count), addressCountList.get(position)));
                } else {
                    tvAddressCount.setText(String.format(getString(R.string.currency_address_count), addressCountList.get(position)));
                }
                TextView tvSelectAddress = viewHolder.findViewById(R.id.tv_select_address);
                ivCurrencyIcon.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), item.type));
                tvCurrencyName.setText(item.getName());
                String hexAddress = null;
                if (item.isCurrency()) {
                    hexAddress = item.getAddress();
                } else if (item.isETHToken()) {
                    hexAddress = item.getAddress();
                } else if (item.isEosBalance()) {
                    EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                    hexAddress = eosAccount.getAccountName();
                }
                tvSelectAddress.setText(hexAddress);
            }

            @Override
            protected void onItemClick(View view, ConvertViewBean item, int position) {
                Bundle args = new Bundle();
                if (item.isCurrency()) {
                    args.putSerializable(Constants.INTENT_KEY1, item.currency);
                    getBaseActivity().go2Activity(MineCurrencyAddressDetailActivity.class, args);
                } else if (item.isETHToken()) {
                    args.putSerializable(Constants.INTENT_KEY1, item.ethToken);
                    getBaseActivity().go2Activity(MineETHTokenAddressDetailActivity.class, args);
                } else if (item.isEosBalance()) {
                    getBaseActivity().go2Activity(MineEosAddressDetailActivity.class);
                }
            }
        };
        rvList.setAdapter(mAdapter);
    }

    @Override
    public void initData() {
        EventBus.getDefault().register(this);
        addETHToken();
    }


    private void addETHToken() {
        allConvertViewList.clear();
        List<Currency> currencyList = HDAddressManager.getInstance().getCurrencyList();
        for (int i = 0; i < currencyList.size(); i++) {
            allConvertViewList.add(ConvertViewBean.currencyConvert(currencyList.get(i)));
        }
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
                        for (int i = 0; i < ethTokens.size(); i++) {
                            allConvertViewList.add(ConvertViewBean.ethTokenConvert(ethTokens.get(i)));
                        }
                        EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                        if (eosAccount != null) {
                            allConvertViewList.add(ConvertViewBean.eosTokenConvert(EosUsdtBalance.getZeroEosBalance()));
                        }
                        Collections.sort(allConvertViewList);
                        getCurrencyAddressCount();
                    }
                })
                .start();
    }

    public void getCurrencyAddressCount() {
        addressCountList.clear();
        new CommonAsyncTask.Builder<Void, Void, Void>()
                .setIDoInBackground(new IDoInBackground<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(IPublishProgress<Void> publishProgress, Void... params) {
                        for (int i = 0; i < allConvertViewList.size(); i++) {
                            if (allConvertViewList.get(i).isCurrency()) {
                                addressCountList.add(HDAddressProvider.getInstance().getAvailableAddressCount(allConvertViewList.get(i).currency));
                            } else if (allConvertViewList.get(i).isETHToken()) {
                                addressCountList.add(1);
                            } else if (allConvertViewList.get(i).isEosBalance()) {
                                addressCountList.add(1);
                            }
                        }
                        return null;
                    }
                })
                .setIPostExecute(new IPostExecute<Void>() {
                    @Override
                    public void onPostExecute(Void aVoid) {
                        mAdapter.replaceAll(allConvertViewList);
                    }
                })
                .start();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEventMessage(EventMessage msg) {
        if (msg.eventType == EventMessage.TYPE_MINE_ADDRESS_CHANGED || msg.eventType == EventMessage.TYPE_ACTIVE) {
            addETHToken();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
