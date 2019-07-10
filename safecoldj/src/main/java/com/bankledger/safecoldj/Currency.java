package com.bankledger.safecoldj;

import com.bankledger.safecoldj.core.AbstractHD;
import com.bankledger.safecoldj.utils.GsonUtils;

import java.io.Serializable;

/**
 * Created by zm on 2018/6/21.
 */

public class Currency implements Serializable, Comparable<Currency> {

    public int addressHeader;
    public int p2shHeader;
    public int dumpedPrivateKeyHeader;
    public String coin;
    public String fullName;
    public String selectAddress;
    public int protocolVersion;
    public int txVersion;
    public long lowFee;
    public long normalFee;
    public long highFee;

    public AbstractHD.PathType pathType;

    public boolean isQtum() {
        return SafeColdSettings.QTUM.equalsIgnoreCase(coin);
    }

    public boolean isLtc() {
        return SafeColdSettings.LTC.equalsIgnoreCase(coin);
    }

    public boolean isSafe() {
        return SafeColdSettings.SAFE.equalsIgnoreCase(coin);
    }

    public boolean isBchOrBsv() {
        return SafeColdSettings.BCH.equalsIgnoreCase(coin) || SafeColdSettings.BSV.equalsIgnoreCase(coin);
    }

    public boolean isBtg() {
        return SafeColdSettings.BTG.equalsIgnoreCase(coin);
    }

    public boolean isUsdt() {
        return SafeColdSettings.USDT.equalsIgnoreCase(coin);
    }

    public boolean isDashBranch() { //是否达世分叉
        return SafeColdSettings.DASH.equalsIgnoreCase(coin) || isSafe() || SafeColdSettings.FTO.equalsIgnoreCase(coin);
    }

    @Override
    public int compareTo(Currency o) {
        return coin.compareTo(o.coin);
    }

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }
}
