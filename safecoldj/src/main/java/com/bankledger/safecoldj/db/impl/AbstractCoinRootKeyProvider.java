package com.bankledger.safecoldj.db.impl;

import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.ICoinRootKeyProvider;
import com.bankledger.safecoldj.db.impl.base.ICursor;
import com.bankledger.safecoldj.db.impl.base.IDb;
import com.bankledger.safecoldj.exception.AddressFormatException;
import com.bankledger.safecoldj.utils.Base58;
import com.google.common.base.Function;

import javax.annotation.Nullable;
/**
 * $desc
 *
 * @author bankledger
 * @time 2018/9/8 14:33
 */
public abstract class AbstractCoinRootKeyProvider extends AbstractProvider implements ICoinRootKeyProvider {

    @Override
    public void addRootKey(String coin, byte[] rootKey) {
        IDb db = getWriteDb();
        db.beginTransaction();
        this.insertCoinRootKeyToDb(db, coin, Base58.encode(rootKey));
        db.endTransaction();
    }

    @Override
    public byte[] getRootKey(String coin) {
        final byte[][] rootKey = {null};
        String sql = "select root_key from coin_root_key where coin = ?";
        this.execQueryLoop(sql, new String[]{coin}, new Function<ICursor, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable ICursor c) {
                int idColumn = c.getColumnIndex(AbstractDb.CoinRootKeyColumns.ROOT_KEY);
                if (idColumn != -1) {
                    String pubStr = c.getString(idColumn);
                    try {
                        rootKey[0] = Base58.decode(pubStr);
                    } catch (AddressFormatException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        });
        return rootKey[0];
    }

    protected abstract int insertCoinRootKeyToDb(IDb db, String coin, String rootKey);
}
