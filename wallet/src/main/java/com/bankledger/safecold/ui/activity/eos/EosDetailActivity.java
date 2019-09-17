package com.bankledger.safecold.ui.activity.eos;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.bankledger.protobuf.bean.CoinBalance;
import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.CurrencyReceiveActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.ui.widget.CommonTextWidget;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.StringUtil;
import com.bankledger.safecold.utils.SyncBalanceUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zm on 2018/11/13.
 */
public class EosDetailActivity extends ToolbarBaseActivity implements View.OnClickListener {

    private EosAccount eosAccount;
    private EosBalance eosBalance;
    private TextView tvAmount;
    private TextView tvUnit;
    private RecyclerView rvAddress;
    private CommonAdapter<EosNameItem> mAdapter;
    private View btSend;


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
        mAdapter = new CommonAdapter<EosNameItem>(R.layout.listitem_currency_eos_detail) {
            @Override
            protected void convert(ViewHolder viewHolder, final EosNameItem item, int position) {
                CommonTextWidget twName = viewHolder.findViewById(R.id.tw_name);
                twName.setLeftText(item.name);
                if (!item.isName) {
                    twName.setRightImageResource(R.drawable.ic_chevron_right_gray_24dp);
                } else {
                    twName.setRightImageDrawable(null);
                }
            }

            @Override
            protected void onItemClick(View view, EosNameItem item, int position) {
                if (item.name.equals(getString(R.string.eos_look_pubkey))) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                    go2Activity(MineEosAddressDetailActivity.class, args);
                } else if (item.name.equals(getString(R.string.eos_buy_ram))) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                    go2Activity(BuyRamActivity.class, args);
                } else if (item.name.equals(getString(R.string.eos_delegatebw))) {
                    Bundle args = new Bundle();
                    args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                    go2Activity(DelegatebwActivity.class, args);
                }
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
        setTitle(SafeColdSettings.EOS);
        EventBus.getDefault().register(this);
        eosAccount = EosAccountProvider.getInstance().queryAvailableEosAccount();
        eosBalance = (EosBalance) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        setTitle(eosBalance.tokenName);
        tvUnit.setText(eosBalance.tokenName);
        mAdapter.add(new EosNameItem(eosAccount.getAccountName(), true));
        mAdapter.add(new EosNameItem(getString(R.string.eos_look_pubkey), false));
        if (Utils.isEos(eosBalance.tokenName)) {
            mAdapter.add(new EosNameItem(getString(R.string.eos_buy_ram), false));
            mAdapter.add(new EosNameItem(getString(R.string.eos_delegatebw), false));
        }
        getNewBalance();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEventMessage(EventMessage msg) {
        if (msg.eventType == EventMessage.TYPE_BALANCE_CHANGED) {
            getNewBalance();
        }
    }

    private void getNewBalance() {
        new CommonAsyncTask.Builder<String, Void, EosBalance>()
                .setIDoInBackground(new IDoInBackground<String, Void, EosBalance>() {
                    @Override
                    public EosBalance doInBackground(IPublishProgress<Void> publishProgress, String... tokenNames) {
                        return EosUsdtBalanceProvider.getInstance().getEosBalance(tokenNames[0]);
                    }
                })
                .setIPostExecute(new IPostExecute<EosBalance>() {
                    @Override
                    public void onPostExecute(EosBalance balance) {
                        eosBalance = balance;
                        tvAmount.setText(StringUtil.subZeroAndDot(eosBalance.balance));
                    }
                })
                .start(eosBalance.tokenName);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_refresh_balance://更新余额
                verifyPermissions(Manifest.permission.CAMERA, new PermissionCallBack() {
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
                Bundle args = new Bundle();
                args.putSerializable(Constants.INTENT_KEY1, eosBalance);
                go2Activity(EosSendActivity.class, args);
                break;

            case R.id.bt_receive://接收
                args = new Bundle();
                args.putSerializable(Constants.INTENT_KEY1, ConvertViewBean.eosTokenConvert(eosBalance));
                go2Activity(CurrencyReceiveActivity.class, args);
                break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE1 && resultCode == Constants.RESULT_SUCCESS) {
            new QrProtocolUtil()
                    .setIBalanceDecode(new QrProtocolUtil.IBalanceDecode() {
                        @Override
                        public void onBalanceDecode(List<CoinBalance> coinBalanceList) {
                            SyncBalanceUtil.replaceOuts(coinBalanceList);
                        }
                    })
                    .setIDecodeFail(new QrProtocolUtil.IDecodeFail() {
                        @Override
                        public void onQrDecodeFail(String errMsg) {
                            ToastUtil.showToast(errMsg);
                        }

                        @Override
                        public void onProtocolUpgrade(boolean isSelf) {
                            DialogUtil.showProtocolUpdateDilog(EosDetailActivity.this, isSelf);
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

    class EosNameItem {
        String name;
        boolean isName;

        public EosNameItem(String name, boolean isName) {
            this.name = name;
            this.isName = isName;
        }
    }
}
