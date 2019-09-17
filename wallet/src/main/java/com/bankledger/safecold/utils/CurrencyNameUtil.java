package com.bankledger.safecold.utils;

import com.bankledger.protobuf.bean.EosBalance;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.entity.ETHToken;

/**
 * @author bankledger
 * @time 2018/11/21 19:28
 */
public class CurrencyNameUtil {

    public static String getCurrencyName(Currency currency) {
        if (currency.isUsdt()) {
            return SafeColdApplication.mContext.getString(R.string.usdt_btc_series);
        } else if (SafeColdSettings.BCH.equalsIgnoreCase(currency.coin)) {
            return "BCHABC";
        } else if (SafeColdSettings.BSV.equalsIgnoreCase(currency.coin)) {
            return "BCHSV";
        } else {
            return currency.coin;
        }
    }

    public static String getCurrencyName(HDAddress hdAddress) {
        if (SafeColdSettings.USDT.equalsIgnoreCase(hdAddress.getCoin())) {
            return SafeColdApplication.mContext.getString(R.string.usdt_btc_series);
        } else if (SafeColdSettings.BCH.equalsIgnoreCase(hdAddress.getCoin())) {
            return "BCHABC";
        } else if (SafeColdSettings.BSV.equalsIgnoreCase(hdAddress.getCoin())) {
            return "BCHSV";
        } else {
            return hdAddress.getCoin();
        }
    }

    public static String getEthTokenName(ETHToken ethToken) {
        if (ethToken.isErc20()) {
            if (ethToken.isUsdt()) { //ETH USDT
                return SafeColdApplication.mContext.getString(R.string.usdt_eth_series);
            } else {
                return ethToken.symbol;
            }
        } else {
            return ethToken.name;
        }
    }

    public static String getContactsCoinName(ContactsAddress contactsAddress) {
        if (contactsAddress.isCurrencyAddress() && SafeColdSettings.USDT.equalsIgnoreCase(contactsAddress.getCoin())) {
            return SafeColdApplication.mContext.getString(R.string.usdt_btc_series);
        } else if (contactsAddress.isCurrencyAddress() && SafeColdSettings.BCH.equalsIgnoreCase(contactsAddress.getCoin())) {
            return "BCHABC";
        }  else if (contactsAddress.isCurrencyAddress() && SafeColdSettings.BSV.equalsIgnoreCase(contactsAddress.getCoin())) {
            return "BCHSV";
        } else if (contactsAddress.isTokenAddress() && ETHToken.USDT_CONTRACTSADDRESS.equalsIgnoreCase(contactsAddress.getContractAddress())) {
            return SafeColdApplication.mContext.getString(R.string.usdt_eth_series);
        } else {
            return contactsAddress.getCoin();
        }
    }

}
