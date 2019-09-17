package com.bankledger.safecold.ui.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.bankledger.protobuf.bean.CoinBalance;
import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.protobuf.bean.EthToken;
import com.bankledger.protobuf.bean.TransDate;
import com.bankledger.protobuf.bean.TransRetEos;
import com.bankledger.protobuf.bean.TransSignParam;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.BaseActivity;
import com.bankledger.safecold.ui.activity.CurrencyDetailActivity;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.ui.activity.CurrencyReceiveActivity;
import com.bankledger.safecold.ui.activity.CurrencySendActivity;
import com.bankledger.safecold.ui.activity.eos.ChoiceAccountActivity;
import com.bankledger.safecold.ui.activity.eos.EosDetailActivity;
import com.bankledger.safecold.ui.activity.eos.EosSendActivity;
import com.bankledger.safecold.ui.activity.eth.ETHTokenDetailActivity;
import com.bankledger.safecold.ui.activity.eth.ETHTokenSendActivity;
import com.bankledger.safecold.utils.BigDecimalUtils;
import com.bankledger.safecold.utils.CurrencyLogoUtil;
import com.bankledger.safecold.utils.DateTimeUtil;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.EthUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.StringUtil;
import com.bankledger.safecold.utils.SyncBalanceUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.utils.GsonUtils;
import com.bankledger.safecoldj.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by zm on 2018/6/22.
 */

public class WalletFragment extends BaseFragment {

    private RecyclerView mRvView;
    private CommonAdapter<ConvertViewBean> mAdapter;
    private AppBarLayout ablWallet;
    private SearchView svSearch;
    private Switch switchHide;
    private View includeNotData;
    private List<ConvertViewBean> allConvertViewList = new ArrayList<>();
    private List<ConvertViewBean> filterConvertViewList = new ArrayList<>();
    private View llSearch;

    @Override
    public int setContentView() {
        return R.layout.fragment_wallet;
    }

    @Override
    public void initView() {
        ablWallet = findViewById(R.id.abl_wallet);
        findViewById(R.id.tv_wallet_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBaseActivity().verifyPermissions(Manifest.permission.CAMERA, new BaseActivity.PermissionCallBack() {
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
        findViewById(R.id.tv_wallet_receive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2Activity(CurrencyReceiveActivity.class);
            }
        });
        findViewById(R.id.tv_wallet_transfer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go2Activity(CurrencySendActivity.class);
            }
        });

        llSearch = findViewById(R.id.ll_search);

        final View tvZero = findViewById(R.id.tv_zero);

        svSearch = findViewById(R.id.sv_search);
        //去除searchView默认下划线
        TextView textView = svSearch.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        textView.setTextSize(14);
        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (svSearch != null) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(svSearch.getWindowToken(), 0);
                        svSearch.clearFocus();
                    }
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCurrency();
                tvZero.setVisibility(newText.length() > 0 ? View.GONE : View.VISIBLE);
                switchHide.setVisibility(newText.length() > 0 ? View.GONE : View.VISIBLE);
                return true;
            }
        });

        switchHide = findViewById(R.id.switch_hide);
        switchHide.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filterCurrency();
            }
        });

        mRvView = findViewById(R.id.rv_view);
        mRvView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRvView.addItemDecoration(new RecyclerViewDivider(getActivity(), LinearLayoutManager.HORIZONTAL));
        mAdapter = new CommonAdapter<ConvertViewBean>(R.layout.listitem_wallet) {
            @Override
            protected void convert(ViewHolder viewHolder, ConvertViewBean item, int position) {

                ImageView ivLogo = viewHolder.findViewById(R.id.iv_logo);
                TextView mTvCurrency = viewHolder.findViewById(R.id.tv_currency);
                TextView mTvBalance = viewHolder.findViewById(R.id.tv_balance);
                ivLogo.setImageResource(CurrencyLogoUtil.getResourceByCoin(item.getCoin(), item.type));
                mTvCurrency.setText(item.getName());
                if (item.isCurrency()) {
                    mTvBalance.setText(item.balance);
                } else if (item.isETHToken()) {
                    mTvBalance.setText(EthUtil.formatAmount(item.ethToken));
                } else if (item.isEosBalance()) {
                    mTvBalance.setText(item.balance);
                } else if (item.isSafeAsset()) {
                    mTvBalance.setText(item.balance);
                }
            }

            @Override
            protected void onItemClick(View view, ConvertViewBean item, int position) {
                Bundle args = new Bundle();
                if (item.isCurrency()) {
                    args.putSerializable(Constants.INTENT_KEY1, item.currency);
                    go2Activity(CurrencyDetailActivity.class, args);
                } else if (item.isETHToken()) {
                    args.putSerializable(Constants.INTENT_KEY1, item.ethToken);
                    go2Activity(ETHTokenDetailActivity.class, args);
                } else if (item.isEosBalance()) {
                    EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                    if (eosAccount != null) {
                        args.putSerializable(Constants.INTENT_KEY1, item.eosBalance);
                        go2Activity(EosDetailActivity.class, args);
                    } else {
                        go2Activity(ChoiceAccountActivity.class);
                    }
                } else if (item.isSafeAsset()) {
                    args.putSerializable(Constants.INTENT_KEY1, HDAddressManager.getInstance().getCurrencyMap().get(SafeColdSettings.SAFE));
                    args.putSerializable(Constants.INTENT_KEY2, item.safeAsset);
                    go2Activity(CurrencyDetailActivity.class, args);
                }
            }
        };
        mRvView.setAdapter(mAdapter);

        includeNotData = findViewById(R.id.include_not_data);
    }

    @Override
    public void onResume() {
        super.onResume();
        llSearch.setFocusable(true);
        llSearch.setFocusableInTouchMode(true);
        llSearch.requestFocus();
    }

    @Override
    public void initData() {
        EventBus.getDefault().register(this);
        checkCoinBalance();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEventMessage(EventMessage msg) {
        if (msg.eventType == EventMessage.TYPE_BALANCE_CHANGED || msg.eventType == EventMessage.TYPE_TOKEN_CHANGED) {
            checkCoinBalance();
            filterCurrency();
        }
    }

    private void checkCoinBalance() {
        allConvertViewList.clear();
        List<Currency> currencyList = HDAddressManager.getInstance().getCurrencyList();
        for (int i = 0; i < currencyList.size(); i++) {
            allConvertViewList.add(ConvertViewBean.currencyConvert(currencyList.get(i)));
        }
        ConvertViewBean[] arrConvertView = new ConvertViewBean[allConvertViewList.size()];
        allConvertViewList.toArray(arrConvertView);
        new CommonAsyncTask.Builder<ConvertViewBean, Void, Void>()
                .setIDoInBackground(new IDoInBackground<ConvertViewBean, Void, Void>() {
                    @Override
                    public Void doInBackground(IPublishProgress<Void> publishProgress, ConvertViewBean... convertViews) {
                        for (ConvertViewBean convertView : convertViews) {
                            if (convertView.currency.isUsdt()) {
                                convertView.balance = StringUtil.subZeroAndDot(EosUsdtBalanceProvider.getInstance().getUsdtBalance());
                            } else {
                                long balance = OutProvider.getInstance().getBalanceWithCoin(convertView.currency.coin);
                                convertView.balance = BigDecimalUtils.formatSatoshi2Btc(Long.toString(balance));
                            }
                        }
                        return null;
                    }
                })
                .setIPostExecute(new IPostExecute<Void>() {
                    @Override
                    public void onPostExecute(Void aVoid) {
                        addSafeAsset();
                    }
                })
                .start(arrConvertView);
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
                            long balance = OutProvider.getInstance().getBalanceWithCoinSafeAsset(SafeColdSettings.SAFE, safeAsset.assetId);
                            safeAsset.assetBalance = BigDecimalUtils.formatShowAmount(balance, safeAsset.assetDecimals);
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
                        for (int i = 0; i < ethTokens.size(); i++) {
                            allConvertViewList.add(ConvertViewBean.ethTokenConvert(ethTokens.get(i)));
                        }
                        addEosBalance();
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

    private void filterCurrency() {
        filterConvertViewList.clear();
        for (int i = 0; i < allConvertViewList.size(); i++) {
            ConvertViewBean bean = allConvertViewList.get(i);
            String coin = bean.isSafeAsset() ? bean.getName() : bean.getCoin();
            String query = svSearch.getQuery().toString();
            if (TextUtils.isEmpty(query) || coin.toUpperCase().contains(query.toUpperCase())) {
                if (!switchHide.isChecked() ||
                        (switchHide.isChecked() && TextUtils.isEmpty(allConvertViewList.get(i).balance)) ||
                        (switchHide.isChecked() && BigDecimalUtils.greaterThan(allConvertViewList.get(i).balance, "0"))) {
                    filterConvertViewList.add(allConvertViewList.get(i));
                }
            }
        }
        if (filterConvertViewList.size() == 0) {
            fixationHead(true);
            mRvView.setVisibility(View.GONE);
            includeNotData.setVisibility(View.VISIBLE);
        } else {
            fixationHead(false);
            mRvView.setVisibility(View.VISIBLE);
            includeNotData.setVisibility(View.GONE);
        }
        mAdapter.replaceAll(filterConvertViewList);
    }

    private void fixationHead(boolean fixation) {
        View mAppBarChildAt = ablWallet.getChildAt(0);
        AppBarLayout.LayoutParams mAppBarParams = (AppBarLayout.LayoutParams) mAppBarChildAt.getLayoutParams();
        if (fixation) {
            mAppBarParams.setScrollFlags(0);
        } else {
            mAppBarParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        }
        mAppBarChildAt.setLayoutParams(mAppBarParams);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE1 && resultCode == Constants.RESULT_SUCCESS) {
            new QrProtocolUtil()
                    .setICoinAddressDecode(new QrProtocolUtil.ICoinAddressDecode() {
                        @Override
                        public void onCoinAddressDecode(UriDecode uriDecode) {
                            String fullName = uriDecode.scheme;
                            if (Utils.fullNameIsBtc(fullName)) {
                                Currency currency = Utils.findCurrencyForFullName(fullName);
                                if (currency != null) {
                                    Bundle args = new Bundle();
                                    args.putSerializable(Constants.INTENT_KEY1, currency);
                                    args.putString(Constants.INTENT_KEY2, uriDecode.path);
                                    if (uriDecode.params != null && uriDecode.params.get("amount") != null) {
                                        args.putString(Constants.INTENT_KEY3, uriDecode.params.get("amount"));
                                    }
                                    if (uriDecode.params != null && uriDecode.params.get("token") != null) {
                                        String assetName = uriDecode.params.get("token");
                                        SafeAsset safeAsset = OutProvider.getInstance().getSafeAsset(assetName);
                                        args.putSerializable(Constants.INTENT_KEY4, safeAsset);
                                    }
                                    go2Activity(CurrencySendActivity.class, args);
                                }
                            } else if (Utils.fullNameIsEth(fullName)) {
                                go2ETHTokenSend(uriDecode);
                            } else if (Utils.fullNameIsEtc(fullName)) {
                                go2ETCTokenSend(uriDecode);
                            } else if (Utils.fullNameIsEos(fullName)) {
                                go2EosSend(uriDecode);
                            } else {
                                ToastUtil.showToast(R.string.unidentifiable_qr_code);
                            }
                        }
                    })
                    .setIBalanceDecode(new QrProtocolUtil.IBalanceDecode() {
                        @Override
                        public void onBalanceDecode(List<CoinBalance> coinBalanceList) {
                            SyncBalanceUtil.replaceOuts(coinBalanceList);
                        }
                    })
                    .setIDateDecode(new QrProtocolUtil.IDateDecode() {
                        @Override
                        public void onDateDecode(TransDate transDate) {
                            try {
                                SystemClock.setCurrentTimeMillis(DateTimeUtil.getDateTimeForTime(transDate.date));
                                ToastUtil.showToast(R.string.time_sync);
                            } catch (Exception e) {
                                e.printStackTrace();
                                ToastUtil.showToast(R.string.unidentifiable_qr_code);
                            }
                        }
                    })
                    .setIActiveEthTokenDecode(new QrProtocolUtil.IActiveEthTokenDecode() {
                        @Override
                        public void onActiveEthTokenDecode(EthToken ethToken) {
                            ETHToken ethAddress = ETHTokenProvider.getInstance().queryETH();
                            ETHToken token = ETHToken.activeToken(ethAddress.ethAddress, ethToken.name, ethToken.contractsAddress, ethToken.symbol, ethToken.decimals, ethToken.totalSupply);
                            addToken(token);
                        }
                    })
                    .setRetEos(new QrProtocolUtil.IRetEos() {
                        @Override
                        public void onRetEos(TransRetEos retEos) {
                            updateEosAccount(retEos);
                        }
                    })
                    .setISignParamDecode(new QrProtocolUtil.ISignParam() {
                        @Override
                        public void onSignParamDecode(TransSignParam signParam) {
                            EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
                            if (eosAccount != null) {
                                go2EosSend(signParam);
                            } else {
                                go2Activity(ChoiceAccountActivity.class);
                                ToastUtil.showToast(R.string.eos_first_create_account);
                            }
                        }
                    })
                    .setIDecodeFail(new QrProtocolUtil.IDecodeFail() {
                        @Override
                        public void onQrDecodeFail(String errMsg) {
                            if (errMsg.equals(getString(R.string.unidentifiable_qr_code))) {
                                DialogUtil.showTextDialog(getActivity(), getString(R.string.scan_content), data.getStringExtra(Constants.INTENT_KEY1), getString(R.string.copy_clipboard), true, new DialogUtil.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which, String content) {
                                        if (which == Dialog.BUTTON_POSITIVE) {
                                            ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                            clipboardManager.setPrimaryClip(ClipData.newPlainText("scan text", data.getStringExtra(Constants.INTENT_KEY1)));
                                            ToastUtil.showToast(getString(R.string.copy_success));
                                        }
                                    }
                                });
                            } else {
                                ToastUtil.showToast(errMsg);
                            }
                        }

                        @Override
                        public void onProtocolUpgrade(boolean isSelf) {
                            DialogUtil.showProtocolUpdateDilog(getContext(), isSelf);
                        }
                    })
                    .decode(data.getStringExtra(Constants.INTENT_KEY1));
        }
    }

    private void addToken(final ETHToken ethToken) {
        new CommonAsyncTask.Builder<ETHToken, Void, Void>()
                .setIDoInBackground(new IDoInBackground<ETHToken, Void, Void>() {
                    @Override
                    public Void doInBackground(IPublishProgress<Void> publishProgress, ETHToken... ethTokens) {
                        ETHTokenProvider.getInstance().addETH(ethTokens[0]);
                        return null;
                    }
                })
                .setIPostExecute(new IPostExecute<Void>() {
                    @Override
                    public void onPostExecute(Void aVoid) {
                        ToastUtil.showToast(R.string.active_success);
                        checkCoinBalance();
                        filterCurrency();
                        EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_ACTIVE));
                    }
                })
                .start(ethToken);
    }

    private void updateEosAccount(final TransRetEos retEos) {
        new CommonAsyncTask.Builder<Void, Void, EosAccount>()
                .setIDoInBackground(new IDoInBackground<Void, Void, EosAccount>() {
                    @Override
                    public EosAccount doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                        if (EosAccountProvider.getInstance().isExistPubkey(retEos.owner, retEos.active)) {
                            EosAccount eosAccount = new EosAccount();
                            eosAccount.setState(EosAccount.AvailableState.STATE_AVAILABLE);
                            eosAccount.setAccountName(retEos.account);
                            eosAccount.setOwnerPubKey(retEos.owner);
                            eosAccount.setActivePubKey(retEos.active);
                            return eosAccount;
                        } else {
                            return null;
                        }
                    }
                })
                .setIPostExecute(new IPostExecute<EosAccount>() {
                    @Override
                    public void onPostExecute(EosAccount eosAccount) {
                        if (eosAccount != null) {
                            EosAccountProvider.getInstance().updateEosAccountAvailable(eosAccount);
                            ToastUtil.showToast(R.string.eos_account_updated);
                        }
                    }
                })
                .start();
    }

    private void go2EosSend(final UriDecode uriDecode) {
        if (uriDecode.params != null && uriDecode.params.get("token") != null) {
            String token = uriDecode.params.get("token");
            new CommonAsyncTask.Builder<String, Void, EosBalance>()
                    .setIDoInBackground(new IDoInBackground<String, Void, EosBalance>() {
                        @Override
                        public EosBalance doInBackground(IPublishProgress<Void> publishProgress, String... token) {
                            return EosUsdtBalanceProvider.getInstance().getEosBalance(token[0]);
                        }
                    })
                    .setIPostExecute(new IPostExecute<EosBalance>() {
                        @Override
                        public void onPostExecute(EosBalance eosBalance) {
                            if (eosBalance != null) {
                                Bundle args = new Bundle();
                                args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                                args.putString(Constants.INTENT_KEY2, uriDecode.path);
                                if (uriDecode.params != null)
                                    args.putString(Constants.INTENT_KEY3, uriDecode.params.get("amount"));
                                go2Activity(EosSendActivity.class, args);
                            } else {
                                ToastUtil.showToast(getString(R.string.eos_unsupport_token));
                            }
                        }
                    }).start(token);
        } else {
            EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
            if (eosAccount != null) {
                new CommonAsyncTask.Builder<Void, Void, EosBalance>()
                        .setIDoInBackground(new IDoInBackground<Void, Void, EosBalance>() {
                            @Override
                            public EosBalance doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                                return EosUsdtBalanceProvider.getInstance().getEosBalance();
                            }
                        })
                        .setIPostExecute(new IPostExecute<EosBalance>() {
                            @Override
                            public void onPostExecute(EosBalance eosBalance) {
                                Bundle args = new Bundle();
                                args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                                args.putString(Constants.INTENT_KEY2, uriDecode.path);
                                if (uriDecode.params != null)
                                    args.putString(Constants.INTENT_KEY3, uriDecode.params.get("amount"));
                                go2Activity(EosSendActivity.class, args);
                            }
                        }).start();
            } else {
                ToastUtil.showToast(R.string.eos_account_disabled);
            }
        }
    }

    private void go2EosSend(final TransSignParam signParam) {
        EosAccount eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
        if (eosAccount != null) {
            new CommonAsyncTask.Builder<Void, Void, EosBalance>()
                    .setIDoInBackground(new IDoInBackground<Void, Void, EosBalance>() {
                        @Override
                        public EosBalance doInBackground(IPublishProgress<Void> publishProgress, Void... voids) {
                            return EosUsdtBalanceProvider.getInstance().getEosBalance();
                        }
                    })
                    .setIPostExecute(new IPostExecute<EosBalance>() {
                        @Override
                        public void onPostExecute(EosBalance eosBalance) {
                            Bundle args = new Bundle();
                            args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                            args.putSerializable(Constants.INTENT_KEY4, signParam);
                            go2Activity(EosSendActivity.class, args);
                        }
                    }).start();
        } else {
            ToastUtil.showToast(R.string.eos_account_disabled);
        }
    }

    private void go2ETHTokenSend(final UriDecode uriDecode) {
        if (uriDecode.params != null && uriDecode.params.get("token") != null) {
            String token = uriDecode.params.get("token");
            new CommonAsyncTask.Builder<String, Void, ETHToken>()
                    .setIDoInBackground(new IDoInBackground<String, Void, ETHToken>() {
                        @Override
                        public ETHToken doInBackground(IPublishProgress<Void> publishProgress, String... token) {
                            return ETHTokenProvider.getInstance().queryETHTokenWithContractAddress(token[0]);
                        }
                    })
                    .setIPostExecute(new IPostExecute<ETHToken>() {
                        @Override
                        public void onPostExecute(ETHToken ethToken) {
                            if (ethToken != null) {
                                Bundle args = new Bundle();
                                args.putSerializable(Constants.INTENT_KEY1, ethToken);
                                args.putString(Constants.INTENT_KEY2, uriDecode.path);
                                if (uriDecode.params != null) {
                                    if (uriDecode.params.get("amount") != null) {
                                        args.putString(Constants.INTENT_KEY3, uriDecode.params.get("amount"));
                                    }
                                }
                                go2Activity(ETHTokenSendActivity.class, args);
                            } else {
                                ToastUtil.showToast(getString(R.string.eth_unsupport_token));
                            }
                        }
                    })
                    .start(token);
        } else {
            new CommonAsyncTask.Builder<UriDecode, Void, ETHToken>()
                    .setIDoInBackground(new IDoInBackground<UriDecode, Void, ETHToken>() {
                        @Override
                        public ETHToken doInBackground(IPublishProgress<Void> publishProgress, UriDecode... uriDecodes) {
                            return ETHTokenProvider.getInstance().queryETH();
                        }
                    })
                    .setIPostExecute(new IPostExecute<ETHToken>() {
                        @Override
                        public void onPostExecute(ETHToken ethToken) {
                            Bundle args = new Bundle();
                            args.putSerializable(Constants.INTENT_KEY1, ethToken);
                            args.putString(Constants.INTENT_KEY2, uriDecode.path);
                            if (uriDecode.params != null) {
                                if (uriDecode.params.get("amount") != null) {
                                    args.putString(Constants.INTENT_KEY3, uriDecode.params.get("amount"));
                                }
                            }
                            go2Activity(ETHTokenSendActivity.class, args);
                        }
                    })
                    .start(uriDecode);
        }
    }

    private void go2ETCTokenSend(final UriDecode uriDecode) {
        new CommonAsyncTask.Builder<UriDecode, Void, ETHToken>()
                .setIDoInBackground(new IDoInBackground<UriDecode, Void, ETHToken>() {
                    @Override
                    public ETHToken doInBackground(IPublishProgress<Void> publishProgress, UriDecode... uriDecodes) {
                        return ETHTokenProvider.getInstance().queryETC();
                    }
                })
                .setIPostExecute(new IPostExecute<ETHToken>() {
                    @Override
                    public void onPostExecute(ETHToken ethToken) {
                        Bundle args = new Bundle();
                        args.putSerializable(Constants.INTENT_KEY1, ethToken);
                        args.putString(Constants.INTENT_KEY2, uriDecode.path);
                        if (uriDecode.params != null) {
                            if (uriDecode.params.get("amount") != null) {
                                args.putString(Constants.INTENT_KEY3, uriDecode.params.get("amount"));
                            }
                        }
                        go2Activity(ETHTokenSendActivity.class, args);
                    }
                })
                .start(uriDecode);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
