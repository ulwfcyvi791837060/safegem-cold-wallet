package com.bankledger.safecold.bean;

import android.support.annotation.NonNull;

import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.utils.CurrencyNameUtil;
import com.bankledger.safecold.utils.StringUtil;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.core.Out;
import com.bankledger.safecoldj.core.SafeAsset;
import com.bankledger.safecoldj.entity.ETHToken;
import com.bankledger.safecoldj.utils.GsonUtils;

import java.io.Serializable;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/9/12 10:39
 */
public class ConvertViewBean implements Serializable, Comparable<ConvertViewBean> {

    public static final int TYPE_CURRENCY = 0;
    public static final int TYPE_ETH_TOKEN = 1;
    public static final int TYPE_EOS_COIN = 2;
    public static final int TYPE_SAFE_ASSET = 3;

    public int type;

    public Currency currency;
    public ETHToken ethToken;
    public EosBalance eosBalance;
    public SafeAsset safeAsset;

    public String balance;

    public static ConvertViewBean currencyConvert(Currency currency) {
        ConvertViewBean addressView = new ConvertViewBean();
        addressView.type = TYPE_CURRENCY;
        addressView.currency = currency;
        return addressView;
    }

    public static ConvertViewBean ethTokenConvert(ETHToken ethToken) {
        ConvertViewBean addressView = new ConvertViewBean();
        addressView.type = TYPE_ETH_TOKEN;
        addressView.ethToken = ethToken;
        addressView.balance = ethToken.balance;
        return addressView;
    }

    public static ConvertViewBean eosTokenConvert(EosBalance eosBalance) {
        ConvertViewBean addressView = new ConvertViewBean();
        addressView.type = TYPE_EOS_COIN;
        addressView.eosBalance = eosBalance;
        addressView.balance = StringUtil.subZeroAndDot(eosBalance.balance);
        return addressView;
    }


    public static ConvertViewBean safeAssetConvert(SafeAsset safeAsset) {
        ConvertViewBean addressView = new ConvertViewBean();
        addressView.type = TYPE_SAFE_ASSET;
        addressView.safeAsset = safeAsset;
        addressView.balance = StringUtil.subZeroAndDot(safeAsset.assetBalance);
        return addressView;
    }

    public String getCoin() {
        if (isCurrency()) {
            return currency.coin;
        } else if (isETHToken()) {
            if (ethToken.isErc20()) {
                return ethToken.symbol;
            } else {
                return ethToken.name;
            }
        } else if (isEosBalance()) {
            return eosBalance == null ? SafeColdSettings.EOS : eosBalance.tokenName;
        } else if (isSafeAsset()) {
            return SafeColdSettings.SAFE;
        } else {
            return "";
        }
    }

    public String getName() {
        if (isCurrency()) {
            return CurrencyNameUtil.getCurrencyName(currency);
        } else if (isETHToken()) {
            return CurrencyNameUtil.getEthTokenName(ethToken);
        } else if (isEosBalance()) {
            return eosBalance == null ? SafeColdSettings.EOS : eosBalance.tokenName;
        } else if (isSafeAsset()) {
            return safeAsset.assetName;
        } else {
            return "";
        }
    }

    public String getAddress() {
        if (isCurrency()) {
            return currency.selectAddress;
        } else if (isETHToken()) {
            return ethToken.ethAddress;
        } else if (isSafeAsset()) {
            return HDAddressManager.getInstance().getCurrencyCoin(SafeColdSettings.SAFE).selectAddress;
        } else {
            return "";
        }
    }

    public boolean isCurrency() {
        return type == TYPE_CURRENCY;
    }

    public boolean isETHToken() {
        return type == TYPE_ETH_TOKEN;
    }

    public boolean isEosBalance() {
        return type == TYPE_EOS_COIN;
    }

    public boolean isSafeAsset() {
        return type == TYPE_SAFE_ASSET;
    }

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }

    @Override
    public int compareTo(@NonNull ConvertViewBean o) {
        return getCoin().compareTo(o.getCoin());
    }

}
