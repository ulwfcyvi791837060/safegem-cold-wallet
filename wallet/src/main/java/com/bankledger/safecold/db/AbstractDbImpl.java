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

package com.bankledger.safecold.db;


import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.ICoinRootKeyProvider;
import com.bankledger.safecoldj.db.IContactsAddressProvider;
import com.bankledger.safecoldj.db.IETHTokenProvider;
import com.bankledger.safecoldj.db.IEosAccountProvider;
import com.bankledger.safecoldj.db.IEosUsdtBalanceProvider;
import com.bankledger.safecoldj.db.IHDAddressProvider;
import com.bankledger.safecoldj.db.IHDAccountProvider;
import com.bankledger.safecoldj.db.IOutProvider;

public class AbstractDbImpl extends AbstractDb {

    @Override
    public IOutProvider initOutProvider() {
        return OutProvider.getInstance();
    }

    @Override
    public IHDAccountProvider initHDAccountProvider() {
        return HDAccountProvider.getInstance();
    }

    @Override
    public IHDAddressProvider initHDAddressProvider() {
        return HDAddressProvider.getInstance();
    }

    @Override
    public ICoinRootKeyProvider initCoinRootKeyProvider() {
        return CoinRootKeyProvider.getInstance();
    }

    @Override
    public IContactsAddressProvider initContactsAddressProvider() {
        return ContactsAddressProvider.getInstance();
    }

    @Override
    public IETHTokenProvider initETHTokenProvider() {
        return ETHTokenProvider.getInstance();
    }

    @Override
    public IEosAccountProvider initEosAccountProvider() {
        return EosAccountProvider.getInstance();
    }

    @Override
    public IEosUsdtBalanceProvider initEosUsdtBalanceProvider() {
        return EosUsdtBalanceProvider.getInstance();
    }
}
