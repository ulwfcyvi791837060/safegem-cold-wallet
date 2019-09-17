package com.bankledger.safecold.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;

import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecold.db.base.AndroidDb;
import com.bankledger.safecoldj.db.AbstractDb;
import com.bankledger.safecoldj.db.impl.AbstractCoinRootKeyProvider;
import com.bankledger.safecoldj.db.impl.base.IDb;

/**
 * @author bankledger
 * @time 2018/9/8 14:34
 */
public class CoinRootKeyProvider extends AbstractCoinRootKeyProvider {
    private final SQLiteOpenHelper helper;

    private static CoinRootKeyProvider coinRootKeyProvider = new CoinRootKeyProvider(SafeColdApplication.dbHelper);

    public static CoinRootKeyProvider getInstance() {
        return coinRootKeyProvider;
    }

    public CoinRootKeyProvider(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    @Override
    public IDb getReadDb() {
        return new AndroidDb(this.helper.getReadableDatabase());
    }

    @Override
    public IDb getWriteDb() {
        return new AndroidDb(this.helper.getWritableDatabase());
    }



    @Override
    protected int insertCoinRootKeyToDb(IDb db, String coin, String rootKey) {
        AndroidDb mdb = (AndroidDb) db;
        ContentValues cv = new ContentValues();
        cv.put(AbstractDb.CoinRootKeyColumns.COIN, coin);
        cv.put(AbstractDb.CoinRootKeyColumns.ROOT_KEY, rootKey);
        return (int) mdb.getSQLiteDatabase().insert(AbstractDb.Tables.COIN_ROOT_KEY, null, cv);
    }
}
