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

public abstract class AbstractDb {

    public static final String CREATE_OUTS_SQL = "create table if not exists " + Tables.OUTS + "(" +
            OutsColumns.TX_HASH + " text not null," +
            OutsColumns.OUT_SN + " integer not null," +
            OutsColumns.COIN + " text not null," +
            OutsColumns.OUT_VALUE + " integer not null," +
            OutsColumns.OUT_STATUS + " integer not null," +
            OutsColumns.MUL_TYPE + " integer not null," +
            OutsColumns.OUT_ADDRESS + " text not null," +
            OutsColumns.UN_LOCK_HEIGHT + " integer," +
            OutsColumns.RESERVE + " text," +
            "primary key (" + OutsColumns.TX_HASH + "," + OutsColumns.OUT_SN + "));";

    public static final String CREATE_HD_ACCOUNT = "create table if not exists " + Tables.HD_ACCOUNT + "(" +
            HDAccountColumns.HD_ACCOUNT_ID + " integer not null primary key autoincrement," +
            HDAccountColumns.ENCRYPT_SEED + " text," +
            HDAccountColumns.ENCRYPT_MNEMONIC_SEED + " text);";

    public static final String CREATE_HD_ADDRESSES = "create table if not exists " + Tables.HD_ADDRESS + "(" +
            HDAddressesColumns.ADDRESS + " text not null," +
            HDAddressesColumns.ADDRESS_INDEX + " integer not null," +
            HDAddressesColumns.PATH_TYPE + " integer not null," +
            HDAddressesColumns.PUB_KEY + " text not null," +
            HDAddressesColumns.ALIAS + " text not null," +
            HDAddressesColumns.COIN + " text not null," +
            HDAddressesColumns.AVAILABLE_STATE + " integer not null," +
            "primary key (" + HDAddressesColumns.ADDRESS + ", " + HDAddressesColumns.COIN + "));";

    public static final String CREATE_COIN_ROOT_KEY = "create table if not exists " + Tables.COIN_ROOT_KEY + "(" +
            CoinRootKeyColumns.COIN + " text not null," +
            CoinRootKeyColumns.ROOT_KEY + " text not null," +
            "primary key (" + CoinRootKeyColumns.COIN + "))";

    public static final String CREATE_CONTACTS_ADDRESS = "create table if not exists " + Tables.CONTACTS_ADDRESS + "(" +
            ContactsAddressColumns.COIN + " text not null," +
            ContactsAddressColumns.ADDRESS + " text not null," +
            ContactsAddressColumns.CONTRACT_ADDRESS + " text," +
            ContactsAddressColumns.COIN_TYPE + " integer not null," +
            ContactsAddressColumns.ALIAS + " text," +
            "primary key (" + ContactsAddressColumns.COIN + "," + ContactsAddressColumns.ADDRESS + "," + ContactsAddressColumns.ALIAS + "))";

    public static final String CREATE_ETH_TOKEN = "create table if not exists " + Tables.ETH_TOKEN + "(" +
            ETHTokenColumns.ADDRESS + " text not null," +
            ETHTokenColumns.BALANCE + " text not null," +
            ETHTokenColumns.IS_TOKEN + " integer not null," +
            ETHTokenColumns.TRANSACTION_COUNT + " integer not null," +
            ETHTokenColumns.NAME + " text," +
            ETHTokenColumns.SYMBOL + " text," +
            ETHTokenColumns.DECIMALS + " integer," +
            ETHTokenColumns.TOTAL_SUPPLY + " text," +
            ETHTokenColumns.CONTRACT_ADDRESS + " text," +
            ETHTokenColumns.ALIAS + " text," +
            "primary key (" + ETHTokenColumns.CONTRACT_ADDRESS + "))";

    public static final String CREATE_EOS_ACCOUNT = "create table if not exists " + Tables.EOS_ACCOUNT + "(" +
            EosAccountColumns.OWNER_PUB_KEY + " text not null," +
            EosAccountColumns.ACTIVE_PUB_KEY + " text not null," +
            EosAccountColumns.ACCOUNT_NAME + " text null," +
            EosAccountColumns.OWNER_PRIV_KEY + " text null," +
            EosAccountColumns.ACTIVE_PRIV_KEY + " text null," +
            EosAccountColumns.OP_TYPE + " integer not null," +
            EosAccountColumns.AVAILABLE_STATE + " integer not null," +
            "primary key (" + EosAccountColumns.OWNER_PUB_KEY + "," + EosAccountColumns.ACTIVE_PUB_KEY + "," + EosAccountColumns.ACCOUNT_NAME + "," + EosAccountColumns.OP_TYPE + "))";

    public static final String CREATE_EOS_USDT_BALANCE = "create table if not exists " + Tables.EOS_UDST_BALANCE + "(" +
            EosUsdtBalanceColumns.BALANCE + " text not null," +
            EosUsdtBalanceColumns.TOKEN_NAME + " text not null," +
            EosUsdtBalanceColumns.COIN_TYPE + " integer not null," +
            "primary key (" + EosUsdtBalanceColumns.TOKEN_NAME + "," + EosUsdtBalanceColumns.COIN_TYPE + "))";

    public static final String CREATE_HD_ADDRESS_INDEX = "create index idx_hd_address_address on hd_addresses (address);";

    public static IOutProvider outProvider;
    public static IHDAccountProvider hdAccountProvider;
    public static IHDAddressProvider hdAddressProvider;
    public static ICoinRootKeyProvider coinRootKeyProvider;
    public static IContactsAddressProvider contactsAddressProvider;
    public static IETHTokenProvider ethTokenProvider;
    public static IEosAccountProvider eosAccountProvider;
    public static IEosUsdtBalanceProvider eosUsdtBalanceProvider;

    public void construct() {
        outProvider = initOutProvider();
        hdAccountProvider = initHDAccountProvider();
        hdAddressProvider = initHDAddressProvider();
        coinRootKeyProvider = initCoinRootKeyProvider();
        contactsAddressProvider = initContactsAddressProvider();
        ethTokenProvider = initETHTokenProvider();
        eosAccountProvider = initEosAccountProvider();
        eosUsdtBalanceProvider = initEosUsdtBalanceProvider();
    }

    public abstract IOutProvider initOutProvider();

    public abstract IHDAccountProvider initHDAccountProvider();

    public abstract IHDAddressProvider initHDAddressProvider();

    public abstract ICoinRootKeyProvider initCoinRootKeyProvider();

    public abstract IContactsAddressProvider initContactsAddressProvider();

    public abstract IETHTokenProvider initETHTokenProvider();

    public abstract IEosAccountProvider initEosAccountProvider();

    public abstract IEosUsdtBalanceProvider initEosUsdtBalanceProvider();

    public interface Tables {
        String OUTS = "outs";
        String HD_ACCOUNT = "hd_account";
        String HD_ADDRESS = "hd_addresses";
        String COIN_ROOT_KEY = "coin_root_key";
        String CONTACTS_ADDRESS = "contacts_address";
        String ETH_TOKEN = "eth_token";
        String EOS_ACCOUNT = "eos_account";
        String EOS_UDST_BALANCE = "eos_usdt_balance";
    }

    public interface OutsColumns {
        String TX_HASH = "tx_hash";
        String OUT_SN = "out_sn";
        String COIN = "coin";
        String OUT_VALUE = "out_value";
        String OUT_STATUS = "out_status";
        String MUL_TYPE = "mul_type";
        String OUT_ADDRESS = "out_address";
        String UN_LOCK_HEIGHT = "un_lock_height";
        String RESERVE = "reserve";
    }

    public interface HDAccountColumns {
        String HD_ACCOUNT_ID = "hd_account_id";
        String ENCRYPT_SEED = "encrypt_seed";
        String ENCRYPT_MNEMONIC_SEED = "encrypt_mnemonic_seed";
    }

    public interface HDAddressesColumns {
        String ADDRESS = "address";
        String ADDRESS_INDEX = "address_index";
        String PATH_TYPE = "path_type";
        String PUB_KEY = "pub_key";
        String ALIAS = "alias";
        String COIN = "coin";
        String AVAILABLE_STATE = "available_state";
    }

    public interface CoinRootKeyColumns {
        String COIN = "coin";
        String ROOT_KEY = "root_key";
    }

    public interface ContactsAddressColumns {
        String COIN = "coin";
        String ADDRESS = "address";
        String CONTRACT_ADDRESS = "contract_address";
        String COIN_TYPE = "coin_type";
        String ALIAS = "alias";
    }

    public interface ETHTokenColumns {
        String IS_TOKEN = "is_token";
        String ADDRESS = "address";
        String BALANCE = "balance";
        String TRANSACTION_COUNT = "transaction_count";
        String NAME = "name";
        String SYMBOL = "symbol";
        String DECIMALS = "decimals";
        String TOTAL_SUPPLY = "total_supply";
        String CONTRACT_ADDRESS = "contract_address";
        String ALIAS = "alias";
    }

    public interface EosAccountColumns {
        String OWNER_PUB_KEY = "owner_pub_key";
        String ACTIVE_PUB_KEY = "active_pub_key";
        String ACCOUNT_NAME = "account_name";
        String OWNER_PRIV_KEY = "owner_priv_key";
        String ACTIVE_PRIV_KEY = "active_priv_key";
        String OP_TYPE = "op_type";
        String AVAILABLE_STATE = "available_state";
    }

    public interface EosUsdtBalanceColumns {
        String BALANCE = "balance";
        String TOKEN_NAME = "token_name";
        String COIN_TYPE = "coin_type";
    }

}
