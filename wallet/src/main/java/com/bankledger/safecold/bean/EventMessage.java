package com.bankledger.safecold.bean;

import com.bankledger.safecoldj.utils.GsonUtils;

public class EventMessage {

    public final static int TYPE_CREATE_HDACCOUNT = 1;
    public final static int TYPE_MINE_ADDRESS_CHANGED = 2;
    public final static int TYPE_CONTACT_ADDRESS_CHANGED = 3;
    public final static int TYPE_BALANCE_CHANGED = 4;
    public final static int TYPE_ACTIVE = 5;
    public final static int TYPE_TOKEN_CHANGED = 6;

    public int eventType;

    public Object eventObj;

    public EventMessage(int eventType) {
        this.eventType = eventType;
    }

    public EventMessage(int eventType, Object eventObj) {
        this.eventType = eventType;
        this.eventObj = eventObj;
    }

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }
}
