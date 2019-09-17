package com.bankledger.safecold.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.utils.GsonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * $desc
 * 选择币种
 *
 * @author bankledger
 * @time 2018/7/24 15:29
 */
public class SelectCurrencyActivity extends ToolbarBaseActivity {

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_MESSAGE_SIGN = 1;

    private SearchView svSelectCurrency;
    private RecyclerView rvCurrency;
    private CommonAdapter<ConvertViewBean> mAdapter;
    private List<ConvertViewBean> allConvertViewList = new ArrayList<>();
    private List<ConvertViewBean> filterConvertViewList = new ArrayList<>();
    private AppBarLayout ablCurrency;
    private View includeNotData;
    private int selectType;

    @Override
    protected int setContentLayout() {
        return R.layout.activity_select_currency;
    }

    @Override
    public void initView() {
        super.initView();
        setTitle(R.string.select_currency);
        setDefaultNavigation();

        ablCurrency = findViewById(R.id.abl_currency);
        rvCurrency = findViewById(R.id.rv_currency);
        rvCurrency.setLayoutManager(new LinearLayoutManager(this));
        rvCurrency.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<ConvertViewBean>(R.layout.listitem_select_currnecy) {
            @Override
            protected void convert(ViewHolder viewHolder, ConvertViewBean item, int position) {
                CommonTextWidget ctwCurrency = viewHolder.findViewById(R.id.ctw_currency);
                ctwCurrency.setBackgroundColor(getColor(R.color.white));
                ctwCurrency.setLeftText(item.getName());
                ctwCurrency.setLeftImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), item.type));
            }

            @Override
            protected void onItemClick(View view, ConvertViewBean item, int position) {
                Intent resultIntent = new Intent();
                if (selectType == TYPE_NORMAL) {
                    resultIntent.putExtra(Constants.INTENT_KEY1, item);
                } else if (selectType == TYPE_MESSAGE_SIGN) {
                    resultIntent.putExtra(Constants.INTENT_KEY1, item.currency);
                }
                setResult(Constants.RESULT_SUCCESS, resultIntent);
                finish();
            }
        };

        rvCurrency.setAdapter(mAdapter);

        svSelectCurrency = findViewById(R.id.sv_select_currency);
        //去除searchView默认下划线
        View plateView = svSelectCurrency.findViewById(android.support.v7.appcompat.R.id.search_plate);
        if (plateView != null) {
            plateView.setBackgroundColor(Color.TRANSPARENT);
        }
        svSelectCurrency.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (svSelectCurrency != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(svSelectCurrency.getWindowToken(), 0);
                        svSelectCurrency.clearFocus();
                    }
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCurrency();
                return true;
            }
        });

        includeNotData = findViewById(R.id.include_not_data);
        rvCurrency.setVisibility(View.GONE);
        includeNotData.setVisibility(View.GONE);
    }

    @Override
    public void initData() {
        super.initData();
        selectType = getIntent().getIntExtra(Constants.INTENT_KEY1, TYPE_NORMAL);

        setTitle(R.string.select_currency);
        List<Currency> currencyList = HDAddressManager.getInstance().getCurrencyList();
        for (int i = 0; i < currencyList.size(); i++) {
            allConvertViewList.add(ConvertViewBean.currencyConvert(currencyList.get(i)));
        }

        if (selectType == TYPE_NORMAL) {
            addSafeAsset();
        } else if (selectType == TYPE_MESSAGE_SIGN) {
            filterCurrency();
        }
    }

    private void addSafeAsset() {
        new CommonAsyncTask.Builder<Void, Void, List<SafeAsset>>()
                .setIDoInBackground(new IDoInBackground<Void, Void, List<SafeAsset>>() {
                    @Override
                    public List<SafeAsset> doInBackground(IPublishProgress<Void> publishProgress, Void... aVoid) {
                        List<String> assetList = OutProvider.getInstance().getSafeAsset();
                        List<SafeAsset> safeAssetList = new ArrayList<>();
                        for(String item : assetList){
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


    private void filterCurrency() {
        filterConvertViewList.clear();
        for (int i = 0; i < allConvertViewList.size(); i++) {
            ConvertViewBean bean = allConvertViewList.get(i);
            String coin = bean.isSafeAsset() ? bean.getName() : bean.getCoin();
            String query = svSelectCurrency.getQuery().toString();
            if (TextUtils.isEmpty(query) || coin.toUpperCase().contains(query.toUpperCase())) {
                filterConvertViewList.add(allConvertViewList.get(i));
            }
        }
        if (filterConvertViewList.size() == 0) {
            fixationHead(true);
            rvCurrency.setVisibility(View.GONE);
            includeNotData.setVisibility(View.VISIBLE);
        } else {
            fixationHead(false);
            rvCurrency.setVisibility(View.VISIBLE);
            includeNotData.setVisibility(View.GONE);
        }

        mAdapter.replaceAll(filterConvertViewList);
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
                        for (int i = 0; i < ethTokens.size(); i++) {
                            allConvertViewList.add(ConvertViewBean.ethTokenConvert(ethTokens.get(i)));
                        }
                        EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                        if (eosAccount != null) {
                            addEosBalance();
                        } else {
                            Collections.sort(allConvertViewList);
                            filterCurrency();
                        }
                    }
                })
                .start();
    }

    private void addEosBalance() {
        new CommonAsyncTask.Builder<Void, Void, List<EosUsdtBalance>>()
                .setIDoInBackground(new IDoInBackground<Void, Void, List<EosUsdtBalance>>() {
                    @Override
                    public List<EosUsdtBalance> doInBackground(IPublishProgress<Void> publishProgress, Void... aVoid) {
                        return EosUsdtBalanceProvider.getInstance().getEosBalanceList();
                    }
                })
                .setIPostExecute(new IPostExecute<List<EosUsdtBalance>>() {
                    @Override
                    public void onPostExecute(List<EosUsdtBalance> eosBalanceList) {
                        for (int i = 0; i < eosBalanceList.size(); i++) {
                            allConvertViewList.add(ConvertViewBean.eosTokenConvert(eosBalanceList.get(i)));
                        }
                        Collections.sort(allConvertViewList);
                        filterCurrency();
                    }
                })
                .start();
    }

    private void fixationHead(boolean fixation) {
        View mAppBarChildAt = ablCurrency.getChildAt(0);
        AppBarLayout.LayoutParams mAppBarParams = (AppBarLayout.LayoutParams) mAppBarChildAt.getLayoutParams();
        if (fixation) {
            mAppBarParams.setScrollFlags(0);
        } else {
            mAppBarParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        }
        mAppBarChildAt.setLayoutParams(mAppBarParams);
    }

}
