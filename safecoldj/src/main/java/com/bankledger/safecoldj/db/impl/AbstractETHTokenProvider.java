package com.bankledger.safecoldj.db.impl;

import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.IETHTokenProvider;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.bankledger.safecoldj.entity.ETHToken;
import com.google.common.base.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * @author bankledger
 * @time 2018/9/11 14:09
 */
public abstract class AbstractETHTokenProvider extends AbstractProvider implements IETHTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(AbstractETHTokenProvider.class);

    private ETHToken applyCursorETHToken(ICursor c) {
        ETHToken ethToken = new ETHToken();
        int idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.IS_TOKEN);
        if (idColumn != -1) {
            ethToken.isToken = c.getInt(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.ADDRESS);
        if (idColumn != -1) {
            ethToken.ethAddress = c.getString(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.BALANCE);
        if (idColumn != -1) {
            ethToken.balance = c.getString(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.TRANSACTION_COUNT);
        if (idColumn != -1) {
            ethToken.transactionCount = c.getInt(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.NAME);
        if (idColumn != -1) {
            ethToken.name = c.getString(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.SYMBOL);
        if (idColumn != -1) {
            ethToken.symbol = c.getString(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.DECIMALS);
        if (idColumn != -1) {
            ethToken.decimals = c.getInt(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.TOTAL_SUPPLY);
        if (idColumn != -1) {
            ethToken.totalSupply = c.getString(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.CONTRACT_ADDRESS);
        if (idColumn != -1) {
            ethToken.contractsAddress = c.getString(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.ETHTokenColumns.ALIAS);
        if (idColumn != -1) {
            ethToken.alias = c.getString(idColumn);
        }
        return ethToken;
    }

    @Override
    public void refreshBalance(ETHToken ethToken) {
        if (ethToken.isEth() || ethToken.isEtc()) {
            String sql = "update " + AbstractDb.Tables.ETH_TOKEN + " set " + AbstractDb.ETHTokenColumns.BALANCE + "= ?," + AbstractDb.ETHTokenColumns.TRANSACTION_COUNT + " = ? where "
                    + AbstractDb.ETHTokenColumns.ADDRESS + " = ? and " + AbstractDb.ETHTokenColumns.IS_TOKEN + " = ?";
            this.execUpdate(sql, new String[]{ethToken.balance, Integer.toString(ethToken.transactionCount), ethToken.ethAddress, Integer.toString(ethToken.isToken)});
        } else if (ethToken.isErc20()) {
            String sql = "insert or replace into " + AbstractDb.Tables.ETH_TOKEN + " (is_token,address,balance,transaction_count,name,symbol,decimals,total_supply,contract_address) values (?,?,?,?,?,?,?,?,?)";
            this.execUpdate(sql, new String[]{
                    Integer.toString(ethToken.isToken),
                    ethToken.ethAddress,
                    ethToken.balance,
                    Integer.toString(ethToken.transactionCount),
                    ethToken.name,
                    ethToken.symbol,
                    Integer.toString(ethToken.decimals),
                    ethToken.totalSupply,
                    ethToken.contractsAddress});
        }
    }

    @Override
    public void addToken(ETHToken ethToken) {
        String sql = "insert into " + AbstractDb.Tables.ETH_TOKEN + " (is_token ,address, balance, transaction_count, name, symbol, decimals, total_supply, contract_address)" +
                " values (? ,?, ?, ?, ?, ?, ?, ?, ?) where not exists (select * from " + AbstractDb.Tables.ETH_TOKEN + " where contract_address = ?)";
        this.execUpdate(sql, new String[]{
                Integer.toString(ethToken.isToken),
                ethToken.ethAddress,
                ethToken.balance,
                Integer.toString(ethToken.transactionCount),
                ethToken.name,
                ethToken.symbol,
                Integer.toString(ethToken.decimals),
                ethToken.totalSupply,
                ethToken.contractsAddress,
                ethToken.contractsAddress});
    }

    @Override
    public void addETH(ETHToken eth) {
        String sql = "insert or replace into " + AbstractDb.Tables.ETH_TOKEN + " (name,is_token, address,contract_address, balance, transaction_count,symbol,total_supply,decimals) values ( ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        this.execUpdate(sql, new String[]{
                eth.name,
                Integer.toString(eth.isToken),
                eth.ethAddress,
                eth.contractsAddress,
                eth.balance,
                Integer.toString(eth.transactionCount),
                eth.symbol,
                eth.totalSupply,
                Integer.toString(eth.decimals)}
        );
    }

    @Override
    public List<ETHToken> queryAll() {
        final List<ETHToken> ethTokenList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.ETH_TOKEN;
        this.execQueryLoop(sql, new String[]{}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                ethTokenList.add(applyCursorETHToken(c));
                return null;
            }
        });
        return ethTokenList;
    }

    @Override
    public void setETHTokenAlias(String address, String alias) {
        String sql = "update " + AbstractDb.Tables.ETH_TOKEN + " set " + AbstractDb.ETHTokenColumns.ALIAS + "= ? where " +
                AbstractDb.ETHTokenColumns.CONTRACT_ADDRESS + " = ?";
        this.execUpdate(sql, new String[]{alias, address});
    }

    @Override
    public ETHToken queryETHTokenWithContractAddress(String contractAddress) {
        final ETHToken[] ethTokens = {null};
        String sql = "select * from " + AbstractDb.Tables.ETH_TOKEN + " where " +
                AbstractDb.ETHTokenColumns.CONTRACT_ADDRESS + " = ? ";
        this.execQueryLoop(sql, new String[]{contractAddress}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                ethTokens[0] = applyCursorETHToken(c);
                return null;
            }
        });
        return ethTokens[0];
    }

    @Override
    public ETHToken queryETH() {
        final ETHToken[] ethTokens = {null};
        String sql = "select * from " + AbstractDb.Tables.ETH_TOKEN + " where " +
                AbstractDb.ETHTokenColumns.IS_TOKEN + " = ? ";
        this.execQueryLoop(sql, new String[]{Integer.toString(ETHToken.IS_ETH)}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                ethTokens[0] = applyCursorETHToken(c);
                return null;
            }
        });
        return ethTokens[0];
    }

    @Override
    public ETHToken queryETC() {
        final ETHToken[] ethTokens = {null};
        String sql = "select * from " + AbstractDb.Tables.ETH_TOKEN + " where " +
                AbstractDb.ETHTokenColumns.IS_TOKEN + " = ? ";
        this.execQueryLoop(sql, new String[]{Integer.toString(ETHToken.IS_ETC)}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                ethTokens[0] = applyCursorETHToken(c);
                return null;
            }
        });
        return ethTokens[0];
    }

    @Override
    public List<ETHToken> matchETH(String matchStr) {
        final List<ETHToken> ethTokens = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.ETH_TOKEN + " where " + AbstractDb.ETHTokenColumns.IS_TOKEN + " != ? and " +
                "(address like ? or name like ? or alias like ?)";
        this.execQueryLoop(sql, new String[]{Integer.toString(ETHToken.IS_ERC20), "%" + matchStr + "%", "%" + matchStr + "%", "%" + matchStr + "%"}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                ethTokens.add(applyCursorETHToken(c));
                return null;
            }
        });
        return ethTokens;
    }

    @Override
    public List<ETHToken> queryToken() {
        final List<ETHToken> ethTokens = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.ETH_TOKEN + " where " +
                AbstractDb.ETHTokenColumns.IS_TOKEN + " = ? ";
        this.execQueryLoop(sql, new String[]{Integer.toString(ETHToken.IS_ERC20)}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                ethTokens.add(applyCursorETHToken(c));
                return null;
            }
        });
        return ethTokens;
    }

    @Override
    public List<ETHToken> matchToken(String matchStr) {
        final List<ETHToken> ethTokens = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.ETH_TOKEN + " where " + AbstractDb.ETHTokenColumns.IS_TOKEN + " = ? and " +
                "(address like ? or symbol like ? or alias like ?)";
        this.execQueryLoop(sql, new String[]{Integer.toString(ETHToken.IS_ERC20), "%" + matchStr + "%", "%" + matchStr + "%", "%" + matchStr + "%"}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                ethTokens.add(applyCursorETHToken(c));
                return null;
            }
        });
        return ethTokens;
    }

    @Override
    public void removeToken(ETHToken ethToken) {
        this.execUpdate("delete from " + AbstractDb.Tables.ETH_TOKEN + " where " + AbstractDb.ETHTokenColumns.CONTRACT_ADDRESS + " = ? and " + AbstractDb.ETHTokenColumns.IS_TOKEN + " = ?", new String[]{ ethToken.contractsAddress, Integer.toString(ETHToken.IS_ERC20)});
    }
}
