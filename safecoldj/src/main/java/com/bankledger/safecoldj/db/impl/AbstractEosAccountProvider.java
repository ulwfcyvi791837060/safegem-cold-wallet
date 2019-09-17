package com.bankledger.safecoldj.db.impl;

import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.EosAccount;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.IEosAccountProvider;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.bankledger.safecoldj.entity.ContactsAddress;
import com.bankledger.safecoldj.entity.ETHToken;
import com.google.common.base.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Created by zm on 2018/11/13.
 */

public abstract class AbstractEosAccountProvider extends AbstractProvider implements IEosAccountProvider {

    public static final Logger log = LoggerFactory.getLogger(AbstractEosAccountProvider.class);

    public static EosAccount applyCursorEosAccount(ICursor c) {
        EosAccount item = new EosAccount();

        int idColumn = c.getColumnIndex(AbstractDb.EosAccountColumns.OWNER_PUB_KEY);
        if (idColumn != -1) {
            item.setOwnerPubKey(c.getString(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.EosAccountColumns.ACTIVE_PUB_KEY);
        if (idColumn != -1) {
            item.setActivePubKey(c.getString(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.EosAccountColumns.ACCOUNT_NAME);
        if (idColumn != -1) {
            item.setAccountName(c.getString(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.EosAccountColumns.ACTIVE_PRIV_KEY);
        if (idColumn != -1) {
            item.setActivePrivKey(c.getString(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.EosAccountColumns.OWNER_PRIV_KEY);
        if (idColumn != -1) {
            item.setOwnerPrivKey(c.getString(idColumn));
        }

        idColumn = c.getColumnIndex(AbstractDb.EosAccountColumns.OP_TYPE);
        if (idColumn != -1) {
            int state = c.getInt(idColumn);
            if (state == EosAccount.OpType.TYPE_CREATE.getValue()) {
                item.setOpType(EosAccount.OpType.TYPE_CREATE);
            } else if (state == EosAccount.OpType.TYPE_IMPORT.getValue()) {
                item.setOpType(EosAccount.OpType.TYPE_IMPORT);
            }
        }

        idColumn = c.getColumnIndex(AbstractDb.EosAccountColumns.AVAILABLE_STATE);
        if (idColumn != -1) {
            int state = c.getInt(idColumn);
            if (state == EosAccount.AvailableState.STATE_AVAILABLE.getValue()) {
                item.setState(EosAccount.AvailableState.STATE_AVAILABLE);
            } else if (state == EosAccount.AvailableState.STATE_DISABLED.getValue()) {
                item.setState(EosAccount.AvailableState.STATE_DISABLED);
            }
        }

        return item;
    }

    @Override
    public void addEosAccount(EosAccount account) {
        String sql = "insert into " + AbstractDb.Tables.EOS_ACCOUNT + " (owner_pub_key, active_pub_key, account_name, owner_priv_key, active_priv_key, op_type, available_state) values (?, ?, ?, ?, ?, ?, ?)";
        this.execUpdate(sql, new String[]{
                account.getOwnerPubKey(),
                account.getActivePubKey(),
                account.getAccountName(),
                account.getOwnerPrivKey(),
                account.getActivePrivKey(),
                Integer.toString(account.getOpType().getValue()),
                Integer.toString(account.getState().getValue())}
        );
    }

    public void addOrUpdateEosAccount(EosAccount account) {
        EosAccount eosAccount = queryEosAccount(account.getOpType());
        if (eosAccount != null) {
            String sql = "update " + AbstractDb.Tables.EOS_ACCOUNT + " set owner_pub_key = ?, active_pub_key = ?, account_name = ?, owner_priv_key = ?, active_priv_key = ?, available_state = ? where op_type = ?";
            this.execUpdate(sql, new String[]{account.getOwnerPubKey(), account.getActivePubKey(), account.getAccountName(), account.getOwnerPrivKey(), account.getActivePrivKey(), Integer.toString(account.getState().getValue()), Integer.toString(account.getOpType().getValue())});
        } else {
            addEosAccount(account);
        }
    }

    @Override
    public void updateEosAccountAvailable(EosAccount account) {
        String sql = "update " + AbstractDb.Tables.EOS_ACCOUNT + " set account_name = ?, available_state = ? where owner_pub_key = ? and active_pub_key = ?";
        this.execUpdate(sql, new String[]{account.getAccountName(), Integer.toString(account.getState().getValue()), account.getOwnerPubKey(), account.getActivePubKey()});
    }

    @Override
    public boolean isExistPubkey(String ownerPubKey, String activePubKey) {
        final int[] count = {0};
        String sql = "select ifnull(count(*),0) from " + AbstractDb.Tables.EOS_ACCOUNT + " where owner_pub_key = ? and active_pub_key = ?";
        this.execQueryOneRecord(sql, new String[]{ownerPubKey, activePubKey}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                count[0] = c.getInt(0);
                return null;
            }
        });
        return count[0] > 0;
    }

    public EosAccount queryEosAccount(EosAccount.OpType opType) {
        final EosAccount[] eosAccounts = {null};
        String sql = "select * from " + AbstractDb.Tables.EOS_ACCOUNT + " where op_type = ? limit 1";
        this.execQueryOneRecord(sql, new String[]{Integer.toString(opType.getValue())}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                eosAccounts[0] = applyCursorEosAccount(c);
                return null;
            }
        });
        return eosAccounts[0];
    }

    @Override
    public EosAccount queryCreateEosAccount() {
        final EosAccount[] eosAccounts = {null};
        String sql = "select * from " + AbstractDb.Tables.EOS_ACCOUNT + " where op_type = ? limit 1";
        this.execQueryOneRecord(sql, new String[]{Integer.toString(EosAccount.OpType.TYPE_CREATE.getValue())}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                eosAccounts[0] = applyCursorEosAccount(c);
                return null;
            }
        });
        return eosAccounts[0];
    }

    @Override
    public EosAccount queryAvailableEosAccount() {
        final EosAccount[] eosAccounts = {null};
        String sql = "select * from " + AbstractDb.Tables.EOS_ACCOUNT + " where available_state = ? limit 1";
        this.execQueryOneRecord(sql, new String[]{Integer.toString(EosAccount.AvailableState.STATE_AVAILABLE.getValue())}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                eosAccounts[0] = applyCursorEosAccount(c);
                return null;
            }
        });
        return eosAccounts[0];
    }

    @Override
    public EosAccount matchEos(String matchStr) {
        EosAccount account = queryAvailableEosAccount();
        if (matchStr != null && account != null && (SafeColdSettings.EOS.contains(matchStr.toUpperCase()) || account.getAccountName().toUpperCase().contains(matchStr.toUpperCase()))) {
            return account;
        } else {
            return null;
        }
    }
}
