package com.bankledger.safecoldj.db;

import com.bankledger.safecoldj.core.EosUsdtBalance;

import java.util.List;

/**
 * Created by zm on 2018/11/13.
 */
public interface IEosUsdtBalanceProvider {

    void addBalance(EosUsdtBalance balance);

    void updateBalance(EosUsdtBalance balance);

    void updateUsdtBalance(String balance);

    void addBalanceList(List<EosUsdtBalance> balanceList);

    EosUsdtBalance getEosBalance(String tokenName);

    EosUsdtBalance getEosBalance();

    boolean checkBalanceExist(String tokenName, EosUsdtBalance.CoinType coinType);

    List<EosUsdtBalance> getBalanceList();

    List<EosUsdtBalance> getEosBalanceList();

    String getUsdtBalance();

    void deleteBalance(String tokenName, EosUsdtBalance.CoinType coinType);

}
