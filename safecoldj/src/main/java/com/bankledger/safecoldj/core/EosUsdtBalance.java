package com.bankledger.safecoldj.core;

import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.safecoldj.SafeColdSettings;

import java.io.Serializable;

/**
 * Created by zm on 2018/11/13.
 */

public class EosUsdtBalance extends EosBalance implements Serializable {

    public enum CoinType {
        TYPE_EOS(0),
        TYPE_USDT(1);

        private int value;

        CoinType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    protected CoinType coinType =  CoinType.TYPE_EOS;

    public CoinType getCoinType() {
        return coinType;
    }

    public void setCoinType(CoinType coinType) {
        this.coinType = coinType;
    }

    public static EosUsdtBalance getZeroEosBalance(){
        EosUsdtBalance balance = new EosUsdtBalance();
        balance.balance = "0";
        balance.tokenName = SafeColdSettings.EOS;
        balance.coinType = CoinType.TYPE_EOS;
        return balance;
    }

    public static EosUsdtBalance getZeroUsdtBalance(){
        EosUsdtBalance balance = new EosUsdtBalance();
        balance.balance = "0";
        balance.tokenName = SafeColdSettings.USDT;
        balance.coinType = CoinType.TYPE_USDT;
        return balance;
    }
}
