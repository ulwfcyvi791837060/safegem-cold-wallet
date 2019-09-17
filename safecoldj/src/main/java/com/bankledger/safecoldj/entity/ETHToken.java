package com.bankledger.safecoldj.entity;

import com.bankledger.protobuf.bean.EthToken;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.utils.GsonUtils;

/**
 * @author bankledger
 * @time 2018/9/11 14:10
 */
public class ETHToken extends EthToken {

    public static final int IS_ETH = 0;
    public static final int IS_ERC20 = 1;
    public static final int IS_ETC = 2;

    public static final String USDT_CONTRACTSADDRESS = "0xdac17f958d2ee523a2206206994597c13d831ec7";

    public String alias;

    public ETHToken() {
    }

    public ETHToken(EthToken ethToken) {
        super(ethToken);
        this.contractsAddress = ethToken.contractsAddress.toLowerCase();
    }

    public ETHToken(int isToken, String ethAddress, String balance, int transactionCount, String name, String symbol, int decimals, String totalSupply, String contractsAddress, String alias) {
        this.isToken = isToken;
        this.ethAddress = ethAddress;
        this.balance = balance;
        this.transactionCount = transactionCount;
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.totalSupply = totalSupply;
        this.contractsAddress = contractsAddress;
        this.alias = alias;
    }

    public static ETHToken newEth(String ethAddress) {
        ETHToken ethToken = new ETHToken();
        ethToken.isToken = IS_ETH;
        ethToken.ethAddress = ethAddress;
        ethToken.contractsAddress = ethAddress;
        ethToken.balance = "0";
        ethToken.transactionCount = 0;
        ethToken.decimals = 18;
        ethToken.symbol = "ether";
        ethToken.name = SafeColdSettings.ETH;
        return ethToken;
    }

    public static ETHToken newEtc(String ethAddress) {
        ETHToken ethToken = new ETHToken();
        ethToken.isToken = IS_ETC;
        ethToken.ethAddress = ethAddress;
        ethToken.contractsAddress = ethAddress;
        ethToken.balance = "0";
        ethToken.transactionCount = 0;
        ethToken.decimals = 18;
        ethToken.symbol = "etc";
        ethToken.name = SafeColdSettings.ETC;
        return ethToken;
    }

    public static ETHToken activeToken(String ethAddress, String name, String contractsAddress, String symbol, int decimals, String totalSupply) {
        ETHToken ethToken = new ETHToken();
        ethToken.isToken = IS_ERC20;
        ethToken.ethAddress = ethAddress;
        ethToken.balance = "0";
        ethToken.name = name;
        ethToken.contractsAddress = contractsAddress.toLowerCase();
        ethToken.symbol = symbol;
        ethToken.decimals = decimals;
        ethToken.totalSupply = totalSupply;
        return ethToken;
    }

    public boolean isEth() {
        return isToken == IS_ETH;
    }

    public boolean isEtc() {
        return isToken == IS_ETC;
    }

    public boolean isErc20() {
        return isToken == IS_ERC20;
    }


    public boolean isUsdt() {
        return contractsAddress.equalsIgnoreCase(ETHToken.USDT_CONTRACTSADDRESS);
    }

    @Override
    public String toString() {
        return GsonUtils.toString(this);
    }
}
