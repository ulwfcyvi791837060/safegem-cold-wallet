package com.bankledger.safecoldj.db.impl;

import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosUsdtBalance;
import com.bankledger.safecoldj.core.HDAddress;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.IEosUsdtBalanceProvider;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.bankledger.safecoldj.db.impl.base.IDb;
import com.google.common.base.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Created by zm on 2018/11/13.
 */

public abstract class AbstractEosUsdtBalanceProvider extends AbstractProvider implements IEosUsdtBalanceProvider {

    public static final Logger log = LoggerFactory.getLogger(AbstractEosUsdtBalanceProvider.class);

    public static EosUsdtBalance applyCursorEosBalance(ICursor c) {
        EosUsdtBalance item = new EosUsdtBalance();
        int idColumn = c.getColumnIndex(AbstractDb.EosUsdtBalanceColumns.BALANCE);
        if (idColumn != -1) {
            item.balance = c.getString(idColumn);
        }

        idColumn = c.getColumnIndex(AbstractDb.EosUsdtBalanceColumns.TOKEN_NAME);
        if (idColumn != -1) {
            item.tokenName = c.getString(idColumn);
        }
        idColumn = c.getColumnIndex(AbstractDb.EosUsdtBalanceColumns.COIN_TYPE);
        if (idColumn != -1) {
            int state = c.getInt(idColumn);
            if (state == EosUsdtBalance.CoinType.TYPE_EOS.getValue()) {
                item.setCoinType(EosUsdtBalance.CoinType.TYPE_EOS);
            } else {
                item.setCoinType(EosUsdtBalance.CoinType.TYPE_USDT);
            }
        }
        return item;
    }

    @Override
    public void addBalance(EosUsdtBalance balance) {
        String sql = "insert into " + AbstractDb.Tables.EOS_UDST_BALANCE + " (balance, token_name, coin_type) values (?, ?, ?)";
        this.execUpdate(sql, new String[]{balance.balance, balance.tokenName, Integer.toString(balance.getCoinType().getValue())});
    }

    @Override
    public void updateBalance(EosUsdtBalance balance) {
        String sql = "update " + AbstractDb.Tables.EOS_UDST_BALANCE + " set balance = ? where token_name = ? and coin_type = ?";
        this.execUpdate(sql, new String[]{balance.balance, balance.tokenName, Integer.toString(balance.getCoinType().getValue())});
    }

    @Override
    public void updateUsdtBalance(String balance) {
        EosUsdtBalance usdt = new EosUsdtBalance();
        usdt.balance = balance;
        usdt.tokenName = SafeColdSettings.USDT;
        usdt.setCoinType(EosUsdtBalance.CoinType.TYPE_USDT);
        updateBalance(usdt);
    }

    @Override
    public void addBalanceList(List<EosUsdtBalance> balanceList) {
        for (EosUsdtBalance item : balanceList) {
            addBalance(item);
        }
    }


    @Override
    public boolean checkBalanceExist(String tokenName, EosUsdtBalance.CoinType coinType) {
        final List<EosUsdtBalance> balanceList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.EOS_UDST_BALANCE + " where token_name = ? and coin_type = ?";
        this.execQueryLoop(sql, new String[]{tokenName, Integer.toString(coinType.getValue())}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                balanceList.add(applyCursorEosBalance(c));
                return null;
            }
        });
        return balanceList.size() > 0;
    }

    @Override
    public List<EosUsdtBalance> getBalanceList() {
        final List<EosUsdtBalance> balanceList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.EOS_UDST_BALANCE;
        this.execQueryLoop(sql, new String[]{}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                balanceList.add(applyCursorEosBalance(c));
                return null;
            }
        });
        return balanceList;
    }

    @Override
    public List<EosUsdtBalance> getEosBalanceList() {
        final List<EosUsdtBalance> balanceList = new ArrayList<>();
        String sql = "select * from " + AbstractDb.Tables.EOS_UDST_BALANCE + " where coin_type = ?";
        this.execQueryLoop(sql, new String[]{Integer.toString(EosUsdtBalance.CoinType.TYPE_EOS.getValue())}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                balanceList.add(applyCursorEosBalance(c));
                return null;
            }
        });
        return balanceList;
    }

    @Override
    public EosUsdtBalance getEosBalance() {
        return getBalance(SafeColdSettings.EOS, EosUsdtBalance.CoinType.TYPE_EOS);
    }

    @Override
    public EosUsdtBalance getEosBalance(String tokenName) {
        return getBalance(tokenName, EosUsdtBalance.CoinType.TYPE_EOS);
    }

    @Override
    public String getUsdtBalance() {
        return getBalance(SafeColdSettings.USDT, EosUsdtBalance.CoinType.TYPE_USDT).balance;
    }

    private EosUsdtBalance getBalance(String tokenName, EosUsdtBalance.CoinType type) {
        final EosUsdtBalance[] balances = {null};
        String sql = "select * from " + AbstractDb.Tables.EOS_UDST_BALANCE + " where token_name = ? and coin_type = ?";
        this.execQueryOneRecord(sql, new String[]{tokenName, Integer.toString(type.getValue())}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                balances[0] = applyCursorEosBalance(c);
                return null;
            }
        });
        return balances[0];
    }

    @Override
    public void deleteBalance(String tokenName, EosUsdtBalance.CoinType coinType) {
        IDb db = getWriteDb();
        db.beginTransaction();
        this.execUpdate(db, "delete from " + AbstractDb.Tables.EOS_UDST_BALANCE + " where token_name = ? and coin_type = ?", new String[]{tokenName, Integer.toString(coinType.getValue())});
        db.endTransaction();
    }

}
