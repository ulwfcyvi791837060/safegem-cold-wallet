package com.bankledger.safecold.utils;

import android.text.TextUtils;

import com.bankledger.protobuf.bean.CoinBalance;
import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.safecold.R;
import com.bankledger.safecold.asynctask.CommonAsyncTask;
import com.bankledger.safecold.asynctask.IDoInBackground;
import com.bankledger.safecold.asynctask.IPostExecute;
import com.bankledger.safecold.asynctask.IPublishProgress;
import com.bankledger.safecold.bean.EventMessage;
import com.bankledger.safecold.db.ETHTokenProvider;
import com.bankledger.safecold.db.EosAccountProvider;
import com.bankledger.safecold.db.EosUsdtBalanceProvider;
import com.bankledger.safecold.db.HDAddressProvider;
import com.bankledger.safecold.db.OutProvider;
import com.bankledger.safecold.utils.ToastUtil;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.utils.GsonUtils;
import com.bankledger.safecoldj.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/11/30 16:19
 */
public class SyncBalanceUtil {

    public static void replaceOuts(final List<CoinBalance> coinBalanceList) {
        replaceOuts(coinBalanceList, null);
    }

    public static void replaceOuts(final List<CoinBalance> coinBalanceList, final OnSyncListener onSyncListener) {
        new CommonAsyncTask.Builder<Void, Void, Void>()
                .setIDoInBackground(new IDoInBackground<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(IPublishProgress<Void> publishProgress, Void... aVoid) {
                        List<EosUsdtBalance> eosUsdtList = new ArrayList<>();
                        List<CoinBalance> btcList = new ArrayList<>();
                        for (CoinBalance coinBalance : coinBalanceList) { //处理ETH和ETC的情况
                            if (Utils.isEthOrEtc(coinBalance.coin)) {
                                ETHTokenProvider.getInstance().refreshBalance(new ETHToken(coinBalance.ethToken));
                            } else if (Utils.isEosOrUsdt(coinBalance.coin)) {  //处理EOS和USDT的情况
                                if (coinBalance.eosBalance != null && coinBalance.eosBalance.account != null) {
                                    if (Utils.isEos(coinBalance.coin)) { //是否EOS
                                        if (!TextUtils.isEmpty(coinBalance.eosBalance.account) && !TextUtils.isEmpty(coinBalance.eosBalance.active) && !TextUtils.isEmpty(coinBalance.eosBalance.owner)) {
                                            if(EosAccountProvider.getInstance().isExistPubkey(coinBalance.eosBalance.owner, coinBalance.eosBalance.active)){
                                                EosAccount eosAccount = new EosAccount();
                                                eosAccount.setState(EosAccount.AvailableState.STATE_AVAILABLE);
                                                eosAccount.setAccountName(coinBalance.eosBalance.account);
                                                eosAccount.setOwnerPubKey(coinBalance.eosBalance.owner);
                                                eosAccount.setActivePubKey(coinBalance.eosBalance.active);
                                                EosAccountProvider.getInstance().updateEosAccountAvailable(eosAccount);
                                            }
                                        }
                                    }
                                    if (Utils.isUsdt(coinBalance.coin)) { //是否USDT
                                        String usdtAddress = HDAddressManager.getInstance().getCurrencyMap().get(SafeColdSettings.USDT).selectAddress;
                                        if (!coinBalance.eosBalance.account.equalsIgnoreCase(usdtAddress)) {
                                            continue;
                                        }
                                    }
                                    EosUsdtBalance eosUsdtBalance = Utils.coinBalance2EosUsdtBalance(coinBalance);
                                    if (EosUsdtBalanceProvider.getInstance().checkBalanceExist(eosUsdtBalance.tokenName, eosUsdtBalance.getCoinType())) {
                                        EosUsdtBalanceProvider.getInstance().deleteBalance(eosUsdtBalance.tokenName, eosUsdtBalance.getCoinType());
                                    }
                                    eosUsdtList.add(eosUsdtBalance);
                                }
                            } else if (Utils.isSafe(coinBalance.coin)) { //处理SAFE和SAFE资产的情况
                                if (TextUtils.isEmpty(coinBalance.utxo.reserve) || coinBalance.utxo.reserve.equals("safe")) {
                                    if (coinBalance.utxo.address.size() == 1) { //单地址情况
                                        OutProvider.getInstance().clearOuts(coinBalance.coin);
                                        btcList.add(coinBalance);
                                    }
                                } else {
                                    SafeAsset safeAsset = GsonUtils.getObjFromJSON(coinBalance.utxo.reserve, SafeAsset.class);
                                    if (coinBalance.utxo.address.size() == 1) { //单地址情况
                                        OutProvider.getInstance().clearOuts(coinBalance.coin, safeAsset.assetId);
                                        btcList.add(coinBalance);
                                    }
                                }
                            } else { //其他BTC系列的情况
                                if (coinBalance.utxo.address.size() == 1) { //单地址情况
                                    OutProvider.getInstance().clearOuts(coinBalance.coin);
                                    btcList.add(coinBalance);
                                }
                            }
                        }
                        for (CoinBalance btcItem : btcList) {
                            if (HDAddressProvider.getInstance().checkAddressExist(btcItem.coin, btcItem.utxo.address.get(0))) {
                                Out out = new Out(btcItem.utxo);
                                out.setCoinExistAsset(btcItem.coin);
                                OutProvider.getInstance().addOut(out);
                            }
                        }
                        EosUsdtBalanceProvider.getInstance().addBalanceList(eosUsdtList);
                        return null;
                    }
                })
                .setIPostExecute(new IPostExecute<Void>() {
                    @Override
                    public void onPostExecute(Void aVoid) {
                        if (onSyncListener != null)
                            onSyncListener.onSync();
                        EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_BALANCE_CHANGED));
                        EventBus.getDefault().post(new EventMessage(EventMessage.TYPE_ACTIVE));
                        ToastUtil.showToast(R.string.synchronous_balance);

                    }
                })
                .start();
    }

    public interface OnSyncListener {
        void onSync();
    }
}
