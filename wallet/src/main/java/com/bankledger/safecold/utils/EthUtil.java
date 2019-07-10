package com.bankledger.safecold.utils;


import com.bankledger.safecoldj.entity.ETHToken;

import org.web3j.utils.Convert;

import java.math.BigDecimal;

public class EthUtil {

    public static String formatAmountUnit(ETHToken mETHToken) {
        if (mETHToken.isErc20()) {
            BigDecimal balance = new BigDecimal(mETHToken.balance);
            balance = balance.divide(BigDecimal.TEN.pow(mETHToken.decimals));
            BigDecimal decimal = Convert.fromWei(balance, Convert.Unit.ETHER);
            return decimal.toPlainString() + " " + mETHToken.symbol;
        } else {
            BigDecimal decimal = Convert.fromWei(new BigDecimal(mETHToken.balance), Convert.Unit.ETHER);
            return decimal.toPlainString() + " " + mETHToken.symbol;
        }
    }

    public static String formatAmount(ETHToken mETHToken) {
        if (mETHToken.isErc20()) {
            BigDecimal balance = new BigDecimal(mETHToken.balance);
            balance = balance.divide(BigDecimal.TEN.pow(mETHToken.decimals));
            return balance.toPlainString();
        } else {
            BigDecimal decimal = Convert.fromWei(new BigDecimal(mETHToken.balance), Convert.Unit.ETHER);
            return decimal.toPlainString();
        }
    }

}
