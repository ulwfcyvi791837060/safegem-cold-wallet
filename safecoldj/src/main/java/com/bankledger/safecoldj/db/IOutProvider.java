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

package com.bankledger.safecoldj.db;

import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.core.SafeAsset;

import java.util.List;

public interface IOutProvider {

    void addOut(Out out);

    void addOut(Out out, SafeAsset safeAsset);

    void clearOuts(String coin);

    void clearOuts(String coin, String assetId);

    List<Out> getUnspendOutsWithCoin(String coin);

    List<Out> getUnspendOutsWithCoinSafeAsset(String coin, String assetId);

    List<Out> getUnspendOutsWithAddress(String address);

    List<Out> getUnspendOutsWithAddressSafeAsset(String address, String assetId);

    long getBalanceWithAddress(String address);

    long getBalanceWithAddressSafeAsset(String address, String assetId);

    long getBalanceWithCoin(String coin);

    long getBalanceWithCoinSafeAsset(String coin, String assetId);

    void labelOutSpend(String txHash, int outSn);

    Out getOut(String txHash, int outSn);

    List<String> getSafeAsset();

    SafeAsset getSafeAsset(String assetName);

}
