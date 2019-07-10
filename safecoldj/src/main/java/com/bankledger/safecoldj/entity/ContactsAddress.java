package com.bankledger.safecoldj.entity;

import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.utils.GsonUtils;

import java.io.Serializable;

/**
 * 联系人地址
 *
 * @author bankledger
 * @time 2018/9/8 17:13
 */
public class ContactsAddress implements Comparable<ContactsAddress>, Serializable {

    public static final int COIN_TYPE_CURRENCY = 0;
    public static final int COIN_TYPE_ETH = 1;
    public static final int COIN_TYPE_TOKEN = 2;
    public static final int COIN_TYPE_ETC = 3;
    public static final int COIN_TYPE_EOS = 4;
    public static final int COIN_TYPE_SAFE_ASSET = 5;
    public static final int COIN_TYPE_EOS_TOKEN = 6;

    private String coin;
    private String address;
    private String alias;
    private String contractAddress;
    private int coinType;

    public ContactsAddress() {
    }

    public ContactsAddress(String coin, String address, String alias, String contractAddress, int coinType) {
        this.coin = coin;
        this.address = address;
        this.alias = alias;
        this.contractAddress = contractAddress;
        this.coinType = coinType;
    }

    public static ContactsAddress createSafeAssetContactsAddress(String coin, String address, String alias) {
        ContactsAddress contactsAddress = new ContactsAddress();
        contactsAddress.coin = coin;
        contactsAddress.address = address;
        contactsAddress.alias = alias;
        contactsAddress.coinType = COIN_TYPE_SAFE_ASSET;
        return contactsAddress;
    }

    public static ContactsAddress createCurrencyContactsAddress(String coin, String address, String alias) {
        ContactsAddress contactsAddress = new ContactsAddress();
        contactsAddress.coin = coin;
        contactsAddress.address = address;
        contactsAddress.alias = alias;
        contactsAddress.coinType = COIN_TYPE_CURRENCY;
        return contactsAddress;
    }

    public static ContactsAddress createETHContactsAddress(String coin, String address, String alias, String contractAddress) {
        ContactsAddress contactsAddress = new ContactsAddress();
        contactsAddress.coin = coin;
        contactsAddress.address = address;
        contactsAddress.alias = alias;
        contactsAddress.coinType = COIN_TYPE_ETH;
        contactsAddress.contractAddress = contractAddress;
        return contactsAddress;
    }

    public static ContactsAddress createETCContactsAddress(String coin, String address, String alias, String contractAddress) {
        ContactsAddress contactsAddress = new ContactsAddress();
        contactsAddress.coin = coin;
        contactsAddress.address = address;
        contactsAddress.alias = alias;
        contactsAddress.coinType = COIN_TYPE_ETC;
        contactsAddress.contractAddress = contractAddress;
        return contactsAddress;
    }

    public static ContactsAddress createTokenContactsAddress(String name, String address, String alias, String contractAddress) {
        ContactsAddress contactsAddress = new ContactsAddress();
        contactsAddress.coin = name;
        contactsAddress.address = address;
        contactsAddress.alias = alias;
        contactsAddress.coinType = COIN_TYPE_TOKEN;
        contactsAddress.contractAddress = contractAddress;
        return contactsAddress;
    }


    public static ContactsAddress createEosContactsAccount(String account, String alias) {
        ContactsAddress contactsAddress = new ContactsAddress();
        contactsAddress.coin = SafeColdSettings.EOS;
        contactsAddress.address = account;
        contactsAddress.alias = alias;
        contactsAddress.coinType = COIN_TYPE_EOS;
        return contactsAddress;
    }

    public static ContactsAddress createEosTokenContactsAccount(String account, String token, String alias) {
        ContactsAddress contactsAddress = new ContactsAddress();
        contactsAddress.coin = token;
        contactsAddress.address = account;
        contactsAddress.alias = alias;
        contactsAddress.coinType = COIN_TYPE_EOS_TOKEN;
        return contactsAddress;
    }

    public int getCoinType() {
        return coinType;
    }

    public void setCoinType(int coinType) {
        this.coinType = coinType;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public boolean isCurrencyAddress() {
        return coinType == COIN_TYPE_CURRENCY;
    }

    public boolean isETHAddress() {
        return coinType == COIN_TYPE_ETH;
    }

    public boolean isETCAddress() {
        return coinType == COIN_TYPE_ETC;
    }

    public boolean isTokenAddress() {
        return coinType == COIN_TYPE_TOKEN;
    }

    public boolean isEosAddress() {
        return coinType == COIN_TYPE_EOS;
    }

    public boolean isEosTokenAddress() {
        return coinType == COIN_TYPE_EOS_TOKEN;
    }

    public boolean isSafeAsset() {
        return coinType == COIN_TYPE_SAFE_ASSET;
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public int compareTo(ContactsAddress o) {
        return getCoin().compareTo(o.getCoin());
    }

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }
}
