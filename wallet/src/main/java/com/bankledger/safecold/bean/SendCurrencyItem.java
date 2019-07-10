package com.bankledger.safecold.bean;

import com.bankledger.safecoldj.utils.GsonUtils;

import java.io.Serializable;

/**
 * @author bankledger
 * @time 2018/8/16 11:25
 */
public class SendCurrencyItem implements Serializable{

    public String address;
    public String amount;

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }
}
