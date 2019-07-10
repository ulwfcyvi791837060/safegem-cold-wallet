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

package com.bankledger.safecoldj;

public class SafeColdSettings {

    public static final boolean SEED_SAVE_TO_CHIP = true; //是否存入芯片

    public static final boolean DEV_DEBUG = false; // true 测试环境， false 正式环境

    public static final int MAX_TX_SIZE = 100000;

    public static final int MAX_BLOCK_SIZE = 1 * 1000 * 1000;

    public static final long MAX_MONEY = 21000000L * 100000000l;

    public static final boolean ensureMinFee = true;

    public static final int MAX_HDADDRESS_COUNT = 5;//单币种地址最大数量

    public static final int MAX_ONCE_SEND_COUNT = 5;//次发送添加接收人个数

    public static final String BTC = "BTC";
    public static final String LTC = "LTC";
    public static final String SAFE = "SAFE";
    public static final String DASH = "DASH";
    public static final String QTUM = "QTUM";
    public static final String BCH = "BCH";
    public static final String BSV = "BSV";
    public static final String BTG = "BTG";
    public static final String ETH = "ETH";
    public static final String FTO = "FTO";
    public static final String ETC = "ETC";
    public static final String USDT = "USDT";
    public static final String EOS = "EOS";

    public static final String BTC_FULL_NAME = "bitcoin";
    public static final String LTC_FULL_NAME = "litecoin";
    public static final String SAFE_FULL_NAME = "safe";
    public static final String DASH_FULL_NAME = "dash";
    public static final String QTUM_FULL_NAME = "qtum";
    public static final String BCH_FULL_NAME = "bitcoincash";
    public static final String BSV_FULL_NAME = "bitcoinsv";
    public static final String BTG_FULL_NAME = "bitcoingold";
    public static final String FTO_FULL_NAME = "futurocoin";
    public static final String USDT_FULL_NAME = "tether";
    public static final String EOS_FULL_NAME = "eos";
    public static final String ETH_FULL_NAME = "ethereum";
    public static final String ETC_FULL_NAME = "ethereumclassic";

}
