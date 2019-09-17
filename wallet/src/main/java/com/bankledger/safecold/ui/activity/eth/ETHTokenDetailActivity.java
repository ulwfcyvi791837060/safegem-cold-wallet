package com.bankledger.safecold.ui.activity.eth;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bankledger.protobuf.bean.CoinBalance;
import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.ConvertViewBean;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.scan.ScanActivity;
import com.bankledger.safecold.ui.activity.AddWalletActivity;
import com.bankledger.safecold.ui.activity.CurrencyReceiveActivity;
import com.bankledger.safecold.ui.activity.ToolbarBaseActivity;
import com.bankledger.safecold.ui.fragment.SynchronousBalanceDialogFragment;
import com.bankledger.safecold.utils.CommonUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.EthUtil;
import com.bankledger.safecold.utils.QrProtocolUtil;
import com.bankledger.safecold.utils.RecyclerViewDivider;
import com.bankledger.safecold.utils.SyncBalanceUtil;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecold.utils.WalletInfoManager;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.bankledger.safecoldj.qrcode.SyncBalanceBean;
import com.bankledger.safecoldj.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bankledger
 * @time 2018/7/30 15:26
 */
public class ETHTokenDetailActivity extends ToolbarBaseActivity implements View.OnClickListener {

    private ETHToken mETHToken;
    private TextView tvAmount;
    private TextView tvUnit;
    private RecyclerView rvAddress;
    private CommonAdapter<ETHToken> mAdapter;
    private View btSend;

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
        mAdapter = new CommonAdapter<ETHToken>(R.layout.listitem_currency_address_detail) {
            @Override
            protected void convert(ViewHolder viewHolder, final ETHToken item, int position) {
                TextView tvPosition = viewHolder.findViewById(R.id.tv_position);
                tvPosition.setText(Integer.toString(++position));
                TextView tvAlias = viewHolder.findViewById(R.id.tv_alias);
                if (TextUtils.isEmpty(item.alias)) {
                    tvAlias.setText(R.string.no_alias);
                } else {
                    tvAlias.setText(item.alias);
                }

                TextView tvAmount = viewHolder.findViewById(R.id.tv_amount);

                tvAmount.setText(EthUtil.formatAmount(mETHToken));

                TextView tvAddress = viewHolder.findViewById(R.id.tv_address);
                tvAddress.setText(item.ethAddress);

                viewHolder.findViewById(R.id.bt_refresh_balance).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refreshBalance(item);
                    }
                });
            }

            @Override
            protected void onItemClick(View view, ETHToken item, int position) {

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
        mETHToken = (ETHToken) getIntent().getSerializableExtra(Constants.INTENT_KEY1);
        setTitle(mETHToken.isErc20() ? mETHToken.symbol : mETHToken.name);
        tvUnit.setText(mETHToken.symbol);
        getETHToken();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEventMessage(EventMessage msg) {
        if (msg.eventType == EventMessage.TYPE_BALANCE_CHANGED) {
            getETHToken();
        }
    }

    private void getETHToken() {
        new CommonAsyncTask.Builder<ETHToken, Void, ETHToken>()
                .setIDoInBackground(new IDoInBackground<ETHToken, Void, ETHToken>() {
                    @Override
                    public ETHToken doInBackground(IPublishProgress<Void> publishProgress, ETHToken... ethTokens) {
                        return ETHTokenProvider.getInstance().queryETHTokenWithContractAddress(ethTokens[0].contractsAddress);
                    }
                })
                .setIPostExecute(new IPostExecute<ETHToken>() {
                    @Override
                    public void onPostExecute(ETHToken ethToken) {
                        mETHToken = ethToken;
                        tvAmount.setText(EthUtil.formatAmount(mETHToken));
                        List<ETHToken> ethTokens = new ArrayList<>(1);
                        ethTokens.add(mETHToken);
                        mAdapter.replaceAll(ethTokens);
                    }
                })
                .start(mETHToken);
    }


    private void refreshBalance(ETHToken ethToken) {
        synDialog = SynchronousBalanceDialogFragment.newInstance(ethToken);
        synDialog.show(getSupportFragmentManager(), "syn");
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
                Bundle args3 = new Bundle();
                args3.putSerializable(Constants.INTENT_KEY1, mETHToken);
                go2Activity(ETHTokenSendActivity.class, args3);
                break;

            case R.id.bt_receive://接收
                Bundle args4 = new Bundle();
                args4.putSerializable(Constants.INTENT_KEY1, ConvertViewBean.ethTokenConvert(mETHToken));
                go2Activity(CurrencyReceiveActivity.class, args4);
                break;

            default:
                break;
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
                            DialogUtil.showProtocolUpdateDilog(ETHTokenDetailActivity.this, isSelf);
                        }
                    })
                    .decode(data.getStringExtra(Constants.INTENT_KEY1));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mETHToken.isErc20()) {
            getMenuInflater().inflate(R.menu.delete_menu, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_del) {
            DialogUtil.showTextDialog(this, getString(R.string.tip), getString(R.string.confirm_delete_token, mETHToken.symbol), new DialogUtil.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, String content) {
                    ETHTokenProvider.getInstance().removeToken(mETHToken);
                    EventMessage message = new EventMessage(EventMessage.TYPE_TOKEN_CHANGED);
                    EventBus.getDefault().post(message);
                    finish();
                }
            }, null);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
