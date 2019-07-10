package com.bankledger.safecoldj.core;

import com.bankledger.safecoldj.db.AbstractDb;

import java.io.Serializable;

/**
 * Created by zm on 2018/11/13.
 */

public class EosAccount implements Serializable {

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

    public enum OpType {

        TYPE_CREATE(1),
        TYPE_IMPORT(2);

        private int value;

        OpType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

    }

    protected String ownerPubKey;
    protected String activePubKey;
    protected String accountName;
    protected String ownerPrivKey;
    protected String activePrivKey;
    protected OpType opType;
    protected AvailableState state;

    public String getOwnerPubKey() {
        return ownerPubKey;
    }

    public void setOwnerPubKey(String ownerPubKey) {
        this.ownerPubKey = ownerPubKey;
    }

    public String getActivePubKey() {
        return activePubKey;
    }

    public void setActivePubKey(String activePubKey) {
        this.activePubKey = activePubKey;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public AvailableState getState() {
        return state;
    }

    public void setState(AvailableState state) {
        this.state = state;
    }

    public String getOwnerPrivKey() {
        return ownerPrivKey;
    }

    public void setOwnerPrivKey(String ownerPrivKey) {
        this.ownerPrivKey = ownerPrivKey;
    }

    public String getActivePrivKey() {
        return activePrivKey;
    }

    public void setActivePrivKey(String activePrivKey) {
        this.activePrivKey = activePrivKey;
    }

    public OpType getOpType() {
        return opType;
    }

    public void setOpType(OpType opType) {
        this.opType = opType;
    }

    @Override
    public String toString() {
        return "EosAccount{" +
                "ownerPubKey='" + ownerPubKey + '\'' +
                ", activePubKey='" + activePubKey + '\'' +
                ", accountName='" + accountName + '\'' +
                ", ownerPrivKey='" + ownerPrivKey + '\'' +
                ", activePrivKey='" + activePrivKey + '\'' +
                ", opType=" + opType +
                ", state=" + state +
                '}';
    }
}
