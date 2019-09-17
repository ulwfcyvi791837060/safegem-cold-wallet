package com.bankledger.safecoldj.core;

import com.bankledger.safecoldj.utils.GsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Created by zm on 2019/1/7.
 */
public class SafeAsset implements Serializable {

    public String assetId; // 资产ID

    public String assetShortName; // 资产简称

    public String assetName; // 资产名称

    public String assetUnit; // 资产单位

    public long assetDecimals; // 最小单位

    public String assetBalance; // 资产余额

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }
}
