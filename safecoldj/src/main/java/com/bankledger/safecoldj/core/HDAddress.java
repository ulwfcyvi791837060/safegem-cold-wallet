package com.bankledger.safecoldj.core;

import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.utils.Utils;

import java.io.Serializable;

public class HDAddress implements Comparable<HDAddress>, Serializable {

    public enum AvailableState {
        STATE_AVAILABLE(0),
        STATE_DISABLED(1);

        private int value;

        AvailableState(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    private String coin;
    private String address;
    private String alias;

    private int addressIndex;
    private AbstractHD.PathType pathType;
    private byte[] pubKey;
    private AvailableState availableState;

    public HDAddress() {

    }

    public HDAddress(byte[] pubKey, AbstractHD.PathType pathType, int addressIndex, Currency mCurrency) {
        this(Utils.toAddress(Utils.sha256hash160(pubKey), mCurrency), pubKey, pathType, addressIndex, mCurrency.coin, "");
    }

    public HDAddress(String address, byte[] pubKey, AbstractHD.PathType pathType, int
            addressIndex, String coin, String alias) {
        this(address, pubKey, pathType, addressIndex, coin, alias, AvailableState.STATE_AVAILABLE);
    }

    public HDAddress(String address, byte[] pubKey, AbstractHD.PathType pathType, int
            addressIndex, String coin, String alias, AvailableState availableState) {
        this.address = address;
        this.addressIndex = addressIndex;
        this.pathType = pathType;
        this.pubKey = pubKey;
        this.alias = alias;
        this.coin = coin;
        this.availableState = availableState;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAddressIndex() {
        return addressIndex;
    }

    public void setAddressIndex(int addressIndex) {
        this.addressIndex = addressIndex;
    }

    public AbstractHD.PathType getPathType() {
        return pathType;
    }

    public void setPathType(AbstractHD.PathType pathType) {
        this.pathType = pathType;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public AvailableState getAvailableState() {
        return availableState;
    }

    public void setAvailableState(AvailableState availableState) {
        this.availableState = availableState;
    }

    public boolean isUsdt() {
        return SafeColdSettings.USDT.equalsIgnoreCase(coin);
    }


    @Override
    public int compareTo(HDAddress address) {
        return coin.compareTo(address.coin);
    }

    @Override
    public String toString() {
        return "HDAddress{" +
                "coin='" + coin + '\'' +
                ", address='" + address + '\'' +
                ", alias='" + alias + '\'' +
                ", addressIndex=" + addressIndex +
                ", pathType=" + pathType +
                ", availableState=" + availableState +
                '}';
    }
}