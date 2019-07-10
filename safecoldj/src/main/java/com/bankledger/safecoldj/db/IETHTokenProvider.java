package com.bankledger.safecoldj.db;

import com.bankledger.safecoldj.entity.ETHToken;

import java.util.List;

/**
 * @author bankledger
 * @time 2018/9/11 14:00
 */
public interface IETHTokenProvider {

    void refreshBalance(ETHToken ethToken);

    void addToken(ETHToken ethToken);

    void addETH(ETHToken eth);

    List<ETHToken> queryAll();

    void setETHTokenAlias(String address, String alias);

    ETHToken queryETHTokenWithContractAddress(String contactsAddress);

    ETHToken queryETH();

    ETHToken queryETC();

    List<ETHToken> matchETH(String matchStr);

    List<ETHToken> queryToken();

    List<ETHToken> matchToken(String matchStr);

    void removeToken(ETHToken ethToken);
}
