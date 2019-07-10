package com.bankledger.safecoldj.qrcode;

import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.entity.ETHToken;

import java.util.List;

/**
 * @author bankledger
 * @time 2018/9/4 14:22
 */
public class SyncBalanceBean {
    private String deviceId;
    private List<Out> outList;
    private List<ETHToken> ethTokenList;

    public SyncBalanceBean(String deviceId, List<Out> outList, List<ETHToken> ethTokenList) {
        this.deviceId = deviceId;
        this.outList = outList;
        this.ethTokenList = ethTokenList;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public List<Out> getOutList() {
        return outList;
    }

    public void setOutList(List<Out> outList) {
        this.outList = outList;
    }

    public List<ETHToken> getEthTokenList() {
        return ethTokenList;
    }

    public void setEthTokenList(List<ETHToken> ethTokenList) {
        this.ethTokenList = ethTokenList;
    }

    @Override
    public String toString() {
        return "SyncBalanceBean{" +
                "deviceId='" + deviceId + '\'' +
                ", outList=" + outList +
                ", ethTokenList=" + ethTokenList +
                '}';
    }
}
