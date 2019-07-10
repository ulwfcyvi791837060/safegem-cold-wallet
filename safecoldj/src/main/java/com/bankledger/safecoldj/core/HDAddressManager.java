/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bankledger.safecoldj.core;

import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.db.AbstractDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HDAddressManager {

    private final byte[] lock = new byte[0];

    private Map<String, Currency> currencyMap = new HashMap<>();

    private Map<String, String> fullNameMap = new HashMap<>();

    private static HDAddressManager uniqueInstance = new HDAddressManager();

    private HDAddressManager() {
        synchronized (lock) {
            initCurrencys();
        }
    }

    public static HDAddressManager getInstance() {
        return uniqueInstance;
    }

    public boolean hasMnemonicSeed() {
        synchronized (lock) {
            return AbstractDb.hdAccountProvider.hasMnemonicSeed();
        }
    }

    public HDAccount getHDAccount(CharSequence password) {
        synchronized (lock) {
            if (AbstractDb.hdAccountProvider.hasMnemonicSeed()) {
                return new HDAccount(password);
            }
            return null;
        }
    }

    public HDAccount getHDAccount() {
        synchronized (lock) {
            if (AbstractDb.hdAccountProvider.hasMnemonicSeed()) {
                return new HDAccount();
            }
            return null;
        }
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public List<Currency> getCurrencyList() {
        List<Currency> currencyList = new ArrayList<>();
        currencyList.addAll(currencyMap.values());
        Collections.sort(currencyList);
        return currencyList;
    }

    public String getFullNameCoin(String fullName) {
        return fullNameMap.get(fullName);
    }

    public Currency getCurrencyCoin(String coin) {
        return currencyMap.get(coin);
    }

    private void initCurrencys() {

        //SAFE
        Currency safe = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            safe.addressHeader = 76;
            safe.p2shHeader = 16;
            safe.dumpedPrivateKeyHeader = 204;
        } else {
            safe.addressHeader = 76;
            safe.p2shHeader = 16;
            safe.dumpedPrivateKeyHeader = 204;
        }
        safe.protocolVersion = 70206;
        safe.txVersion = 103;
        safe.coin = SafeColdSettings.SAFE;
        safe.fullName = SafeColdSettings.SAFE_FULL_NAME;
        safe.lowFee = 10000;
        safe.normalFee = 20000;
        safe.highFee = 200000;
        safe.pathType = AbstractHD.PathType.KEY_PATH_SAFE;
        currencyMap.put(safe.coin, safe);

        //比特币
        Currency btc = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            btc.addressHeader = 111;
            btc.p2shHeader = 196;
            btc.dumpedPrivateKeyHeader = 239;
        } else {
            btc.addressHeader = 0;
            btc.p2shHeader = 5;
            btc.dumpedPrivateKeyHeader = 128;
        }
        btc.protocolVersion = 70001;
        btc.txVersion = 1;
        btc.coin = SafeColdSettings.BTC;
        btc.fullName = SafeColdSettings.BTC_FULL_NAME;
        btc.lowFee = 1000;
        btc.normalFee = 20000;
        btc.highFee = 200000;
        btc.pathType = AbstractHD.PathType.KEY_PATH_BTC;
        currencyMap.put(btc.coin, btc);

        //泰达币
        Currency usdt = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            usdt.addressHeader = 111;
            usdt.p2shHeader = 196;
            usdt.dumpedPrivateKeyHeader = 239;
        } else {
            usdt.addressHeader = 0;
            usdt.p2shHeader = 5;
            usdt.dumpedPrivateKeyHeader = 128;
        }
        usdt.protocolVersion = 70001;
        usdt.txVersion = 1;
        usdt.coin = SafeColdSettings.USDT;
        usdt.fullName = SafeColdSettings.USDT_FULL_NAME;
        usdt.lowFee = 1000;
        usdt.normalFee = 20000;
        usdt.highFee = 200000;
        usdt.pathType = AbstractHD.PathType.KEY_PATH_BTC;
        currencyMap.put(usdt.coin, usdt);

        //莱特币
        Currency ltc = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            ltc.addressHeader = 111;
            ltc.p2shHeader = 196;
            ltc.dumpedPrivateKeyHeader = 239;
        } else {
            ltc.addressHeader = 48;
            ltc.p2shHeader = 5;
            ltc.dumpedPrivateKeyHeader = 176;
        }
        ltc.protocolVersion = 70002;
        ltc.txVersion = 1;
        ltc.coin = SafeColdSettings.LTC;
        ltc.fullName = SafeColdSettings.LTC_FULL_NAME;
        ltc.lowFee = 100000;
        ltc.normalFee = 100000;
        ltc.highFee = 2000000;
        ltc.pathType = AbstractHD.PathType.KEY_PATH_LTC;
        currencyMap.put(ltc.coin, ltc);

        //BCHABC
        Currency bch = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            bch.addressHeader = 111;
            bch.p2shHeader = 196;
            bch.dumpedPrivateKeyHeader = 239;
        } else {
            bch.addressHeader = 0;
            bch.p2shHeader = 5;
            bch.dumpedPrivateKeyHeader = 128;
        }
        bch.protocolVersion = 70015;
        bch.txVersion = 1;
        bch.coin = SafeColdSettings.BCH;
        bch.fullName = SafeColdSettings.BCH_FULL_NAME;
        bch.lowFee = 1000;
        bch.normalFee = 10000;
        bch.highFee = 20000;
        bch.pathType = AbstractHD.PathType.KEY_PATH_BCH;
        currencyMap.put(bch.coin, bch);

        //BSV
        Currency bsv = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            bsv.addressHeader = 111;
            bsv.p2shHeader = 196;
            bsv.dumpedPrivateKeyHeader = 239;
        } else {
            bsv.addressHeader = 0;
            bsv.p2shHeader = 5;
            bsv.dumpedPrivateKeyHeader = 128;
        }
        bsv.protocolVersion = 70015;
        bsv.txVersion = 1;
        bsv.coin = SafeColdSettings.BSV;
        bsv.fullName = SafeColdSettings.BSV_FULL_NAME;
        bsv.lowFee = 1000;
        bsv.normalFee = 10000;
        bsv.highFee = 20000;
        bsv.pathType = AbstractHD.PathType.KEY_PATH_BSV;
        currencyMap.put(bsv.coin, bsv);

        //比特币黄金
        Currency btg = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            btg.addressHeader = 111;
            btg.p2shHeader = 196;
            btg.dumpedPrivateKeyHeader = 239;
        } else {
            btg.addressHeader = 38;
            btg.p2shHeader = 23;
            btg.dumpedPrivateKeyHeader = 128;
        }
        btg.protocolVersion = 70016;
        btg.txVersion = 1;
        btg.coin = SafeColdSettings.BTG;
        btg.fullName = SafeColdSettings.BTG_FULL_NAME;
        btg.lowFee = 1000;
        btg.normalFee = 10000;
        btg.highFee = 200000;
        btg.pathType = AbstractHD.PathType.KEY_PATH_BTG;
        currencyMap.put(btg.coin, btg);

        //达世币
        Currency dash = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            dash.addressHeader = 140;
            dash.p2shHeader = 19;
            dash.dumpedPrivateKeyHeader = 204;
        } else {
            dash.addressHeader = 76;
            dash.p2shHeader = 16;
            dash.dumpedPrivateKeyHeader = 204;
        }
        dash.protocolVersion = 70210;
        dash.txVersion = 1;
        dash.coin = SafeColdSettings.DASH;
        dash.fullName = SafeColdSettings.DASH_FULL_NAME;
        dash.lowFee = 1000;
        dash.normalFee = 50000;
        dash.highFee = 200000;
        dash.pathType = AbstractHD.PathType.KEY_PATH_DASH;
        currencyMap.put(dash.coin, dash);

        //量子链
        Currency qtum = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            qtum.addressHeader = 120;
            qtum.p2shHeader = 110;
            qtum.dumpedPrivateKeyHeader = 239;
        } else {
            qtum.addressHeader = 58;
            qtum.p2shHeader = 50;
            qtum.dumpedPrivateKeyHeader = 128;
        }
        qtum.protocolVersion = 70016;
        qtum.txVersion = 1;
        qtum.coin = SafeColdSettings.QTUM;
        qtum.fullName = SafeColdSettings.QTUM_FULL_NAME;
        qtum.lowFee = 300000;
        qtum.normalFee = 500000;
        qtum.highFee = 100000000;
        qtum.pathType = AbstractHD.PathType.KEY_PATH_QTUM;
        currencyMap.put(qtum.coin, qtum);

        //FTO正式
        Currency fto = new Currency();
        if (SafeColdSettings.DEV_DEBUG) {
            fto.addressHeader = 36;
            fto.p2shHeader = 13;
            fto.dumpedPrivateKeyHeader = 164;
        } else {
            fto.addressHeader = 36;
            fto.p2shHeader = 13;
            fto.dumpedPrivateKeyHeader = 164;
        }
        fto.protocolVersion = 70208;
        fto.txVersion = 1;
        fto.coin = SafeColdSettings.FTO;
        fto.fullName = SafeColdSettings.FTO_FULL_NAME;
        fto.lowFee = 60000;
        fto.normalFee = 70000;
        fto.highFee = 120000;
        fto.pathType = AbstractHD.PathType.KEY_PATH_FTO;
        currencyMap.put(fto.coin, fto);

        fullNameMap.put(SafeColdSettings.BTC_FULL_NAME, SafeColdSettings.BTC);
        fullNameMap.put(SafeColdSettings.LTC_FULL_NAME, SafeColdSettings.LTC);
        fullNameMap.put(SafeColdSettings.SAFE_FULL_NAME, SafeColdSettings.SAFE);
        fullNameMap.put(SafeColdSettings.DASH_FULL_NAME, SafeColdSettings.DASH);
        fullNameMap.put(SafeColdSettings.QTUM_FULL_NAME, SafeColdSettings.QTUM);
        fullNameMap.put(SafeColdSettings.BCH_FULL_NAME, SafeColdSettings.BCH);
        fullNameMap.put(SafeColdSettings.BSV_FULL_NAME, SafeColdSettings.BSV);
        fullNameMap.put(SafeColdSettings.BTG_FULL_NAME, SafeColdSettings.BTG);
        fullNameMap.put(SafeColdSettings.FTO_FULL_NAME, SafeColdSettings.FTO);
        fullNameMap.put(SafeColdSettings.USDT_FULL_NAME, SafeColdSettings.USDT);
        fullNameMap.put(SafeColdSettings.EOS_FULL_NAME, SafeColdSettings.EOS);
        fullNameMap.put(SafeColdSettings.ETH_FULL_NAME, SafeColdSettings.ETH);
        fullNameMap.put(SafeColdSettings.ETC_FULL_NAME, SafeColdSettings.ETC);
    }


}
